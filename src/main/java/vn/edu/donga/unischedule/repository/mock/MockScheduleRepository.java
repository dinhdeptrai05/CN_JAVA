package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.repository.ScheduleRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MockScheduleRepository implements ScheduleRepository {
    private final MockDataStore store;

    public MockScheduleRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<ScheduleEntry> findAll() {
        return new ArrayList<>(store.schedules());
    }

    @Override
    public List<ScheduleEntry> findByWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return store.schedules().stream()
                .filter(entry -> !entry.getEndDate().isBefore(weekStart) && !entry.getStartDate().isAfter(weekEnd))
                .toList();
    }

    @Override
    public Optional<ScheduleEntry> findById(Long id) {
        return store.schedules().stream().filter(entry -> entry.getId().equals(id)).findFirst();
    }

    @Override
    public ScheduleEntry save(ScheduleEntry entity) {
        if (entity.getId() == null) {
            entity.setId(store.nextScheduleId());
            store.schedules().add(entity);
            return entity;
        }
        deleteById(entity.getId());
        store.schedules().add(entity);
        return entity;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.schedules().removeIf(entry -> entry.getId().equals(id));
    }
}
