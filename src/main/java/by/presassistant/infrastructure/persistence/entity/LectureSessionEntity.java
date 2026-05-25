package by.presassistant.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lecture_sessions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureSessionEntity {

    @Id
    private UUID id;

    private String title;

    @Column(name = "current_slide")
    private int currentSlide;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "file_path")
    private String filePath;

    @Builder.Default
    @Column(name = "ended", nullable = false)
    private boolean ended = false;
}
