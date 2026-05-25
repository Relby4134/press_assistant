package by.presassistant.domain.event;

import java.util.UUID;

public record SlideChangedEvent(UUID lectureId, int slideNumber) {}