package com.natyrov.spring.detection_with_vae.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PythonAnalysisRequestDto {
    private Long taskId;
    private String filePath;
    private String fileType;
    private String timeColumn;
    private List<String> featureColumns;
    private Integer windowSize;
    private Integer stride;
    private Integer latentDim;
    private Integer epochs;
    private Integer batchSize;
    private Boolean autoThreshold;
    private Double thresholdValue;
    private String modelType;
}