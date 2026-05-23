package com.ohgiraffers.backend.ai;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatHistoryRepository extends MongoRepository<ChatHistoryDocument, String> {
    List<ChatHistoryDocument> findByUsernameOrderByCreatedAtDesc(String username);

    List<ChatHistoryDocument> findByUsernameAndSessionIdOrderByCreatedAtAsc(String username, String sessionId);
}
