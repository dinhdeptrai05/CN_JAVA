package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.AcademicStaff;
import vn.edu.donga.unischedule.model.Administrator;
import vn.edu.donga.unischedule.model.Lecturer;
import vn.edu.donga.unischedule.model.Student;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.UserStatus;
import vn.edu.donga.unischedule.repository.UserRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import java.util.List;

public class UserService {
    private final UserRepository userRepository;
    private final CatalogService catalogService;

    public UserService(UserRepository userRepository, CatalogService catalogService) {
        this.userRepository = userRepository;
        this.catalogService = catalogService;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(User user) {
        Validator.username(user.getUsername());
        Validator.required(user.getFullName(), "Họ tên");
        Validator.email(user.getEmail());
        boolean duplicate = userRepository.findAll().stream()
                .anyMatch(existing -> existing.getUsername().equalsIgnoreCase(user.getUsername())
                        && (user.getId() == null || !existing.getId().equals(user.getId())));
        if (duplicate) {
            throw new ValidationException("Username đã tồn tại trong dữ liệu giả.");
        }
        return userRepository.save(user);
    }

    public User createUser(Role role, String username, String fullName, String email, String phone) {
        return switch (role) {
            case ADMIN -> new Administrator(null, username, "123456", fullName, email, phone);
            case ACADEMIC -> new AcademicStaff(null, username, "123456", fullName, email, phone);
            case LECTURER -> new Lecturer(null, username, "123456", fullName, email, phone,
                    catalogService.getDepartments().get(0), "GV" + (findAll().size() + 1));
            case STUDENT -> new Student(null, username, "123456", fullName, email, phone,
                    "SV" + (findAll().size() + 1), "CNTT22A");
        };
    }

    public void toggleLock(User user) {
        user.setStatus(user.getStatus() == UserStatus.ACTIVE ? UserStatus.LOCKED : UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public void resetPassword(User user) {
        user.setPassword("123456");
        userRepository.save(user);
    }
}
