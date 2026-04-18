package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserChoiceHistoryRepository extends JpaRepository<UserChoiceHistoryEntity, Long> {
    Page<UserChoiceHistoryEntity> findByUserIdOrderByChosenAtDesc(Long userId, Pageable pageable);
    Page<UserChoiceHistoryEntity> findByUserIdOrderByChosenAtDescIdDesc(Long userId, Pageable pageable);
    List<UserChoiceHistoryEntity> findByUserIdOrderByChosenAtDesc(Long userId);
    List<UserChoiceHistoryEntity> findByUserIdAndChosenAtAfterOrderByChosenAtDesc(Long userId, LocalDateTime after);
}
