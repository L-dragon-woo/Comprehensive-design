package com.ohgiraffers.backend.consultation;

public class InvalidConsultationMessageException extends RuntimeException {

    public InvalidConsultationMessageException(String message) {
        super(message);
    }
}
