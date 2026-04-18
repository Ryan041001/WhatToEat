package com.zjgsu.whattoeat.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_metric_snapshot")
public class RestaurantMetricSnapshotEntity {

    @Id
    @Column(name = "poi_id", nullable = false, length = 64)
    private String poiId;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "avg_rating", precision = 2, scale = 1)
    private BigDecimal avgRating;

    @Column(name = "avg_per_capita_price")
    private Integer avgPerCapitaPrice;

    @Column(name = "ai_tag_1", length = 32)
    private String aiTag1;

    @Column(name = "ai_tag_2", length = 32)
    private String aiTag2;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_status", nullable = false, length = 16)
    private String aiStatus;

    @Column(name = "last_review_at")
    private LocalDateTime lastReviewAt;

    @Column(name = "last_ai_generated_at")
    private LocalDateTime lastAiGeneratedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (reviewCount == null) {
            reviewCount = 0;
        }
        if (aiStatus == null || aiStatus.isBlank()) {
            aiStatus = "idle";
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getPoiId() {
        return poiId;
    }

    public void setPoiId(String poiId) {
        this.poiId = poiId;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Integer getAvgPerCapitaPrice() {
        return avgPerCapitaPrice;
    }

    public void setAvgPerCapitaPrice(Integer avgPerCapitaPrice) {
        this.avgPerCapitaPrice = avgPerCapitaPrice;
    }

    public String getAiTag1() {
        return aiTag1;
    }

    public void setAiTag1(String aiTag1) {
        this.aiTag1 = aiTag1;
    }

    public String getAiTag2() {
        return aiTag2;
    }

    public void setAiTag2(String aiTag2) {
        this.aiTag2 = aiTag2;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(String aiStatus) {
        this.aiStatus = aiStatus;
    }

    public LocalDateTime getLastReviewAt() {
        return lastReviewAt;
    }

    public void setLastReviewAt(LocalDateTime lastReviewAt) {
        this.lastReviewAt = lastReviewAt;
    }

    public LocalDateTime getLastAiGeneratedAt() {
        return lastAiGeneratedAt;
    }

    public void setLastAiGeneratedAt(LocalDateTime lastAiGeneratedAt) {
        this.lastAiGeneratedAt = lastAiGeneratedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
