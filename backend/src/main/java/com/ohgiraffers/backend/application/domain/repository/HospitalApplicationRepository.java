package com.ohgiraffers.backend.application.domain.repository;

import com.ohgiraffers.backend.application.domain.model.HospitalApplication;

import java.util.List;
import java.util.Optional;

public interface HospitalApplicationRepository {

    HospitalApplication save(HospitalApplication application);

    List<HospitalApplication> findAllLatestFirst();

    Optional<HospitalApplication> findById(String id);
}
