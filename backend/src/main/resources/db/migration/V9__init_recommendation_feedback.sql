CREATE TABLE recommendation_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    poi_id VARCHAR(64) NULL,
    poi_name_snapshot VARCHAR(128) NULL,
    feedback_type VARCHAR(32) NOT NULL,
    detail VARCHAR(255) NULL,
    request_question VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_feedback_user_created (user_id, created_at, id),
    KEY idx_feedback_user_poi (user_id, poi_id),
    CONSTRAINT fk_recommendation_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
