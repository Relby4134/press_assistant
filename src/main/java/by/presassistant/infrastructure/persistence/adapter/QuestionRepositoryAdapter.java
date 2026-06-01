package by.presassistant.infrastructure.persistence.adapter;

import by.presassistant.application.port.out.QuestionRepository;
import by.presassistant.domain.model.Question;
import by.presassistant.infrastructure.persistence.entity.QuestionEntity;
import by.presassistant.infrastructure.persistence.repository.QuestionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QuestionRepositoryAdapter implements QuestionRepository {

    private final QuestionJpaRepository jpa;

    @Override
    public Question save(Question question) {
        return toDomain(jpa.save(toEntity(question)));
    }

    @Override
    public List<Question> findAllByLectureId(UUID lectureId) {
        return jpa.findByLectureSessionIdOrderByAskedAtAsc(lectureId).stream()
                  .map(this::toDomain)
                  .collect(Collectors.toList());
    }

    @Override
    public long countByLectureIdAndChatId(UUID lectureId, Long chatId) {
        return jpa.countByLectureSessionIdAndChatId(lectureId, chatId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    private Question toDomain(QuestionEntity e) {
        return new Question(e.getId(), e.getLectureSessionId(), e.getChatId(),
                e.getStudentName(), e.getText(), e.getAskedAt());
    }

    private QuestionEntity toEntity(Question q) {
        return QuestionEntity.builder()
                .id(q.getId())
                .lectureSessionId(q.getLectureSessionId())
                .chatId(q.getChatId())
                .studentName(q.getStudentName())
                .text(q.getText())
                .askedAt(q.getAskedAt())
                .build();
    }
}