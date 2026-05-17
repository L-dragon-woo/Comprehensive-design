package com.ohgiraffers.backend.application;

public class InvalidHospitalApplicationException extends RuntimeException {

    public InvalidHospitalApplicationException(String message) {
        super(message);
    }
}
