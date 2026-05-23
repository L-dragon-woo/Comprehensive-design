package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

    public AiProxyController(WebClient.Builder builder, AiClientProperties properties, ChatHistoryService chatHistoryService) {
        this.aiClient = builder.baseUrl(properties.baseUrl()).build();
        this.chatHistoryService = chatHistoryService;
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
            @RequestParam(defaultValue = "female") String gender
    ) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("image", image.getResource())
                .filename(image.getOriginalFilename() == null ? "image.jpg" : image.getOriginalFilename())
                .contentType(MediaType.parseMediaType(image.getContentType() == null ? "image/jpeg" : image.getContentType()));
        bodyBuilder.part("gender", gender);

        return aiClient.post()
                .uri("/api/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
