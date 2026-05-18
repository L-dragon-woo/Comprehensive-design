package com.ohgiraffers.backend.hospital.domain.repository;

import com.ohgiraffers.backend.hospital.domain.model.Hospital;

import java.util.List;

public interface HospitalPlaceSearchClient {

    List<Hospital> searchNearby(String query, double latitude, double longitude);
}
