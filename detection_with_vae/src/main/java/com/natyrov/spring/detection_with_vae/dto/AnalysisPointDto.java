package com.natyrov.spring.detection_with_vae.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AnalysisPointDto {
    private Integer index;
    private String timestamp;
    private Double score;
    private Boolean anomaly;
}