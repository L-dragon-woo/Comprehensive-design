package com.ohgiraffers.backend.analysis.infrastructure;

import com.ohgiraffers.backend.analysis.domain.model.AnalysisMetricResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_metrics")
public class AnalysisMetricJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequenceId;

    @Column(nullable = false)
    private String metricId;

    @Column(nullable = false)
    private String title;

    private int score;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisJpaEntity analysis;

    protected AnalysisMetricJpaEntity() {
    }

    private AnalysisMetricJpaEntity(String metricId, String title, int score, String status, String description, AnalysisJpaEntity analysis) {
        this.metricId = metricId;
        this.title = title;
        this.score = score;
        this.status = status;
        this.description = description;
        this.analysis = analysis;
    }

    public static AnalysisMetricJpaEntity fromDomain(AnalysisMetricResponse metric, AnalysisJpaEntity analysis) {
        return new AnalysisMetricJpaEntity(metric.id(), metric.title(), metric.score(), metric.status(), metric.description(), analysis);
    }

    public AnalysisMetricResponse toDomain() {
        return new AnalysisMetricResponse(metricId, title, score, status, description);
    }
}
