package com.ohgiraffers.backend.analysis.infrastructure;

import com.ohgiraffers.backend.analysis.domain.model.Analysis;
import com.ohgiraffers.backend.analysis.domain.model.AnalysisMetricResponse;
import com.ohgiraffers.backend.analysis.domain.model.AnalysisTreatmentResponse;
import com.ohgiraffers.backend.analysis.domain.repository.AnalysisRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaAnalysisRepository implements AnalysisRepository {

    private final AnalysisJpaRepository analysisJpaRepository;

    public JpaAnalysisRepository(AnalysisJpaRepository analysisJpaRepository) {
        this.analysisJpaRepository = analysisJpaRepository;
    }

    @Override
    public Analysis save(Analysis analysis) {
        return analysisJpaRepository.save(AnalysisJpaEntity.fromDomain(analysis)).toDomain();
    }

    @Override
    public List<Analysis> findAllLatestFirst() {
        return analysisJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AnalysisJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Analysis> findById(String id) {
        return analysisJpaRepository.findById(id)
                .map(AnalysisJpaEntity::toDomain);
    }

    @Override
    public Analysis createMockAnalysis(String id, Instant createdAt, String imageUrl) {
        // AI 서버가 준비되기 전까지 결과 화면과 같은 구조의 고정 mock 데이터를 제공합니다.
        return new Analysis(
                id,
                createdAt,
                LocalDate.of(2026, 4, 30),
                78,
                "복합성",
                List.of("T존 유분 과다", "볼 색소침착", "수분 부족"),
                List.of(
                        new AnalysisMetricResponse("hydration", "수분", 72, "보통", "피부 수분이 약간 부족해요"),
                        new AnalysisMetricResponse("sebum", "유분", 65, "주의", "T존 유분이 과다해요"),
                        new AnalysisMetricResponse("pigmentation", "색소", 70, "보통", "볼 부위 색소침착 상담이 필요해요"),
                        new AnalysisMetricResponse("texture", "피부결", 81, "양호", "피부결은 비교적 안정적이에요")
                ),
                List.of(
                        new AnalysisTreatmentResponse("treatment_001", "리쥬란 힐러", "추천", "볼 건조와 피부결 개선 상담에 적합해요", "민감도와 통증 정도를 상담하세요"),
                        new AnalysisTreatmentResponse("treatment_002", "피코토닝", "추천", "색소침착과 톤 개선 상담에 적합해요", "시술 전후 자외선 차단 계획이 중요해요")
                ),
                List.of(
                        "시술 전 1주일은 강한 각질 케어 피하기",
                        "상담 시 색소침착 부위와 민감도 공유하기",
                        "시술 후 자외선 차단과 보습 계획 세우기"
                ),
                imageUrl,
                5,
                List.of("수분 개선", "모공 케어")
        );
    }
}
