package by.presassistant.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Question {

    private final UUID id;
    private final UUID lectureSessionId;
    private final Long chatId;
    private final String studentName;
    private final String text;
    private final LocalDateTime askedAt;

    public Question(UUID id, UUID lectureSessionId, Long chatId,
                    String studentName, String text) {
        this.id = id;
        this.lectureSessionId = lectureSessionId;
        this.chatId = chatId;
        this.studentName = studentName;
        this.text = text;
        this.askedAt = LocalDateTime.now();
    }

    public Question(UUID id, UUID lectureSessionId, Long chatId,
                    String studentName, String text, LocalDateTime askedAt) {
        this.id = id;
        this.lectureSessionId = lectureSessionId;
        this.chatId = chatId;
        this.studentName = studentName;
        this.text = text;
        this.askedAt = askedAt;
    }

    public UUID getId()               { return id; }
    public UUID getLectureSessionId() { return lectureSessionId; }
    public Long getChatId()           { return chatId; }
    public String getStudentName()    { return studentName; }
    public String getText()           { return text; }
    public LocalDateTime getAskedAt() { return askedAt; }
}