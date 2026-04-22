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

    @PrePersist
    public void prePersist() {
        if (chosenAt == null) {
            chosenAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPoiId() {
        return poiId;
    }

    public void setPoiId(String poiId) {
        this.poiId = poiId;
    }

    public String getPoiName() {
        return poiName;
    }

    public void setPoiName(String poiName) {
        this.poiName = poiName;
    }

    public LocalDateTime getChosenAt() {
        return chosenAt;
    }

    public void setChosenAt(LocalDateTime chosenAt) {
        this.chosenAt = chosenAt;
    }
}
