package by.presassistant.application.port.in;

import java.util.UUID;

public interface GetCurrentSlideFileUseCase {
    byte[] execute(UUID lectureId);
}