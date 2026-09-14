package vn.edu.donga.unischedule.model;

import java.time.*;
import java.util.*;

public record Report(Filter filter, LocalDateTime generatedAt, String scope,
                     long sessions, long usedRooms, long registrations, long requests, List<Table> tables) {
    public Report { tables = List.copyOf(tables); }
    public record Filter(LocalDate from, LocalDate to, Long semesterId, Long departmentId) { }
    public record Table(String title, String note, List<String> columns, List<List<Object>> rows) {
        public Table { columns=List.copyOf(columns);rows=rows.stream().map(List::copyOf).toList(); }
    }
    public record Source(List<ScheduleEntry> schedules, List<Classroom> rooms, List<TimeSlot> slots,
                         List<CourseSection> sections, List<ChangeRequest> requests) { }
}
