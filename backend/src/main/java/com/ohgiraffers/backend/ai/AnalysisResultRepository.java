package com.ohgiraffers.backend.ai;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnalysisResultRepository extends MongoRepository<AnalysisResultDocument, String> {
    List<AnalysisResultDocument> findByUsernameOrderByCreatedAtDesc(String username);

    Optional<AnalysisResultDocument> findByIdAndUsername(String id, String username);
}
