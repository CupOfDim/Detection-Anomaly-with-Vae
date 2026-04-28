package com.natyrov.spring.detection_with_vae.service.datasets;

import com.natyrov.spring.detection_with_vae.entity.Dataset;
import com.natyrov.spring.detection_with_vae.entity.User;
import com.natyrov.spring.detection_with_vae.repository.DatasetRepository;
import com.natyrov.spring.detection_with_vae.repository.UserRepository;
import com.natyrov.spring.detection_with_vae.service.files.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService{

    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.upload.dir}")
    private String uploadDir;


    @Override
    public void uploadDataset(MultipartFile file, String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("Пользователь не найден"));
        String storedFileName = fileStorageService.storeFile(file);
        String originalFileName = file.getOriginalFilename();
        String fileType = getExtension(originalFileName);
        String filePath = Paths.get(uploadDir,storedFileName)
                .toAbsolutePath()
                .normalize()
                .toString();

        Dataset dataset = Dataset.builder()
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath)
                .fileType(fileType)
                .fileSize(file.getSize())
                .uploadTime(LocalDateTime.now())
                .status("UPLOADED")
                .owner(user)
                .build();
        datasetRepository.save(dataset);
    }

    @Override
    public List<Dataset> getUserDatasets(String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("Пользователь не найден"));
        return datasetRepository.findByOwnerOrderByUploadTimeDesc(user);
    }

    @Override
    public void deleteDataset(Long datasetId, String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("Пользователь не найден"));

        Dataset dataset = datasetRepository.findByOwnerAndId(user, datasetId)
                .orElseThrow(()-> new RuntimeException("Датасет не найден или доступ запрещен"));

        fileStorageService.deleteFile(dataset.getFilePath());
        datasetRepository.delete(dataset);
    }

    private String getExtension(String fileName){
        int lastDotIndex = fileName.lastIndexOf(".");
        if(lastDotIndex == -1 || lastDotIndex == fileName.length() - 1){
            throw new RuntimeException("Файл должен иметь расширение");
        }
        return fileName.substring(lastDotIndex+1);
    }
}
