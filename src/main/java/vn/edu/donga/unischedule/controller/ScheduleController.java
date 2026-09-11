package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.ScheduleService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class ScheduleController {
    private final ScheduleService service;

    public ScheduleController(ScheduleService service) { this.service = service; }

    public List<ScheduleEntry> findAll() { return service.findAll(); }

    public List<ScheduleEntry> findByWeekForUser(LocalDate weekStart, User user) { return service.findByWeekForUser(weekStart, user); }

    public ScheduleEntry save(ScheduleEntry entry) { return service.save(entry); }

    public boolean delete(Long id) { return service.delete(id); }
    public List<ScheduleEntry> search(LocalDate weekStart, User user, String keyword, String semester, String department, String lecturer, String room) {
        return service.findByWeekForUser(weekStart, user).stream()
                .filter(entry -> semester == null || semester.startsWith("Tất cả") || entry.getCourseSection().getSemester().getName().equals(semester))
                .filter(entry -> department == null || department.startsWith("Tất cả") || entry.getCourseSection().getCourse().getDepartment().getName().equals(department))
                .filter(entry -> lecturer == null || lecturer.startsWith("Tất cả") || entry.getCourseSection().getLecturer().getFullName().equals(lecturer))
                .filter(entry -> room == null || room.startsWith("Tất cả") || entry.getRoom().getCode().equals(room))
                .filter(entry -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(entry.getCourseSection().getCode(), keyword)
                        || TextUtils.containsIgnoreAccent(entry.getCourseSection().getCourse().getName(), keyword)
                        || TextUtils.containsIgnoreAccent(entry.getCourseSection().getLecturer().getFullName(), keyword)
                        || TextUtils.containsIgnoreAccent(entry.getRoom().getCode(), keyword))
                .toList();
    }
    public List<ScheduleEntry> forRoom(Long roomId) {
        return service.findAll().stream().filter(entry -> entry.getRoom().getId().equals(roomId)).toList();
    }
}
