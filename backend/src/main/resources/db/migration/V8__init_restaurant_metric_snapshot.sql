CREATE TABLE restaurant_metric_snapshot (
    poi_id VARCHAR(64) NOT NULL,
    review_count INT NOT NULL DEFAULT 0,
    avg_rating DECIMAL(2,1) NULL,
    avg_per_capita_price INT NULL,
    ai_tag_1 VARCHAR(32) NULL,
    ai_tag_2 VARCHAR(32) NULL,
    ai_summary TEXT NULL,
    ai_status VARCHAR(16) NOT NULL DEFAULT 'idle',
    last_review_at DATETIME NULL,
    last_ai_generated_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (poi_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
