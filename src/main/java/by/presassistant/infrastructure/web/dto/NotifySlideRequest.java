package by.presassistant.infrastructure.web.dto;

import java.util.UUID;

public record NotifySlideRequest(UUID lectureId, int slideNumber) {}