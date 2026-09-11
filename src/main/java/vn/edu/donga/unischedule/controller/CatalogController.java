package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.CatalogService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) { this.service = service; }

    public List<Department> getDepartments() { return service.getDepartments(); }

    public List<Semester> getSemesters() { return service.getSemesters(); }

    public List<TimeSlot> getTimeSlots() { return service.getTimeSlots(); }

    public List<Course> getCourses() { return service.getCourses(); }

    public List<Lecturer> getLecturers() { return service.getLecturers(); }
}

