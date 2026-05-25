package by.presassistant.domain.event;

import java.util.UUID;

public record LectureEndedEvent(UUID lectureId) {}