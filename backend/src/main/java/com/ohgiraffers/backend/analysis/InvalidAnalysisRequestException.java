package com.ohgiraffers.backend.analysis;

public class InvalidAnalysisRequestException extends RuntimeException {

    public InvalidAnalysisRequestException(String message) {
        super(message);
    }
}
