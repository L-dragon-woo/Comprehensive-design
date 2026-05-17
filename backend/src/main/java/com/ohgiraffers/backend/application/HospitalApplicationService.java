package com.ohgiraffers.backend.application;

import com.ohgiraffers.backend.hospital.Hospital;
import com.ohgiraffers.backend.hospital.HospitalService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class HospitalApplicationService {

    private final HospitalApplicationRepository applicationRepository;
    private final HospitalService hospitalService;

    public HospitalApplicationService(HospitalApplicationRepository applicationRepository, HospitalService hospitalService) {
        this.applicationRepository = applicationRepository;
        this.hospitalService = hospitalService;
    }

    public HospitalApplicationResponse submit(HospitalApplicationRequest request) {
        validateSubmitRequest(request);
        // 존재하는 병원에만 제출할 수 있게 병원 도메인의 조회 로직을 재사용합니다.
        Hospital hospital = hospitalService.requireHospital(request.hospitalId());

        HospitalApplication application = new HospitalApplication(
                "application_" + UUID.randomUUID().toString().substring(0, 8),
                request.analysisId(),
                hospital.id(),
                hospital.name(),
                Instant.now(),
                ApplicationStatus.submitted,
                List.copyOf(request.includedItems())
        );

        return toResponse(applicationRepository.save(application));
    }

    public HospitalApplicationListResponse findApplications(boolean latest) {
        List<HospitalApplication> applications = applicationRepository.findAllLatestFirst();
        if (latest && !applications.isEmpty()) {
            // latest=true는 홈 화면 카드용으로 가장 최근 제출 1건만 반환합니다.
            applications = applications.subList(0, 1);
        }

        List<HospitalApplicationListItemResponse> items = applications.stream()
                .map(this::toListItem)
                .toList();
        return new HospitalApplicationListResponse(items, items.size());
    }

    public HospitalApplicationDetailResponse getApplication(String applicationId) {
        HospitalApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new HospitalApplicationNotFoundException(applicationId));
        // 신청 저장 시점 이후 병원 상세 정보가 바뀔 수 있으므로 상세 조회 시 병원 정보를 다시 읽습니다.
        Hospital hospital = hospitalService.requireHospital(application.hospitalId());

        return new HospitalApplicationDetailResponse(
                application.id(),
                application.analysisId(),
                new HospitalSummaryForApplicationResponse(hospital.id(), hospital.name(), hospital.phone(), hospital.address()),
                application.submittedAt(),
                application.status(),
                application.includedItems()
        );
    }

    private void validateSubmitRequest(HospitalApplicationRequest request) {
        // 분석 결과지는 민감한 정보이므로 제출 동의가 true일 때만 저장합니다.
        if (request == null || !request.consent()) {
            throw new InvalidHospitalApplicationException("consent must be true");
        }
        if (!StringUtils.hasText(request.analysisId())) {
            throw new InvalidHospitalApplicationException("analysisId is required");
        }
        if (!StringUtils.hasText(request.hospitalId())) {
            throw new InvalidHospitalApplicationException("hospitalId is required");
        }
        if (request.includedItems() == null || request.includedItems().isEmpty()) {
            throw new InvalidHospitalApplicationException("includedItems is required");
        }
    }

    private HospitalApplicationResponse toResponse(HospitalApplication application) {
        return new HospitalApplicationResponse(
                application.id(),
                application.analysisId(),
                application.hospitalId(),
                application.hospitalName(),
                application.submittedAt(),
                application.status(),
                application.includedItems()
        );
    }

    private HospitalApplicationListItemResponse toListItem(HospitalApplication application) {
        return new HospitalApplicationListItemResponse(
                application.id(),
                application.hospitalName(),
                application.submittedAt(),
                application.status(),
                application.includedItems()
        );
    }
}
