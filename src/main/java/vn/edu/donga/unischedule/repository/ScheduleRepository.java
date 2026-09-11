package vn.edu.donga.unischedule.repository;

import vn.edu.donga.unischedule.model.ScheduleEntry;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends Repository<ScheduleEntry> {
    List<ScheduleEntry> findByWeek(LocalDate weekStart);
}
