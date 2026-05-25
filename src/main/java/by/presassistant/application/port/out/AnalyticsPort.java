package by.presassistant.application.port.out;

import by.presassistant.infrastructure.analytics.StudentAnalytics;

import java.util.List;
import java.util.UUID;

public interface AnalyticsPort {
    void recordRequest(UUID lectureId, Long chatId, int slideNumber);
    List<StudentAnalytics> getAnalytics(UUID lectureId);
}
