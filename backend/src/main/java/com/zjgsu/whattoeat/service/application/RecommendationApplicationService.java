package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.ai.AiAssistantClient;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.service.domain.RecommendationDomainService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RecommendationApplicationService {

    private static final int RANDOM_CANDIDATE_SIZE = 20;
    private static final int DEFAULT_ASK_RESULT_SIZE = 3;
    private static final int MIN_ASK_CANDIDATE_SIZE = 12;

    private final AmapClient amapClient;
    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;
    private final AiAssistantClient aiAssistantClient;
    private final RecommendationDomainService recommendationDomainService;
    private final UserChoiceHistoryApplicationService userChoiceHistoryApplicationService;
    private final UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService;
    private final UserPreferenceProfileApplicationService userPreferenceProfileApplicationService;
    private final MeterRegistry meterRegistry;

    public RecommendationApplicationService(
            AmapClient amapClient,
            UserRepository userRepository,
            UserBlacklistRepository userBlacklistRepository,
            RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository,
            AiAssistantClient aiAssistantClient,
            RecommendationDomainService recommendationDomainService,
            UserChoiceHistoryApplicationService userChoiceHistoryApplicationService,
            UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService,
            UserPreferenceProfileApplicationService userPreferenceProfileApplicationService,
            MeterRegistry meterRegistry) {
        this.amapClient = amapClient;
        this.userRepository = userRepository;
        this.userBlacklistRepository = userBlacklistRepository;
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
        this.aiAssistantClient = aiAssistantClient;
        this.recommendationDomainService = recommendationDomainService;
        this.userChoiceHistoryApplicationService = userChoiceHistoryApplicationService;
        this.userRecommendationFeedbackApplicationService = userRecommendationFeedbackApplicationService;
        this.userPreferenceProfileApplicationService = userPreferenceProfileApplicationService;
        this.meterRegistry = meterRegistry;
    }

    public RecommendationResult recommendRandom(Long userId, double longitude, double latitude, int radius) {
        try {
            List<AmapPoi> candidates = loadCandidates(userId, longitude, latitude, radius, RANDOM_CANDIDATE_SIZE, Set.of());
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
            List<AmapPoi> result = loadCandidates(userId, longitude, latitude, radius, size, Set.of());
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
            List<AmapPoi> candidates = loadCandidates(
                    userId,
                    longitude,
                    latitude,
                    radius,
                    requestedCandidateSize,
                    Set.copyOf(context.rejectedPoiIds()));
            EnrichedRecommendationCandidates enriched = enrichCandidates(candidates);
            eventConsumer.accept(new AskStreamEvent(
                    "retrieval.completed",
                    Map.of("candidateCount", enriched.cards().size())));

            Map<String, RecommendationCandidateCard> candidateByPoiId = enriched.cards().stream()
                    .collect(Collectors.toMap(RecommendationCandidateCard::poiId, candidate -> candidate));
            AtomicInteger emittedCardCount = new AtomicInteger();
            Set<String> emittedPoiIds = new java.util.HashSet<>();

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
                            case "answer.delta", "answer.done", "done" -> eventConsumer.accept(new AskStreamEvent(
                                    upstreamEvent.name(),
                                    upstreamEvent.data()));
                            case "error" -> eventConsumer.accept(new AskStreamEvent("error", upstreamEvent.data()));
                            default -> {
                                // Ignore unknown upstream events for forward compatibility.
                            }
                        }
                    });
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
            List<AmapPoi> candidates = loadCandidates(
                    userId,
                    longitude,
                    latitude,
                    radius,
                    requestedCandidateSize,
                    Set.copyOf(context.rejectedPoiIds()));
            EnrichedRecommendationCandidates enriched = enrichCandidates(candidates);
            AiAssistantClient.RecommendationAdvice advice = aiAssistantClient.recommend(
                    new AiAssistantClient.RecommendationRequest(
                            question,
                            enriched.aiCandidates(),
                            buildAiContext(userId, context)));

            List<RecommendationCandidateCard> orderedRecommendations = orderAskRecommendations(
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

    private AiAssistantClient.RecommendationContext buildAiContext(Long userId, AskContext requestContext) {
        if (userId == null) {
            return new AiAssistantClient.RecommendationContext(
                    requestContext.previousQuestion(),
                    requestContext.rejectedPoiIds(),
                    requestContext.selectedPoiIds(),
                    requestContext.userSignals(),
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
                profile.summary(),
                profile.preferredTags(),
                profile.avoidedTags(),
                profile.recentFeedbackSignals());
    }

    private void incrementRequestCounter(String endpoint, String result) {
        Counter.builder("recommendation.requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private List<AmapPoi> loadCandidates(
            Long userId,
            double longitude,
            double latitude,
            int radius,
            int size,
            Set<String> requestRejectedPoiIds) {
        validateUser(userId);
        Set<String> hardExcludedPoiIds = blacklistPoiIds(userId);
        LinkedHashSet<String> softExcludedPoiIds = new LinkedHashSet<>();
        if (requestRejectedPoiIds != null) {
            softExcludedPoiIds.addAll(requestRejectedPoiIds);
        }
        if (userId != null) {
            softExcludedPoiIds.addAll(userChoiceHistoryApplicationService.recentPoiIds(userId));
            softExcludedPoiIds.addAll(userRecommendationFeedbackApplicationService.recentRejectedPoiIds(userId));
        }

        List<AmapPoi> preferred = collectCandidates(
                longitude,
                latitude,
                radius,
                size,
                mergeExcludedPoiIds(hardExcludedPoiIds, softExcludedPoiIds));
        if (!preferred.isEmpty() || softExcludedPoiIds.isEmpty()) {
            if (preferred.isEmpty()) {
                throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
            }
            return preferred;
        }
        List<AmapPoi> relaxed = collectCandidates(longitude, latitude, radius, size, hardExcludedPoiIds);
        if (relaxed.isEmpty()) {
            throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
        }
        return relaxed;
    }

    private Set<String> mergeExcludedPoiIds(Set<String> hardExcludedPoiIds, Set<String> softExcludedPoiIds) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(hardExcludedPoiIds);
        merged.addAll(softExcludedPoiIds);
        return Set.copyOf(merged);
    }

    private List<AmapPoi> collectCandidates(
            double longitude,
            double latitude,
            int radius,
            int size,
            Set<String> excludedPoiIds) {
        List<AmapPoi> collected = new ArrayList<>();
        Set<String> seenPoiIds = new LinkedHashSet<>();
        long total = Long.MAX_VALUE;
        long fetchedCount = 0L;
        int page = 1;

        while (collected.size() < size && fetchedCount < total) {
            AmapClient.AmapSearchResult result = amapClient.searchNearby(longitude, latitude, radius, page, size);
            total = result.total();
            fetchedCount += result.items().size();
            for (AmapPoi item : result.items()) {
                if (excludedPoiIds.contains(item.poiId()) || !seenPoiIds.add(item.poiId())) {
                    continue;
                }
                collected.add(item);
                if (collected.size() >= size) {
                    break;
                }
            }
            if (result.items().isEmpty()) {
                break;
            }
            page++;
        }

        return List.copyOf(collected);
    }

    private EnrichedRecommendationCandidates enrichCandidates(List<AmapPoi> candidates) {
        Map<String, RestaurantMetricSnapshotEntity> snapshotByPoiId = restaurantMetricSnapshotRepository.findAllById(
                        candidates.stream().map(AmapPoi::poiId).toList())
                .stream()
                .collect(Collectors.toMap(RestaurantMetricSnapshotEntity::getPoiId, snapshot -> snapshot));

        List<RecommendationCandidateCard> cards = new ArrayList<>();
        List<AiAssistantClient.RecommendationCandidate> aiCandidates = new ArrayList<>();
        for (AmapPoi candidate : candidates) {
            RestaurantMetricSnapshotEntity snapshot = snapshotByPoiId.get(candidate.poiId());
            List<String> aiTags = snapshot == null
                    ? List.of()
                    : Stream.of(snapshot.getAiTag1(), snapshot.getAiTag2())
                    .filter(tag -> tag != null && !tag.isBlank())
                    .toList();
            String aiSummary = snapshot == null || !"ready".equalsIgnoreCase(snapshot.getAiStatus())
                    ? null
                    : snapshot.getAiSummary();
            List<String> derivedTags = RecommendationInsightHeuristics.deriveCandidateSignals(
                    candidate.category(),
                    aiTags,
                    aiSummary);
            cards.add(new RecommendationCandidateCard(
                    candidate.poiId(),
                    candidate.name(),
                    candidate.address(),
                    candidate.longitude(),
                    candidate.latitude(),
                    candidate.category(),
                    candidate.distance(),
                    snapshot == null ? null : snapshot.getAvgRating(),
                    snapshot == null || snapshot.getReviewCount() == null ? 0 : snapshot.getReviewCount(),
                    snapshot == null ? null : snapshot.getAvgPerCapitaPrice(),
                    aiTags,
                    null));
            aiCandidates.add(new AiAssistantClient.RecommendationCandidate(
                    candidate.poiId(),
                    candidate.name(),
                    candidate.address(),
                    candidate.category(),
                    candidate.distance(),
                    snapshot == null ? null : snapshot.getAvgRating(),
                    snapshot == null || snapshot.getReviewCount() == null ? 0 : snapshot.getReviewCount(),
                    snapshot == null ? null : snapshot.getAvgPerCapitaPrice(),
                    aiTags,
                    aiSummary,
                    derivedTags));
        }
        return new EnrichedRecommendationCandidates(List.copyOf(cards), List.copyOf(aiCandidates));
    }

    private List<RecommendationCandidateCard> orderAskRecommendations(
            List<RecommendationCandidateCard> candidates,
            List<AiAssistantClient.RecommendationChoice> choices,
            int size) {
        Map<String, RecommendationCandidateCard> candidateByPoiId = candidates.stream()
                .collect(Collectors.toMap(RecommendationCandidateCard::poiId, candidate -> candidate));
        List<RecommendationCandidateCard> ordered = new ArrayList<>();
        if (choices != null) {
            for (AiAssistantClient.RecommendationChoice choice : choices) {
                RecommendationCandidateCard candidate = candidateByPoiId.remove(choice.poiId());
                if (candidate != null) {
                    ordered.add(candidate.withMatchReason(choice.reason()));
                }
            }
        }
        if (ordered.size() < size) {
            candidates.stream()
                    .filter(candidate -> candidateByPoiId.containsKey(candidate.poiId()))
                    .limit(size - ordered.size())
                    .forEach(ordered::add);
        }
        if (ordered.size() <= size) {
            return List.copyOf(ordered);
        }
        return List.copyOf(ordered.subList(0, size));
    }

    private void validateUser(Long userId) {
        if (userId != null && !userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Set<String> blacklistPoiIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return userBlacklistRepository.findByUserId(userId).stream()
                .map(blacklist -> blacklist.getPoiId())
                .collect(Collectors.toSet());
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

    private record EnrichedRecommendationCandidates(
            List<RecommendationCandidateCard> cards,
            List<AiAssistantClient.RecommendationCandidate> aiCandidates) {
    }

    @SuppressWarnings("unchecked")
    private void emitRecommendationCard(
            Map<String, Object> upstreamData,
            Map<String, RecommendationCandidateCard> candidateByPoiId,
            int maxCards,
            AtomicInteger emittedCardCount,
            Set<String> emittedPoiIds,
            Consumer<AskStreamEvent> eventConsumer) {
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
        if (!(poiIdValue instanceof String poiId)) {
            return;
        }
        RecommendationCandidateCard candidate = candidateByPoiId.get(poiId);
        if (candidate == null || emittedPoiIds.contains(poiId)) {
            return;
        }
        int rank = emittedCardCount.incrementAndGet();
        if (rank > maxCards) {
            return;
        }
        emittedPoiIds.add(poiId);
        Object rankValue = arguments.get("rank");
        if (rankValue instanceof Number number && number.intValue() > 0) {
            rank = number.intValue();
        }
        String reason = arguments.get("reason") instanceof String text ? text : candidate.matchReason();
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
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
        payload.put("matchReason", reason);
        eventConsumer.accept(new AskStreamEvent("recommendation.card", payload));
    }
}
