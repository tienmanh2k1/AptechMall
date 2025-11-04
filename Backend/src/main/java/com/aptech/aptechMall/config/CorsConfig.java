package com.aptech.aptechMall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;  // ← QUAN TRỌNG: Dùng CorsFilter, không phải CorsWebFilter!

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {  // ← CorsFilter cho Spring MVC
        CorsConfiguration config = new CorsConfiguration();

        // Cho phép credentials
        config.setAllowCredentials(true);

        // Cho phép origins
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",  // Next.js
                "http://localhost:5173",  // Vite
                "http://localhost:4200"   // Angular
        ));

        // Cho phép methods
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Cho phép headers
        config.setAllowedHeaders(Arrays.asList("*"));

        // Expose headers
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        // Max age
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);  // ← CorsFilter, không phải CorsWebFilter!
    }
}