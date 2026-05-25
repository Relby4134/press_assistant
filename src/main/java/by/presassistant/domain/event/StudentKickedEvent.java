package by.presassistant.domain.event;

import java.util.UUID;

public record StudentKickedEvent(UUID lectureId, Long chatId) {}