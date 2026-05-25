package by.presassistant.application.command;

import java.util.UUID;

public record KickStudentCommand(UUID lectureId, Long chatId) {}