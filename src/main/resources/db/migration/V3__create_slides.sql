CREATE TABLE slides (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lecture_session_id  UUID NOT NULL REFERENCES lecture_sessions(id),
    slide_number        INT NOT NULL,
    image_path          VARCHAR(1000),
    UNIQUE (lecture_session_id, slide_number)
);