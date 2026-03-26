package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserBlacklistApplicationServiceTest {

    private UserRepository userRepository;
    private UserBlacklistRepository userBlacklistRepository;
    private UserBlacklistApplicationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userBlacklistRepository = mock(UserBlacklistRepository.class);
        service = new UserBlacklistApplicationService(userRepository, userBlacklistRepository);
    }

    @Test
    void addBlacklistShouldTranslateUniqueConstraintFailureToBusinessConflict() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userBlacklistRepository.existsByUserIdAndPoiId(1L, "B0FF123456")).thenReturn(false, true);
        when(userBlacklistRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(UserBlacklistEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.addBlacklist(1L, "B0FF123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("重复拉黑");
    }
}
