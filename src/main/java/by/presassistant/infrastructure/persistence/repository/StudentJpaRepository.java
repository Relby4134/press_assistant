package by.presassistant.infrastructure.persistence.repository;

import by.presassistant.infrastructure.persistence.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, UUID> {

    Optional<StudentEntity> findByChatIdAndLectureSessionId(Long chatId, UUID lectureSessionId);

    List<StudentEntity> findByLectureSessionId(UUID lectureSessionId);

    @Query("""
        SELECT s FROM StudentEntity s
        JOIN LectureSessionEntity ls ON ls.id = s.lectureSessionId
        WHERE s.chatId = :chatId AND ls.ended = false AND s.kicked = false
        ORDER BY s.joinedAt DESC LIMIT 1
    """)
    Optional<StudentEntity> findActiveByChatId(@Param("chatId") Long chatId);
}