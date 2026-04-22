package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserChoiceHistoryApplicationService {

    static final int RECENT_SUPPRESSION_DAYS = 3;

    private final UserRepository userRepository;
    private final UserChoiceHistoryRepository userChoiceHistoryRepository;

    public UserChoiceHistoryApplicationService(
            UserRepository userRepository,
            UserChoiceHistoryRepository userChoiceHistoryRepository) {
        this.userRepository = userRepository;
        this.userChoiceHistoryRepository = userChoiceHistoryRepository;
    }

    @Transactional
    public void createChoiceHistory(Long userId, String poiId, String poiName) {
        validateUserExists(userId);
        UserChoiceHistoryEntity entity = new UserChoiceHistoryEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId.trim());
        entity.setPoiName(normalize(poiName));
        userChoiceHistoryRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public ChoiceHistoryPage listChoiceHistory(Long userId, int page, int size) {
        validateUserExists(userId);
        Page<UserChoiceHistoryEntity> result = userChoiceHistoryRepository.findByUserIdOrderByChosenAtDescIdDesc(
                userId,
                PageRequest.of(page - 1, size));
        return new ChoiceHistoryPage(
                result.getContent().stream()
                        .map(item -> new ChoiceHistoryItem(item.getId(), item.getPoiId(), item.getPoiName(), item.getChosenAt()))
                        .toList(),
                page,
                size,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Set<String> recentPoiIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        validateUserExists(userId);
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_SUPPRESSION_DAYS);
        return userChoiceHistoryRepository.findByUserIdAndChosenAtAfterOrderByChosenAtDesc(userId, since).stream()
                .map(UserChoiceHistoryEntity::getPoiId)
                .collect(Collectors.toSet());
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ChoiceHistoryPage(java.util.List<ChoiceHistoryItem> items, int page, int size, long total) {
    }

    public record ChoiceHistoryItem(Long id, String poiId, String poiName, LocalDateTime chosenAt) {
    }
}
