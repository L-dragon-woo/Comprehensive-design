package com.ohgiraffers.backend.application.domain.exception;

public class HospitalApplicationNotFoundException extends RuntimeException {

    public HospitalApplicationNotFoundException(String applicationId) {
        super("Hospital application not found: " + applicationId);
    }
}
