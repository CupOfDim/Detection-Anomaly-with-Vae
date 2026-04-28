package com.natyrov.spring.detection_with_vae.service.files;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file);
    void deleteFile(String filePath);
}
