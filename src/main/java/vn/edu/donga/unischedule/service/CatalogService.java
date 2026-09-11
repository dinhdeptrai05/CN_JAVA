package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.repository.CatalogRepository;

import java.util.List;

public class CatalogService {
    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public List<Department> getDepartments() {
        return repository.getDepartments();
    }

    public List<Semester> getSemesters() {
        return repository.getSemesters();
    }

    public List<TimeSlot> getTimeSlots() {
        return repository.getTimeSlots();
    }

    public List<Course> getCourses() {
        return repository.getCourses();
    }

    public List<Lecturer> getLecturers() {
        return repository.getLecturers();
    }
}
