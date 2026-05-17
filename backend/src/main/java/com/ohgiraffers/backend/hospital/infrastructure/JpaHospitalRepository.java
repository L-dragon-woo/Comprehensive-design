package com.ohgiraffers.backend.hospital.infrastructure;

import com.ohgiraffers.backend.hospital.domain.model.Hospital;
import com.ohgiraffers.backend.hospital.domain.repository.HospitalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaHospitalRepository implements HospitalRepository {

    private final HospitalJpaRepository hospitalJpaRepository;

    public JpaHospitalRepository(HospitalJpaRepository hospitalJpaRepository) {
        this.hospitalJpaRepository = hospitalJpaRepository;
    }

    @Override
    public List<Hospital> findAll() {
        return hospitalJpaRepository.findAll().stream()
                .map(HospitalJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Hospital> findById(String id) {
        return hospitalJpaRepository.findById(id)
                .map(HospitalJpaEntity::toDomain);
    }
}
