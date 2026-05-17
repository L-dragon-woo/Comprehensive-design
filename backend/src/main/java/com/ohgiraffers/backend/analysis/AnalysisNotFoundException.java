package com.ohgiraffers.backend.analysis;

public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(String analysisId) {
        super("Analysis not found: " + analysisId);
    }
}
