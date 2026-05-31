package by.presassistant.infrastructure.persistence.adapter;

import by.presassistant.application.port.out.StudentRepository;
import by.presassistant.domain.model.Student;
import by.presassistant.infrastructure.persistence.entity.StudentEntity;
import by.presassistant.infrastructure.persistence.repository.StudentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StudentRepositoryAdapter implements StudentRepository {

    private final StudentJpaRepository jpa;

    @Override
    public Student save(Student student) {
        return toDomain(jpa.save(toEntity(student)));
    }

    @Override
    public Optional<Student> findByChatIdAndLectureId(Long chatId, UUID lectureId) {
        return jpa.findByChatIdAndLectureSessionId(chatId, lectureId).map(this::toDomain);
    }

    @Override
    public Optional<Student> findActiveStudentByChatId(Long chatId) {
        return jpa.findActiveByChatId(chatId).map(this::toDomain);
    }

    @Override
    public List<Student> findAllByLectureId(UUID lectureId) {
        return jpa.findByLectureSessionId(lectureId).stream()
                  .map(this::toDomain)
                  .collect(Collectors.toList());
    }

    private Student toDomain(StudentEntity e) {
        return new Student(e.getId(), e.getChatId(), e.getFirstName(),
                e.getUsername(), e.isKicked(), e.getJoinedAt(), e.getLectureSessionId(), e.getFullName());
    }

    private StudentEntity toEntity(Student s) {
        return StudentEntity.builder()
                .id(s.getId())
                .chatId(s.getChatId())
                .firstName(s.getFirstName())
                .username(s.getUsername())
                .fullName(s.getFullName())
                .lectureSessionId(s.getLectureSessionId())
                .joinedAt(s.getJoinedAt())
                .kicked(s.isKicked())
                .build();
    }
}