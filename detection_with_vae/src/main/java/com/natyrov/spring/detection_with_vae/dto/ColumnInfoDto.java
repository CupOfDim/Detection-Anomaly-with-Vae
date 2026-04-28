package com.natyrov.spring.detection_with_vae.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ColumnInfoDto {
    private String name;
    private String detectedType;
    private boolean numeric;
    private boolean timestampCandidate;
    private long missingCount;
}
