-- READ ONLY. Select only a simulation database before running.
-- First two queries: zero rows means no enrollment/assignment inconsistencies.
SELECT cs.id, cs.student_count, COUNT(e.id) actual_count
FROM course_sections cs LEFT JOIN student_enrollments e
  ON e.course_section_id=cs.id AND e.status='ACTIVE'
GROUP BY cs.id,cs.student_count HAVING cs.student_count<>COUNT(e.id);

SELECT s.id FROM schedules s JOIN lecturer_assignments a ON a.id=s.lecturer_assignment_id
WHERE s.course_section_id<>a.course_section_id;

-- Must return 0. Includes room, lecturer, section and individual student collisions.
SELECT COUNT(*) AS conflicting_pairs
FROM schedules a JOIN schedules b ON a.id<b.id
 AND a.status='PUBLISHED' AND b.status='PUBLISHED'
 AND a.day_of_week=b.day_of_week AND a.start_date<=b.end_date AND b.start_date<=a.end_date
 AND a.start_slot_id<=b.end_slot_id AND b.start_slot_id<=a.end_slot_id
JOIN lecturer_assignments la ON la.id=a.lecturer_assignment_id
JOIN lecturer_assignments lb ON lb.id=b.lecturer_assignment_id
WHERE a.classroom_id=b.classroom_id OR la.lecturer_id=lb.lecturer_id OR a.course_section_id=b.course_section_id
 OR EXISTS (SELECT 1 FROM student_enrollments ea JOIN student_enrollments eb ON ea.student_id=eb.student_id
    WHERE ea.course_section_id=a.course_section_id AND eb.course_section_id=b.course_section_id
    AND ea.status='ACTIVE' AND eb.status='ACTIVE');

-- Independently expand recurring schedules and compute valid sessions/minutes.
WITH RECURSIVE occurrences AS (
 SELECT id, end_date, start_slot_id, end_slot_id,
   DATE_ADD(start_date,INTERVAL MOD(day_of_week-2-WEEKDAY(start_date)+7,7) DAY) lesson_date
 FROM schedules WHERE status='PUBLISHED'
 UNION ALL
 SELECT id,end_date,start_slot_id,end_slot_id,DATE_ADD(lesson_date,INTERVAL 7 DAY)
 FROM occurrences WHERE DATE_ADD(lesson_date,INTERVAL 7 DAY)<=end_date
)
SELECT YEAR(o.lesson_date) year,COUNT(DISTINCT o.id,o.lesson_date) sessions,
 COUNT(*) used_room_slots,SUM(TIME_TO_SEC(TIMEDIFF(t.end_time,t.start_time))/60) teaching_minutes
FROM occurrences o JOIN time_slots t ON t.id BETWEEN o.start_slot_id AND o.end_slot_id
WHERE o.lesson_date<=o.end_date GROUP BY YEAR(o.lesson_date) ORDER BY year;

SELECT status,COUNT(*) users FROM users GROUP BY status;
SELECT status,COUNT(*) requests FROM change_requests GROUP BY status;
