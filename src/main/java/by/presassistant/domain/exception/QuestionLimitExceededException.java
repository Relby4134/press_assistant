package by.presassistant.domain.exception;

public class QuestionLimitExceededException extends RuntimeException {
    public QuestionLimitExceededException(Long chatId) {
        super("Question limit exceeded for student: chatId=" + chatId);
    }
}