package com.ohgiraffers.backend.hospital.infrastructure;

import com.ohgiraffers.backend.hospital.domain.model.Hospital;
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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hospitals")
public class HospitalJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private double rating;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String phone;

    private double latitude;

    private double longitude;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_specialties", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "specialty", nullable = false)
    private List<String> specialties = new ArrayList<>();

    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TreatmentJpaEntity> treatments = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_available_times", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "available_time", nullable = false)
    private List<String> availableTimes = new ArrayList<>();

    protected HospitalJpaEntity() {
    }

    private HospitalJpaEntity(String id, String name, double rating, String address, String phone, double latitude, double longitude, List<String> specialties, List<String> availableTimes) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.specialties = new ArrayList<>(specialties);
        this.availableTimes = new ArrayList<>(availableTimes);
    }

    public static HospitalJpaEntity fromDomain(Hospital hospital) {
        HospitalJpaEntity entity = new HospitalJpaEntity(
                hospital.id(),
                hospital.name(),
                hospital.rating(),
                hospital.address(),
                hospital.phone(),
                hospital.latitude(),
                hospital.longitude(),
                hospital.specialties(),
                hospital.availableTimes()
        );
        hospital.treatments().forEach(treatment -> entity.addTreatment(TreatmentJpaEntity.fromDomain(treatment, entity)));
        return entity;
    }

    public Hospital toDomain() {
        return new Hospital(
                id,
                name,
                rating,
                address,
                phone,
                latitude,
                longitude,
                List.copyOf(specialties),
                treatments.stream().map(TreatmentJpaEntity::toDomain).toList(),
                List.copyOf(availableTimes)
        );
    }

    private void addTreatment(TreatmentJpaEntity treatment) {
        treatments.add(treatment);
    }
}
