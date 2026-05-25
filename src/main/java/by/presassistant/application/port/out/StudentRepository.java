package by.presassistant.application.port.out;

import by.presassistant.domain.model.Student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {
    Student save(Student student);
    Optional<Student> findByChatIdAndLectureId(Long chatId, UUID lectureId);
    Optional<Student> findActiveStudentByChatId(Long chatId);
    List<Student> findAllByLectureId(UUID lectureId);
}
