package by.presassistant.application.port.in;

import by.presassistant.application.command.AskQuestionCommand;
import by.presassistant.domain.model.Question;

public interface AskQuestionUseCase {
    Question execute(AskQuestionCommand command);
}