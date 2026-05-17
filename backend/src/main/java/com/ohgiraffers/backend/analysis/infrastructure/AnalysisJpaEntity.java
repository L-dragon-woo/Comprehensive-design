package com.ohgiraffers.backend.analysis.infrastructure;

import com.ohgiraffers.backend.analysis.domain.model.Analysis;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analyses")
public class AnalysisJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private LocalDate date;

    private int overallScore;

    @Column(nullable = false)
    private String skinType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_concerns", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "concern", nullable = false)
    private List<String> concerns = new ArrayList<>();

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AnalysisMetricJpaEntity> metrics = new ArrayList<>();

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AnalysisTreatmentJpaEntity> treatments = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_recommendations", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "recommendation", nullable = false)
    private List<String> recommendations = new ArrayList<>();

    @Column(nullable = false)
    private String imageUrl;

    private int changeScore;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_improvements", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "improvement", nullable = false)
    private List<String> improvements = new ArrayList<>();

    protected AnalysisJpaEntity() {
    }

    private AnalysisJpaEntity(String id, Instant createdAt, LocalDate date, int overallScore, String skinType, List<String> concerns, List<String> recommendations, String imageUrl, int changeScore, List<String> improvements) {
        this.id = id;
        this.createdAt = createdAt;
        this.date = date;
        this.overallScore = overallScore;
        this.skinType = skinType;
        this.concerns = new ArrayList<>(concerns);
        this.recommendations = new ArrayList<>(recommendations);
        this.imageUrl = imageUrl;
        this.changeScore = changeScore;
        this.improvements = new ArrayList<>(improvements);
    }

    public static AnalysisJpaEntity fromDomain(Analysis analysis) {
        AnalysisJpaEntity entity = new AnalysisJpaEntity(
                analysis.id(),
                analysis.createdAt(),
                analysis.date(),
                analysis.overallScore(),
                analysis.skinType(),
                analysis.concerns(),
                analysis.recommendations(),
                analysis.imageUrl(),
                analysis.change(),
                analysis.improvements()
        );
        analysis.metrics().forEach(metric -> entity.metrics.add(AnalysisMetricJpaEntity.fromDomain(metric, entity)));
        analysis.treatments().forEach(treatment -> entity.treatments.add(AnalysisTreatmentJpaEntity.fromDomain(treatment, entity)));
        return entity;
    }

    public Analysis toDomain() {
        return new Analysis(
                id,
                createdAt,
                date,
                overallScore,
                skinType,
                List.copyOf(concerns),
                metrics.stream().map(AnalysisMetricJpaEntity::toDomain).toList(),
                treatments.stream().map(AnalysisTreatmentJpaEntity::toDomain).toList(),
                List.copyOf(recommendations),
                imageUrl,
                changeScore,
                List.copyOf(improvements)
        );
    }
}
