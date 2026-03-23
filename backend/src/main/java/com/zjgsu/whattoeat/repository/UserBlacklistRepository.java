package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBlacklistRepository extends JpaRepository<UserBlacklistEntity, Long> {
    List<UserBlacklistEntity> findByUserId(Long userId);
    Optional<UserBlacklistEntity> findByUserIdAndPoiId(Long userId, String poiId);
    boolean existsByUserIdAndPoiId(Long userId, String poiId);
}
