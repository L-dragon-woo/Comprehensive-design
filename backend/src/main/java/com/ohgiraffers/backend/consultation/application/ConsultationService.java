package com.ohgiraffers.backend.consultation.application;

import com.ohgiraffers.backend.consultation.domain.exception.InvalidConsultationMessageException;
import com.ohgiraffers.backend.consultation.presentation.dto.ConsultationMessageRequest;
import com.ohgiraffers.backend.consultation.presentation.dto.ConsultationMessageResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConsultationService {

    public ConsultationMessageResponse reply(ConsultationMessageRequest request) {
        // AI 서버 호출 전에도 프론트 채팅 플로우를 검증할 수 있게 최소 입력만 검증합니다.
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new InvalidConsultationMessageException("message is required");
        }

        return new ConsultationMessageResponse(
                "msg_" + UUID.randomUUID().toString().substring(0, 8),
                "assistant",
                buildMockReply(request.message()),
                Instant.now()
        );
    }

    private String buildMockReply(String message) {
        String normalizedMessage = message.toLowerCase();

        // AI 연동 전 임시 로직입니다. 프론트의 상담 UX를 연결하기 위해 주요 시술 키워드만 분기합니다.
        if (normalizedMessage.contains("리쥬란")) {
            return "현재 mock 분석 결과 기준으로는 수분 부족과 피부결 고민이 보여서 리쥬란 힐러 상담을 먼저 받아보는 흐름이 자연스러워요. 민감도와 통증 정도는 병원 상담 때 꼭 같이 확인하세요.";
        }
        if (normalizedMessage.contains("피코") || normalizedMessage.contains("토닝") || normalizedMessage.contains("색소")) {
            return "색소침착이나 피부 톤이 가장 신경 쓰인다면 피코토닝 상담이 적합할 수 있어요. 시술 전후 자외선 차단과 보습 계획까지 같이 잡는 것을 권장해요.";
        }
        if (normalizedMessage.contains("모공") || normalizedMessage.contains("피지") || normalizedMessage.contains("유분")) {
            return "T존 유분과 모공 고민은 아쿠아필, 피지 관리, 레이저 계열 상담을 비교해볼 수 있어요. 자극을 줄이기 위해 최근 사용한 각질 케어 제품도 병원에 공유하세요.";
        }
        if (normalizedMessage.contains("우선") || normalizedMessage.contains("먼저")) {
            return "mock 결과만 보면 수분과 피부결 관리 상담을 우선하고, 색소 고민은 자외선 관리 계획과 함께 이어서 상담하는 순서가 무난해요.";
        }
        return "아직 AI 상담 서버가 연결되지 않아 mock 답변을 제공하고 있어요. 현재 분석 결과 기준으로 수분 부족, T존 유분, 볼 색소침착을 중심으로 병원 상담을 받아보면 좋아요.";
    }
}
