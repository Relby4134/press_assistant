package by.presassistant.infrastructure.web;

import by.presassistant.application.command.ChangeSlideCommand;
import by.presassistant.application.command.SaveSlideCommand;
import by.presassistant.application.command.StartLectureCommand;
import by.presassistant.application.port.in.*;
import by.presassistant.application.service.StudentService;
import by.presassistant.domain.model.LectureSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/lecture")
@RequiredArgsConstructor
public class LectureController {

    private final StartLectureUseCase startLecture;
    private final EndLectureUseCase endLecture;
    private final GetLectureUseCase getLecture;
    private final ChangeSlideUseCase changeSlide;
    private final SaveSlideUseCase saveSlide;
    private final GetCurrentSlideFileUseCase getCurrentSlideFile;
    private final StudentService studentService;

    @PostMapping("/start")
    public LectureSession start(@RequestParam String title,
                                @RequestParam(required = false) String fileUrl) {
        return startLecture.execute(new StartLectureCommand(title, fileUrl));
    }

    @GetMapping("/{id}")
    public LectureSession get(@PathVariable UUID id) {
        return getLecture.execute(id);
    }

    @PostMapping("/{id}/slide-changed")
    public LectureSession slideChanged(@PathVariable UUID id,
                                       @RequestBody java.util.Map<String, Integer> body) {
        return changeSlide.execute(new ChangeSlideCommand(id, body.get("slideNumber")));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Void> end(@PathVariable UUID id) {
        studentService.notifyLectureEnded(id);
        endLecture.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/slides")
    public ResponseEntity<Void> uploadSlide(@PathVariable UUID id,
                                            @RequestBody java.util.Map<String, Object> body) {
        saveSlide.execute(new SaveSlideCommand(id,
                (Integer) body.get("slideNumber"),
                (String) body.get("imagePath")));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/current-slide/file")
    public ResponseEntity<byte[]> currentSlideFile(@PathVariable UUID id) {
        byte[] bytes = getCurrentSlideFile.execute(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes);
    }

    @GetMapping("/ping")
    public String ping() { return "pong"; }
}