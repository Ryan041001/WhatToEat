package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.model.RecommendationFeedbackType;
import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

final class RecommendationInsightHeuristics {

    private RecommendationInsightHeuristics() {
    }

    static List<String> deriveCandidateSignals(String category, List<String> aiTags, String aiSummary) {
        String text = combineTexts(Stream.concat(
                Stream.of(category, aiSummary),
                aiTags == null ? Stream.empty() : aiTags.stream()).toList());
        LinkedHashSet<String> signals = new LinkedHashSet<>(derivePreferredTags(List.of(text)));
        if (containsAny(text, "健身", "减脂", "高蛋白", "轻食", "鸡胸", "蛋白")) {
            signals.add("健身友好");
        }
        if (containsAny(text, "快餐")) {
            signals.add("快餐");
        }
        return List.copyOf(signals);
    }

    static List<String> deriveRecommendedScenarios(
            Integer avgPerCapitaPrice,
            List<String> aiTags,
            String aiSummary,
            List<String> reviewContents) {
        String text = combineTexts(Stream.concat(
                Stream.concat(
                        aiTags == null ? Stream.empty() : aiTags.stream(),
                        Stream.of(aiSummary)),
                reviewContents == null ? Stream.empty() : reviewContents.stream()).toList());
        LinkedHashSet<String> scenarios = new LinkedHashSet<>();
        if (containsAny(text, "工作日", "午餐", "出餐快", "上菜快", "方便")) {
            scenarios.add("工作日午餐");
            scenarios.add("一个人快吃");
        }
        if (containsAny(text, "热汤", "汤", "面", "粉", "粥", "热乎")) {
            scenarios.add("想吃热汤时");
        }
        if (avgPerCapitaPrice != null && avgPerCapitaPrice <= 35) {
            scenarios.add("预算 30 左右");
        }
        if (containsAny(text, "轻食", "高蛋白", "减脂", "健身")) {
            scenarios.add("健身期更友好");
        }
        if (containsAny(text, "安静", "久坐", "聊天", "学习", "办公", "咖啡")) {
            scenarios.add("想坐久一点");
        }
        return scenarios.stream().limit(5).toList();
    }

    static List<String> derivePreferredTags(Collection<String> texts) {
        String combined = combineTexts(texts);
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (containsAny(combined, "清淡", "轻食", "减脂", "爽口", "少油", "轻盈")) {
            tags.add("清淡");
        }
        if (containsAny(combined, "高蛋白", "健身", "鸡胸", "蛋白", "牛肉", "虾", "减脂")) {
            tags.add("高蛋白");
        }
        if (containsAny(combined, "热汤", "汤", "面", "粉", "粥", "砂锅", "热乎")) {
            tags.add("热汤");
        }
        if (containsAny(combined, "出餐快", "上菜快", "工作日午餐", "不用等", "方便")) {
            tags.add("出餐快");
        }
        if (containsAny(combined, "安静", "久坐", "聊天", "学习", "办公", "咖啡")) {
            tags.add("适合久坐");
        }
        if (containsAny(combined, "性价比", "实惠", "划算", "便宜", "学生友好")) {
            tags.add("性价比");
        }
        return List.copyOf(tags);
    }

    static List<String> deriveAvoidedTags(Collection<String> texts) {
        String combined = combineTexts(texts);
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (containsAny(combined, "太油", "油腻", "很腻")) {
            tags.add("太油");
        }
        if (containsAny(combined, "太贵", "贵", "预算高", "控制预算")) {
            tags.add("太贵");
        }
        if (containsAny(combined, "太远", "远", "距离远")) {
            tags.add("太远");
        }
        if (containsAny(combined, "快餐")) {
            tags.add("快餐");
        }
        if (containsAny(combined, "不卫生", "卫生差", "脏")) {
            tags.add("不卫生");
        }
        return List.copyOf(tags);
    }

    static List<String> deriveRecentFeedbackSignals(List<RecommendationFeedbackEntity> feedbacks) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        for (RecommendationFeedbackEntity feedback : feedbacks) {
            RecommendationFeedbackType type = parseType(feedback.getFeedbackType());
            if (type == null) {
                continue;
            }
            switch (type) {
                case TOO_EXPENSIVE -> signals.add("更在意预算");
                case TOO_FAR -> signals.add("更在意距离");
                case DONT_WANT_THIS_TODAY -> signals.add("今天想换口味");
                case LOOKS_UNHYGIENIC -> signals.add("对卫生更敏感");
                case ALREADY_ATE -> signals.add("想避开最近吃过的店");
            }
        }
        return List.copyOf(signals);
    }

    static RecommendationFeedbackType parseType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        try {
            return RecommendationFeedbackType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String combineTexts(Collection<String> texts) {
        List<String> normalized = new ArrayList<>();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            String trimmed = text.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return String.join(" ", normalized);
    }
}
