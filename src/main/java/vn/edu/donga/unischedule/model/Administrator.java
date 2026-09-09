package vn.edu.donga.unischedule.model;

import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.UserStatus;

public class Administrator extends User {
    public Administrator(Long id, String username, String password, String fullName, String email, String phone) {
        super(id, username, password, fullName, email, phone, Role.ADMIN, UserStatus.ACTIVE);
    }
}
