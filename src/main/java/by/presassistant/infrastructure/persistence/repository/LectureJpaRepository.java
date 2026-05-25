package by.presassistant.infrastructure.persistence.repository;

import by.presassistant.infrastructure.persistence.entity.LectureSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LectureJpaRepository extends JpaRepository<LectureSessionEntity, UUID> {
    Optional<LectureSessionEntity> findFirstByTitleIgnoreCaseAndEndedFalseOrderByStartedAtDesc(String title);
    List<LectureSessionEntity> findByEndedFalse();
}