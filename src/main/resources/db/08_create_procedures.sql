-- Optional reporting procedures. Run with the MySQL client after schema setup.
DELIMITER $$
CREATE PROCEDURE sp_get_student_timetable(IN p_student_id BIGINT UNSIGNED, IN p_semester_id BIGINT UNSIGNED)
BEGIN
 SELECT t.* FROM vw_student_timetable t JOIN course_sections cs ON cs.id=t.course_section_id WHERE t.student_id=p_student_id AND cs.semester_id=p_semester_id;
END$$
CREATE PROCEDURE sp_get_lecturer_timetable(IN p_lecturer_id BIGINT UNSIGNED, IN p_semester_id BIGINT UNSIGNED)
BEGIN
 SELECT t.* FROM vw_lecturer_timetable t JOIN course_sections cs ON cs.id=t.course_section_id WHERE t.lecturer_id=p_lecturer_id AND cs.semester_id=p_semester_id;
END$$
CREATE PROCEDURE sp_find_available_rooms(IN p_date DATE, IN p_start_order INT, IN p_end_order INT, IN p_capacity INT)
BEGIN
 SELECT r.* FROM classrooms r WHERE r.status='AVAILABLE' AND r.capacity>=p_capacity
 AND NOT EXISTS(SELECT 1 FROM vw_schedule_details s WHERE s.classroom_id=r.id AND s.status<>'CANCELLED' AND s.day_of_week=WEEKDAY(p_date)+2 AND p_date BETWEEN s.start_date AND s.end_date AND s.start_order<=p_end_order AND s.end_order>=p_start_order)
 AND NOT EXISTS(SELECT 1 FROM maintenance_records m WHERE m.classroom_id=r.id AND m.status IN ('REPORTED','IN_PROGRESS') AND m.start_date<=p_date AND (m.end_date IS NULL OR m.end_date>=p_date));
END$$
DELIMITER ;
