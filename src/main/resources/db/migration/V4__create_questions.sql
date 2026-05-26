CREATE TABLE questions (
    id                  UUID PRIMARY KEY,
    lecture_session_id  UUID NOT NULL REFERENCES lecture_sessions(id),
    chat_id             BIGINT NOT NULL,
    student_name        VARCHAR(255),
    text                TEXT NOT NULL,
    asked_at            TIMESTAMP NOT NULL DEFAULT NOW()
);