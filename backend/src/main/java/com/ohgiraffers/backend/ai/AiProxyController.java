package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class AiProxyController {

    private final WebClient aiClient;
    private final ChatHistoryService chatHistoryService;
    private final AnalysisResultService analysisResultService;

    public AiProxyController(
            WebClient.Builder builder,
            AiClientProperties properties,
            ChatHistoryService chatHistoryService,
            AnalysisResultService analysisResultService
    ) {
        this.aiClient = builder.baseUrl(properties.baseUrl()).build();
        this.chatHistoryService = chatHistoryService;
        this.analysisResultService = analysisResultService;
    }

    @GetMapping("/api/ai/health")
    public Map<String, Object> health() {
        return aiClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    @PostMapping("/api/consultations/messages")
    public Map<String, Object> sendConsultationMessage(@org.springframework.web.bind.annotation.RequestBody ChatMessageRequest request, Authentication authentication) {
        Map<String, Object> response = aiClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (response != null) {
            String username = authentication == null ? "anonymous" : authentication.getName();
            ChatHistoryDocument saved = chatHistoryService.saveTurn(username, request, response);
            response.putIfAbsent("messageId", saved.getId());
            response.putIfAbsent("createdAt", instantString(saved.getCreatedAt()));
        }
        return response;
    }

    private String instantString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    @PostMapping(value = "/api/analyses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> analyzeImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam(defaultValue = "female") String gender,
            Authentication authentication
    ) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("image", image.getResource())
                .filename(image.getOriginalFilename() == null ? "image.jpg" : image.getOriginalFilename())
                .contentType(MediaType.parseMediaType(image.getContentType() == null ? "image/jpeg" : image.getContentType()));
        bodyBuilder.part("gender", gender);

        Map<String, Object> response = aiClient.post()
                .uri("/api/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (response != null) {
            AnalysisResultDocument saved = analysisResultService.save(authentication.getName(), response);
            response.putIfAbsent("analysisId", saved.getId());
            response.putIfAbsent("createdAt", instantString(saved.getCreatedAt()));
        }
        return response;
    }

    @GetMapping("/api/analyses")
    public List<SavedAnalysisResponse> listAnalyses(Authentication authentication) {
        return analysisResultService.list(authentication.getName()).stream()
                .map(this::toSavedAnalysis)
                .toList();
    }

    @GetMapping("/api/analyses/{id}")
    public SavedAnalysisResponse getAnalysis(@PathVariable String id, Authentication authentication) {
        return toSavedAnalysis(analysisResultService.get(authentication.getName(), id));
    }

    private SavedAnalysisResponse toSavedAnalysis(AnalysisResultDocument document) {
        return new SavedAnalysisResponse(
                document.getId(),
                instantString(document.getCreatedAt()),
                document.getAnalysis()
        );
    }

    public record SavedAnalysisResponse(String analysisId, String createdAt, Map<String, Object> analysis) {}

    public record AnalysisSummaryRequest(Map<String, Object> analysis, String gender, String sessionId, String analysisId) {}

    @PostMapping("/api/analyses/summary")
    public Map<String, Object> analysesSummary(
            @org.springframework.web.bind.annotation.RequestBody AnalysisSummaryRequest request,
            Authentication authentication) {
        Map<String, Object> response = aiClient.post()
                .uri("/api/analyses/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (response != null && request.analysisId() != null && !request.analysisId().isBlank()) {
            Object content = response.get("content");
            if (content instanceof String summary && !summary.isBlank()) {
                String username = authentication == null ? "anonymous" : authentication.getName();
                Object mode = response.get("mode");
                analysisResultService.saveAiSummary(username, request.analysisId(), summary, mode == null ? null : String.valueOf(mode));
                response.put("analysisId", request.analysisId());
            }
        }
        return response;
    }
}
