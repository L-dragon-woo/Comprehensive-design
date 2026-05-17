package com.ohgiraffers.backend.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospital-applications")
public class HospitalApplicationController {

    private final HospitalApplicationService applicationService;

    public HospitalApplicationController(HospitalApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public HospitalApplicationResponse submit(@RequestBody HospitalApplicationRequest request) {
        // 사용자가 선택한 병원에 분석 결과지 제출 요청을 생성합니다.
        return applicationService.submit(request);
    }

    @GetMapping
    public HospitalApplicationListResponse findApplications(@RequestParam(defaultValue = "false") boolean latest) {
        // 홈 화면의 최신 신청 카드와 신청 목록 화면을 같은 endpoint로 처리합니다.
        return applicationService.findApplications(latest);
    }

    @GetMapping("/{applicationId}")
    public HospitalApplicationDetailResponse getApplication(@PathVariable String applicationId) {
        // 제출 상세 화면에서 병원 연락처와 제출 항목을 함께 보여주기 위한 응답입니다.
        return applicationService.getApplication(applicationId);
    }
}
