package com.ohgiraffers.backend.application.infrastructure;

import com.ohgiraffers.backend.application.domain.model.ApplicationStatus;
import com.ohgiraffers.backend.application.domain.model.HospitalApplication;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hospital_applications")
public class HospitalApplicationJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String analysisId;

    @Column(nullable = false)
    private String hospitalId;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_application_included_items", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "included_item", nullable = false)
    private List<String> includedItems = new ArrayList<>();

    protected HospitalApplicationJpaEntity() {
    }

    private HospitalApplicationJpaEntity(String id, String analysisId, String hospitalId, String hospitalName, Instant submittedAt, ApplicationStatus status, List<String> includedItems) {
        this.id = id;
        this.analysisId = analysisId;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.submittedAt = submittedAt;
        this.status = status;
        this.includedItems = new ArrayList<>(includedItems);
    }

    public static HospitalApplicationJpaEntity fromDomain(HospitalApplication application) {
        return new HospitalApplicationJpaEntity(
                application.id(),
                application.analysisId(),
                application.hospitalId(),
                application.hospitalName(),
                application.submittedAt(),
                application.status(),
                application.includedItems()
        );
    }

    public HospitalApplication toDomain() {
        return new HospitalApplication(
                id,
                analysisId,
                hospitalId,
                hospitalName,
                submittedAt,
                status,
                List.copyOf(includedItems)
        );
    }
}
