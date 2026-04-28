package com.natyrov.spring.detection_with_vae.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.natyrov.spring.detection_with_vae.dto.AnalysisPointDto;
import com.natyrov.spring.detection_with_vae.dto.AnalysisTaskDto;
import com.natyrov.spring.detection_with_vae.entity.AnalysisTask;
import com.natyrov.spring.detection_with_vae.repository.AnalysisTaskRepository;
import com.natyrov.spring.detection_with_vae.service.analysis.AnalysisService;
import com.natyrov.spring.detection_with_vae.service.parsing.DatasetParsingService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final DatasetParsingService datasetParsingService;

    @GetMapping
    public String analysisList(Authentication authentication,
                               Model model){
        String email = authentication.getName();
        model.addAttribute("tasks", analysisService.getUserTasks(email));
        return "analysis-list";
    }

    @GetMapping("/create/{datasetId}")
    public String createAnalysisPage(@PathVariable long datasetId,
                                     Authentication authentication,
                                     Model model){
        String email = authentication.getName();

        var preview = datasetParsingService.previewDataset(datasetId,email);

        AnalysisTaskDto dto = new AnalysisTaskDto();
        dto.setDatasetId(datasetId);
        dto.setTaskName("Анализ датасета " + preview.getFileName());
        dto.setWindowSize(32);
        dto.setStride(1);
        dto.setLatentDim(8);
        dto.setEpochs(30);
        dto.setBatchSize(32);
        dto.setAutoThreshold(true);
        dto.setModelType("DENSE_VAE");

        model.addAttribute("preview", preview);
        model.addAttribute("analysisDto", dto);

        return "analysis-create";
    }

    @PostMapping("/create")
    public String createAnalysis(@Valid @ModelAttribute("analysisDto") AnalysisTaskDto dto,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model) {

        String email = authentication.getName();

        if (bindingResult.hasErrors()) {
            model.addAttribute("preview", datasetParsingService.previewDataset(dto.getDatasetId(), email));
            return "analysis-create";
        }

        try {
            AnalysisTask task = analysisService.createTask(dto, email);
            return "redirect:/analysis/" + task.getId();
        } catch (Exception e) {
            model.addAttribute("preview", datasetParsingService.previewDataset(dto.getDatasetId(), email));
            model.addAttribute("errorMessage", e.getMessage());
            return "analysis-create";
        }
    }

    @GetMapping("/{id}")
    public String analysisDetails(@PathVariable Long id,
                                  Authentication authentication,
                                  Model model) {
        String email = authentication.getName();

        model.addAttribute("task", analysisService.getTaskById(id, email));
        model.addAttribute("anomalousPoints", analysisService.getAnomalousPoints(id, email));
        model.addAttribute("topPoints", analysisService.getTopPoints(id, email, 20));
        model.addAttribute("anomalousPoints", analysisService.getAnomalousPoints(id, email).stream().limit(100).toList());
        return "analysis-details";
    }

    @PostMapping("/{id}/run")
    public String runAnalysis(@PathVariable Long id,
                              Authentication authentication) {
        String email = authentication.getName();
        analysisService.runTask(id, email);
        return "redirect:/analysis/" + id;
    }

    @GetMapping("/{id}/export")
    public void exportAnalysisCsv(@PathVariable Long id,
                                  Authentication authentication,
                                  HttpServletResponse response) throws Exception {
        String email = authentication.getName();
        List<AnalysisPointDto> points = analysisService.getTopPoints(id, email, Integer.MAX_VALUE);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=analysis_" + id + "_results.csv");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("index,timestamp,score,anomaly");

            for (AnalysisPointDto point : points) {
                writer.printf("%d,%s,%.6f,%s%n",
                        point.getIndex(),
                        point.getTimestamp(),
                        point.getScore(),
                        point.getAnomaly());
            }
        }
    }
}
