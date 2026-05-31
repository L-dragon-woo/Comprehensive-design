package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.LinkedHashMap;
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

    public AnalysisResultDocument saveAiSummary(String username, String id, String content, String mode) {
        AnalysisResultDocument document = get(username, id);
        Map<String, Object> analysis = new LinkedHashMap<>(document.getAnalysis());
        analysis.put("aiSummary", content);
        analysis.put("aiSummaryMode", mode);
        analysis.put("aiSummaryUpdatedAt", Instant.now().toString());

        Object nested = analysis.get("result");
        if (nested instanceof Map<?, ?> nestedMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            nestedMap.forEach((key, value) -> {
                if (key != null) result.put(String.valueOf(key), value);
            });
            result.put("aiSummary", content);
            result.put("aiSummaryMode", mode);
            result.put("aiSummaryUpdatedAt", Instant.now().toString());
            analysis.put("result", result);
        }

        document.setAnalysis(analysis);
        return repository.save(document);
    }
}
