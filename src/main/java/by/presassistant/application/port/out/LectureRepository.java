package by.presassistant.application.port.out;

import by.presassistant.domain.model.LectureSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LectureRepository {
    LectureSession save(LectureSession lecture);
    Optional<LectureSession> findById(UUID id);
    Optional<LectureSession> findActiveByTitle(String title);
    List<LectureSession> findAllActive();
}
