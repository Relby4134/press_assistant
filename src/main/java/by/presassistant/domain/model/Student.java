package by.presassistant.domain.model;

import by.presassistant.domain.exception.StudentAlreadyKickedException;

import java.time.LocalDateTime;
import java.util.UUID;

public class Student {

    private final UUID id;
    private final Long chatId;
    private final String firstName;
    private final String username;
    private final String fullName;
    private boolean kicked;
    private final LocalDateTime joinedAt;
    private final UUID lectureSessionId;

    public Student(UUID id, Long chatId, String firstName, String username,
                   UUID lectureSessionId, String fullName) {
        this.id = id;
        this.chatId = chatId;
        this.firstName = firstName;
        this.username = username;
        this.fullName = fullName;
        this.lectureSessionId = lectureSessionId;
        this.kicked = false;
        this.joinedAt = LocalDateTime.now();
    }

    public Student(UUID id, Long chatId, String firstName, String username,
                   boolean kicked, LocalDateTime joinedAt, UUID lectureSessionId, String fullName) {
        this.id = id;
        this.chatId = chatId;
        this.firstName = firstName;
        this.username = username;
        this.fullName = fullName;
        this.kicked = kicked;
        this.joinedAt = joinedAt;
        this.lectureSessionId = lectureSessionId;
    }

    public void kick() {
        if (kicked) throw new StudentAlreadyKickedException(chatId);
        this.kicked = true;
    }

    public String getDisplayName() {
        return fullName != null && !fullName.isBlank() ? fullName : firstName;
    }

    public UUID getId()               { return id; }
    public Long getChatId()           { return chatId; }
    public String getFirstName()      { return firstName; }
    public String getUsername()       { return username; }
    public String getFullName()       { return fullName; }
    public boolean isKicked()         { return kicked; }
    public LocalDateTime getJoinedAt(){ return joinedAt; }
    public UUID getLectureSessionId() { return lectureSessionId; }
}