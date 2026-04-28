package com.natyrov.spring.detection_with_vae.service.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natyrov.spring.detection_with_vae.client.PythonAnalysisClient;
import com.natyrov.spring.detection_with_vae.dto.*;
import com.natyrov.spring.detection_with_vae.entity.AnalysisTask;
import com.natyrov.spring.detection_with_vae.entity.Dataset;
import com.natyrov.spring.detection_with_vae.entity.User;
import com.natyrov.spring.detection_with_vae.repository.AnalysisTaskRepository;
import com.natyrov.spring.detection_with_vae.repository.DatasetRepository;
import com.natyrov.spring.detection_with_vae.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final PythonAnalysisClient pythonAnalysisClient;
    private final ObjectMapper objectMapper;

    @Override
    public AnalysisTask createTask(AnalysisTaskDto dto, String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("Пользователь не найден"));

        Dataset dataset = datasetRepository.findById(dto.getDatasetId())
                .orElseThrow(()-> new RuntimeException("Датасет не найден"));

        if(!dataset.getOwner().getId().equals(user.getId())){
            throw new RuntimeException("Нет доступа к этому датасету");
        }

        if(Boolean.FALSE.equals(dto.getAutoThreshold())&& dto.getThresholdValue()==null){
            throw new RuntimeException("Укажите thresholdValue или включите автоматический порог");
        }

        AnalysisTask task = AnalysisTask.builder()
                .taskName(dto.getTaskName())
                .dataset(dataset)
                .owner(user)
                .timeColumn(dto.getTimeColumn())
                .featureColumns(String.join(",", dto.getFeatureColumns()))
                .windowSize(dto.getWindowSize())
                .stride(dto.getStride())
                .latentDim(dto.getLatentDim())
                .epochs(dto.getEpochs())
                .batchSize(dto.getBatchSize())
                .autoThreshold(dto.getAutoThreshold())
                .thresholdValue(dto.getThresholdValue())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .modelType(dto.getModelType())
                .build();
        return analysisTaskRepository.save(task);
    }

    @Override
    public List<AnalysisTask> getUserTasks(String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("Пользователь не найден"));
        return analysisTaskRepository.findByOwnerOrderByCreatedAtDesc(user);
    }

    @Override
    public AnalysisTask getTaskById(Long taskId, String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("Пользователь не найден"));

        AnalysisTask task = analysisTaskRepository.findById(taskId)
                .orElseThrow(()-> new RuntimeException("Задача анализа не найден"));

        if (!task.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Нет доступа к задаче");
        }
        return task;
    }

    @Override
    public void runTask(Long taskId, String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        AnalysisTask task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Задача анализа не найдена"));


        if (!task.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Нет доступа к задаче");
        }

        try {
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            analysisTaskRepository.save(task);

            PythonAnalysisRequestDto request = PythonAnalysisRequestDto.builder()
                    .taskId(task.getId())
                    .filePath(task.getDataset().getFilePath())
                    .fileType(task.getDataset().getFileType())
                    .timeColumn(task.getTimeColumn())
                    .featureColumns(List.of(task.getFeatureColumns().split(",")))
                    .windowSize(task.getWindowSize())
                    .stride(task.getStride())
                    .latentDim(task.getLatentDim())
                    .epochs(task.getEpochs())
                    .batchSize(task.getBatchSize())
                    .autoThreshold(task.getAutoThreshold())
                    .thresholdValue(task.getThresholdValue())
                    .modelType(task.getModelType())
                    .build();

            PythonAnalysisResponseDto response = pythonAnalysisClient.runAnalysis(request);
            String summary = String.format(
                    "Rows: %d, Features: %d, Windows: %d, Mean error: %.6f, Max error: %.6f, Threshold: %.6f, Anomalous windows: %d, Point threshold: %.6f, Anomalous points: %d",
                    response.getTotalRows(),
                    response.getTotalFeatures(),
                    response.getTotalWindows(),
                    response.getMeanError(),
                    response.getMaxError(),
                    response.getThreshold(),
                    response.getAnomalyWindowIndices() != null ? response.getAnomalyWindowIndices().size() : 0,
                    response.getPointThreshold(),
                    response.getAnomalyPointIndices() != null ? response.getAnomalyPointIndices().size() : 0
            );

            task.setResultSummary(summary);
            task.setChartDataJson(objectMapper.writeValueAsString(response));
            task.setStatus(response.getStatus() != null ? response.getStatus() : "COMPLETED");
            task.setFinishedAt(LocalDateTime.now());

            analysisTaskRepository.save(task);

        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setFinishedAt(LocalDateTime.now());
            task.setErrorMessage(e.getMessage());
            analysisTaskRepository.save(task);

            throw new RuntimeException("Ошибка запуска Python-анализа: " + e.getMessage(), e);
        }
    }

    private AnalysisChartDataDto getChartData(AnalysisTask task) {
        try {
            if (task.getChartDataJson() == null || task.getChartDataJson().isBlank()) {
                return null;
            }
            return objectMapper.readValue(task.getChartDataJson(), AnalysisChartDataDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось прочитать chartDataJson", e);
        }
    }

    @Override
    public List<AnalysisPointDto> getAnomalousPoints(Long taskId, String userEmail) {
        AnalysisTask task = getTaskById(taskId, userEmail);
        AnalysisChartDataDto chartData = getChartData(task);

        if (chartData == null || chartData.getTimestamps() == null ||
                chartData.getPointScores() == null || chartData.getPointAnomalyFlags() == null) {
            return List.of();
        }

        List<AnalysisPointDto> result = new java.util.ArrayList<>();

        for (int i = 0; i < chartData.getTimestamps().size(); i++) {
            Boolean anomaly = chartData.getPointAnomalyFlags().get(i);
            if (Boolean.TRUE.equals(anomaly)) {
                result.add(new AnalysisPointDto(
                        i,
                        chartData.getTimestamps().get(i),
                        chartData.getPointScores().get(i),
                        true
                ));
            }
        }

        return result;
    }

    @Override
    public List<AnalysisPointDto> getTopPoints(Long taskId, String userEmail, int limit) {
        AnalysisTask task = getTaskById(taskId, userEmail);
        AnalysisChartDataDto chartData = getChartData(task);

        if (chartData == null || chartData.getTimestamps() == null ||
                chartData.getPointScores() == null) {
            return List.of();
        }

        List<AnalysisPointDto> allPoints = new java.util.ArrayList<>();

        for (int i = 0; i < chartData.getTimestamps().size(); i++) {
            boolean anomaly = false;
            if (chartData.getPointAnomalyFlags() != null && i < chartData.getPointAnomalyFlags().size()) {
                anomaly = Boolean.TRUE.equals(chartData.getPointAnomalyFlags().get(i));
            }

            allPoints.add(new AnalysisPointDto(
                    i,
                    chartData.getTimestamps().get(i),
                    chartData.getPointScores().get(i),
                    anomaly
            ));
        }

        return allPoints.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .toList();
    }
}
