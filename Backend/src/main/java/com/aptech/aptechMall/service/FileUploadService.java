package com.aptech.aptechMall.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {
    private final Path avatarUploadDir = Paths.get("uploads/avatars");

    private static final String AVATAR_URL_PREFIX = "/uploads/avatars/";

    public FileUploadService() throws IOException {
        List<Path> pathVariables = List.of((
                avatarUploadDir
        ));
        for(Path path : pathVariables){
            if(!Files.exists(path)) {
                Files.createDirectories(path);
            }
        }

    }

    public String saveAvatar(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = avatarUploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return AVATAR_URL_PREFIX + filename;
    }
}
