package com.zjgsu.whattoeat.model.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityLifecycleTest {

    @Test
    void userEntityShouldSetDefaultsAndExposeFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 29, 9, 0);
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        entity.setOpenid("openid-1");
        entity.setNickname("Alice");
        entity.setAvatarUrl("https://example.com/avatar.png");
        entity.setCreatedAt(createdAt);

        entity.prePersist();
        LocalDateTime firstUpdatedAt = entity.getUpdatedAt();
        entity.preUpdate();

        assertEquals(1L, entity.getId());
        assertEquals("openid-1", entity.getOpenid());
        assertEquals("Alice", entity.getNickname());
        assertEquals("https://example.com/avatar.png", entity.getAvatarUrl());
        assertEquals(createdAt, entity.getCreatedAt());
        assertNotNull(firstUpdatedAt);
        assertTrue(!entity.getUpdatedAt().isBefore(firstUpdatedAt));
    }

    @Test
    void restaurantMetricSnapshotShouldApplyPersistDefaultsAndExposeFields() {
        LocalDateTime reviewAt = LocalDateTime.of(2026, 4, 28, 20, 0);
        RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
        entity.setPoiId("poi-1");
        entity.setAvgRating(new BigDecimal("4.5"));
        entity.setAvgPerCapitaPrice(32);
        entity.setAiTag1("清淡");
        entity.setAiTag2("高蛋白");
        entity.setAiSummary("适合工作日午餐");
        entity.setLastReviewAt(reviewAt);
        entity.setLastAiGeneratedAt(reviewAt.plusMinutes(5));

        entity.prePersist();
        LocalDateTime firstUpdatedAt = entity.getUpdatedAt();
        entity.preUpdate();

        assertEquals("poi-1", entity.getPoiId());
        assertEquals(0, entity.getReviewCount());
        assertEquals(new BigDecimal("4.5"), entity.getAvgRating());
        assertEquals(32, entity.getAvgPerCapitaPrice());
        assertEquals("清淡", entity.getAiTag1());
        assertEquals("高蛋白", entity.getAiTag2());
        assertEquals("适合工作日午餐", entity.getAiSummary());
        assertEquals("idle", entity.getAiStatus());
        assertEquals(reviewAt, entity.getLastReviewAt());
        assertEquals(reviewAt.plusMinutes(5), entity.getLastAiGeneratedAt());
        assertTrue(!entity.getUpdatedAt().isBefore(firstUpdatedAt));
    }

    @Test
    void recommendationFeedbackShouldPreservePresetValuesAndSetCreatedAt() {
        RecommendationFeedbackEntity entity = new RecommendationFeedbackEntity();
        entity.setId(10L);
        entity.setUserId(3L);
        entity.setPoiId("poi-2");
        entity.setPoiNameSnapshot("桂香卤味拌饭");
        entity.setFeedbackType("TOO_EXPENSIVE");
        entity.setDetail("今天预算低一点");
        entity.setRequestQuestion("午饭吃什么");

        entity.prePersist();

        assertEquals(10L, entity.getId());
        assertEquals(3L, entity.getUserId());
        assertEquals("poi-2", entity.getPoiId());
        assertEquals("桂香卤味拌饭", entity.getPoiNameSnapshot());
        assertEquals("TOO_EXPENSIVE", entity.getFeedbackType());
        assertEquals("今天预算低一点", entity.getDetail());
        assertEquals("午饭吃什么", entity.getRequestQuestion());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void reviewAndNoteEntitiesShouldRefreshUpdatedAtOnUpdate() {
        RestaurantReviewEntity review = new RestaurantReviewEntity();
        review.setId(11L);
        review.setUserId(4L);
        review.setPoiId("poi-review");
        review.setPoiNameSnapshot("兰州拉面");
        review.setRatingScore(new BigDecimal("4.0"));
        review.setPerCapitaPrice(25);
        review.setContent("汤面稳定");
        review.prePersist();
        LocalDateTime reviewUpdatedAt = review.getUpdatedAt();
        review.preUpdate();

        UserRestaurantNoteEntity note = new UserRestaurantNoteEntity();
        note.setId(12L);
        note.setUserId(4L);
        note.setPoiId("poi-note");
        note.setNote("少放辣");
        note.prePersist();
        LocalDateTime noteUpdatedAt = note.getUpdatedAt();
        note.preUpdate();

        assertEquals(11L, review.getId());
        assertEquals(4L, review.getUserId());
        assertEquals("poi-review", review.getPoiId());
        assertEquals("兰州拉面", review.getPoiNameSnapshot());
        assertEquals(new BigDecimal("4.0"), review.getRatingScore());
        assertEquals(25, review.getPerCapitaPrice());
        assertEquals("汤面稳定", review.getContent());
        assertNotNull(review.getCreatedAt());
        assertTrue(!review.getUpdatedAt().isBefore(reviewUpdatedAt));

        assertEquals(12L, note.getId());
        assertEquals(4L, note.getUserId());
        assertEquals("poi-note", note.getPoiId());
        assertEquals("少放辣", note.getNote());
        assertNotNull(note.getCreatedAt());
        assertTrue(!note.getUpdatedAt().isBefore(noteUpdatedAt));
    }

    @Test
    void blacklistAndChoiceHistoryShouldExposeFieldsAndDefaultTimestamps() {
        UserBlacklistEntity blacklist = new UserBlacklistEntity();
        blacklist.setId(13L);
        blacklist.setUserId(5L);
        blacklist.setPoiId("poi-blacklist");
        blacklist.setReason("太远");
        blacklist.prePersist();

        UserChoiceHistoryEntity history = new UserChoiceHistoryEntity();
        history.setId(14L);
        history.setUserId(5L);
        history.setPoiId("poi-history");
        history.setPoiName("米线");
        history.prePersist();

        assertEquals(13L, blacklist.getId());
        assertEquals(5L, blacklist.getUserId());
        assertEquals("poi-blacklist", blacklist.getPoiId());
        assertEquals("太远", blacklist.getReason());
        assertNotNull(blacklist.getCreatedAt());

        assertEquals(14L, history.getId());
        assertEquals(5L, history.getUserId());
        assertEquals("poi-history", history.getPoiId());
        assertEquals("米线", history.getPoiName());
        assertNotNull(history.getChosenAt());
    }
}
