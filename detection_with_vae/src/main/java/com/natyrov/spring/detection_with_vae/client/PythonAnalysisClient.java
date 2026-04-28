package com.natyrov.spring.detection_with_vae.client;

import com.natyrov.spring.detection_with_vae.dto.PythonAnalysisRequestDto;
import com.natyrov.spring.detection_with_vae.dto.PythonAnalysisResponseDto;

public interface PythonAnalysisClient {
    PythonAnalysisResponseDto runAnalysis(PythonAnalysisRequestDto request);
}
