package com.ohgiraffers.backend.hospital.infrastructure;

import com.ohgiraffers.backend.hospital.domain.model.TreatmentInfo;
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
@Table(name = "hospital_treatments")
public class TreatmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalJpaEntity hospital;

    protected TreatmentJpaEntity() {
    }

    private TreatmentJpaEntity(String name, String description, HospitalJpaEntity hospital) {
        this.name = name;
        this.description = description;
        this.hospital = hospital;
    }

    public static TreatmentJpaEntity fromDomain(TreatmentInfo treatment, HospitalJpaEntity hospital) {
        return new TreatmentJpaEntity(treatment.name(), treatment.description(), hospital);
    }

    public TreatmentInfo toDomain() {
        return new TreatmentInfo(name, description);
    }
}
