package by.presassistant.infrastructure.persistence.adapter;

import by.presassistant.application.port.out.LectureRepository;
import by.presassistant.domain.model.LectureSession;
import by.presassistant.infrastructure.persistence.entity.LectureSessionEntity;
import by.presassistant.infrastructure.persistence.repository.LectureJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LectureRepositoryAdapter implements LectureRepository {

    private final LectureJpaRepository jpa;

    @Override
    public LectureSession save(LectureSession lecture) {
        return toDomain(jpa.save(toEntity(lecture)));
    }

    @Override
    public Optional<LectureSession> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<LectureSession> findActiveByTitle(String title) {
        return jpa.findFirstByTitleIgnoreCaseAndEndedFalseOrderByStartedAtDesc(title)
                  .map(this::toDomain);
    }

    @Override
    public List<LectureSession> findAllActive() {
        return jpa.findByEndedFalse().stream()
                  .map(this::toDomain)
                  .collect(Collectors.toList());
    }

    private LectureSession toDomain(LectureSessionEntity e) {
        return new LectureSession(e.getId(), e.getTitle(), e.getCurrentSlide(),
                e.isEnded(), e.getStartedAt(), e.getFilePath());
    }

    private LectureSessionEntity toEntity(LectureSession l) {
        return LectureSessionEntity.builder()
                .id(l.getId())
                .title(l.getTitle())
                .currentSlide(l.getCurrentSlide())
                .ended(l.isEnded())
                .startedAt(l.getStartedAt())
                .filePath(l.getFilePath())
                .build();
    }
}