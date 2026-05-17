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
        return applicationService.submit(request);
    }

    @GetMapping
    public HospitalApplicationListResponse findApplications(@RequestParam(defaultValue = "false") boolean latest) {
        return applicationService.findApplications(latest);
    }

    @GetMapping("/{applicationId}")
    public HospitalApplicationDetailResponse getApplication(@PathVariable String applicationId) {
        return applicationService.getApplication(applicationId);
    }
}
