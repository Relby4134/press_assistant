package by.presassistant.application.port.in;

import by.presassistant.application.command.SaveSlideCommand;

public interface SaveSlideUseCase {
    void execute(SaveSlideCommand command);
}