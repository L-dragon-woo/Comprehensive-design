package com.ohgiraffers.backend.hospital;

public class HospitalNotFoundException extends RuntimeException {

    public HospitalNotFoundException(String hospitalId) {
        super("Hospital not found: " + hospitalId);
    }
}
