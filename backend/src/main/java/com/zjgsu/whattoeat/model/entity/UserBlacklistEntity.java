package com.zjgsu.whattoeat.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_blacklist", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "poi_id"}))
public class UserBlacklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "poi_id", nullable = false)
    private String poiId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
