package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.Conflict;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.Enums.ConflictStatus;
import vn.edu.donga.unischedule.model.Enums.ConflictType;
import vn.edu.donga.unischedule.model.Enums.ScheduleStatus;
import vn.edu.donga.unischedule.repository.ScheduleRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDate;

/**
 * Checks mock schedule data for room, lecturer and course-section conflicts.
 */
public class ConflictService {
    private final ScheduleRepository scheduleRepository;
    private final Map<String, ConflictStatus> statusByConflictId = new HashMap<>();

    public ConflictService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public List<Conflict> findAllConflicts() {
        return detect(scheduleRepository.findAll());
    }

    public List<Conflict> findConflictsInWeek(LocalDate weekStart) {
        return detect(scheduleRepository.findByWeek(weekStart));
    }

    private List<Conflict> detect(List<ScheduleEntry> schedules) {
        List<ScheduleEntry> entries = schedules.stream()
                .filter(entry -> entry.getStatus() != ScheduleStatus.CANCELLED)
                .sorted(Comparator.comparing(ScheduleEntry::getId))
                .toList();
        List<Conflict> result = new ArrayList<>();
        Map<String, List<ScheduleEntry>> candidates = new HashMap<>();
        Set<String> compared = new HashSet<>();
        for (ScheduleEntry entry : entries) {
            String[] keys = {
                "R:" + entry.getDayOfWeek() + ":" + entry.getRoom().getId(),
                "L:" + entry.getDayOfWeek() + ":" + entry.getCourseSection().getLecturer().getId(),
                "C:" + entry.getDayOfWeek() + ":" + entry.getCourseSection().getId()
            };
            for (String key : keys) {
                List<ScheduleEntry> earlier = candidates.computeIfAbsent(key, ignored -> new ArrayList<>());
                for (ScheduleEntry other : earlier) {
                    if (other.getEndDate().isBefore(entry.getStartDate())
                            || entry.getEndDate().isBefore(other.getStartDate())
                            || other.getStartSlot().getOrder() > entry.getEndSlot().getOrder()
                            || entry.getStartSlot().getOrder() > other.getEndSlot().getOrder()) continue;
                    String pair = other.getId() + ":" + entry.getId();
                    if (compared.add(pair)) result.addAll(compare(other, entry));
                }
                earlier.add(entry);
            }
        }
        return result;
    }

    public List<Conflict> findConflictsFor(ScheduleEntry candidate, Long excludedScheduleId) {
        List<Conflict> result = new ArrayList<>();
        for (ScheduleEntry existing : scheduleRepository.findAll()) {
            if (existing.getStatus() == ScheduleStatus.CANCELLED) {
                continue;
            }
            if (excludedScheduleId != null && existing.getId().equals(excludedScheduleId)) {
                continue;
            }
            result.addAll(compare(candidate, existing));
        }
        if (candidate.getRoom() != null
                && candidate.getCourseSection() != null
                && candidate.getRoom().getCapacity() < candidate.getCourseSection().getStudentCount()) {
            String id = "CF-CAPACITY-" + value(candidate.getId()) + "-" + candidate.getRoom().getId();
            result.add(new Conflict(id, ConflictType.CAPACITY, candidate, null,
                    "Phòng " + candidate.getRoom().getCode() + " chỉ có " + candidate.getRoom().getCapacity()
                            + " chỗ, nhỏ hơn sĩ số " + candidate.getCourseSection().getStudentCount() + ".",
                    statusByConflictId.getOrDefault(id, ConflictStatus.OPEN)));
        }
        return result;
    }

    public void markResolved(String conflictId) {
        if(scheduleRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcScheduleRepository) {
            if(findAllConflicts().stream().anyMatch(c->c.getId().equals(conflictId))) throw new vn.edu.donga.unischedule.validation.ValidationException("Cần sửa hoặc hủy lịch gây trùng trước khi xác nhận đã xử lý.");
            return;
        }
        statusByConflictId.put(conflictId, ConflictStatus.RESOLVED);
    }

    private List<Conflict> compare(ScheduleEntry first, ScheduleEntry second) {
        List<Conflict> result = new ArrayList<>();
        if (!timeOverlaps(first, second)) {
            return result;
        }
        if (first.getRoom().getId().equals(second.getRoom().getId())) {
            result.add(conflict(ConflictType.ROOM, first, second,
                    "Hai lịch cùng dùng phòng " + first.getRoom().getCode() + "."));
        }
        if (first.getCourseSection().getLecturer().getId().equals(second.getCourseSection().getLecturer().getId())) {
            result.add(conflict(ConflictType.LECTURER, first, second,
                    "Giảng viên " + first.getCourseSection().getLecturer().getFullName() + " bị trùng giờ dạy."));
        }
        if (first.getCourseSection().getId().equals(second.getCourseSection().getId())) {
            result.add(conflict(ConflictType.COURSE_SECTION, first, second,
                    "Lớp học phần " + first.getCourseSection().getCode() + " có hai lịch cùng thời điểm."));
        }
        return result;
    }

    public boolean timeOverlaps(ScheduleEntry first, ScheduleEntry second) {
        return first.getDayOfWeek() == second.getDayOfWeek()
                && first.getStartSlot().getOrder() <= second.getEndSlot().getOrder()
                && second.getStartSlot().getOrder() <= first.getEndSlot().getOrder()
                && !first.getEndDate().isBefore(second.getStartDate())
                && !first.getStartDate().isAfter(second.getEndDate());
    }

    private Conflict conflict(ConflictType type, ScheduleEntry first, ScheduleEntry second, String message) {
        String id = "CF-" + type.name() + "-" + value(first.getId()) + "-" + value(second.getId());
        return new Conflict(id, type, first, second, message, statusByConflictId.getOrDefault(id, ConflictStatus.OPEN));
    }

    private String value(Long id) {
        return id == null ? "NEW" : String.valueOf(id);
    }
}
