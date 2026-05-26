package by.presassistant.application.port.in;

import by.presassistant.application.command.ChangeSlideCommand;
import by.presassistant.domain.model.LectureSession;

public interface ChangeSlideUseCase {
    LectureSession changeSlide(ChangeSlideCommand command);
}