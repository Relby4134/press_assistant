package by.presassistant.infrastructure.websocket;

import by.presassistant.application.port.out.RealtimeNotificationPort;
import by.presassistant.domain.model.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketNotificationAdapter implements RealtimeNotificationPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastSlideChanged(UUID lectureId, int slideNumber) {
        messagingTemplate.convertAndSend(
                "/topic/lecture/" + lectureId,
                (Object) Map.of("lectureId", lectureId, "slideNumber", slideNumber));
    }

    @Override
    public void broadcastNewQuestion(UUID lectureId, Question question) {
        messagingTemplate.convertAndSend(
                "/topic/lecture/" + lectureId + "/questions",
                (Object) Map.of(
                        "id", question.getId(),
                        "studentName", question.getStudentName() != null ? question.getStudentName() : "",
                        "text", question.getText(),
                        "askedAt", question.getAskedAt().toString()
                ));
    }
}