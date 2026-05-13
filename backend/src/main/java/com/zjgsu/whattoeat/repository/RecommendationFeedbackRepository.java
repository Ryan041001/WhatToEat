package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedbackEntity, Long> {
    Page<RecommendationFeedbackEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
    List<RecommendationFeedbackEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    List<RecommendationFeedbackEntity> findByUserIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(Long userId, LocalDateTime after);
}
