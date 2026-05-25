package by.presassistant.infrastructure.analytics;

import by.presassistant.application.port.out.AnalyticsPort;
import by.presassistant.application.port.out.StudentRepository;
import by.presassistant.domain.model.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InMemoryAnalyticsAdapter implements AnalyticsPort {

    // lectureId → chatId → slideNumber → count
    private final Map<UUID, Map<Long, Map<Integer, Integer>>> cache = new ConcurrentHashMap<>();

    private final StudentRepository studentRepository;

    @Override
    public void recordRequest(UUID lectureId, Long chatId, int slideNumber) {
        cache.computeIfAbsent(lectureId, k -> new ConcurrentHashMap<>())
             .computeIfAbsent(chatId, k -> new ConcurrentHashMap<>())
             .merge(slideNumber, 1, Integer::sum);
    }

    @Override
    public List<StudentAnalytics> getAnalytics(UUID lectureId) {
        Map<Long, Map<Integer, Integer>> byStudent =
                cache.getOrDefault(lectureId, Collections.emptyMap());
        if (byStudent.isEmpty()) return Collections.emptyList();

        List<Student> students = studentRepository.findAllByLectureId(lectureId);
        return students.stream()
                .filter(s -> byStudent.containsKey(s.getChatId()))
                .map(s -> {
                    Map<Integer, Integer> slideCounts = byStudent.get(s.getChatId());
                    int total = slideCounts.values().stream().mapToInt(Integer::intValue).sum();
                    List<StudentAnalytics.SlideStats> slides = slideCounts.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> new StudentAnalytics.SlideStats(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());
                    return new StudentAnalytics(s.getChatId(), s.getFirstName(),
                            s.getUsername(), s.isKicked(), total, slides);
                })
                .sorted(Comparator.comparingInt(StudentAnalytics::totalRequests).reversed())
                .collect(Collectors.toList());
    }
}
