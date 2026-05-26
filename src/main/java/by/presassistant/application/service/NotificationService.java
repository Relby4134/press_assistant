package by.presassistant.application.service;

import by.presassistant.application.port.out.NotificationPort;
import by.presassistant.application.port.out.RealtimeNotificationPort;
import by.presassistant.application.port.out.StudentRepository;
import by.presassistant.domain.event.LectureEndedEvent;
import by.presassistant.domain.event.QuestionAskedEvent;
import by.presassistant.domain.event.SlideChangedEvent;
import by.presassistant.domain.model.Question;
import by.presassistant.domain.model.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final StudentRepository studentRepository;
    private final NotificationPort notificationPort;
    private final RealtimeNotificationPort realtimePort;
    private final StudentService studentService;

    public NotificationService(StudentRepository studentRepository,
                                @Lazy NotificationPort notificationPort,
                                RealtimeNotificationPort realtimePort,
                                StudentService studentService) {
        this.studentRepository = studentRepository;
        this.notificationPort = notificationPort;
        this.realtimePort = realtimePort;
        this.studentService = studentService;
    }

    @EventListener
    public void onSlideChanged(SlideChangedEvent event) {
        realtimePort.broadcastSlideChanged(event.lectureId(), event.slideNumber());

        List<Student> students = studentRepository.findAllByLectureId(event.lectureId());
        students.stream()
                .filter(s -> !s.isKicked())
                .forEach(s -> notificationPort.sendSlideNotification(s.getChatId(), event.slideNumber()));

        log.info("Slide change notified: lecture={} slide={} students={}",
                event.lectureId(), event.slideNumber(), students.size());
    }

    @EventListener
    public void onLectureEnded(LectureEndedEvent event) {
        studentService.notifyLectureEnded(event.lectureId());
    }

    @EventListener
    public void onQuestionAsked(QuestionAskedEvent event) {
        Question question = studentService.getQuestions(event.lectureId()).stream()
                .filter(q -> q.getId().equals(event.questionId()))
                .findFirst().orElse(null);
        if (question != null) {
            realtimePort.broadcastNewQuestion(event.lectureId(), question);
        }
        notificationPort.sendMessage(event.chatId(), "Вопрос принят ✅");
    }
}
