package com.ohgiraffers.backend.consultation.domain.exception;

public class InvalidConsultationMessageException extends RuntimeException {

    public InvalidConsultationMessageException(String message) {
        super(message);
    }
}
