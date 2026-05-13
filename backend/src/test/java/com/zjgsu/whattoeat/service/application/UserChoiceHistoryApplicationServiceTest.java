package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
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

class UserChoiceHistoryApplicationServiceTest {

    @Test
    void recentPoiIdsShouldUseInjectedClockWindow() {
        UserRepository userRepository = mock(UserRepository.class);
        UserChoiceHistoryRepository choiceHistoryRepository = mock(UserChoiceHistoryRepository.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-18T07:30:00Z"), ZoneId.of("Asia/Shanghai"));
        UserChoiceHistoryApplicationService service = new UserChoiceHistoryApplicationService(
                fixedClock,
                userRepository,
                choiceHistoryRepository);
        LocalDateTime expectedSince = LocalDateTime.of(2026, 4, 15, 15, 30);
        when(userRepository.existsById(12L)).thenReturn(true);
        when(choiceHistoryRepository.findByUserIdAndChosenAtAfterOrderByChosenAtDesc(12L, expectedSince))
                .thenReturn(List.of(choice("poi-recent"), choice("poi-other")));

        Set<String> recentPoiIds = service.recentPoiIds(12L);

        assertEquals(Set.of("poi-recent", "poi-other"), recentPoiIds);
        verify(choiceHistoryRepository).findByUserIdAndChosenAtAfterOrderByChosenAtDesc(12L, expectedSince);
    }

    private UserChoiceHistoryEntity choice(String poiId) {
        UserChoiceHistoryEntity entity = new UserChoiceHistoryEntity();
        entity.setPoiId(poiId);
        return entity;
    }
}
