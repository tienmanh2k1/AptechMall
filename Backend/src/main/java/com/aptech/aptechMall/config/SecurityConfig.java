package com.aptech.aptechMall.config;

import com.aptech.aptechMall.security.filters.JwtAuthenticationFilter;
import com.aptech.aptechMall.security.filters.TokenBlacklistFilter;
import com.aptech.aptechMall.service.authentication.JpaUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JpaUserDetailsService userDetailsService;

    private final CorsFilter corsFilter;
    private final TokenBlacklistFilter tokenBlacklistFilter;

    @Bean
    @Order(1) //For External Origin Authentication with Apis
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/*", "/api/auth/**", "/api/debug/**", "/api/aliexpress/**", "/api/1688/**", "/api/products/**", "/api/exchange-rates/**", "/api/bank-transfer/**").permitAll()
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/wallet/*/lock", "/api/wallet/*/unlock").hasRole("ADMIN")
                        .requestMatchers("/api/wallet/**").authenticated()
                        .requestMatchers("/api/cart/**", "/api/orders/**").authenticated()
                        .anyRequest().authenticated()
                )
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class) // để kết nối React với SpringBoot mà không bị chặn
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // dành cho việc lưu thông đăng nhập qua jwt, đảm nhận chức năng duyệt bảo mật thông qua jwt token
                .addFilterBefore(tokenBlacklistFilter, UsernamePasswordAuthenticationFilter.class) // blacklist jwt token trong springboot khi người dùng logout, thông qua Redis.
                .userDetailsService(userDetailsService)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsService(userDetailsService);
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
