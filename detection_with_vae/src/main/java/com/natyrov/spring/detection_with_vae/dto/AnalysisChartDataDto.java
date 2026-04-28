package com.natyrov.spring.detection_with_vae.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AnalysisChartDataDto {
    private String status;
    private String message;
    private Integer totalRows;
    private Integer totalFeatures;
    private Integer totalWindows;
    private Integer windowSize;
    private String detectedTimeColumn;
    private List<String> usedFeatures;

    private Double meanError;
    private Double maxError;
    private Double threshold;
    private List<Integer> anomalyWindowIndices;

    private Double pointThreshold;
    private List<Integer> anomalyPointIndices;
    private List<String> timestamps;
    private List<Double> pointScores;
    private List<Boolean> pointAnomalyFlags;
    private Map<String, List<Double>> featureSeries;
}