package com.natyrov.spring.detection_with_vae.service.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService{

    private Path uploadPath;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xlsx");

    public FileStorageServiceImpl(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        this.uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadPath);
    }

    @Override
    public String storeFile(MultipartFile file) {
        if(file == null || file.isEmpty()){
            throw new RuntimeException("Файл не найден.");
        }

        String originalFileName=StringUtils.cleanPath(file.getOriginalFilename());
        if(originalFileName.contains("..")){
            throw new RuntimeException("Некорректное имя файла");
        }

        String extension = getExtension(originalFileName);
        if(!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())){
            throw new RuntimeException("Поддерживаются только файлы формата .csv и .xlsx");
        }

        String storedFileName = UUID.randomUUID()+"."+extension;

        try{
            Path targetLocation = this.uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения файла",e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try{
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Щшибка удаления файла",e);
        }
    }

    private String getExtension(String fileName){
        int lastDotIndex = fileName.lastIndexOf(".");
        if(lastDotIndex == -1 || lastDotIndex == fileName.length() - 1){
            throw new RuntimeException("Файл должен иметь расширение");
        }
        return fileName.substring(lastDotIndex+1);
    }
}
