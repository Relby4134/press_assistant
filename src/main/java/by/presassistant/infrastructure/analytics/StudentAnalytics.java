package by.presassistant.infrastructure.analytics;

import java.util.List;

public record StudentAnalytics(
        Long chatId,
        String firstName,
        String username,
        boolean kicked,
        int totalRequests,
        List<SlideStats> slides
) {
    public record SlideStats(int slideNumber, int count) {}
}