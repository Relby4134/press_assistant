package by.presassistant.infrastructure.web;

import by.presassistant.application.command.*;
import by.presassistant.application.port.in.*;
import by.presassistant.application.service.AnalyticsService;
import by.presassistant.application.service.StudentService;
import by.presassistant.domain.event.SlideChangedEvent;
import by.presassistant.domain.model.Question;
import by.presassistant.domain.model.Student;
import by.presassistant.infrastructure.analytics.StudentAnalytics;
import by.presassistant.infrastructure.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final AnalyticsService analyticsService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/join")
    public Student join(@RequestBody JoinRequest req) {
        return studentService.join(
                new JoinLectureCommand(req.chatId(), req.firstName(), req.username(), req.lectureId(), null));
    }

    @PostMapping("/question")
    public ResponseEntity<Question> question(@RequestBody QuestionRequest req) {
        try {
            Question q = studentService.ask(
                    new AskQuestionCommand(req.lectureId(), req.chatId(), req.studentName(), req.text()));
            return ResponseEntity.ok(q);
        } catch (by.presassistant.domain.exception.QuestionLimitExceededException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/questions/{lectureId}")
    public List<Question> questions(@PathVariable UUID lectureId) {
        return studentService.getQuestions(lectureId);
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID questionId) {
        studentService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notify/slide")
    public ResponseEntity<Void> notifySlide(@RequestBody NotifySlideRequest req) {
        eventPublisher.publishEvent(new SlideChangedEvent(req.lectureId(), req.slideNumber()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/kick")
    public ResponseEntity<Void> kick(@RequestBody KickRequest req) {
        studentService.kick(new KickStudentCommand(req.lectureId(), req.chatId()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody BroadcastRequest req) {
        studentService.broadcast(new BroadcastCommand(req.lectureId(), req.message()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list/{lectureId}")
    public List<Student> list(@PathVariable UUID lectureId) {
        return studentService.getStudents(lectureId);
    }

    @GetMapping("/analytics/{lectureId}")
    public List<StudentAnalytics> analytics(@PathVariable UUID lectureId) {
        return analyticsService.getAnalytics(lectureId);
    }
}