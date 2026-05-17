package com.ohgiraffers.backend.analysis.infrastructure;

import com.ohgiraffers.backend.analysis.domain.model.AnalysisTreatmentResponse;
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
@Table(name = "analysis_treatments")
public class AnalysisTreatmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequenceId;

    @Column(nullable = false)
    private String treatmentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String matchLevel;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisJpaEntity analysis;

    protected AnalysisTreatmentJpaEntity() {
    }

    private AnalysisTreatmentJpaEntity(String treatmentId, String name, String matchLevel, String reason, String note, AnalysisJpaEntity analysis) {
        this.treatmentId = treatmentId;
        this.name = name;
        this.matchLevel = matchLevel;
        this.reason = reason;
        this.note = note;
        this.analysis = analysis;
    }

    public static AnalysisTreatmentJpaEntity fromDomain(AnalysisTreatmentResponse treatment, AnalysisJpaEntity analysis) {
        return new AnalysisTreatmentJpaEntity(treatment.id(), treatment.name(), treatment.match(), treatment.reason(), treatment.note(), analysis);
    }

    public AnalysisTreatmentResponse toDomain() {
        return new AnalysisTreatmentResponse(treatmentId, name, matchLevel, reason, note);
    }
}
