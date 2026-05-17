package com.ohgiraffers.backend.analysis.presentation;

import com.ohgiraffers.backend.analysis.application.AnalysisService;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisCreateResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisDetailResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisListResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisStatusResponse;
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
        // 기록 화면에서 기간 필터와 페이지 정보를 넘겨 분석 목록을 조회합니다.
        return analysisService.findAnalyses(period, page, pageSize);
    }

    @PostMapping
    public AnalysisCreateResponse createAnalysis(
            @RequestParam MultipartFile image,
            @RequestParam(required = false) String targetArea,
            @RequestParam(required = false) String memo
    ) {
        // AI 서버가 준비되기 전까지 업로드 요청만 받고 mock 분석 작업을 생성합니다.
        return analysisService.createAnalysis(image, targetArea, memo);
    }

    @GetMapping("/{analysisId}")
    public AnalysisDetailResponse getAnalysis(@PathVariable String analysisId) {
        // 결과 화면과 기록 상세 화면에서 같은 분석 상세 응답을 사용합니다.
        return analysisService.getAnalysis(analysisId);
    }

    @GetMapping("/{analysisId}/status")
    public AnalysisStatusResponse getStatus(@PathVariable String analysisId) {
        // 로딩 화면에서 진행률과 단계별 상태를 폴링할 때 사용합니다.
        return analysisService.getStatus(analysisId);
    }
}
