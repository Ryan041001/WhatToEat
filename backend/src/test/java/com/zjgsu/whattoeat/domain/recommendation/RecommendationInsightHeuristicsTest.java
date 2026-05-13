package com.zjgsu.whattoeat.domain.recommendation;

import com.zjgsu.whattoeat.model.RecommendationFeedbackType;
import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecommendationInsightHeuristicsTest {

    @Test
    void derivePreferredTagsShouldKeepStableOrderForMatchedSignals() {
        List<String> tags = RecommendationInsightHeuristics.derivePreferredTags(List.of(
                "今天想吃清淡少油的轻食",
                "最好有鸡胸牛肉这种高蛋白",
                "出餐快一点，学生友好"));

        assertEquals(List.of("清淡", "高蛋白", "出餐快", "性价比"), tags);
    }

    @Test
    void deriveAvoidedTagsShouldMapNegativeLanguageToAvoidTags() {
        List<String> tags = RecommendationInsightHeuristics.deriveAvoidedTags(List.of(
                "这家有点太油，而且太贵",
                "距离远，卫生差"));

        assertEquals(List.of("太油", "太贵", "太远", "不卫生"), tags);
    }

    @Test
    void deriveRecommendedScenariosShouldCombinePriceTagsSummaryAndReviews() {
        List<String> scenarios = RecommendationInsightHeuristics.deriveRecommendedScenarios(
                32,
                List.of("轻食", "高蛋白"),
                "出餐快，适合工作日午餐",
                List.of("有热汤，店里安静可以久坐"));

        assertEquals(List.of("工作日午餐", "一个人快吃", "想吃热汤时", "预算 30 左右", "健身期更友好"), scenarios);
    }

    @Test
    void deriveCandidateSignalsShouldIncludeCategoryAndSummarySignals() {
        List<String> signals = RecommendationInsightHeuristics.deriveCandidateSignals(
                "快餐简餐",
                List.of("轻食", "高蛋白"),
                "鸡胸饭适合健身减脂");

        assertEquals(List.of("清淡", "高蛋白", "健身友好", "快餐"), signals);
    }

    @Test
    void deriveRecentFeedbackSignalsShouldIgnoreUnknownTypesAndMapKnownTypes() {
        List<RecommendationFeedbackEntity> feedbacks = List.of(
                feedback("too_expensive"),
                feedback("TOO_FAR"),
                feedback("dont_want_this_today"),
                feedback("looks_unhygienic"),
                feedback("already_ate"),
                feedback("unknown"));

        List<String> signals = RecommendationInsightHeuristics.deriveRecentFeedbackSignals(feedbacks);

        assertEquals(List.of("更在意预算", "更在意距离", "今天想换口味", "对卫生更敏感", "想避开最近吃过的店"), signals);
    }

    @Test
    void parseTypeShouldTrimAndRejectBlankOrInvalidValues() {
        assertEquals(RecommendationFeedbackType.TOO_FAR, RecommendationInsightHeuristics.parseType(" too_far "));
        assertNull(RecommendationInsightHeuristics.parseType(null));
        assertNull(RecommendationInsightHeuristics.parseType(" "));
        assertNull(RecommendationInsightHeuristics.parseType("not-a-type"));
    }

    private RecommendationFeedbackEntity feedback(String feedbackType) {
        RecommendationFeedbackEntity entity = new RecommendationFeedbackEntity();
        entity.setFeedbackType(feedbackType);
        return entity;
    }
}
