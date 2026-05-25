package by.presassistant.domain.exception;

public class StudentAlreadyKickedException extends RuntimeException {
    public StudentAlreadyKickedException(Long chatId) {
        super("Student already kicked: chatId=" + chatId);
    }
}