package com.aptech.aptechMall.service.authentication;

import com.aptech.aptechMall.Exception.*;

import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.requests.*;
import com.aptech.aptechMall.service.FileUploadService;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisService redisService;
    private final FileUploadService fileUploadService;

    public ProfileResponse getProfile(HttpServletRequest request, HttpServletResponse response){
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        try {
            String subject = jwtService.extractUsername(authHeader.replace("Bearer ", ""));
            boolean hasUsername = userRepository.existsByUsername(subject);

            User user = hasUsername ? userRepository.findByUsername(subject).orElseThrow() :
                    userRepository.findByEmail(subject).orElseThrow(() -> new UsernameNotFoundException("Can't retrieve Profile because User is null"));

            return ProfileResponse.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .emailVerified(user.isEmailVerified())
                    .phone(user.getPhone())
                    .avatarUrl(user.getAvatarUrl())
                    .fullName(user.getFullName())
                    .registeredAt(user.getRegisteredAt())
                    .lastLogin(user.getLastLogin())
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error in getProfile: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    public ProfileResponse updateProfile(HttpServletRequest request, HttpServletResponse response, UpdateProfile updatedProfile){
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        try {
            String subject = jwtService.extractUsername(authHeader.substring(7));
            boolean hasUsername = userRepository.existsByUsername(subject);
            String avatarFilePath = updatedProfile.getAvatar() != null ? fileUploadService.saveAvatar(updatedProfile.getAvatar()) : updatedProfile.getAvatarUrl();

            User user = hasUsername ? userRepository.findByUsername(subject).orElseThrow() :
                    userRepository.findByEmail(subject).orElseThrow(() -> new UsernameNotFoundException("Can't retrieve Profile because User is null"));
            user.setFullName(updatedProfile.getFullName());
            user.setPhone(updatedProfile.getPhone());
            user.setUsername(updatedProfile.getUsername());
            user.setAvatarUrl(avatarFilePath);

            userRepository.save(user);

            return ProfileResponse.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .emailVerified(user.isEmailVerified())
                    .phone(user.getPhone())
                    .avatarUrl(user.getAvatarUrl())
                    .fullName(user.getFullName())
                    .registeredAt(user.getRegisteredAt())
                    .lastLogin(user.getLastLogin())
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error in getProfile: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
        }

        Role role = Role.CUSTOMER;

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .email(request.getEmail())
                .build();

        userRepository.save(user);

        return new RegisterResponse("Successfully registered the user " + user.getUsername());
    }

    public AuthResponse authenticate(AuthRequest request, HttpServletResponse response) {
        boolean existUsername = userRepository.existsByUsername(request.getUsername());
        User user = existUsername ?
                userRepository.findByUsername(request.getUsername())
                        .orElseThrow(() -> new UsernameNotFoundException("User Does not exists")) :
                userRepository.findByEmail(request.getUsername())
                        .orElseThrow(() -> new UsernameNotFoundException("User Does not exists"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())
                || request.getPassword().isEmpty()) {
            throw new BadCredentialsException("Incorrect login info");
        }

        switch (user.getStatus()){
            case SUSPENDED -> throw new AccountSuspendedException("Your account has been suspended. Please contact support.");
            case DELETED -> throw new AccountDeletedException("This account were marked as removed and may no longer be able to login");
            case ACTIVE -> {
                if (passwordEncoder.matches("", user.getPassword())) throw new BadCredentialsException("You can only login through third party OAuth");

                String accessJwt = jwtService.generateToken(user.getUsername(), "access_token");
                String refreshJwt = jwtService.generateToken(user.getUsername(), "refresh_token");

                redisService.saveToken(user.getEmail(), refreshJwt, JwtService.REFRESH_TOKEN_TTL.getSeconds());

                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                log.info("User " + user.getUsername() + " has logged in at " + user.getLastLogin());
                return new AuthResponse(accessJwt);
            }
            default -> throw new AccountNotActiveException("Account is not active or does not have a valid status identifiers. Please contact support.");
        }
    }

    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader("Authorization").substring(7);
        String email = jwtService.extractEmail(accessToken);

        // Check token blacklist if Redis is available
        try {
            if(redisService.getToken(email) != null)
                throw new EntityExistsException("Refresh Token haven't yet been invalidated or expired");
        } catch (Exception e) {
            log.warn("Redis not available for token blacklist check: " + e.getMessage());
        }

        String currentRefreshToken = redisService.getToken(email);


        if(currentRefreshToken != null) {
            revokeToken(currentRefreshToken);
            if(jwtService.validateToken(currentRefreshToken)) {
                var username = jwtService.extractUsername(currentRefreshToken);
                String jwt = jwtService.generateToken(username, "access_token");
                String newRefreshToken = jwtService.generateToken(username, "refresh_token");

                if (redisService.deleteToken(email)) {
                    redisService.saveToken(email, newRefreshToken, JwtService.REFRESH_TOKEN_TTL.getSeconds());
                }

                return new AuthResponse(jwt);
            }
        }
        return null;
    }

    public void preRegister(RegisterRequest request) {
        Role role = Role.fromString(request.getRole());

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(role)
                .build();
        user.setEmailVerified(true);

        userRepository.save(user);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String accessToken = request.getHeader("Authorization").substring(7);
            String email = jwtService.extractEmail(accessToken);

            revokeToken(redisService.getToken(email));
            revokeToken(accessToken);
            redisService.deleteToken(email);

        } catch (Exception e) {
            log.error("Logout Failure: " + e.getMessage());
        }
    }

    private void revokeToken(String token) {
        try {
            var expirationTime = (jwtService.extractExpiration(token).getTime() - System.currentTimeMillis()) / 1000;
            redisService.setToken(token, "blacklisted", expirationTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis not available for token revocation: " + e.getMessage());
            // Token won't be blacklisted, but app will continue to work
        }
    }

    public boolean verifyUserExists(String username){
        return userRepository.existsByUsername(username);
    }
    public boolean verifyEmailExists(String email){
        return userRepository.existsByEmail(email);
    }

    public AuthResponse authenticateGoogle(AuthRequest request){
        User user; String accessJwt;

        user = userRepository.findByOAuthEmail(request.getEmail(), request.getGoogleSub()).orElseGet(() -> {
            Map<String, Object> oAuthGoogle = new HashMap<>();
            oAuthGoogle.put("provider", "google");
            oAuthGoogle.put("sub", request.getGoogleSub());
            oAuthGoogle.put("email", request.getEmail());
            oAuthGoogle.put("verified", true);

            User newUser = new User();
            newUser.setEmail(request.getEmail());
            newUser.setFullName(request.getFullName());
            newUser.setUsername(request.getUsername()); //Let frontend set username during registration before request hit backend
            newUser.setPassword(passwordEncoder.encode("")); // No blank login from authenticate() should be possible
            newUser.setEmailVerified(true);
            newUser.setOAuth(oAuthGoogle);
            return userRepository.save(newUser);
        });

        switch (user.getStatus()){
            case SUSPENDED -> throw new AccountSuspendedException("Your account has been suspended. Please contact support.");
            case DELETED -> throw new AccountDeletedException("This account were marked as removed and may no longer be able to login");
            case ACTIVE -> {
                Map<String, Object> oauth = user.getOAuth();
                boolean isVerified = (Boolean) oauth.getOrDefault("verified", false);
                if (isVerified){
                    accessJwt = jwtService.generateToken(user, "access_token");
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    log.info("Google Auth successfully authenticated for " + request.getEmail());
                } else accessJwt = "";
                return new AuthResponse(accessJwt);
            }
            default -> throw new AccountNotActiveException("Account is not active or does not have a valid status identifiers. Please contact support.");
        }
    }

    public void generateRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response){
        String accessToken = request.getHeader("Authorization");
        try {
            String subject = jwtService.extractEmail(accessToken.substring(7));
            String refreshJwt = jwtService.generateToken(subject, "refresh_token");

            redisService.saveToken(subject, refreshJwt, JwtService.REFRESH_TOKEN_TTL.getSeconds());

        } catch (Exception e) {
            log.error("Error extracting subject from JWT: " + e.getMessage());
        }
    }

    public void updateEmailOrPassword(HttpServletRequest request, HttpServletResponse response, UpdateCredential credential){
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        try {
            User user = userRepository.findByEmail(credential.getOldEmail()).orElseThrow(() -> new UsernameNotFoundException("Email not in Database"));
            boolean existUsername = userRepository.existsByUsername(user.getUsername());

            if (!credential.getPassword().isEmpty()){
                if (passwordEncoder.matches("", user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(credential.getPassword()));
                } else {
                    if (!passwordEncoder.matches(credential.getOldPassword(), user.getPassword())){
                        throw new BadCredentialsException("Old Password does not match");
                    }
                    user.setPassword(passwordEncoder.encode(credential.getPassword()));
                }
            }

            if(!credential.getEmail().equals(credential.getOldEmail())){
                user.setEmail(credential.getEmail());
                String currentRefreshToken = redisService.getToken(credential.getOldEmail());

                String refreshJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "refresh_token");

                revokeToken(currentRefreshToken);
                redisService.deleteToken(credential.getOldEmail());
                redisService.saveToken(credential.getEmail(), refreshJwt, JwtService.REFRESH_TOKEN_TTL.getSeconds());
            }
            userRepository.save(user);

        } catch (Exception e) {
            log.error("Error extracting subject from JWT: " + e.getMessage());
        }
    }
}
