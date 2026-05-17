package com.ohgiraffers.backend.hospital.domain.exception;

public class HospitalNotFoundException extends RuntimeException {

    public HospitalNotFoundException(String hospitalId) {
        super("Hospital not found: " + hospitalId);
    }
}
