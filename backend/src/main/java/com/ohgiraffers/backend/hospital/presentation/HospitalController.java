package com.ohgiraffers.backend.hospital.presentation;

import com.ohgiraffers.backend.hospital.application.HospitalService;
import com.ohgiraffers.backend.hospital.presentation.dto.HospitalDetailResponse;
import com.ohgiraffers.backend.hospital.presentation.dto.HospitalListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public HospitalListResponse findHospitals(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String treatments,
            @RequestParam(required = false) String sort
    ) {
        // 병원 목록 화면에서 검색어, 위치, 추천 시술명, 정렬 조건을 한 번에 전달받습니다.
        return hospitalService.findHospitals(query, lat, lng, treatments, sort);
    }

    @GetMapping("/{hospitalId}")
    public HospitalDetailResponse getHospital(@PathVariable String hospitalId) {
        // 병원 상세 화면에서 필요한 기본 정보와 제공 시술 정보를 반환합니다.
        return hospitalService.getHospital(hospitalId);
    }
}
