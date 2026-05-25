package by.presassistant.application.port.in;

import java.util.UUID;

public interface EndLectureUseCase {
    void execute(UUID lectureId);
}