package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.repository.CatalogRepository;
import java.util.List;

public final class MockCatalogRepository implements CatalogRepository {
    private final MockDataStore store;
    public MockCatalogRepository(MockDataStore store) { this.store = store; }
    public List<Department> getDepartments() { return List.copyOf(store.departments()); }
    public List<Semester> getSemesters() { return List.copyOf(store.semesters()); }
    public List<TimeSlot> getTimeSlots() { return List.copyOf(store.timeSlots()); }
    public List<Course> getCourses() { return List.copyOf(store.courses()); }
    public List<Lecturer> getLecturers() {
        return store.users().stream().filter(Lecturer.class::isInstance).map(Lecturer.class::cast).toList();
    }
}
