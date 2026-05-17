package com.ohgiraffers.backend.application.infrastructure;

import com.ohgiraffers.backend.application.domain.model.HospitalApplication;
import com.ohgiraffers.backend.application.domain.repository.HospitalApplicationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaHospitalApplicationRepository implements HospitalApplicationRepository {

    private final HospitalApplicationJpaRepository applicationJpaRepository;

    public JpaHospitalApplicationRepository(HospitalApplicationJpaRepository applicationJpaRepository) {
        this.applicationJpaRepository = applicationJpaRepository;
    }

    @Override
    public HospitalApplication save(HospitalApplication application) {
        return applicationJpaRepository.save(HospitalApplicationJpaEntity.fromDomain(application)).toDomain();
    }

    @Override
    public List<HospitalApplication> findAllLatestFirst() {
        return applicationJpaRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(HospitalApplicationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<HospitalApplication> findById(String id) {
        return applicationJpaRepository.findById(id)
                .map(HospitalApplicationJpaEntity::toDomain);
    }
}
