package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.Conflict;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.TimetablePeriod;
import vn.edu.donga.unischedule.model.Student;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.ConflictType;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.RoomStatus;
import vn.edu.donga.unischedule.repository.ScheduleRepository;
import vn.edu.donga.unischedule.validation.ValidationException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Owns schedule validation and keeps the UI independent from persistence details.
 */
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ConflictService conflictService;

    public ScheduleService(ScheduleRepository scheduleRepository, ConflictService conflictService) {
        this.scheduleRepository = scheduleRepository;
        this.conflictService = conflictService;
    }

    public List<ScheduleEntry> findAll() {
        return scheduleRepository.findAll();
    }

    public List<ScheduleEntry> findByWeek(LocalDate weekStart) {
        return scheduleRepository.findByWeek(weekStart);
    }

    public List<ScheduleEntry> findByWeekForUser(LocalDate weekStart, User user) {
        if (scheduleRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcScheduleRepository jdbc)
            return visibleRows(jdbc.findForUser(weekStart, user), weekStart, user);
        List<ScheduleEntry> entries = scheduleRepository.findByWeek(weekStart);
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ACADEMIC) {
            return visibleRows(entries, weekStart, user);
        }
        if (user.getRole() == Role.LECTURER) {
            return visibleRows(entries.stream()
                    .filter(entry -> entry.getCourseSection().getLecturer().getId().equals(user.getId()))
                    .toList(), weekStart, user);
        }
        if (user instanceof Student student) {
            return visibleRows(entries.stream()
                    .filter(entry -> entry.getCourseSection().getCourse().getDepartment().getCode()
                            .equalsIgnoreCase(student.getClassCode().substring(0, 4)))
                    .toList(), weekStart, user);
        }
        return visibleRows(entries, weekStart, user);
    }

    /** Read-only institutional calendar. Students and lecturers see published entries only. */
    public List<ScheduleEntry> findPublishedByWeekForUser(LocalDate weekStart, User user) {
        return visibleRows(scheduleRepository.findByWeek(weekStart), weekStart, user).stream()
                .filter(entry -> entry.getStatus() == vn.edu.donga.unischedule.model.Enums.ScheduleStatus.PUBLISHED)
                .toList();
    }

    private List<ScheduleEntry> visibleRows(List<ScheduleEntry> entries, LocalDate weekStart, User user) {
        LocalDate today = LocalDate.now();
        return entries.stream()
                .filter(entry -> TimetablePeriod.visibleTo(entry.getCourseSection().getSemester(), user.getRole(), today))
                .filter(entry -> {
                    LocalDate classDay = weekStart.plusDays(entry.getDayOfWeek() - 2L);
                    return !classDay.isBefore(entry.getStartDate()) && !classDay.isAfter(entry.getEndDate());
                })
                .toList();
    }

    public ScheduleEntry save(ScheduleEntry entry) {
        validate(entry);
        return scheduleRepository.save(entry);
    }

    public boolean delete(Long id) {
        return scheduleRepository.deleteById(id);
    }

    public void validate(ScheduleEntry entry) {
        if (entry.getCourseSection() == null) {
            throw new ValidationException("Lớp học phần không được để trống.");
        }
        if (entry.getRoom() == null) {
            throw new ValidationException("Phòng học không được để trống.");
        }
        if (entry.getStartSlot() == null || entry.getEndSlot() == null) {
            throw new ValidationException("Ca bắt đầu và ca kết thúc không được để trống.");
        }
        if (entry.getDayOfWeek() < 2 || entry.getDayOfWeek() > 8) throw new ValidationException("Ngày trong tuần không hợp lệ.");
        if (entry.getStartSlot().getOrder() > entry.getEndSlot().getOrder()) {
            throw new ValidationException("Ca kết thúc phải sau ca bắt đầu.");
        }
        if (entry.getStartDate() == null || entry.getEndDate() == null) {
            throw new ValidationException("Ngày bắt đầu và ngày kết thúc không được để trống.");
        }
        if (entry.getEndDate().isBefore(entry.getStartDate())) {
            throw new ValidationException("Ngày kết thúc không được trước ngày bắt đầu.");
        }
        if (entry.getRoom().getRoomStatus() == RoomStatus.MAINTENANCE
                || entry.getRoom().getRoomStatus() == RoomStatus.INACTIVE) {
            throw new ValidationException("Phòng " + entry.getRoom().getCode() + " không sẵn sàng để xếp lịch.");
        }
        List<Conflict> conflicts = conflictService.findConflictsFor(entry, entry.getId());
        List<Conflict> blocking = conflicts.stream()
                .filter(conflict -> conflict.getType() != ConflictType.CAPACITY)
                .toList();
        if (!blocking.isEmpty()) {
            String detail = blocking.stream()
                    .map(conflict -> "- " + conflict.getType().getDisplayName() + ": " + conflict.getMessage())
                    .collect(Collectors.joining("\n"));
            throw new ValidationException("Không thể lưu lịch vì có xung đột:\n" + detail);
        }
        conflicts.stream()
                .filter(conflict -> conflict.getType() == ConflictType.CAPACITY)
                .findFirst()
                .ifPresent(conflict -> {
                    throw new ValidationException(conflict.getMessage());
                });
    }
}
