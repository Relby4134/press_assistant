package by.presassistant.domain.event;

import java.util.UUID;

public record StudentJoinedEvent(UUID lectureId, Long chatId, int currentSlide) {}