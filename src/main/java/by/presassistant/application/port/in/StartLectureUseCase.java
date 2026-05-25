package by.presassistant.application.port.in;

import by.presassistant.application.command.StartLectureCommand;
import by.presassistant.domain.model.LectureSession;

public interface StartLectureUseCase {
    LectureSession execute(StartLectureCommand command);
}