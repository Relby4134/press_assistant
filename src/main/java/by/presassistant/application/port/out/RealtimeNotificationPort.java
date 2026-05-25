package by.presassistant.application.port.out;

import by.presassistant.domain.model.Question;

import java.util.UUID;

public interface RealtimeNotificationPort {
    void broadcastSlideChanged(UUID lectureId, int slideNumber);
    void broadcastNewQuestion(UUID lectureId, Question question);
}
