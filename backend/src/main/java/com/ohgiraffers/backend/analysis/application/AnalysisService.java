package com.ohgiraffers.backend.analysis.application;

import com.ohgiraffers.backend.analysis.domain.exception.AnalysisNotFoundException;
import com.ohgiraffers.backend.analysis.domain.exception.InvalidAnalysisRequestException;
import com.ohgiraffers.backend.analysis.domain.model.Analysis;
import com.ohgiraffers.backend.analysis.domain.model.AnalysisStatus;
import com.ohgiraffers.backend.analysis.domain.repository.AnalysisRepository;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisCreateResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisDetailResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisListItemResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisListResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisStatusResponse;
import com.ohgiraffers.backend.analysis.presentation.dto.AnalysisStepResponse;
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
        // mock 데이터라도 실제 목록 API와 동일하게 기간 필터와 페이지네이션을 적용합니다.
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
        // AI 작업 큐가 없으므로 생성 시각부터 지난 시간으로 진행률을 흉내 냅니다.
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
        // 지금은 이미지 파일을 디스크에 저장하지 않고, 추후 파일 저장 API로 교체할 URL만 계약 형태로 남깁니다.
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
        // 각 단계의 완료 기준값과 현재 진행률 차이로 pending/processing/completed를 결정합니다.
        if (progress >= completedAt) {
            return "completed";
        }
        return completedAt - progress <= 20 ? "processing" : "pending";
    }

    private String formatDate(Analysis analysis) {
        return analysis.date().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }
}
