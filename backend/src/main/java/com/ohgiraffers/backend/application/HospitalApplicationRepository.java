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

    // 임시 저장소입니다. 실제 DB가 붙기 전까지 서버 프로세스가 살아있는 동안만 데이터가 유지됩니다.
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
