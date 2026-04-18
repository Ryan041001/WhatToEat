package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantReviewRepository extends JpaRepository<RestaurantReviewEntity, Long> {
    List<RestaurantReviewEntity> findByPoiIdOrderByUpdatedAtDescIdDesc(String poiId);
    Page<RestaurantReviewEntity> findByPoiIdOrderByUpdatedAtDescIdDesc(String poiId, Pageable pageable);
    List<RestaurantReviewEntity> findByUserIdOrderByUpdatedAtDescIdDesc(Long userId);
    Optional<RestaurantReviewEntity> findByUserIdAndPoiId(Long userId, String poiId);
    boolean existsByUserIdAndPoiId(Long userId, String poiId);
}
