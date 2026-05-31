package by.presassistant.domain.model;

import by.presassistant.domain.exception.LectureAlreadyEndedException;

import java.time.LocalDateTime;
import java.util.UUID;

public class LectureSession {

    private final UUID id;
    private String title;
    private int currentSlide;
    private boolean ended;
    private final LocalDateTime startedAt;
    private String filePath;
    private boolean requireNames;

    public LectureSession(UUID id, String title, String filePath, boolean requireNames) {
        this.id = id;
        this.title = title;
        this.filePath = filePath;
        this.requireNames = requireNames;
        this.currentSlide = 1;
        this.ended = false;
        this.startedAt = LocalDateTime.now();
    }

    public LectureSession(UUID id, String title, int currentSlide, boolean ended,
                          LocalDateTime startedAt, String filePath, boolean requireNames) {
        this.id = id;
        this.title = title;
        this.currentSlide = currentSlide;
        this.ended = ended;
        this.startedAt = startedAt;
        this.filePath = filePath;
        this.requireNames = requireNames;
    }

    public void changeSlide(int slideNumber) {
        if (ended) throw new LectureAlreadyEndedException(id);
        this.currentSlide = slideNumber;
    }

    public void end() {
        this.ended = true;
    }

    public boolean isActive() {
        return !ended;
    }

    public UUID getId()              { return id; }
    public String getTitle()         { return title; }
    public int getCurrentSlide()     { return currentSlide; }
    public boolean isEnded()         { return ended; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public String getFilePath()      { return filePath; }
    public boolean isRequireNames()  { return requireNames; }
}