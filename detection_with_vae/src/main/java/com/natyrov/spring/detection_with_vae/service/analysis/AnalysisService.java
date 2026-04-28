package com.natyrov.spring.detection_with_vae.service.analysis;

import com.natyrov.spring.detection_with_vae.dto.AnalysisPointDto;
import com.natyrov.spring.detection_with_vae.dto.AnalysisTaskDto;
import com.natyrov.spring.detection_with_vae.entity.AnalysisTask;

import java.util.List;

public interface AnalysisService {
    AnalysisTask createTask(AnalysisTaskDto dto, String userEmail);
    List<AnalysisTask> getUserTasks(String userEmail);
    AnalysisTask getTaskById(Long taskId, String userEmail);
    void runTask(Long taskId, String userEmail);

    List<AnalysisPointDto> getAnomalousPoints(Long taskId, String userEmail);
    List<AnalysisPointDto> getTopPoints(Long taskId, String userEmail, int limit);
}
