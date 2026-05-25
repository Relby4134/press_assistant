package by.presassistant.application.command;

import java.util.UUID;

public record ChangeSlideCommand(UUID lectureId, int slideNumber) {}