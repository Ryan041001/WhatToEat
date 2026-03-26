package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
