package by.presassistant.application.command;

import java.util.UUID;

public record SaveSlideCommand(UUID lectureId, int slideNumber, byte[] imageBytes) {}