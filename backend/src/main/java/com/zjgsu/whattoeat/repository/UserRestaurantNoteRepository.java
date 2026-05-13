package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.UserRestaurantNoteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRestaurantNoteRepository extends JpaRepository<UserRestaurantNoteEntity, Long> {
    List<UserRestaurantNoteEntity> findByUserId(Long userId);
    Page<UserRestaurantNoteEntity> findByUserIdOrderByUpdatedAtDescIdDesc(Long userId, Pageable pageable);
    Page<UserRestaurantNoteEntity> findByUserIdAndNoteContainingOrderByUpdatedAtDescIdDesc(Long userId, String keyword, Pageable pageable);
    Optional<UserRestaurantNoteEntity> findByIdAndUserId(Long id, Long userId);
    Optional<UserRestaurantNoteEntity> findByUserIdAndPoiId(Long userId, String poiId);
    boolean existsByUserIdAndPoiId(Long userId, String poiId);
}
