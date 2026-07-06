CREATE TABLE note_chunks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    chunk_index INT          NOT NULL,
    chunk_text  TEXT         NOT NULL,
    chunk_id    VARCHAR(64)  NOT NULL COMMENT 'Qdrant point id',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_note_id (note_id),
    INDEX idx_user_id (user_id),
    UNIQUE INDEX uk_chunk_id (chunk_id),
    FULLTEXT INDEX ft_chunk_text (chunk_text) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
