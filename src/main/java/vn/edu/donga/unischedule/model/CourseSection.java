package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.CourseSectionStatus;

public class CourseSection {
    private Long id;
    private String code;
    private Course course;
    private Semester semester;
    private Lecturer lecturer;
    private int capacity;
    private int studentCount;
    private CourseSectionStatus status;

    public CourseSection(Long id, String code, Course course, Semester semester, Lecturer lecturer,
                         int capacity, int studentCount, CourseSectionStatus status) {
        this.id = id;
        this.code = code;
        this.course = course;
        this.semester = semester;
        this.lecturer = lecturer;
        this.capacity = capacity;
        this.studentCount = studentCount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(Semester semester) {
        this.semester = semester;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }

    public void setLecturer(Lecturer lecturer) {
        this.lecturer = lecturer;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public CourseSectionStatus getStatus() {
        return status;
    }

    public void setStatus(CourseSectionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return code + " - " + course.getName();
    }
}
