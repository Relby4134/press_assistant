package by.presassistant.application.service;

import by.presassistant.application.command.ChangeSlideCommand;
import by.presassistant.application.command.SaveSlideCommand;
import by.presassistant.application.command.StartLectureCommand;
import by.presassistant.application.port.in.*;
import by.presassistant.application.port.out.LectureRepository;
import by.presassistant.application.port.out.SlideStoragePort;
import by.presassistant.domain.event.LectureEndedEvent;
import by.presassistant.domain.event.LectureStartedEvent;
import by.presassistant.domain.event.SlideChangedEvent;
import by.presassistant.domain.exception.LectureNotFoundException;
import by.presassistant.domain.model.LectureSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureService implements
        StartLectureUseCase,
        EndLectureUseCase,
        GetLectureUseCase,
        ChangeSlideUseCase,
        SaveSlideUseCase,
        GetCurrentSlideFileUseCase {

    private final LectureRepository lectureRepository;
    private final SlideStoragePort slideStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LectureSession execute(StartLectureCommand command) {
        LectureSession lecture = new LectureSession(UUID.randomUUID(), command.title(), command.fileUrl());
        LectureSession saved = lectureRepository.save(lecture);
        eventPublisher.publishEvent(new LectureStartedEvent(saved.getId(), saved.getTitle()));
        log.info("Lecture started: id={} title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Override
    @Transactional
    public void execute(UUID lectureId) {
        LectureSession lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new LectureNotFoundException(lectureId));
        lecture.end();
        lectureRepository.save(lecture);
        slideStorage.clear(lectureId);
        eventPublisher.publishEvent(new LectureEndedEvent(lectureId));
        log.info("Lecture ended: id={}", lectureId);
    }

    @Override
    public LectureSession execute(UUID lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new LectureNotFoundException(lectureId));
    }

    @Override
    @Transactional
    public LectureSession execute(ChangeSlideCommand command) {
        LectureSession lecture = lectureRepository.findById(command.lectureId())
                .orElseThrow(() -> new LectureNotFoundException(command.lectureId()));
        lecture.changeSlide(command.slideNumber());
        LectureSession saved = lectureRepository.save(lecture);
        eventPublisher.publishEvent(new SlideChangedEvent(command.lectureId(), command.slideNumber()));
        return saved;
    }

    @Override
    public void execute(SaveSlideCommand command) {
        try {
            byte[] imageBytes = Files.readAllBytes(Path.of(command.imagePath()));
            slideStorage.storeSlide(command.lectureId(), command.slideNumber(), imageBytes);
        } catch (IOException e) {
            log.error("Failed to read slide image: {}", command.imagePath(), e);
        }
    }

    @Override
    public byte[] execute(UUID lectureId) {
        LectureSession lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new LectureNotFoundException(lectureId));
        return slideStorage.getSlide(lectureId, lecture.getCurrentSlide())
                .orElseThrow(() -> new RuntimeException("Slide image not found"));
    }

    @Transactional
    public void endAllActiveLectures() {
        List<LectureSession> active = lectureRepository.findAllActive();
        active.forEach(l -> {
            l.end();
            lectureRepository.save(l);
        });
        if (!active.isEmpty()) {
            log.info("Ended {} stale active lecture(s)", active.size());
        }
    }
}
