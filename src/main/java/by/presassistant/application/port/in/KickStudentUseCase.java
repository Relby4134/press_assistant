package by.presassistant.application.port.in;

import by.presassistant.application.command.KickStudentCommand;

public interface KickStudentUseCase {
    void kick(KickStudentCommand command);
}