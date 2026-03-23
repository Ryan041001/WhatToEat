package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChoiceHistoryRepository extends JpaRepository<UserChoiceHistoryEntity, Long> {
    Page<UserChoiceHistoryEntity> findByUserIdOrderByChosenAtDesc(Long userId, Pageable pageable);
}
