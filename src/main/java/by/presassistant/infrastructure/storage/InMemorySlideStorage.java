package by.presassistant.infrastructure.storage;

import by.presassistant.application.port.out.SlideStoragePort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySlideStorage implements SlideStoragePort {

    // lectureId → slideNumber → imageBytes
    private final Map<UUID, Map<Integer, byte[]>> cache = new ConcurrentHashMap<>();

    @Override
    public void storeSlide(UUID lectureId, int slideNumber, byte[] imageBytes) {
        cache.computeIfAbsent(lectureId, k -> new ConcurrentHashMap<>())
             .put(slideNumber, imageBytes);
    }

    @Override
    public Optional<byte[]> getSlide(UUID lectureId, int slideNumber) {
        return Optional.ofNullable(cache.getOrDefault(lectureId, Collections.emptyMap())
                                       .get(slideNumber));
    }

    @Override
    public Set<UUID> getActiveLectureIds() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    @Override
    public void clear(UUID lectureId) {
        cache.remove(lectureId);
    }
}