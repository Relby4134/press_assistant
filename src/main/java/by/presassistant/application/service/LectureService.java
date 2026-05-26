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
    public LectureSession start(StartLectureCommand command) {
        LectureSession lecture = new LectureSession(UUID.randomUUID(), command.title(), command.fileUrl());
        LectureSession saved = lectureRepository.save(lecture);
        eventPublisher.publishEvent(new LectureStartedEvent(saved.getId(), saved.getTitle(), saved.getFilePath()));
        log.info("Lecture started: id={} title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Override
    @Transactional
    public void end(UUID lectureId) {
        LectureSession lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new LectureNotFoundException(lectureId));
        lecture.end();
        lectureRepository.save(lecture);
        slideStorage.clear(lectureId);
        eventPublisher.publishEvent(new LectureEndedEvent(lectureId));
        log.info("Lecture ended: id={}", lectureId);
    }

    @Override
    public LectureSession findById(UUID lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new LectureNotFoundException(lectureId));
    }

    @Override
    @Transactional
    public LectureSession changeSlide(ChangeSlideCommand command) {
        LectureSession lecture = lectureRepository.findById(command.lectureId())
                .orElseThrow(() -> new LectureNotFoundException(command.lectureId()));
        lecture.changeSlide(command.slideNumber());
        LectureSession saved = lectureRepository.save(lecture);
        eventPublisher.publishEvent(new SlideChangedEvent(command.lectureId(), command.slideNumber()));
        return saved;
    }

    @Override
    public void saveSlide(SaveSlideCommand command) {
        slideStorage.storeSlide(command.lectureId(), command.slideNumber(), command.imageBytes());
    }

    @Override
    public byte[] getFile(UUID lectureId) {
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
