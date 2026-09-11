package vn.edu.donga.unischedule.repository;

import vn.edu.donga.unischedule.model.*;
import java.util.List;

public interface CatalogRepository {
    List<Department> getDepartments();
    List<Semester> getSemesters();
    List<TimeSlot> getTimeSlots();
    List<Course> getCourses();
    List<Lecturer> getLecturers();
}
