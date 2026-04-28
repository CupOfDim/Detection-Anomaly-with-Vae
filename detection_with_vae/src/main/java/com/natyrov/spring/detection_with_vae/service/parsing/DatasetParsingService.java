package com.natyrov.spring.detection_with_vae.service.parsing;

import com.natyrov.spring.detection_with_vae.dto.DatasetPreviewDto;

public interface DatasetParsingService {
    DatasetPreviewDto previewDataset(Long datasetId, String userEmail);
}
