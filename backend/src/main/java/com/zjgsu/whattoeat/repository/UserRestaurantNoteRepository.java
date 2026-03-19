package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.UserRestaurantNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRestaurantNoteRepository extends JpaRepository<UserRestaurantNoteEntity, Long> {
    List<UserRestaurantNoteEntity> findByUserId(Long userId);
    Optional<UserRestaurantNoteEntity> findByUserIdAndPoiId(Long userId, String poiId);
}
