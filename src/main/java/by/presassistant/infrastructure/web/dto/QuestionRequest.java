package by.presassistant.infrastructure.web.dto;

import java.util.UUID;

public record QuestionRequest(UUID lectureId, Long chatId, String studentName, String text) {}