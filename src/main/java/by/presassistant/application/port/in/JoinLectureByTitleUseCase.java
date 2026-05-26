package by.presassistant.application.port.in;

import by.presassistant.domain.model.Student;

public interface JoinLectureByTitleUseCase {
    Student joinByTitle(Long chatId, String firstName, String username, String title);
}