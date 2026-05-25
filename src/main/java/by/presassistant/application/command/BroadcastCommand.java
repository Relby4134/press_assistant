package by.presassistant.application.command;

import java.util.UUID;

public record BroadcastCommand(UUID lectureId, String message) {}