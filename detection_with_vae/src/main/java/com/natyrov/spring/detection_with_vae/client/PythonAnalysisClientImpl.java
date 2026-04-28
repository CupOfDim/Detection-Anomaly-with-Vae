package com.natyrov.spring.detection_with_vae.client;

import com.natyrov.spring.detection_with_vae.dto.PythonAnalysisRequestDto;
import com.natyrov.spring.detection_with_vae.dto.PythonAnalysisResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
@RequiredArgsConstructor
public class PythonAnalysisClientImpl implements PythonAnalysisClient{

    @Value("${python.api.base-url}")
    private String pythonApiBaseUrl;

    private final RestTemplate restTemplate;

    @Override
    public PythonAnalysisResponseDto runAnalysis(PythonAnalysisRequestDto request) {

        String url = pythonApiBaseUrl + "/analyze";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PythonAnalysisRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PythonAnalysisResponseDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                PythonAnalysisResponseDto.class);

        return response.getBody();
    }
}
