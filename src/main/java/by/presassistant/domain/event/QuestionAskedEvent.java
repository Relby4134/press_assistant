package by.presassistant.domain.event;

import java.util.UUID;

public record QuestionAskedEvent(UUID lectureId, UUID questionId, Long chatId) {}