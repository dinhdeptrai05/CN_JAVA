package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.UserStatus;

public class Lecturer extends User {
    private Department department;
    private String lecturerCode;

    public Lecturer(Long id, String username, String password, String fullName, String email, String phone,
                    Department department, String lecturerCode) {
        super(id, username, password, fullName, email, phone, Role.LECTURER, UserStatus.ACTIVE);
        this.department = department;
        this.lecturerCode = lecturerCode;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getLecturerCode() {
        return lecturerCode;
    }

    public void setLecturerCode(String lecturerCode) {
        this.lecturerCode = lecturerCode;
    }
}
