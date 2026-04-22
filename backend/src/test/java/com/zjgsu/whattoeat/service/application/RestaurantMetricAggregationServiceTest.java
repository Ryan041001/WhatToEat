package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RestaurantMetricAggregationServiceTest {

    @Autowired
    private RestaurantMetricAggregationService aggregationService;

    @Autowired
    private RestaurantReviewRepository restaurantReviewRepository;

    @Autowired
    private RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        restaurantReviewRepository.deleteAll();
        restaurantMetricSnapshotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void refreshSnapshotShouldAggregateReviewCountRatingAndPrice() {
        UserEntity alice = createUser("mock-openid-review-001", "Alice");
        UserEntity bob = createUser("mock-openid-review-002", "Bob");

        saveReview(alice.getId(), "B0FFREVIEW001", "沙县小吃", "出餐快", "4.5", 25,
                LocalDateTime.of(2026, 4, 17, 9, 0),
                LocalDateTime.of(2026, 4, 17, 9, 5));
        saveReview(bob.getId(), "B0FFREVIEW001", "沙县小吃", "性价比高", "4.0", 29,
                LocalDateTime.of(2026, 4, 17, 10, 0),
                LocalDateTime.of(2026, 4, 17, 10, 10));

        aggregationService.refreshSnapshot("B0FFREVIEW001");

        var snapshot = restaurantMetricSnapshotRepository.findById("B0FFREVIEW001")
                .orElseThrow();
        assertThat(snapshot.getReviewCount()).isEqualTo(2);
        assertThat(snapshot.getAvgRating()).isEqualByComparingTo("4.3");
        assertThat(snapshot.getAvgPerCapitaPrice()).isEqualTo(27);
    }

    private UserEntity createUser(String openId, String nickname) {
        UserEntity entity = new UserEntity();
        entity.setOpenid(openId);
        entity.setNickname(nickname);
        return userRepository.saveAndFlush(entity);
    }

    private void saveReview(
            Long userId,
            String poiId,
            String poiNameSnapshot,
            String content,
            String ratingScore,
            int perCapitaPrice,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        RestaurantReviewEntity entity = new RestaurantReviewEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setPoiNameSnapshot(poiNameSnapshot);
        entity.setContent(content);
        entity.setRatingScore(new BigDecimal(ratingScore));
        entity.setPerCapitaPrice(perCapitaPrice);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        restaurantReviewRepository.saveAndFlush(entity);
    }
}
