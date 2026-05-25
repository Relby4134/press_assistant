package by.presassistant.infrastructure.lifecycle;

import by.presassistant.application.service.LectureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupCleanup {

    private final LectureService lectureService;

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        lectureService.endAllActiveLectures();
        log.info("Startup cleanup complete");
    }
}