package com.ohgiraffers.backend.application;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class HospitalApplicationRepository {

    private final Map<String, HospitalApplication> applications = new ConcurrentHashMap<>();

    public HospitalApplication save(HospitalApplication application) {
        applications.put(application.id(), application);
        return application;
    }

    public List<HospitalApplication> findAllLatestFirst() {
        return new ArrayList<>(applications.values()).stream()
                .sorted(Comparator.comparing(HospitalApplication::submittedAt).reversed())
                .toList();
    }

    public Optional<HospitalApplication> findById(String id) {
        return Optional.ofNullable(applications.get(id));
    }
}
