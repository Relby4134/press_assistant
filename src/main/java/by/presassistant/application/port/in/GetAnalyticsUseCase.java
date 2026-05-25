package by.presassistant.application.port.in;

import by.presassistant.infrastructure.analytics.StudentAnalytics;

import java.util.List;
import java.util.UUID;

public interface GetAnalyticsUseCase {
    List<StudentAnalytics> execute(UUID lectureId);
}