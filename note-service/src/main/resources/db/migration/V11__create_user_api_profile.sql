CREATE TABLE user_api_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    profile_name VARCHAR(50) NOT NULL,
    provider_key VARCHAR(30) NOT NULL,
    api_key VARCHAR(255) NOT NULL COMMENT 'AES encrypted',
    base_url VARCHAR(255) DEFAULT NULL,
    enabled_models JSON DEFAULT NULL COMMENT '["deepseek-chat","bge-m3"]',
    is_default TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_profile (user_id, profile_name)
);
