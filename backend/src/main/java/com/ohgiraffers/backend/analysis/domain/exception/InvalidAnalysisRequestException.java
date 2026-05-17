package com.ohgiraffers.backend.analysis.domain.exception;

public class InvalidAnalysisRequestException extends RuntimeException {

    public InvalidAnalysisRequestException(String message) {
        super(message);
    }
}
