package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.repository.mock.MockDataStore;

import java.util.List;

public class CatalogService {
    private final MockDataStore store;

    public CatalogService(MockDataStore store) {
        this.store = store;
    }

    public List<Department> getDepartments() {
        return store.departments();
    }

    public List<Semester> getSemesters() {
        return store.semesters();
    }

    public List<TimeSlot> getTimeSlots() {
        return store.timeSlots();
    }

    public List<Course> getCourses() {
        return store.courses();
    }

    public List<Lecturer> getLecturers() {
        return store.users().stream()
                .filter(user -> user instanceof Lecturer)
                .map(user -> (Lecturer) user)
                .toList();
    }
}
