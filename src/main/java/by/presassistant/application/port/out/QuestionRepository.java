package by.presassistant.application.port.out;

import by.presassistant.domain.model.Question;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);
    List<Question> findAllByLectureId(UUID lectureId);
    long countByLectureIdAndChatId(UUID lectureId, Long chatId);
    void deleteById(UUID id);
}
