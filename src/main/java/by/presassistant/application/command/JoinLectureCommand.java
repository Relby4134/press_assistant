package by.presassistant.application.command;

import java.util.UUID;

public record JoinLectureCommand(Long chatId, String firstName, String username, UUID lectureId) {}