package by.presassistant.application.port.in;

import by.presassistant.domain.model.Student;

import java.util.List;
import java.util.UUID;

public interface GetStudentsUseCase {
    List<Student> execute(UUID lectureId);
}