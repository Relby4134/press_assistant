package by.presassistant.infrastructure.web.dto;

import java.util.UUID;

public record BroadcastRequest(UUID lectureId, String message) {}