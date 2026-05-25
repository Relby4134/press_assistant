package by.presassistant.infrastructure.web.dto;

import java.util.UUID;

public record JoinRequest(Long chatId, String firstName, String username, UUID lectureId) {}