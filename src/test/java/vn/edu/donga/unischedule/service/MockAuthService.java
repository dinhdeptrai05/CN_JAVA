package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.Enums.UserStatus;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.repository.UserRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import java.time.LocalDateTime;

public class MockAuthService implements AuthService {
    private final UserRepository userRepository;

    public MockAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User login(String username, String password) throws ValidationException {
        Validator.required(username, "Tên đăng nhập");
        Validator.required(password, "Mật khẩu");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException("Tên đăng nhập hoặc mật khẩu không đúng."));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ValidationException("Tài khoản đang bị khóa, vui lòng liên hệ quản trị viên.");
        }
        if (!user.getPassword().equals(password)) {
            throw new ValidationException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }
        user.setLastLogin(LocalDateTime.now());
        return user;
    }
}

