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
    public List<CourseSection> findForUser(vn.edu.donga.unischedule.model.User user) {
        if(courseSectionRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcCourseSectionRepository jdbc) return jdbc.findForUser(user);
        if(user.getRole()==vn.edu.donga.unischedule.model.Enums.Role.LECTURER) return findAll().stream().filter(s->s.getLecturer()!=null && s.getLecturer().getId().equals(user.getId())).toList();
        return findAll();
    }
    public void enroll(Long sectionId,Long studentId,boolean active) {
        if(courseSectionRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcCourseSectionRepository jdbc) jdbc.enroll(sectionId,studentId,active);
        else throw new ValidationException("Đăng ký học cần kết nối cơ sở dữ liệu.");
    }

    public CourseSection save(CourseSection section) {
        Validator.required(section.getCode(), "Mã lớp học phần");
        Validator.positive(section.getCapacity(), "Sức chứa dự kiến");
        if (section.getStudentCount() < 0) throw new ValidationException("Sĩ số không được âm.");
        boolean duplicate = courseSectionRepository.findAll().stream()
                .anyMatch(existing -> existing.getCode().equalsIgnoreCase(section.getCode())
                        && (section.getId() == null || !existing.getId().equals(section.getId())));
        if (duplicate) {
            throw new ValidationException("Mã lớp học phần đã tồn tại trong cơ sở dữ liệu.");
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
