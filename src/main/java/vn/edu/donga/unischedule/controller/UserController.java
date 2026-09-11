package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.UserService;
import vn.edu.donga.unischedule.util.TextUtils;
import java.time.LocalDate;
import java.util.List;

public final class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    public List<User> findAll() { return service.findAll(); }

    public User save(User user) { return service.save(user); }

    public void toggleLock(User user) { service.toggleLock(user); }

    public void resetPassword(User user) { service.resetPassword(user); }

    public User createUser(Role role, String username, String fullName, String email, String phone) { return service.createUser(role, username, fullName, email, phone); }

    public List<User> search(String keyword, String role, String status) {
        return service.findAll().stream()
                .filter(user -> role == null || role.startsWith("Tất cả") || user.getRole().getDisplayName().equals(role))
                .filter(user -> status == null || status.startsWith("Tất cả") || user.getStatus().getDisplayName().equals(status))
                .filter(user -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(user.getUsername(), keyword)
                        || TextUtils.containsIgnoreAccent(user.getFullName(), keyword)
                        || TextUtils.containsIgnoreAccent(user.getEmail(), keyword))
                .toList();
    }
    public void updateProfile(User user, String fullName, String email, String phone) {
        service.updateProfile(user, fullName, email, phone);
    }
    public void changePassword(User user, String oldPassword, String password, String confirmation) {
        service.changePassword(user, oldPassword, password, confirmation);
    }
    public User prepareUser(User editing, Role role, String username, String fullName, String email, String phone, UserStatus status) {
        return service.prepareUser(editing, role, username, fullName, email, phone, status);
    }
}
