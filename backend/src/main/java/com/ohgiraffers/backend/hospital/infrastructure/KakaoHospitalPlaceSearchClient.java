package com.ohgiraffers.backend.hospital.infrastructure;

import com.ohgiraffers.backend.hospital.domain.model.Hospital;
import com.ohgiraffers.backend.hospital.domain.repository.HospitalPlaceSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Component
public class KakaoHospitalPlaceSearchClient implements HospitalPlaceSearchClient {

    private static final String DEFAULT_KEYWORD = "피부과";
    private static final int SEARCH_RADIUS_METERS = 5_000;

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoHospitalPlaceSearchClient(
            RestClient.Builder restClientBuilder,
            @Value("${skinai.kakao.rest-api-key:}") String restApiKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://dapi.kakao.com")
                .build();
        this.restApiKey = restApiKey;
    }

    @Override
    public List<Hospital> searchNearby(String query, double latitude, double longitude) {
        if (!StringUtils.hasText(restApiKey)) {
            return List.of();
        }

        String keyword = StringUtils.hasText(query) ? query.trim() : DEFAULT_KEYWORD;

        KakaoKeywordSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", keyword)
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .queryParam("radius", SEARCH_RADIUS_METERS)
                        .queryParam("sort", "distance")
                        .build())
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .body(KakaoKeywordSearchResponse.class);

        if (response == null || response.documents() == null) {
            return List.of();
        }

        return response.documents().stream()
                .filter(Objects::nonNull)
                .filter(KakaoPlaceDocument::hasCoordinates)
                .map(KakaoPlaceDocument::toHospital)
                .toList();
    }

    private record KakaoKeywordSearchResponse(List<KakaoPlaceDocument> documents) {
    }

    private record KakaoPlaceDocument(
            String id,
            String place_name,
            String category_name,
            String phone,
            String address_name,
            String road_address_name,
            String x,
            String y
    ) {

        boolean hasCoordinates() {
            return StringUtils.hasText(id) && StringUtils.hasText(place_name) && parseDouble(y) != null && parseDouble(x) != null;
        }

        Hospital toHospital() {
            return new Hospital(
                    "kakao-" + id,
                    place_name,
                    0.0,
                    StringUtils.hasText(road_address_name) ? road_address_name : address_name,
                    StringUtils.hasText(phone) ? phone : "전화번호 미제공",
                    parseDouble(y),
                    parseDouble(x),
                    List.of(resolveSpecialty()),
                    List.of(),
                    List.of("지도 정보 확인 필요")
            );
        }

        private String resolveSpecialty() {
            if (!StringUtils.hasText(category_name)) {
                return "병원";
            }

            String[] parts = category_name.split(">");
            return parts.length == 0 ? "병원" : parts[parts.length - 1].trim();
        }

        private static Double parseDouble(String value) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
    }
}
