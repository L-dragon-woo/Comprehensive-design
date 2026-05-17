package com.ohgiraffers.backend.hospital.domain.repository;

import com.ohgiraffers.backend.hospital.domain.model.Hospital;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository {

    List<Hospital> findAll();

    Optional<Hospital> findById(String id);
}
