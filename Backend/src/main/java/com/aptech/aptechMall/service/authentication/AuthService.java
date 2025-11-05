package com.aptech.aptechMall.service.authentication;

import com.aptech.aptechMall.Exception.UsernameAlreadyTaken;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.requests.*;
import com.aptech.aptechMall.service.FileUploadService;
import io.jsonwebtoken.*;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
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
                    .role(user.getRole())
                    .registeredAt(user.getRegisteredAt())
                    .lastLogin(user.getLastLogin())
                    .build();

        } catch (ExpiredJwtException e) {
            System.err.println("JWT expired: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (JwtException e) {
            System.err.println("Invalid JWT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (UsernameNotFoundException e) {
            System.err.println("User not found: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error in getProfile: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    public ProfileResponse updateProfile(HttpServletRequest request, HttpServletResponse response, UpdateProfile updatedProfile, MultipartFile avatar){
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        try {
            String subject = jwtService.extractUsername(authHeader.substring(7));
            boolean hasUsername = userRepository.existsByUsername(subject);
            String avatarFilePath = avatar != null ? fileUploadService.saveAvatar(avatar) : updatedProfile.getAvatarUrl();

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
                    .role(user.getRole())
                    .registeredAt(user.getRegisteredAt())
                    .lastLogin(user.getLastLogin())
                    .build();

        } catch (ExpiredJwtException e) {
            System.err.println("JWT expired: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (JwtException e) {
            System.err.println("Invalid JWT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (UsernameNotFoundException e) {
            System.err.println("User not found: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error in getProfile: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
        }

        // Set default role to CUSTOMER if not provided or use provided role
        Role role = (request.getRole() == null || request.getRole().trim().isEmpty())
                ? Role.CUSTOMER
                : Role.fromString(request.getRole());

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
                    .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại")) :
                userRepository.findByEmail(request.getUsername())
                        .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()) || request.getPassword().isEmpty()) {
            throw new BadCredentialsException("Thông tin đăng nhập không hợp lệ");
        }

        String accessJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "access_token");
        String refreshJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "refresh_token");
        Cookie refreshTokenCookie = getRefreshTokenCookie(refreshJwt);
        setCookieAttribute(response, refreshTokenCookie);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return new AuthResponse(accessJwt);
    }

    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        var refreshTokenCookie = Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals("refresh_token")).findFirst().orElseThrow();
        var currentRefreshToken = refreshTokenCookie.getValue();

        // Check token blacklist if Redis is available
        try {
            if(redisService.hasToken(currentRefreshToken))
                throw new EntityExistsException("Refresh Token haven't yet been invalidated or expired");
        } catch (Exception e) {
            log.warn("Redis not available for token blacklist check: " + e.getMessage());
        }

        revokeToken(currentRefreshToken);
        if(currentRefreshToken != null) {
            if(jwtService.validateToken(currentRefreshToken)) {
                var username = jwtService.extractUsername(currentRefreshToken);
                String jwt = jwtService.generateToken(username, "access_token");
                String newRefreshToken = jwtService.generateToken(username, "refresh_token");
                refreshTokenCookie = getRefreshTokenCookie(newRefreshToken);
                setCookieAttribute(response, refreshTokenCookie);
                return new AuthResponse(jwt);
            }
        }
        return null;
    }

    private Cookie getRefreshTokenCookie(String refreshToken){
        var cookieMaxAge = (int) (jwtService.extractExpiration(refreshToken).getTime() - System.currentTimeMillis()) / 1000;
        if(cookieMaxAge < 0)
            cookieMaxAge = 0;

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setMaxAge(cookieMaxAge);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");

        return refreshTokenCookie;
    }

    private void setCookieAttribute(HttpServletResponse response, Cookie refreshTokenCookie){
        response.addCookie(refreshTokenCookie); // add cookie vao React frontend
        String sameSiteAttribute = "; SameSite=None";
        String header = response.getHeader("Set-Cookie");
        if (header != null && !header.contains("SameSite=")) {
            response.setHeader("Set-Cookie", header + sameSiteAttribute);
        }
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

        userRepository.save(user);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader("Authorization");
        Cookie refreshTokenCookie = Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals("refresh_token")).findFirst().orElseThrow();
        revokeToken(accessToken.substring(7));
        revokeToken(refreshTokenCookie.getValue());
        revokeRefreshTokenCookie(response, refreshTokenCookie);
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

    private void revokeRefreshTokenCookie(HttpServletResponse response, Cookie refreshTokenCookie) {
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        setCookieAttribute(response, refreshTokenCookie);
    }

    public boolean verifyUserExists(String username){
        return userRepository.existsByUsername(username);
    }
    public boolean verifyEmailExists(String email){
        return userRepository.existsByEmail(email);
    }

    public AuthResponse authenticateGoogle(AuthRequest request){
        User user; String accessJwt;

        user = userRepository.findByEmail(request.getUsername()).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(request.getUsername());
            newUser.setFullName(request.getFullname());
            newUser.setPassword(passwordEncoder.encode("")); //temporarily set as blank as OAuth doesn't set password for you, login will not authenticate if password is blank
            newUser.setEmailVerified(true);
            return userRepository.save(newUser);
        });;
        accessJwt = jwtService.generateToken(user, "access_token");
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(accessJwt);

    }

    public void generateRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response){
        String accessToken = request.getHeader("Authorization");
        try {
            String subject = jwtService.extractUsername(accessToken.substring(7));
            String refreshJwt = jwtService.generateToken(subject, "refresh_token");

            Cookie refreshTokenCookie = getRefreshTokenCookie(refreshJwt);
            setCookieAttribute(response, refreshTokenCookie);

        } catch (Exception e) {
            System.err.println("Error extracting subject from JWT: " + e.getMessage());
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
            if (!passwordEncoder.matches(credential.getOldPassword(), user.getPassword())){
                throw new BadCredentialsException("Old Password does not match");
            }
            user.setPassword(passwordEncoder.encode(credential.getPassword()));

            if(!credential.getEmail().equals(credential.getOldEmail())){
                user.setEmail(credential.getEmail());
                var tokenCookie = Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals("refresh_token")).findFirst().orElseThrow();
                var currentRefreshToken = tokenCookie.getValue();
                String refreshJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "refresh_token");

                Cookie refreshTokenCookie = getRefreshTokenCookie(refreshJwt);
                setCookieAttribute(response, refreshTokenCookie);
                revokeToken(currentRefreshToken);
            }
            userRepository.save(user);

        } catch (Exception e) {
            System.err.println("Error extracting subject from JWT: " + e.getMessage());
        }
    }
}
