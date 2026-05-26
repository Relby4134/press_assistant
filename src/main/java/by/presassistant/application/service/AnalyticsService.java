package by.presassistant.application.service;

import by.presassistant.application.port.in.GetAnalyticsUseCase;
import by.presassistant.application.port.out.AnalyticsPort;
import by.presassistant.infrastructure.analytics.StudentAnalytics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService implements GetAnalyticsUseCase {

    private final AnalyticsPort analyticsPort;

    @Override
    public List<StudentAnalytics> getAnalytics(UUID lectureId) {
        return analyticsPort.getAnalytics(lectureId);
    }
}
