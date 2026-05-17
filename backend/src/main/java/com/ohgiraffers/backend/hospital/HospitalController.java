package com.ohgiraffers.backend.hospital;

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
        return hospitalService.findHospitals(query, lat, lng, treatments, sort);
    }

    @GetMapping("/{hospitalId}")
    public HospitalDetailResponse getHospital(@PathVariable String hospitalId) {
        return hospitalService.getHospital(hospitalId);
    }
}
