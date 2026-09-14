CREATE TABLE schedules (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, course_section_id BIGINT UNSIGNED NOT NULL, lecturer_assignment_id BIGINT UNSIGNED NOT NULL,
 classroom_id BIGINT UNSIGNED NOT NULL, start_slot_id BIGINT UNSIGNED NOT NULL, end_slot_id BIGINT UNSIGNED NOT NULL,
 day_of_week TINYINT UNSIGNED NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL, status VARCHAR(20) NOT NULL, note VARCHAR(500),
 created_by BIGINT UNSIGNED NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY(course_section_id) REFERENCES course_sections(id), FOREIGN KEY(lecturer_assignment_id,course_section_id) REFERENCES lecturer_assignments(id,course_section_id),
 FOREIGN KEY(classroom_id) REFERENCES classrooms(id), FOREIGN KEY(start_slot_id) REFERENCES time_slots(id), FOREIGN KEY(end_slot_id) REFERENCES time_slots(id), FOREIGN KEY(created_by) REFERENCES users(id),
 CHECK(day_of_week BETWEEN 2 AND 8), CHECK(end_date>=start_date), CHECK(status IN ('DRAFT','PUBLISHED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
