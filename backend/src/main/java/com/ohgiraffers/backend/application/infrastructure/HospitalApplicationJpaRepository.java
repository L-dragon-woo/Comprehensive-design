package com.ohgiraffers.backend.application.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalApplicationJpaRepository extends JpaRepository<HospitalApplicationJpaEntity, String> {

    List<HospitalApplicationJpaEntity> findAllByOrderBySubmittedAtDesc();
}
