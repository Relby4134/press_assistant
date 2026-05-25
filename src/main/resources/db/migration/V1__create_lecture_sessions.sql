CREATE TABLE lecture_sessions (
    id          UUID PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    current_slide INT NOT NULL DEFAULT 1,
    started_at  TIMESTAMP NOT NULL,
    file_path   VARCHAR(1000),
    ended       BOOLEAN NOT NULL DEFAULT FALSE
);