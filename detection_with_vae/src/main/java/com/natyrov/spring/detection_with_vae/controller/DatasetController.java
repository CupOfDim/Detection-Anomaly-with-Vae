package com.natyrov.spring.detection_with_vae.controller;

import com.natyrov.spring.detection_with_vae.dto.DatasetPreviewDto;
import com.natyrov.spring.detection_with_vae.repository.DatasetRepository;
import com.natyrov.spring.detection_with_vae.service.datasets.DatasetService;
import com.natyrov.spring.detection_with_vae.service.parsing.DatasetParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/datasets")
@RequiredArgsConstructor
public class DatasetController {
    private final DatasetService datasetService;
    private final DatasetParsingService datasetParsingService;

    @GetMapping
    public String datasetsPage(Authentication authentication,
                               Model model,
                               @RequestParam(value = "success", required = false) String success,
                               @RequestParam(value = "error", required = false) String error){

        String email = authentication.getName();
        model.addAttribute("datasets", datasetService.getUserDatasets(email));
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        return "datasets";
    }

    @PostMapping("/upload")
    public String uploadDataset(@RequestParam("file")MultipartFile file,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes){
        try{
            String email = authentication.getName();
            datasetService.uploadDataset(file, email);
            redirectAttributes.addFlashAttribute("success", "Файл успешно загружен");;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/datasets";
    }

    @PostMapping("/delete/{id}")
    public String deleteDataset(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes){
        try{
            String email = authentication.getName();
            datasetService.deleteDataset(id, email);
            redirectAttributes.addFlashAttribute("success", "Файд успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/datasets";
    }

    @GetMapping("/{id}")
    public String viewDataset(@PathVariable Long id,
                              Authentication authentication,
                              Model model){
        String email = authentication.getName();

        model.addAttribute("preview", datasetParsingService.previewDataset(id,email));
        model.addAttribute("datasetId", id);
        return "dataset-details";
    }
}

