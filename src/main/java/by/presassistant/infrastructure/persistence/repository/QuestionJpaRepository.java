package by.presassistant.infrastructure.persistence.repository;

import by.presassistant.infrastructure.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, UUID> {
    List<QuestionEntity> findByLectureSessionIdOrderByAskedAtAsc(UUID lectureSessionId);
    long countByLectureSessionIdAndChatId(UUID lectureSessionId, Long chatId);
}