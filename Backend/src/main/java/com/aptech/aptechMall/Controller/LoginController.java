package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.security.requests.*;
import com.aptech.aptechMall.service.authentication.AuthService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request,
                                              HttpServletResponse response,
                                              @RequestParam(required = false, name="method") String method) {
        if (method != null && method.equals("google")) return ResponseEntity.ok(authService.authenticateGoogle(request));

        return ResponseEntity.ok(authService.authenticate(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);
        return ResponseEntity.ok("Logout Successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response, @RequestParam(required = false, name="method") String method) {
        if (method != null && method.equals("google")){
            authService.generateRefreshTokenCookie(request, response);
        } else {
            AuthResponse accessToken = authService.refreshToken(request, response);
            if(accessToken != null)
                return ResponseEntity.ok(accessToken);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refresh Token");
    }

    @PostMapping("/oauth")
    public ResponseEntity<String> oAuthToken(HttpServletRequest request, HttpServletResponse response){
        try {
            authService.generateRefreshTokenCookie(request, response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Oauth failed to generate refresh");
        }
        return ResponseEntity.ok("Oauth Refresh initiated successfully");
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok(authService.getProfile(request, response));
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            @ModelAttribute UpdateProfile info // maps fields + optional file
    ) {
        return ResponseEntity.ok(
                authService.updateProfile(request, response, info)
        );
    }


    @PostMapping("/update-credentials")
    public ResponseEntity<String> updateAccountCredentials(HttpServletRequest request, HttpServletResponse response, @Valid @RequestBody UpdateCredential credentials){
        authService.updateEmailOrPassword(request, response, credentials);
        return ResponseEntity.ok("Credentials Updated");
    }

    @PostConstruct
    private void PreRegisterUsers(){
        List<RegisterRequest> preRegistrations = new ArrayList<>();
        RegisterRequest request1 = new RegisterRequest("VanA", "123456", "Nguyen Van A", "ADMIN", "NguyenVanA@gmail.com");
        RegisterRequest request2 = new RegisterRequest("VanB", "123456", "Nguyen Van B", "STAFF", "NguyenVanB@gmail.com");
        RegisterRequest request3 = new RegisterRequest("VanC", "123456", "Nguyen Van C", "CUSTOMER", "NguyenVanC@gmail.com");
        RegisterRequest request4 = new RegisterRequest("Demo", "demo123", "Demo User", "CUSTOMER", "demo.account@gmail.com");
        RegisterRequest request5 = new RegisterRequest("Admin", "admin123", "Demo Admin", "ADMIN", "admin@pandamall.com");

        preRegistrations.add(request1);
        preRegistrations.add(request2);
        preRegistrations.add(request3);
        preRegistrations.add(request4);
        preRegistrations.add(request5);

        for(RegisterRequest request : preRegistrations){
            if (!authService.verifyEmailExists(request.getEmail())){
                authService.preRegister(request);
            }
        }
    }
}
