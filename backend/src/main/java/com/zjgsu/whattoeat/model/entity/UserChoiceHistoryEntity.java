package com.zjgsu.whattoeat.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_choice_history")
public class UserChoiceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "poi_id", nullable = false)
    private String poiId;

    @Column(name = "poi_name")
    private String poiName;

    @Column(name = "chosen_at")
    private LocalDateTime chosenAt;
}
