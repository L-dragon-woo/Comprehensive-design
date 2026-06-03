package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.ohgiraffers.backend.storage.S3StorageService;

@RestController
public class AiProxyController {

    private final WebClient aiClient;
    private final ChatHistoryService chatHistoryService;
    private final AnalysisResultService analysisResultService;
    private final S3StorageService s3StorageService;

    public AiProxyController(
            WebClient.Builder builder,
            AiClientProperties properties,
            ChatHistoryService chatHistoryService,
            AnalysisResultService analysisResultService,
            S3StorageService s3StorageService
    ) {
        this.aiClient = builder.baseUrl(properties.baseUrl()).build();
        this.chatHistoryService = chatHistoryService;
        this.analysisResultService = analysisResultService;
        this.s3StorageService = s3StorageService;
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

    @GetMapping("/api/consultations/messages")
    public ConsultationHistoryResponse listConsultationMessages(
            @RequestParam(required = false) String analysisId,
            Authentication authentication
    ) {
        String username = authentication == null ? "anonymous" : authentication.getName();
        List<ChatHistoryDocument> turns = chatHistoryService.latestSessionTurns(username, analysisId);
        String sessionId = turns.stream()
                .map(ChatHistoryDocument::getSessionId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse(null);
        List<ConsultationMessageResponse> messages = turns.stream()
                .flatMap(turn -> List.of(
                        new ConsultationMessageResponse(
                                turn.getId() + "-user",
                                "user",
                                turn.getUserMessage(),
                                instantString(turn.getCreatedAt())
                        ),
                        new ConsultationMessageResponse(
                                turn.getId() + "-assistant",
                                "assistant",
                                aiContent(turn.getAiResponse()),
                                instantString(turn.getCreatedAt())
                        )
                ).stream())
                .filter(message -> message.content() != null && !message.content().isBlank())
                .toList();
        return new ConsultationHistoryResponse(sessionId, messages);
    }

    private String instantString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private String aiContent(Map<String, Object> aiResponse) {
        if (aiResponse == null) return null;
        Object content = aiResponse.get("content");
        return content instanceof String text ? text : null;
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
            String username = authentication.getName();
            AnalysisResultDocument saved = trySaveAnalysis(username, response);
            String analysisId = saved == null ? "local-" + UUID.randomUUID() : saved.getId();
            response.putIfAbsent("analysisId", analysisId);
            response.putIfAbsent("createdAt", saved == null ? Instant.now().toString() : instantString(saved.getCreatedAt()));
            response.putIfAbsent("persisted", saved != null);
            if (saved != null && s3StorageService.enabled()) {
                try {
                    S3StorageService.StoredObject imageObject = s3StorageService.uploadAnalysisImage(username, saved.getId(), image);
                    analysisResultService.saveImageObject(username, saved.getId(), imageObject.key(), imageObject.url(), imageObject.uploadedAt());
                    response.put("imageKey", imageObject.key());
                    response.put("imageUrl", imageObject.url());
                } catch (RuntimeException ignored) {
                    response.put("imageUploadStatus", "failed");
                }
            }
        }
        return response;
    }

    @PostMapping("/api/analyses/from-s3")
    public Map<String, Object> analyzeImageFromS3(
            @org.springframework.web.bind.annotation.RequestBody PresignedAnalysisRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        if (!s3StorageService.isOwnedBy(username, request.imageKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "image key is not owned by current user");
        }

        S3StorageService.DownloadedObject image = s3StorageService.download(request.imageKey());
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("image", new NamedByteArrayResource(image.bytes(), filenameOrDefault(request.filename())))
                .filename(filenameOrDefault(request.filename()))
                .contentType(MediaType.parseMediaType(image.contentType()));
        bodyBuilder.part("gender", request.gender() == null || request.gender().isBlank() ? "female" : request.gender());

        Map<String, Object> response = aiClient.post()
                .uri("/api/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (response != null) {
            AnalysisResultDocument saved = analysisResultService.save(username, response);
            String imageUrl = s3StorageService.presignedUrl(request.imageKey());
            String uploadedAt = Instant.now().toString();
            analysisResultService.saveImageObject(username, saved.getId(), request.imageKey(), imageUrl, uploadedAt);
            response.putIfAbsent("analysisId", saved.getId());
            response.putIfAbsent("createdAt", instantString(saved.getCreatedAt()));
            response.put("imageKey", request.imageKey());
            response.put("imageUrl", imageUrl);
            response.put("imageUploadedAt", uploadedAt);
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

    public record ConsultationHistoryResponse(String sessionId, List<ConsultationMessageResponse> messages) {}

    public record ConsultationMessageResponse(String id, String role, String content, String createdAt) {}

    public record PresignedAnalysisRequest(String imageKey, String gender, String filename) {}

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

    private String filenameOrDefault(String filename) {
        return filename == null || filename.isBlank() ? "capture.jpg" : filename;
    }

    private AnalysisResultDocument trySaveAnalysis(String username, Map<String, Object> response) {
        try {
            return analysisResultService.save(username, response);
        } catch (RuntimeException ignored) {
            response.put("persistenceStatus", "failed");
            return null;
        }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
