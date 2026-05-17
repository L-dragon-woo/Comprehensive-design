package com.ohgiraffers.backend.analysis.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisJpaRepository extends JpaRepository<AnalysisJpaEntity, String> {

    List<AnalysisJpaEntity> findAllByOrderByCreatedAtDesc();
}
