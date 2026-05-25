package by.presassistant.domain.exception;

import java.util.UUID;

public class LectureAlreadyEndedException extends RuntimeException {
    public LectureAlreadyEndedException(UUID id) {
        super("Lecture already ended: " + id);
    }
}