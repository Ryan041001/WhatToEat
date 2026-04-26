package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.model.RecommendationFeedbackType;
import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;
import com.zjgsu.whattoeat.repository.RecommendationFeedbackRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRecommendationFeedbackApplicationServiceTest {

    @Test
    void recentRejectedPoiIdsShouldUseInjectedClockAndIgnoreAlreadyAte() {
        UserRepository userRepository = mock(UserRepository.class);
        RecommendationFeedbackRepository feedbackRepository = mock(RecommendationFeedbackRepository.class);
        UserChoiceHistoryApplicationService choiceHistoryApplicationService = mock(UserChoiceHistoryApplicationService.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-18T07:30:00Z"), ZoneId.of("Asia/Shanghai"));
        UserRecommendationFeedbackApplicationService service = new UserRecommendationFeedbackApplicationService(
                fixedClock,
                userRepository,
                feedbackRepository,
                choiceHistoryApplicationService);
        LocalDateTime expectedSince = LocalDateTime.of(2026, 4, 11, 15, 30);
        when(userRepository.existsById(21L)).thenReturn(true);
        when(feedbackRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(21L, expectedSince))
                .thenReturn(List.of(
                        feedback("poi-rejected", RecommendationFeedbackType.TOO_EXPENSIVE),
                        feedback("poi-already-ate", RecommendationFeedbackType.ALREADY_ATE),
                        feedback(null, RecommendationFeedbackType.DONT_WANT_THIS_TODAY)));

        Set<String> rejectedPoiIds = service.recentRejectedPoiIds(21L);

        assertEquals(Set.of("poi-rejected"), rejectedPoiIds);
        verify(feedbackRepository).findByUserIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(21L, expectedSince);
    }

    private RecommendationFeedbackEntity feedback(String poiId, RecommendationFeedbackType type) {
        RecommendationFeedbackEntity entity = new RecommendationFeedbackEntity();
        entity.setPoiId(poiId);
        entity.setFeedbackType(type.name());
        return entity;
    }
}
