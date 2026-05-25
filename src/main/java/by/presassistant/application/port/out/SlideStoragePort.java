package by.presassistant.application.port.out;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SlideStoragePort {
    void storeSlide(UUID lectureId, int slideNumber, byte[] imageBytes);
    Optional<byte[]> getSlide(UUID lectureId, int slideNumber);
    Set<UUID> getActiveLectureIds();
    void clear(UUID lectureId);
}
