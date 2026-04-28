package com.natyrov.spring.detection_with_vae.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AnalysisTaskDto {

    @NotNull
    private Long datasetId;

    @NotBlank(message = "Название обязательно")
    private String taskName;

    @NotBlank(message = "Выберите временной столбец")
    private String timeColumn;

    @NotEmpty(message = "Выберите хотя бы один признак")
    private List<String> featureColumns;

    @NotNull
    @Min(message = "Размер окна должен быть не меньше 4", value = 2)
    private Integer windowSize;

    @NotNull
    @Min(message = "Шаг окна должен быть не меньше 1", value = 1)
    private Integer stride;

    @NotNull
    @Min(message = "Должен быть не меньше 2", value = 2)
    private Integer latentDim;

    @NotNull
    @Min(message = "Эпох должно быть не меньше 1", value = 1)
    private Integer epochs;

    @NotNull
    @Min(message = "Должен быть не меньше 1", value = 1)
    private Integer batchSize;

    @NotNull
    private Boolean autoThreshold;

    private Double thresholdValue;

    @NotBlank(message = "Выберите модель")
    private String modelType;
}