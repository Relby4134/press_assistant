package by.presassistant.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface FindActiveLectureUseCase {
    Optional<UUID> execute(Long chatId);
}