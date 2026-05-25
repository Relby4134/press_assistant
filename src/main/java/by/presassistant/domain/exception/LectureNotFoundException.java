package by.presassistant.domain.exception;

import java.util.UUID;

public class LectureNotFoundException extends RuntimeException {
    public LectureNotFoundException(UUID id) {
        super("Lecture not found: " + id);
    }
    public LectureNotFoundException(String title) {
        super("Lecture not found by title: " + title);
    }
}