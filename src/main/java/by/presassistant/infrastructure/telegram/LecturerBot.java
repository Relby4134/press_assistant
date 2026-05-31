package by.presassistant.infrastructure.telegram;

import by.presassistant.application.command.AskQuestionCommand;
import by.presassistant.application.command.JoinLectureCommand;
import by.presassistant.application.port.in.*;
import by.presassistant.application.port.out.NotificationPort;
import by.presassistant.domain.exception.LectureNotFoundException;
import by.presassistant.domain.exception.QuestionLimitExceededException;
import by.presassistant.domain.model.LectureSession;
import by.presassistant.application.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LecturerBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final StudentService studentService;
    private final GetLectureUseCase getLecture;
    private final NotificationPort notificationPort;
    private final String botToken;
    private final Set<String> enabledCommands;

    // Values: "join" | "name:<lectureId>"
    private final Map<Long, String> pendingAction = new ConcurrentHashMap<>();

    public LecturerBot(@Value("${telegram.bot.token}") String botToken,
                       @Value("${bot.enabled.commands:join,slide,questions}") String enabledCommandsRaw,
                       TelegramClient telegramClient,
                       StudentService studentService,
                       GetLectureUseCase getLecture,
                       NotificationPort notificationPort) {
        this.telegramClient = telegramClient;
        this.botToken = botToken;
        this.studentService = studentService;
        this.getLecture = getLecture;
        this.notificationPort = notificationPort;
        this.enabledCommands = Arrays.stream(enabledCommandsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override public String getBotToken() { return botToken; }
    @Override public LongPollingUpdateConsumer getUpdatesConsumer() { return this; }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::handleUpdate);
    }

    private void handleUpdate(Update update) {
        Long chatId = null;
        try {
            if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getFrom().getId();
                handleCallbackQuery(update.getCallbackQuery());
                return;
            }
            if (!update.hasMessage() || !update.getMessage().hasText()) return;

            chatId = update.getMessage().getChatId();
            String text      = update.getMessage().getText().trim();
            String firstName = update.getMessage().getFrom().getFirstName();
            String username  = update.getMessage().getFrom().getUserName();

            if (text.startsWith("/")) pendingAction.remove(chatId);

            if      (text.startsWith("/start")) handleStart(chatId, firstName, username, text);
            else if (text.equals("/slide"))     { if (enabledCommands.contains("slide")) handleSlideRequest(chatId); }
            else if (text.equals("/help"))      handleHelp(chatId);
            else if (text.startsWith("/join"))  { if (enabledCommands.contains("join")) handleJoin(chatId, firstName, username, text); }
            else if (pendingAction.containsKey(chatId))
                handlePending(chatId, firstName, username, text);
            else if (enabledCommands.contains("questions"))
                handleQuestion(chatId, firstName, username, text);

        } catch (Exception e) {
            log.error("Error handling update: {}", e.getMessage(), e);
            if (chatId != null)
                notificationPort.sendMessage(chatId, "❌ Произошла ошибка. Попробуйте ещё раз.");
        }
    }

    private void handleStart(long chatId, String firstName, String username, String text) {
        String[] parts = text.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            String joinHint = enabledCommands.contains("join")
                    ? "\nили используйте: /join <название лекции>" : "";
            notificationPort.sendMessage(chatId,
                    "👋 Привет, " + firstName + "!\n\n" +
                    "Для подключения к лекции отсканируйте QR-код" + joinHint);
            return;
        }
        try {
            UUID lectureId = UUID.fromString(parts[1].trim());
            LectureSession lecture = getLecture.findById(lectureId);
            if (lecture.isRequireNames()) {
                pendingAction.put(chatId, "name:" + lectureId);
                notificationPort.sendMessage(chatId,
                        "👤 Для участия в лекции «" + lecture.getTitle() + "» введите вашу фамилию и имя:");
                return;
            }
            studentService.join(new JoinLectureCommand(chatId, firstName, username, lectureId, null));
        } catch (IllegalArgumentException e) {
            notificationPort.sendMessage(chatId, "❌ Неверный формат ссылки.");
        } catch (LectureNotFoundException e) {
            notificationPort.sendMessage(chatId, "❌ Лекция не найдена или уже завершена.");
        }
    }

    private void handleJoin(long chatId, String firstName, String username, String text) {
        String[] parts = text.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            pendingAction.put(chatId, "join");
            notificationPort.sendMessage(chatId, "Введите название лекции:");
            return;
        }
        joinByTitle(chatId, firstName, username, parts[1].trim());
    }

    private void handlePending(long chatId, String firstName, String username, String text) {
        String action = pendingAction.remove(chatId);
        if ("join".equals(action)) {
            joinByTitle(chatId, firstName, username, text);
        } else if (action != null && action.startsWith("name:")) {
            UUID lectureId = UUID.fromString(action.substring(5));
            try {
                studentService.join(new JoinLectureCommand(chatId, firstName, username, lectureId, text.trim()));
            } catch (LectureNotFoundException e) {
                notificationPort.sendMessage(chatId, "❌ Лекция не найдена или уже завершена.");
            }
        }
    }

    private void joinByTitle(long chatId, String firstName, String username, String title) {
        try {
            LectureSession lecture = getLecture.findActiveByTitle(title);
            if (lecture.isRequireNames()) {
                pendingAction.put(chatId, "name:" + lecture.getId());
                notificationPort.sendMessage(chatId,
                        "👤 Для участия в лекции «" + lecture.getTitle() + "» введите вашу фамилию и имя:");
                return;
            }
            studentService.join(new JoinLectureCommand(chatId, firstName, username, lecture.getId(), null));
        } catch (LectureNotFoundException e) {
            notificationPort.sendMessage(chatId, "❌ Лекция \"" + title + "\" не найдена.");
        }
    }

    private void handleSlideRequest(long chatId) {
        studentService.findActiveLecture(chatId).ifPresentOrElse(
                lectureId -> {
                    LectureSession lecture = getLecture.findById(lectureId);
                    int slideNumber = lecture.getCurrentSlide();
                    studentService.recordSlideRequest(lectureId, chatId, slideNumber);
                    studentService.getSlideImageBytes(lectureId, slideNumber)
                            .ifPresentOrElse(
                                    bytes -> notificationPort.sendSlideImage(chatId, bytes, slideNumber),
                                    () -> notificationPort.sendMessage(chatId,
                                            "📊 Текущий слайд: " + slideNumber)
                            );
                },
                () -> notificationPort.sendMessage(chatId,
                        "Вы не подключены к лекции.\nОтсканируйте QR-код или используйте /join <название>."));
    }

    private void handleQuestion(long chatId, String firstName, String username, String text) {
        studentService.findActiveLecture(chatId).ifPresentOrElse(
                lectureId -> {
                    if (studentService.isStudentKicked(chatId, lectureId)) {
                        notificationPort.sendMessage(chatId, "❌ Вы были удалены с лекции.");
                        return;
                    }
                    try {
                        String displayName = studentService.getStudentDisplayName(chatId, lectureId);
                        studentService.ask(new AskQuestionCommand(lectureId, chatId, displayName, text));
                    } catch (QuestionLimitExceededException e) {
                        notificationPort.sendMessage(chatId,
                                "❌ Вы достигли лимита вопросов (10). Новые вопросы не принимаются.");
                    }
                },
                () -> notificationPort.sendMessage(chatId,
                        "Вы не подключены к лекции.\nОтсканируйте QR-код или используйте /join <название>."));
    }

    private void handleCallbackQuery(CallbackQuery query) {
        long chatId = query.getFrom().getId();
        try {
            telegramClient.execute(
                    org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                            .callbackQueryId(query.getId()).build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback: {}", e.getMessage());
        }
        if ("show_slide".equals(query.getData()) && enabledCommands.contains("slide"))
            handleSlideRequest(chatId);
    }

    private void handleHelp(long chatId) {
        StringBuilder sb = new StringBuilder("📖 Доступные команды:\n\n");
        if (enabledCommands.contains("join"))
            sb.append("/join <название> — подключиться к лекции\n");
        if (enabledCommands.contains("slide"))
            sb.append("/slide — получить текущий слайд\n");
        sb.append("/help — показать это сообщение");
        if (enabledCommands.contains("questions"))
            sb.append("\n\nЧтобы задать вопрос лектору — просто напишите его текстом.");
        notificationPort.sendMessage(chatId, sb.toString());
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        try {
            List<BotCommand> commands = new ArrayList<>();
            if (enabledCommands.contains("join"))
                commands.add(BotCommand.builder().command("join").description("Подключиться к лекции").build());
            if (enabledCommands.contains("slide"))
                commands.add(BotCommand.builder().command("slide").description("Получить текущий слайд").build());
            commands.add(BotCommand.builder().command("help").description("Помощь").build());
            telegramClient.execute(SetMyCommands.builder().commands(commands).build());
        } catch (TelegramApiException e) {
            log.error("Failed to set commands: {}", e.getMessage());
        }
        log.info("Bot registered with commands: {}", enabledCommands);
    }
}