package by.presassistant.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEntity {

    @Id
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "first_name")
    private String firstName;

    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "lecture_session_id", nullable = false)
    private UUID lectureSessionId;

    @Builder.Default
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "kicked", nullable = false)
    private boolean kicked = false;
}
