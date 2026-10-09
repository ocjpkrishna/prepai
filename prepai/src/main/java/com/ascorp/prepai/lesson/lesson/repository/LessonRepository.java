package com.ascorp.prepai.lesson.lesson.repository;

import com.ascorp.prepai.lesson.lesson.model.entity.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	Optional<Lesson> findByIdAndUserId(UUID id, UUID userId);

	Page<Lesson> findByUserId(UUID userId, Pageable pageable);

	Page<Lesson> findByUserIdAndSubject(UUID userId, String subject, Pageable pageable);

	List<Lesson> findByUserIdOrderByCreatedAtAsc(UUID userId);

	void deleteByUserId(UUID userId);
}
