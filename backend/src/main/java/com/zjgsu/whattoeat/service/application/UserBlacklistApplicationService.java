package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserBlacklistApplicationService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;

    public UserBlacklistApplicationService(UserRepository userRepository, UserBlacklistRepository userBlacklistRepository) {
        this.userRepository = userRepository;
        this.userBlacklistRepository = userBlacklistRepository;
    }

    @Transactional
    public void addBlacklist(Long userId, String poiId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (userBlacklistRepository.existsByUserIdAndPoiId(userId, poiId)) {
            throw new BusinessException(ErrorCode.BLACKLIST_ALREADY_EXISTS);
        }

        UserBlacklistEntity entity = new UserBlacklistEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        try {
            userBlacklistRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (!userRepository.existsById(userId)) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (userBlacklistRepository.existsByUserIdAndPoiId(userId, poiId)) {
                throw new BusinessException(ErrorCode.BLACKLIST_ALREADY_EXISTS);
            }
            throw ex;
        }
    }

    @Transactional
    public void removeBlacklist(Long userId, String poiId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserBlacklistEntity entity = userBlacklistRepository.findByUserIdAndPoiId(userId, poiId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLACKLIST_NOT_FOUND));
        userBlacklistRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public BlacklistPage listBlacklist(Long userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Page<UserBlacklistEntity> result = userBlacklistRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                userId,
                PageRequest.of(page - 1, size));

        List<BlacklistItem> items = result.getContent().stream()
                .map(entity -> new BlacklistItem(entity.getPoiId(), entity.getCreatedAt()))
                .toList();
        return new BlacklistPage(items, page, size, result.getTotalElements());
    }

    public record BlacklistPage(List<BlacklistItem> items, int page, int size, long total) {
    }

    public record BlacklistItem(String poiId, LocalDateTime createdAt) {
    }
}
