package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantMetricSnapshotRepository extends JpaRepository<RestaurantMetricSnapshotEntity, String> {
}
