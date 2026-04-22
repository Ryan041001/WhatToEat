package com.zjgsu.whattoeat.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_feedback")
public class RecommendationFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "poi_id", length = 64)
    private String poiId;

    @Column(name = "poi_name_snapshot", length = 128)
    private String poiNameSnapshot;

    @Column(name = "feedback_type", nullable = false, length = 32)
    private String feedbackType;

    @Column(name = "detail", length = 255)
    private String detail;

    @Column(name = "request_question", length = 255)
    private String requestQuestion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPoiId() {
        return poiId;
    }

    public void setPoiId(String poiId) {
        this.poiId = poiId;
    }

    public String getPoiNameSnapshot() {
        return poiNameSnapshot;
    }

    public void setPoiNameSnapshot(String poiNameSnapshot) {
        this.poiNameSnapshot = poiNameSnapshot;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getRequestQuestion() {
        return requestQuestion;
    }

    public void setRequestQuestion(String requestQuestion) {
        this.requestQuestion = requestQuestion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
