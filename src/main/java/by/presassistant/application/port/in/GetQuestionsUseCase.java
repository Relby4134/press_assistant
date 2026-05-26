package by.presassistant.application.port.in;

import by.presassistant.domain.model.Question;

import java.util.List;
import java.util.UUID;

public interface GetQuestionsUseCase {
    List<Question> getQuestions(UUID lectureId);
}