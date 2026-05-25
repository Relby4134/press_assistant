package by.presassistant.infrastructure.lifecycle;

import by.presassistant.application.port.out.SlideStoragePort;
import by.presassistant.application.service.LectureService;
import by.presassistant.application.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShutdownNotifier {

    private final SlideStoragePort slideStorage;
    private final StudentService studentService;
    private final LectureService lectureService;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        slideStorage.getActiveLectureIds().forEach(lectureId -> {
            log.info("Shutdown: notifying students for lecture={}", lectureId);
            studentService.notifyServerShutdown(lectureId);
        });
        lectureService.endAllActiveLectures();
        log.info("Shutdown cleanup complete");
    }
}