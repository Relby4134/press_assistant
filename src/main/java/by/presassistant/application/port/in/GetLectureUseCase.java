package by.presassistant.application.port.in;

import by.presassistant.domain.model.LectureSession;

import java.util.UUID;

public interface GetLectureUseCase {
    LectureSession execute(UUID lectureId);
}