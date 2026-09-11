package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.CourseSection;
import vn.edu.donga.unischedule.repository.CourseSectionRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import java.util.List;

public class CourseSectionService {
    private final CourseSectionRepository courseSectionRepository;

    public CourseSectionService(CourseSectionRepository courseSectionRepository) {
        this.courseSectionRepository = courseSectionRepository;
    }

    public List<CourseSection> findAll() {
        return courseSectionRepository.findAll();
    }

    public CourseSection save(CourseSection section) {
        Validator.required(section.getCode(), "Mã lớp học phần");
        Validator.positive(section.getCapacity(), "Sức chứa dự kiến");
        Validator.positive(section.getStudentCount(), "Sĩ số");
        boolean duplicate = courseSectionRepository.findAll().stream()
                .anyMatch(existing -> existing.getCode().equalsIgnoreCase(section.getCode())
                        && (section.getId() == null || !existing.getId().equals(section.getId())));
        if (duplicate) {
            throw new ValidationException("Mã lớp học phần đã tồn tại trong dữ liệu giả.");
        }
        return courseSectionRepository.save(section);
    }

    public void assignLecturer(CourseSection section, vn.edu.donga.unischedule.model.Lecturer lecturer) {
        if (lecturer == null) throw new ValidationException("Vui lòng chọn giảng viên.");
        CourseSection draft = new CourseSection(section.getId(), section.getCode(), section.getCourse(),
                section.getSemester(), lecturer, section.getCapacity(), section.getStudentCount(), section.getStatus());
        save(draft);
        section.setLecturer(lecturer);
    }
}
