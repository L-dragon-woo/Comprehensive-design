package com.ohgiraffers.backend.application.domain.exception;

public class InvalidHospitalApplicationException extends RuntimeException {

    public InvalidHospitalApplicationException(String message) {
        super(message);
    }
}
