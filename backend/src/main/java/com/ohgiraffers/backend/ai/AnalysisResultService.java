package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalysisResultService {
    private final AnalysisResultRepository repository;

    public AnalysisResultService(AnalysisResultRepository repository) {
        this.repository = repository;
    }

    public AnalysisResultDocument save(String username, Map<String, Object> analysis) {
        return repository.save(new AnalysisResultDocument(username, analysis, Instant.now()));
    }

    public List<AnalysisResultDocument> list(String username) {
        return repository.findByUsernameOrderByCreatedAtDesc(username);
    }

    public AnalysisResultDocument get(String username, String id) {
        return repository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "analysis result not found"));
    }
}
