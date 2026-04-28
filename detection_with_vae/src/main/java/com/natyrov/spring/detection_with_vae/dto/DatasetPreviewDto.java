package com.natyrov.spring.detection_with_vae.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class DatasetPreviewDto {
    private String fileName;
    private String fileType;
    private int totalRows;
    private int totalColumns;
    private String detectedTimeColumn;
    private List<String> headers;
    private List<ColumnInfoDto> columns;
    private List<Map<String,String>> previewRows;
    private List<String> numericColumns;
}
