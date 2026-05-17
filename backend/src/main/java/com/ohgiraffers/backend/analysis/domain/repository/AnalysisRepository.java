package com.ohgiraffers.backend.analysis.domain.repository;

import com.ohgiraffers.backend.analysis.domain.model.Analysis;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnalysisRepository {

    Analysis save(Analysis analysis);

    List<Analysis> findAllLatestFirst();

    Optional<Analysis> findById(String id);

    Analysis createMockAnalysis(String id, Instant createdAt, String imageUrl);
}
