package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.ConflictService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class ConflictController {
    private final ConflictService service;

    public ConflictController(ConflictService service) { this.service = service; }

    public List<Conflict> findAllConflicts() { return service.findAllConflicts(); }

    public void markResolved(String id) { service.markResolved(id); }

    public List<Conflict> search(String keyword, String type, String status) {
        return service.findAllConflicts().stream()
                .filter(conflict -> type == null || type.startsWith("Tất cả") || conflict.getType().getDisplayName().equals(type))
                .filter(conflict -> status == null || status.startsWith("Tất cả") || conflict.getStatus().getDisplayName().equals(status))
                .filter(conflict -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(conflict.getId(), keyword)
                        || TextUtils.containsIgnoreAccent(scheduleName(conflict.getFirstSchedule()), keyword)
                        || TextUtils.containsIgnoreAccent(scheduleName(conflict.getSecondSchedule()), keyword)
                        || TextUtils.containsIgnoreAccent(conflict.getMessage(), keyword))
                .toList();
    }

    private String scheduleName(ScheduleEntry entry) {
        return entry == null ? "" : entry.getCourseSection().getCode() + " - " + entry.getRoom().getCode();
    }
}
