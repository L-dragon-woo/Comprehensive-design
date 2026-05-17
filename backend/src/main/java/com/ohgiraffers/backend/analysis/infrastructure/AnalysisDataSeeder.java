package com.ohgiraffers.backend.analysis.infrastructure;

import com.ohgiraffers.backend.analysis.domain.repository.AnalysisRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AnalysisDataSeeder implements CommandLineRunner {

    private final AnalysisJpaRepository analysisJpaRepository;
    private final AnalysisRepository analysisRepository;

    public AnalysisDataSeeder(AnalysisJpaRepository analysisJpaRepository, AnalysisRepository analysisRepository) {
        this.analysisJpaRepository = analysisJpaRepository;
        this.analysisRepository = analysisRepository;
    }

    @Override
    public void run(String... args) {
        if (analysisJpaRepository.existsById("analysis_001")) {
            return;
        }

        // AI 연동 전에도 기록/결과 화면을 실제 DB 조회로 확인할 수 있게 기본 분석 결과를 저장합니다.
        analysisRepository.save(analysisRepository.createMockAnalysis(
                "analysis_001",
                Instant.parse("2026-04-30T09:00:00Z"),
                "/placeholder.jpg"
        ));
    }
}
