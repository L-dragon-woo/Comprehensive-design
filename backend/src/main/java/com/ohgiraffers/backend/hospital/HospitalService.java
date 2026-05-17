package com.ohgiraffers.backend.hospital;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class HospitalService {

    private static final double DEFAULT_LATITUDE = 37.4979;
    private static final double DEFAULT_LONGITUDE = 127.0276;

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    public HospitalListResponse findHospitals(String query, Double lat, Double lng, String treatments, String sort) {
        double baseLat = lat == null ? DEFAULT_LATITUDE : lat;
        double baseLng = lng == null ? DEFAULT_LONGITUDE : lng;
        List<String> requestedTreatments = splitCsv(treatments);

        List<HospitalSummaryResponse> items = hospitalRepository.findAll().stream()
                .filter(hospital -> matchesQuery(hospital, query))
                .map(hospital -> toSummary(hospital, baseLat, baseLng, requestedTreatments))
                .filter(summary -> requestedTreatments.isEmpty() || !summary.matchedTreatments().isEmpty())
                .sorted(resolveSort(sort))
                .toList();

        return new HospitalListResponse(items, items.size());
    }

    public HospitalDetailResponse getHospital(String hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new HospitalNotFoundException(hospitalId));

        return new HospitalDetailResponse(
                hospital.id(),
                hospital.name(),
                hospital.rating(),
                hospital.address(),
                hospital.phone(),
                hospital.specialties(),
                hospital.treatments(),
                hospital.availableTimes()
        );
    }

    public Hospital requireHospital(String hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new HospitalNotFoundException(hospitalId));
    }

    private HospitalSummaryResponse toSummary(Hospital hospital, double baseLat, double baseLng, List<String> requestedTreatments) {
        long distanceMeters = calculateDistanceMeters(baseLat, baseLng, hospital.latitude(), hospital.longitude());
        List<String> matchedTreatments = hospital.treatments().stream()
                .map(TreatmentInfo::name)
                .filter(name -> requestedTreatments.isEmpty() || containsAny(name, requestedTreatments))
                .toList();

        return new HospitalSummaryResponse(
                hospital.id(),
                hospital.name(),
                formatDistance(distanceMeters),
                distanceMeters,
                hospital.rating(),
                hospital.address(),
                hospital.specialties(),
                matchedTreatments,
                hospital.availableTimes().isEmpty() ? "상담 가능" : hospital.availableTimes().get(0),
                hospital.phone()
        );
    }

    private boolean matchesQuery(Hospital hospital, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase();
        return hospital.name().toLowerCase().contains(normalizedQuery)
                || hospital.address().toLowerCase().contains(normalizedQuery)
                || hospital.specialties().stream().anyMatch(value -> value.toLowerCase().contains(normalizedQuery))
                || hospital.treatments().stream().anyMatch(value -> value.name().toLowerCase().contains(normalizedQuery));
    }

    private Comparator<HospitalSummaryResponse> resolveSort(String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(HospitalSummaryResponse::rating).reversed();
        }
        return Comparator.comparingLong(HospitalSummaryResponse::distanceMeters);
    }

    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean containsAny(String value, List<String> candidates) {
        String normalizedValue = value.toLowerCase();
        return candidates.stream()
                .map(String::toLowerCase)
                .anyMatch(candidate -> normalizedValue.contains(candidate) || candidate.contains(normalizedValue));
    }

    private long calculateDistanceMeters(double startLat, double startLng, double endLat, double endLng) {
        // Haversine 공식을 사용해 현재 위치 기준 병원 거리를 계산합니다.
        double earthRadiusMeters = 6_371_000;
        double latDistance = Math.toRadians(endLat - startLat);
        double lngDistance = Math.toRadians(endLng - startLng);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(earthRadiusMeters * c);
    }

    private String formatDistance(long distanceMeters) {
        if (distanceMeters < 1000) {
            return distanceMeters + "m";
        }
        return String.format("%.1fkm", distanceMeters / 1000.0);
    }
}
