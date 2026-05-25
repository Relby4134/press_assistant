package by.presassistant.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionEntity {

    @Id
    private UUID id;

    @Column(name = "lecture_session_id", nullable = false)
    private UUID lectureSessionId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "student_name")
    private String studentName;

    @Column(nullable = false)
    private String text;

    @Builder.Default
    @Column(name = "asked_at", nullable = false)
    private LocalDateTime askedAt = LocalDateTime.now();
}
