package com.ohgiraffers.backend.hospital;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HospitalRepository {

    // DB 연결 전까지 프론트 개발과 API 계약 검증에 사용할 병원 seed 데이터입니다.
    private final List<Hospital> hospitals = List.of(
            new Hospital(
                    "hospital_001",
                    "서울스킨 피부과의원",
                    4.8,
                    "서울 강남구 테헤란로 142",
                    "02-1234-5678",
                    37.5009,
                    127.0364,
                    List.of("리쥬란", "피코토닝", "스킨부스터"),
                    List.of(
                            new TreatmentInfo("리쥬란 힐러", "피부결과 건조 고민 상담에 적합"),
                            new TreatmentInfo("피코토닝", "색소침착과 피부 톤 상담에 적합")
                    ),
                    List.of("오늘 상담 가능", "내일 오전 가능")
            ),
            new Hospital(
                    "hospital_002",
                    "강남맑은 피부클리닉",
                    4.6,
                    "서울 강남구 강남대로 396",
                    "02-9876-5432",
                    37.4971,
                    127.0280,
                    List.of("여드름", "모공", "레이저토닝"),
                    List.of(
                            new TreatmentInfo("아쿠아필", "피지와 모공 고민 상담에 적합"),
                            new TreatmentInfo("레이저토닝", "잡티와 톤 개선 상담에 적합")
                    ),
                    List.of("오늘 오후 가능", "이번 주 토요일 가능")
            ),
            new Hospital(
                    "hospital_003",
                    "더밸런스 의원",
                    4.9,
                    "서울 서초구 서초대로 77길 55",
                    "02-2222-1004",
                    37.5027,
                    127.0249,
                    List.of("보습관리", "스킨부스터", "민감피부"),
                    List.of(
                            new TreatmentInfo("스킨부스터", "수분 부족과 탄력 고민 상담에 적합"),
                            new TreatmentInfo("진정관리", "민감도와 홍조 고민 상담에 적합")
                    ),
                    List.of("내일 오전 가능", "내일 오후 가능")
            )
    );

    public List<Hospital> findAll() {
        return hospitals;
    }

    public Optional<Hospital> findById(String id) {
        return hospitals.stream()
                .filter(hospital -> hospital.id().equals(id))
                .findFirst();
    }
}
