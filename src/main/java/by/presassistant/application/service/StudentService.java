package by.presassistant.application.service;

import by.presassistant.application.command.AskQuestionCommand;
import by.presassistant.application.command.BroadcastCommand;
import by.presassistant.application.command.JoinLectureCommand;
import by.presassistant.application.command.KickStudentCommand;
import by.presassistant.application.port.in.*;
import by.presassistant.application.port.out.*;
import by.presassistant.domain.event.QuestionAskedEvent;
import by.presassistant.domain.event.StudentJoinedEvent;
import by.presassistant.domain.event.StudentKickedEvent;
import by.presassistant.domain.exception.LectureNotFoundException;
import by.presassistant.domain.exception.QuestionLimitExceededException;
import by.presassistant.domain.model.LectureSession;
import by.presassistant.domain.model.Question;
import by.presassistant.domain.model.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class StudentService implements
        JoinLectureUseCase,
        JoinLectureByTitleUseCase,
        AskQuestionUseCase,
        KickStudentUseCase,
        BroadcastMessageUseCase,
        GetStudentsUseCase,
        GetQuestionsUseCase,
        FindActiveLectureUseCase {

    private static final int QUESTION_LIMIT = 10;

    private final StudentRepository studentRepository;
    private final LectureRepository lectureRepository;
    private final QuestionRepository questionRepository;
    private final SlideStoragePort slideStorage;
    private final AnalyticsPort analyticsPort;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPort notificationPort;

    public StudentService(StudentRepository studentRepository,
                          LectureRepository lectureRepository,
                          QuestionRepository questionRepository,
                          SlideStoragePort slideStorage,
                          AnalyticsPort analyticsPort,
                          ApplicationEventPublisher eventPublisher,
                          @Lazy NotificationPort notificationPort) {
        this.studentRepository = studentRepository;
        this.lectureRepository = lectureRepository;
        this.questionRepository = questionRepository;
        this.slideStorage = slideStorage;
        this.analyticsPort = analyticsPort;
        this.eventPublisher = eventPublisher;
        this.notificationPort = notificationPort;
    }

    @Override
    @Transactional
    public Student join(JoinLectureCommand command) {
        LectureSession lecture = lectureRepository.findById(command.lectureId())
                .orElseThrow(() -> new LectureNotFoundException(command.lectureId()));

        Optional<Student> existing = studentRepository
                .findByChatIdAndLectureId(command.chatId(), command.lectureId());

        if (existing.isPresent()) {
            Student student = existing.get();
            if (student.isKicked()) {
                notificationPort.sendMessage(command.chatId(),
                        "❌ Вы были удалены с этой лекции и не можете подключиться повторно.");
                return student;
            }
            notificationPort.sendMessage(command.chatId(),
                    "✅ Вы уже подключены к лекции: " + lecture.getTitle() + "\n\n" +
                    "Чтобы задать вопрос — просто напишите его сюда.");
            notificationPort.sendSlideNotification(command.chatId(), lecture.getCurrentSlide());
            return student;
        }

        Student student = new Student(UUID.randomUUID(), command.chatId(),
                command.firstName(), command.username(), command.lectureId(), command.fullName());
        Student saved = studentRepository.save(student);

        notificationPort.sendMessage(command.chatId(),
                "✅ Вы подключились к лекции: " + lecture.getTitle() + "\n\n" +
                "Чтобы задать вопрос — просто напишите его сюда.");
        notificationPort.sendSlideNotification(command.chatId(), lecture.getCurrentSlide());

        eventPublisher.publishEvent(new StudentJoinedEvent(command.lectureId(),
                command.chatId(), lecture.getCurrentSlide()));
        return saved;
    }

    @Override
    @Transactional
    public Student joinByTitle(Long chatId, String firstName, String username, String title) {
        LectureSession lecture = lectureRepository.findActiveByTitle(title)
                .orElseThrow(() -> new LectureNotFoundException(title));
        return join(new JoinLectureCommand(chatId, firstName, username, lecture.getId(), null));
    }

    @Override
    @Transactional
    public Question ask(AskQuestionCommand command) {
        long count = questionRepository.countByLectureIdAndChatId(command.lectureId(), command.chatId());
        if (count >= QUESTION_LIMIT) {
            throw new QuestionLimitExceededException(command.chatId());
        }

        Question question = new Question(UUID.randomUUID(), command.lectureId(),
                command.chatId(), command.studentName(), command.text());
        Question saved = questionRepository.save(question);

        eventPublisher.publishEvent(new QuestionAskedEvent(command.lectureId(),
                saved.getId(), command.chatId()));
        return saved;
    }

    @Override
    @Transactional
    public void kick(KickStudentCommand command) {
        Student student = studentRepository
                .findByChatIdAndLectureId(command.chatId(), command.lectureId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        student.kick();
        studentRepository.save(student);
        notificationPort.sendMessage(command.chatId(), "❌ Вы были удалены с лекции лектором.");
        eventPublisher.publishEvent(new StudentKickedEvent(command.lectureId(), command.chatId()));
        log.info("Student kicked: chatId={} lecture={}", command.chatId(), command.lectureId());
    }

    @Override
    public void broadcast(BroadcastCommand command) {
        List<Student> students = studentRepository.findAllByLectureId(command.lectureId());
        students.stream()
                .filter(s -> !s.isKicked())
                .forEach(s -> notificationPort.sendMessage(s.getChatId(), "📢 " + command.message()));
        log.info("Broadcast sent to lecture={}", command.lectureId());
    }

    @Override
    public List<Student> getStudents(UUID lectureId) {
        return studentRepository.findAllByLectureId(lectureId);
    }

    @Override
    public List<Question> getQuestions(UUID lectureId) {
        return questionRepository.findAllByLectureId(lectureId);
    }

    @Transactional
    public void deleteQuestion(UUID questionId) {
        questionRepository.deleteById(questionId);
    }

    @Override
    public Optional<UUID> findActiveLecture(Long chatId) {
        return studentRepository.findActiveStudentByChatId(chatId)
                .map(Student::getLectureSessionId);
    }

    public boolean isStudentKicked(Long chatId, UUID lectureId) {
        return studentRepository.findByChatIdAndLectureId(chatId, lectureId)
                .map(Student::isKicked)
                .orElse(false);
    }

    public Optional<byte[]> getSlideImageBytes(UUID lectureId, int slideNumber) {
        return slideStorage.getSlide(lectureId, slideNumber);
    }

    public String getStudentDisplayName(Long chatId, UUID lectureId) {
        return studentRepository.findByChatIdAndLectureId(chatId, lectureId)
                .map(Student::getDisplayName)
                .orElse("Студент");
    }

    public void recordSlideRequest(UUID lectureId, Long chatId, int slideNumber) {
        analyticsPort.recordRequest(lectureId, chatId, slideNumber);
    }

    public void notifyServerShutdown(UUID lectureId) {
        studentRepository.findAllByLectureId(lectureId).stream()
                .filter(s -> !s.isKicked())
                .forEach(s -> notificationPort.sendMessage(s.getChatId(),
                        "⚠️ Сервер завершил работу. Лекция приостановлена."));
    }

    public void notifyLectureEnded(UUID lectureId) {
        studentRepository.findAllByLectureId(lectureId).stream()
                .filter(s -> !s.isKicked())
                .forEach(s -> notificationPort.sendMessage(s.getChatId(),
                        "🔔 Лекция завершена. Спасибо за участие!"));
    }
}
