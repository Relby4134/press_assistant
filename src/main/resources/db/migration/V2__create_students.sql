CREATE TABLE students (
    id                  UUID PRIMARY KEY,
    chat_id             BIGINT NOT NULL,
    first_name          VARCHAR(255),
    username            VARCHAR(255),
    lecture_session_id  UUID NOT NULL REFERENCES lecture_sessions(id),
    joined_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    kicked              BOOLEAN NOT NULL DEFAULT FALSE
);