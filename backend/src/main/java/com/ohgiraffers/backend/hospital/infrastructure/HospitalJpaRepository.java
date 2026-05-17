package com.ohgiraffers.backend.hospital.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalJpaRepository extends JpaRepository<HospitalJpaEntity, String> {
}
