package by.presassistant.domain.event;

import java.util.UUID;

public record LectureStartedEvent(UUID lectureId, String title, String fileUrl) {}