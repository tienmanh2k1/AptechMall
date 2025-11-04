package com.aptech.aptechMall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileConfig implements WebMvcConfigurer {


    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        exposeDirectory("uploads/avatars", registry);
    }

    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName);
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        if (dirName.startsWith("../")) dirName = dirName.replace("../", "");

        registry.addResourceHandler("/" + dirName + "/**").addResourceLocations("file:/"+ uploadPath + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Expose thư mục Spring Boot để frontend lấy Avatars về display
        registry.addMapping("/uploads/avatars/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "HEAD");
    }
}
