package com.ohgiraffers.backend.hospital;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {
    private final WebClient kakaoClient;
    private final String kakaoRestApiKey;

    public HospitalController(WebClient.Builder builder, @Value("${app.kakao.rest-api-key}") String kakaoRestApiKey) {
        this.kakaoClient = builder.baseUrl("https://dapi.kakao.com").build();
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    @GetMapping("/search")
    public List<KakaoPlace> search(
            @RequestParam(defaultValue = "피부과") String query,
            @RequestParam(required = false) String x,
            @RequestParam(required = false) String y,
            @RequestParam(defaultValue = "5000") int radius
    ) {
        if (kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "KAKAO_REST_API_KEY is not configured");
        }
        Map<String, Object> response = kakaoClient.get()
                .uri(uri -> {
                    var b = uri.path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("category_group_code", "HP8")
                            .queryParam("size", 15);
                    if (x != null && !x.isBlank() && y != null && !y.isBlank()) {
                        b.queryParam("x", x).queryParam("y", y).queryParam("radius", radius);
                    }
                    return b.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        Object documents = response == null ? null : response.get("documents");
        if (!(documents instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> toPlace((Map<?, ?>) item)).toList();
    }

    private KakaoPlace toPlace(Map<?, ?> doc) {
        return new KakaoPlace(
                String.valueOf(doc.get("id")),
                String.valueOf(doc.get("place_name")),
                String.valueOf(doc.get("road_address_name")),
                String.valueOf(doc.get("address_name")),
                String.valueOf(doc.get("phone")),
                String.valueOf(doc.get("distance")),
                String.valueOf(doc.get("x")),
                String.valueOf(doc.get("y")),
                String.valueOf(doc.get("place_url"))
        );
    }

    public record KakaoPlace(String id, String name, String roadAddress, String address, String phone, String distance, String x, String y, String placeUrl) {}
}
