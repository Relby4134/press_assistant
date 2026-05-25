package by.presassistant.infrastructure.web.dto;

import java.util.UUID;

public record KickRequest(UUID lectureId, Long chatId) {}