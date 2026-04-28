package com.natyrov.spring.detection_with_vae.service.datasets;

import com.natyrov.spring.detection_with_vae.entity.Dataset;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatasetService {
    void uploadDataset(MultipartFile file, String userEmail);
    List<Dataset> getUserDatasets(String userEmail);
    void deleteDataset(Long datasetId, String userEmail);
}
