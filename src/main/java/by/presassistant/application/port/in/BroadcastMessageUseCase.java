package by.presassistant.application.port.in;

import by.presassistant.application.command.BroadcastCommand;

public interface BroadcastMessageUseCase {
    void execute(BroadcastCommand command);
}