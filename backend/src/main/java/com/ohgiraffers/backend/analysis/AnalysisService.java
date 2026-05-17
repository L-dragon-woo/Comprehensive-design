package com.ohgiraffers.backend.analysis;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {

    private final AnalysisRepository analysisRepository;

    public AnalysisService(AnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
    }

    public AnalysisListResponse findAnalyses(String period, int page, int pageSize) {
        List<AnalysisListItemResponse> allItems = analysisRepository.findAllLatestFirst().stream()
                .filter(analysis -> matchesPeriod(analysis, period))
                .map(this::toListItem)
                .toList();

        int fromIndex = Math.min(Math.max(page - 1, 0) * pageSize, allItems.size());
        int toIndex = Math.min(fromIndex + pageSize, allItems.size());
        return new AnalysisListResponse(allItems.subList(fromIndex, toIndex), allItems.size());
    }

    public AnalysisDetailResponse getAnalysis(String analysisId) {
        return analysisRepository.findById(analysisId)
                .map(this::toDetail)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
    }

    public AnalysisStatusResponse getStatus(String analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
        int progress = calculateProgress(analysis.createdAt());
        AnalysisStatus status = progress >= 100 ? AnalysisStatus.completed : AnalysisStatus.processing;
        String currentStep = resolveCurrentStep(progress);

        return new AnalysisStatusResponse(
                analysis.id(),
                status,
                progress,
                currentStep,
                buildSteps(progress)
        );
    }

    public AnalysisCreateResponse createAnalysis(MultipartFile image, String targetArea, String memo) {
        if (image == null || image.isEmpty()) {
            throw new InvalidAnalysisRequestException("image is required");
        }

        String id = "analysis_" + UUID.randomUUID().toString().substring(0, 8);
        Instant createdAt = Instant.now();
        String imageUrl = "/api/analyses/" + id + "/image";
        analysisRepository.save(analysisRepository.createMockAnalysis(id, createdAt, imageUrl));

        // targetArea와 memo는 AI 연동 시 프롬프트/분석 컨텍스트로 넘길 수 있게 endpoint 계약에 남겨둡니다.
        return new AnalysisCreateResponse(id, AnalysisStatus.processing, createdAt);
    }

    private AnalysisListItemResponse toListItem(Analysis analysis) {
        return new AnalysisListItemResponse(
                analysis.id(),
                analysis.date().toString(),
                formatDate(analysis),
                analysis.overallScore(),
                analysis.change(),
                analysis.improvements()
        );
    }

    private AnalysisDetailResponse toDetail(Analysis analysis) {
        return new AnalysisDetailResponse(
                analysis.id(),
                analysis.date().toString(),
                formatDate(analysis),
                analysis.overallScore(),
                analysis.skinType(),
                analysis.concerns(),
                analysis.metrics(),
                analysis.treatments(),
                analysis.recommendations(),
                analysis.imageUrl()
        );
    }

    private boolean matchesPeriod(Analysis analysis, String period) {
        if ("week".equalsIgnoreCase(period)) {
            return analysis.createdAt().isAfter(Instant.now().minus(Duration.ofDays(7)));
        }
        if ("month".equalsIgnoreCase(period)) {
            return analysis.createdAt().isAfter(Instant.now().minus(Duration.ofDays(30)));
        }
        return true;
    }

    private int calculateProgress(Instant createdAt) {
        long elapsedSeconds = Duration.between(createdAt, Instant.now()).toSeconds();
        return (int) Math.min(100, Math.max(10, elapsedSeconds * 20));
    }

    private String resolveCurrentStep(int progress) {
        if (progress < 30) {
            return "detect_skin_area";
        }
        if (progress < 55) {
            return "skin_tone";
        }
        if (progress < 75) {
            return "pores_texture";
        }
        if (progress < 100) {
            return "consultation_points";
        }
        return "treatment_recommendation";
    }

    private List<AnalysisStepResponse> buildSteps(int progress) {
        return List.of(
                new AnalysisStepResponse("detect_skin_area", "피부 영역 감지", stepStatus(progress, 30)),
                new AnalysisStepResponse("skin_tone", "피부 톤 분석", stepStatus(progress, 55)),
                new AnalysisStepResponse("pores_texture", "모공 및 결 분석", stepStatus(progress, 75)),
                new AnalysisStepResponse("consultation_points", "시술 상담 포인트 정리", stepStatus(progress, 100)),
                new AnalysisStepResponse("treatment_recommendation", "맞춤 시술 추천 생성", progress >= 100 ? "completed" : "pending")
        );
    }

    private String stepStatus(int progress, int completedAt) {
        if (progress >= completedAt) {
            return "completed";
        }
        return completedAt - progress <= 20 ? "processing" : "pending";
    }

    private String formatDate(Analysis analysis) {
        return analysis.date().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }
}
