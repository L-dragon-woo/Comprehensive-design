package com.ohgiraffers.backend.consultation.presentation;

import com.ohgiraffers.backend.consultation.application.ConsultationService;
import com.ohgiraffers.backend.consultation.presentation.dto.ConsultationMessageRequest;
import com.ohgiraffers.backend.consultation.presentation.dto.ConsultationMessageResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations/messages")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    public ConsultationMessageResponse reply(@RequestBody ConsultationMessageRequest request) {
        // 채팅 화면에서 사용자 메시지를 보내면 임시 상담 답변을 생성합니다.
        return consultationService.reply(request);
    }
}
