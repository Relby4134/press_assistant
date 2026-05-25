package by.presassistant.application.command;

import java.util.UUID;

public record AskQuestionCommand(UUID lectureId, Long chatId, String studentName, String text) {}