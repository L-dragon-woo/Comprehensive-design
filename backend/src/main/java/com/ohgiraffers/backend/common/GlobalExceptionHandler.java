package com.ohgiraffers.backend.common;

import com.ohgiraffers.backend.analysis.domain.exception.AnalysisNotFoundException;
import com.ohgiraffers.backend.analysis.domain.exception.InvalidAnalysisRequestException;
import com.ohgiraffers.backend.application.domain.exception.HospitalApplicationNotFoundException;
import com.ohgiraffers.backend.application.domain.exception.InvalidHospitalApplicationException;
import com.ohgiraffers.backend.consultation.domain.exception.InvalidConsultationMessageException;
import com.ohgiraffers.backend.hospital.domain.exception.HospitalNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 도메인별 예외를 HTTP 상태 코드와 공통 에러 응답 형태로 변환합니다.
    @ExceptionHandler(HospitalNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(RuntimeException exception) {
        return new ApiErrorResponse(exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(HospitalApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleApplicationNotFound(RuntimeException exception) {
        return new ApiErrorResponse(exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleAnalysisNotFound(RuntimeException exception) {
        return new ApiErrorResponse(exception.getMessage(), Instant.now());
    }

    @ExceptionHandler({
            InvalidHospitalApplicationException.class,
            InvalidAnalysisRequestException.class,
            InvalidConsultationMessageException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBadRequest(RuntimeException exception) {
        return new ApiErrorResponse(exception.getMessage(), Instant.now());
    }
}
