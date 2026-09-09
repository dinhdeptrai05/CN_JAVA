package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.UserStatus;

public class Student extends User {
    private String studentCode;
    private String classCode;

    public Student(Long id, String username, String password, String fullName, String email, String phone,
                   String studentCode, String classCode) {
        super(id, username, password, fullName, email, phone, Role.STUDENT, UserStatus.ACTIVE);
        this.studentCode = studentCode;
        this.classCode = classCode;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }
}
