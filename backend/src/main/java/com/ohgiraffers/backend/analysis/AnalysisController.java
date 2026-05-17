package com.ohgiraffers.backend.analysis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping
    public AnalysisListResponse findAnalyses(
            @RequestParam(defaultValue = "all") String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return analysisService.findAnalyses(period, page, pageSize);
    }

    @PostMapping
    public AnalysisCreateResponse createAnalysis(
            @RequestParam MultipartFile image,
            @RequestParam(required = false) String targetArea,
            @RequestParam(required = false) String memo
    ) {
        return analysisService.createAnalysis(image, targetArea, memo);
    }

    @GetMapping("/{analysisId}")
    public AnalysisDetailResponse getAnalysis(@PathVariable String analysisId) {
        return analysisService.getAnalysis(analysisId);
    }

    @GetMapping("/{analysisId}/status")
    public AnalysisStatusResponse getStatus(@PathVariable String analysisId) {
        return analysisService.getStatus(analysisId);
    }
}
