package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.CourseSectionService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class CourseSectionController {
    private final CourseSectionService service;

    public CourseSectionController(CourseSectionService service) { this.service = service; }

    public List<CourseSection> findAll() { return service.findAll(); }

    public CourseSection save(CourseSection section) { return service.save(section); }

    public List<CourseSection> search(User user, String keyword, String semester, String department, String status) {
        return service.findForUser(user).stream()
                .filter(section -> semester == null || semester.startsWith("Tất cả")
                        || semester.equals("Học kỳ hiện tại") && !section.getSemester().getStartDate().isAfter(LocalDate.now()) && !section.getSemester().getEndDate().isBefore(LocalDate.now())
                        || section.getSemester().getName().equals(semester)
                        || section.getSemester().toString().equals(semester))
                .filter(section -> department == null || department.startsWith("Tất cả") || section.getCourse().getDepartment().getName().equals(department))
                .filter(section -> status == null || status.startsWith("Tất cả") || section.getStatus().getDisplayName().equals(status))
                .filter(section -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(section.getCode(), keyword)
                        || TextUtils.containsIgnoreAccent(section.getCourse().getName(), keyword))
                .toList();
    }

    private boolean allowedForRole(CourseSection section, User user) {
        if (user.getRole() == Role.LECTURER) return section.getLecturer() != null && section.getLecturer().getId().equals(user.getId());
        if (user instanceof Student student) return student.getClassCode().toUpperCase(java.util.Locale.ROOT).startsWith(section.getCourse().getDepartment().getCode().toUpperCase(java.util.Locale.ROOT));
        return true;
    }
    public void assignLecturer(CourseSection section, Lecturer lecturer) { service.assignLecturer(section, lecturer); }
    public void enroll(Long sectionId,Long studentId,boolean active) { service.enroll(sectionId,studentId,active); }
}
