CREATE TABLE restaurant_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    poi_id VARCHAR(64) NOT NULL,
    poi_name_snapshot VARCHAR(128) NULL,
    rating_score DECIMAL(2,1) NOT NULL,
    per_capita_price INT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_user_poi (user_id, poi_id),
    KEY idx_review_poi_updated (poi_id, updated_at, id),
    CONSTRAINT fk_restaurant_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
