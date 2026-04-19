package com.zjgsu.whattoeat.application.recommendation;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.domain.recommendation.RecommendationDomainService;
import com.zjgsu.whattoeat.infrastructure.ai.AiAssistantClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.service.application.UserPreferenceProfileApplicationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class RecommendationApplicationService {

    private static final int RANDOM_CANDIDATE_SIZE = 20;
    private static final int DEFAULT_ASK_RESULT_SIZE = 3;
    private static final int MIN_ASK_CANDIDATE_SIZE = 12;

    private final AiAssistantClient aiAssistantClient;
    private final RecommendationDomainService recommendationDomainService;
    private final UserPreferenceProfileApplicationService userPreferenceProfileApplicationService;
    private final MeterRegistry meterRegistry;
    private final RecommendationCandidateLoader candidateLoader;
    private final RecommendationCardAssembler cardAssembler;
    private final Clock clock;

    public RecommendationApplicationService(
            AiAssistantClient aiAssistantClient,
            RecommendationDomainService recommendationDomainService,
            UserPreferenceProfileApplicationService userPreferenceProfileApplicationService,
            MeterRegistry meterRegistry,
            RecommendationCandidateLoader candidateLoader,
            RecommendationCardAssembler cardAssembler,
            Clock clock) {
        this.aiAssistantClient = aiAssistantClient;
        this.recommendationDomainService = recommendationDomainService;
        this.userPreferenceProfileApplicationService = userPreferenceProfileApplicationService;
        this.meterRegistry = meterRegistry;
        this.candidateLoader = candidateLoader;
        this.cardAssembler = cardAssembler;
        this.clock = clock;
    }

    public RecommendationResult recommendRandom(Long userId, double longitude, double latitude, int radius) {
        try {
            List<AmapPoi> candidates = candidateLoader.load(
                    userId,
                    longitude,
                    latitude,
                    radius,
                    RANDOM_CANDIDATE_SIZE,
                    Set.of());
            AmapPoi selected = recommendationDomainService.pickRandom(candidates);
            incrementRequestCounter("random", "success");
            return new RecommendationResult(
                    selected.poiId(),
                    selected.name(),
                    selected.address(),
                    selected.longitude(),
                    selected.latitude(),
                    selected.category(),
                    selected.distance(),
                    "符合筛选条件的随机结果");
        } catch (BusinessException e) {
            incrementRequestCounter("random", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("random", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    public List<AmapPoi> recommendCards(Long userId, double longitude, double latitude, int radius, int size) {
        try {
            List<AmapPoi> result = candidateLoader.load(userId, longitude, latitude, radius, size, Set.of());
            incrementRequestCounter("cards", "success");
            return result;
        } catch (BusinessException e) {
            incrementRequestCounter("cards", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("cards", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    public AskRecommendationResult ask(
            Long userId,
            double longitude,
            double latitude,
            int radius,
            int size,
            String question,
            AskContext context) {
        return askComputed(userId, longitude, latitude, radius, size, question, context, "ask").result();
    }

    public void askStream(
            Long userId,
            double longitude,
            double latitude,
            int radius,
            int size,
            String question,
            AskContext context,
            Consumer<AskStreamEvent> eventConsumer) {
        try {
            int normalizedSize = size > 0 ? size : DEFAULT_ASK_RESULT_SIZE;
            int requestedCandidateSize = Math.max(normalizedSize * 2, MIN_ASK_CANDIDATE_SIZE);
            List<AmapPoi> candidates = candidateLoader.load(
                    userId,
                    longitude,
                    latitude,
                    radius,
                    requestedCandidateSize,
                    Set.copyOf(context.rejectedPoiIds()));
            RecommendationCardAssembler.EnrichedRecommendationCandidates enriched = cardAssembler.enrichCandidates(candidates);
            eventConsumer.accept(new AskStreamEvent(
                    "retrieval.completed",
                    Map.of("candidateCount", enriched.cards().size())));

            Map<String, RecommendationCandidateCard> candidateByPoiId = enriched.cards().stream()
                    .collect(Collectors.toMap(RecommendationCandidateCard::poiId, candidate -> candidate));
            AtomicInteger emittedCardCount = new AtomicInteger();
            Set<String> emittedPoiIds = new java.util.HashSet<>();
            StringBuilder streamedAnswer = new StringBuilder();
            final String[] finalAnswer = {null};
            final boolean[] doneReceived = {false};

            aiAssistantClient.streamRecommend(
                    new AiAssistantClient.RecommendationRequest(
                            question,
                            enriched.aiCandidates(),
                            buildAiContext(userId, context)),
                    upstreamEvent -> {
                        switch (upstreamEvent.name()) {
                            case "tool.call" -> emitRecommendationCard(
                                    upstreamEvent.data(),
                                    candidateByPoiId,
                                    normalizedSize,
                                    emittedCardCount,
                                    emittedPoiIds,
                                    eventConsumer);
                            case "answer.delta" -> {
                                appendAnswerDelta(streamedAnswer, upstreamEvent.data());
                                eventConsumer.accept(new AskStreamEvent(upstreamEvent.name(), upstreamEvent.data()));
                            }
                            case "answer.done" -> {
                                finalAnswer[0] = extractAnswer(upstreamEvent.data(), streamedAnswer);
                                eventConsumer.accept(new AskStreamEvent(upstreamEvent.name(), upstreamEvent.data()));
                            }
                            case "done" -> {
                                doneReceived[0] = true;
                                eventConsumer.accept(new AskStreamEvent(upstreamEvent.name(), upstreamEvent.data()));
                            }
                            case "error" -> eventConsumer.accept(new AskStreamEvent("error", upstreamEvent.data()));
                            default -> {
                                // Ignore unknown upstream events for forward compatibility.
                            }
                        }
                    });
            emitFallbackRecommendationCards(
                    enriched.cards(),
                    normalizedSize,
                    emittedCardCount,
                    emittedPoiIds,
                    finalAnswer[0] == null || finalAnswer[0].isBlank()
                            ? streamedAnswer.toString().trim()
                            : finalAnswer[0],
                    eventConsumer);
            if (!doneReceived[0]) {
                eventConsumer.accept(new AskStreamEvent("done", Map.of("finishReason", "stop")));
            }
            incrementRequestCounter("ask-stream", "success");
        } catch (BusinessException e) {
            incrementRequestCounter("ask-stream", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("ask-stream", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    private AskRecommendationComputation askComputed(
            Long userId,
            double longitude,
            double latitude,
            int radius,
            int size,
            String question,
            AskContext context,
            String endpoint) {
        try {
            int normalizedSize = size > 0 ? size : DEFAULT_ASK_RESULT_SIZE;
            int requestedCandidateSize = Math.max(normalizedSize * 2, MIN_ASK_CANDIDATE_SIZE);
            List<AmapPoi> candidates = candidateLoader.load(
                    userId,
                    longitude,
                    latitude,
                    radius,
                    requestedCandidateSize,
                    Set.copyOf(context.rejectedPoiIds()));
            RecommendationCardAssembler.EnrichedRecommendationCandidates enriched = cardAssembler.enrichCandidates(candidates);
            AiAssistantClient.RecommendationAdvice advice = aggregateRecommendationStream(
                    new AiAssistantClient.RecommendationRequest(
                            question,
                            enriched.aiCandidates(),
                            buildAiContext(userId, context)));

            List<RecommendationCandidateCard> orderedRecommendations = cardAssembler.orderRecommendations(
                    enriched.cards(),
                    advice.choices(),
                    normalizedSize);
            incrementRequestCounter(endpoint, "success");
            return new AskRecommendationComputation(
                    requestedCandidateSize,
                    enriched.cards().size(),
                    new AskRecommendationResult(advice.answer(), orderedRecommendations));
        } catch (BusinessException e) {
            incrementRequestCounter(endpoint, e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter(endpoint, ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    private AiAssistantClient.RecommendationAdvice aggregateRecommendationStream(
            AiAssistantClient.RecommendationRequest request) {
        StringBuilder streamedAnswer = new StringBuilder();
        Map<Integer, AiAssistantClient.RecommendationChoice> rankedChoices = new java.util.TreeMap<>();
        List<AiAssistantClient.RecommendationChoice> unrankedChoices = new ArrayList<>();
        final String[] finalAnswer = {null};

        aiAssistantClient.streamRecommend(request, upstreamEvent -> {
            switch (upstreamEvent.name()) {
                case "answer.delta" -> appendAnswerDelta(streamedAnswer, upstreamEvent.data());
                case "answer.done" -> finalAnswer[0] = extractAnswer(upstreamEvent.data(), streamedAnswer);
                case "tool.call" -> collectRecommendationChoice(upstreamEvent.data(), rankedChoices, unrankedChoices);
                case "error" -> throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
                default -> {
                    // Ignore unknown upstream events for forward compatibility.
                }
            }
        });

        List<AiAssistantClient.RecommendationChoice> orderedChoices = new ArrayList<>(rankedChoices.values());
        orderedChoices.addAll(unrankedChoices);
        String answer = finalAnswer[0] == null || finalAnswer[0].isBlank()
                ? streamedAnswer.toString().trim()
                : finalAnswer[0];
        return new AiAssistantClient.RecommendationAdvice(answer, List.copyOf(orderedChoices));
    }

    @SuppressWarnings("unchecked")
    private void collectRecommendationChoice(
            Map<String, Object> upstreamData,
            Map<Integer, AiAssistantClient.RecommendationChoice> rankedChoices,
            List<AiAssistantClient.RecommendationChoice> unrankedChoices) {
        Object toolName = upstreamData.get("toolName");
        if (!(toolName instanceof String tool) || !"show_restaurant_card".equals(tool)) {
            return;
        }
        Object argumentsObject = upstreamData.get("arguments");
        if (!(argumentsObject instanceof Map<?, ?> rawArguments)) {
            return;
        }
        Map<String, Object> arguments = (Map<String, Object>) rawArguments;
        Object poiIdValue = arguments.get("poiId");
        if (!(poiIdValue instanceof String poiId) || poiId.isBlank()) {
            return;
        }
        String reason = arguments.get("reason") instanceof String text ? text : null;
        AiAssistantClient.RecommendationChoice choice = new AiAssistantClient.RecommendationChoice(poiId, reason);
        Object rankValue = arguments.get("rank");
        if (rankValue instanceof Number number && number.intValue() > 0) {
            rankedChoices.putIfAbsent(number.intValue(), choice);
            return;
        }
        unrankedChoices.add(choice);
    }

    private void appendAnswerDelta(StringBuilder streamedAnswer, Map<String, Object> upstreamData) {
        Object delta = upstreamData.get("delta");
        if (delta instanceof String text) {
            streamedAnswer.append(text);
        }
    }

    private String extractAnswer(Map<String, Object> upstreamData, StringBuilder streamedAnswer) {
        Object answer = upstreamData.get("answer");
        if (answer instanceof String text && !text.isBlank()) {
            return text;
        }
        return streamedAnswer.toString().trim();
    }

    private AiAssistantClient.RecommendationContext buildAiContext(Long userId, AskContext requestContext) {
        String temporalContext = buildTemporalContext();
        if (userId == null) {
            return new AiAssistantClient.RecommendationContext(
                    requestContext.previousQuestion(),
                    requestContext.rejectedPoiIds(),
                    requestContext.selectedPoiIds(),
                    requestContext.userSignals(),
                    temporalContext,
                    null,
                    List.of(),
                    List.of(),
                    List.of());
        }
        UserPreferenceProfileApplicationService.PreferenceProfile profile =
                userPreferenceProfileApplicationService.getPreferenceProfile(userId);
        return new AiAssistantClient.RecommendationContext(
                requestContext.previousQuestion(),
                requestContext.rejectedPoiIds(),
                requestContext.selectedPoiIds(),
                requestContext.userSignals(),
                temporalContext,
                profile.summary(),
                profile.preferredTags(),
                profile.avoidedTags(),
                profile.recentFeedbackSignals());
    }

    private String buildTemporalContext() {
        LocalDateTime now = LocalDateTime.now(clock);
        int hour = now.getHour();
        String mealWindow = switch (hour) {
            case 5, 6, 7, 8, 9 -> "早餐";
            case 10, 11, 12, 13 -> "午餐";
            case 14, 15, 16 -> "下午茶或轻食";
            case 17, 18, 19, 20 -> "晚餐";
            default -> "夜宵或随便吃点";
        };
        return "%d月%d日，%s，当前时间 %02d:%02d。这个时间点更像%s场景，但不要机械套模板，仍然要先看候选餐厅本身是否匹配。"
                .formatted(
                        now.getMonthValue(),
                        now.getDayOfMonth(),
                        weekdayText(now.getDayOfWeek()),
                        now.getHour(),
                        now.getMinute(),
                        mealWindow);
    }

    private String weekdayText(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    private void incrementRequestCounter(String endpoint, String result) {
        Counter.builder("recommendation.requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public record RecommendationResult(
            String poiId,
            String name,
            String address,
            double longitude,
            double latitude,
            String category,
            double distance,
            String reason) {
    }

    public record AskRecommendationResult(String answer, List<RecommendationCandidateCard> recommendations) {
    }

    public record AskRecommendationComputation(
            int requestedCandidateCount,
            int candidateCount,
            AskRecommendationResult result) {
    }

    public record AskStreamEvent(String name, Map<String, Object> data) {
    }

    public record AskContext(
            String previousQuestion,
            List<String> rejectedPoiIds,
            List<String> selectedPoiIds,
            List<String> userSignals) {
        public AskContext {
            rejectedPoiIds = rejectedPoiIds == null ? List.of() : List.copyOf(rejectedPoiIds);
            selectedPoiIds = selectedPoiIds == null ? List.of() : List.copyOf(selectedPoiIds);
            userSignals = userSignals == null ? List.of() : List.copyOf(userSignals);
        }

        public static AskContext empty() {
            return new AskContext(null, List.of(), List.of(), List.of());
        }
    }

    public record RecommendationCandidateCard(
            String poiId,
            String name,
            String address,
            double longitude,
            double latitude,
            String category,
            double distance,
            BigDecimal avgRating,
            int reviewCount,
            Integer avgPerCapitaPrice,
            List<String> aiTags,
            String matchReason) {

        RecommendationCandidateCard withMatchReason(String matchReason) {
            return new RecommendationCandidateCard(
                    poiId,
                    name,
                    address,
                    longitude,
                    latitude,
                    category,
                    distance,
                    avgRating,
                    reviewCount,
                    avgPerCapitaPrice,
                    aiTags,
                    matchReason);
        }
    }

    private void emitRecommendationCard(
            Map<String, Object> upstreamData,
            Map<String, RecommendationCandidateCard> candidateByPoiId,
            int maxCards,
            AtomicInteger emittedCardCount,
            Set<String> emittedPoiIds,
            Consumer<AskStreamEvent> eventConsumer) {
        cardAssembler.emitRecommendationCard(
                upstreamData,
                candidateByPoiId,
                maxCards,
                emittedCardCount,
                emittedPoiIds,
                eventConsumer);
    }

    private void emitFallbackRecommendationCards(
            List<RecommendationCandidateCard> candidates,
            int maxCards,
            AtomicInteger emittedCardCount,
            Set<String> emittedPoiIds,
            String answer,
            Consumer<AskStreamEvent> eventConsumer) {
        if (emittedCardCount.get() >= maxCards || candidates.isEmpty()) {
            return;
        }
        for (RecommendationCandidateCard candidate : orderFallbackCards(candidates, answer, emittedPoiIds)) {
            if (emittedCardCount.get() >= maxCards) {
                return;
            }
            emittedPoiIds.add(candidate.poiId());
            int rank = emittedCardCount.incrementAndGet();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("rank", rank);
            payload.put("poiId", candidate.poiId());
            payload.put("name", candidate.name());
            payload.put("address", candidate.address());
            payload.put("category", candidate.category());
            payload.put("distance", candidate.distance());
            payload.put("avgRating", candidate.avgRating());
            payload.put("reviewCount", candidate.reviewCount());
            payload.put("avgPerCapitaPrice", candidate.avgPerCapitaPrice());
            payload.put("aiTags", candidate.aiTags());
            payload.put("matchReason", buildFallbackMatchReason(candidate, answer));
            eventConsumer.accept(new AskStreamEvent("recommendation.card", payload));
        }
    }

    private List<RecommendationCandidateCard> orderFallbackCards(
            List<RecommendationCandidateCard> candidates,
            String answer,
            Set<String> emittedPoiIds) {
        String normalizedAnswer = answer == null ? "" : answer;
        List<RecommendationCandidateCard> ordered = new ArrayList<>();
        candidates.stream()
                .filter(candidate -> !emittedPoiIds.contains(candidate.poiId()))
                .filter(candidate -> !normalizedAnswer.isBlank() && normalizedAnswer.contains(candidate.name()))
                .sorted(java.util.Comparator.comparingInt(candidate -> normalizedAnswer.indexOf(candidate.name())))
                .forEach(ordered::add);
        candidates.stream()
                .filter(candidate -> !emittedPoiIds.contains(candidate.poiId()))
                .filter(candidate -> ordered.stream().noneMatch(existing -> existing.poiId().equals(candidate.poiId())))
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private String buildFallbackMatchReason(RecommendationCandidateCard candidate, String answer) {
        String extractedReason = extractReasonFromAnswer(answer, candidate.name());
        if (extractedReason != null) {
            return extractedReason;
        }
        List<String> parts = new ArrayList<>();
        if (candidate.distance() <= 200) {
            parts.add("离你很近");
        } else if (candidate.distance() <= 500) {
            parts.add("距离不远");
        }
        if (candidate.avgRating() != null && candidate.avgRating().compareTo(new BigDecimal("4.5")) >= 0) {
            parts.add("评分更高");
        }
        if (candidate.avgPerCapitaPrice() != null && candidate.avgPerCapitaPrice() <= 35) {
            parts.add("人均比较稳");
        }
        if (candidate.aiTags() != null && !candidate.aiTags().isEmpty()) {
            parts.add(candidate.aiTags().get(0));
        }
        if (parts.isEmpty()) {
            return "和你当前的需求更匹配";
        }
        return String.join("，", parts.stream().limit(2).toList());
    }

    private String extractReasonFromAnswer(String answer, String restaurantName) {
        if (answer == null || answer.isBlank() || restaurantName == null || restaurantName.isBlank()) {
            return null;
        }
        int nameIndex = answer.indexOf(restaurantName);
        if (nameIndex < 0) {
            return null;
        }
        int start = 0;
        for (int index = nameIndex - 1; index >= 0; index--) {
            char current = answer.charAt(index);
            if (current == '。' || current == '！' || current == '？' || current == '；' || current == '\n') {
                start = index + 1;
                break;
            }
        }
        int end = answer.length();
        for (int index = nameIndex; index < answer.length(); index++) {
            char current = answer.charAt(index);
            if (current == '。' || current == '！' || current == '？' || current == '；' || current == '\n') {
                end = index;
                break;
            }
        }
        String snippet = answer.substring(start, end)
                .replace("**", "")
                .replace("，适合", "，")
                .trim();
        if (snippet.isBlank()) {
            return null;
        }
        if (snippet.length() > 40) {
            snippet = snippet.substring(0, 40).trim();
        }
        return snippet;
    }
}
