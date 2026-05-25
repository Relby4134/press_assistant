package by.presassistant.application.port.in;

import by.presassistant.application.command.JoinLectureCommand;
import by.presassistant.domain.model.Student;

public interface JoinLectureUseCase {
    Student execute(JoinLectureCommand command);
}