package com.ohgiraffers.backend.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 40)
    private String displayName;

    @Column(length = 20)
    private String gender;

    private Integer age;

    @Column(columnDefinition = "TEXT")
    private String skinTreatmentHistory;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hasAllergy;

    @Column(length = 500)
    private String allergyDetails;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hasDisease;

    @Column(length = 500)
    private String diseaseDetails;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    public UserEntity(String username, String passwordHash, String displayName, Instant createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGender() {
        return gender;
    }

    public Integer getAge() {
        return age;
    }

    public String getSkinTreatmentHistory() {
        return skinTreatmentHistory;
    }

    public boolean isHasAllergy() {
        return hasAllergy;
    }

    public String getAllergyDetails() {
        return allergyDetails;
    }

    public boolean isHasDisease() {
        return hasDisease;
    }

    public String getDiseaseDetails() {
        return diseaseDetails;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateProfile(
            String displayName,
            String gender,
            Integer age,
            String skinTreatmentHistory,
            boolean hasAllergy,
            String allergyDetails,
            boolean hasDisease,
            String diseaseDetails
    ) {
        this.displayName = displayName;
        this.gender = gender;
        this.age = age;
        this.skinTreatmentHistory = skinTreatmentHistory;
        this.hasAllergy = hasAllergy;
        this.allergyDetails = allergyDetails;
        this.hasDisease = hasDisease;
        this.diseaseDetails = diseaseDetails;
    }
}
