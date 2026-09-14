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
            throw new ValidationException("Username đã tồn tại trong cơ sở dữ liệu.");
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

    public void updateProfile(User user, String fullName, String email, String phone) {
        Validator.required(fullName, "Họ tên");
        Validator.email(email);
        if(userRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcUserRepository jdbc) {jdbc.updateProfile(user,fullName.trim(),email.trim(),phone.trim());return;}
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        userRepository.save(user);
    }

    public void changePassword(User user, String oldPassword, String password, String confirmation) {
        String stored=userRepository.findById(user.getId()).orElseThrow().getPassword();
        boolean valid=vn.edu.donga.unischedule.util.PasswordHasher.isHash(stored)
            ? vn.edu.donga.unischedule.util.PasswordHasher.verify(oldPassword,stored) : stored.equals(oldPassword);
        if (!valid) throw new ValidationException("Mật khẩu hiện tại không đúng.");
        if (password == null || password.length() < 6) throw new ValidationException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        if (!password.equals(confirmation)) throw new ValidationException("Mật khẩu nhập lại không khớp.");
        String previous=user.getPassword();user.setPassword(password);
        try { userRepository.save(user); } catch (RuntimeException ex) { user.setPassword(previous);throw ex; }
    }

    public User prepareUser(User editing, Role role, String username, String fullName, String email, String phone, UserStatus status) {
        User draft = createUser(role, username, fullName, email, phone);
        if (editing != null) {
            draft.setId(editing.getId());
            draft.setPassword(editing.getPassword());
            draft.setAvatarData(editing.getAvatarData());
            if (draft instanceof Lecturer target && editing instanceof Lecturer original) {
                target.setDepartment(original.getDepartment()); target.setLecturerCode(original.getLecturerCode());
            }
            if (draft instanceof Student target && editing instanceof Student original) {
                target.setClassCode(original.getClassCode()); target.setStudentCode(original.getStudentCode());
            }
        }
        draft.setStatus(status);
        return draft;
    }

    public void updateAvatar(User user, java.nio.file.Path path) {
        byte[] data=path==null?null:vn.edu.donga.unischedule.util.ProfileImages.read(path);
        if(userRepository instanceof vn.edu.donga.unischedule.repository.jdbc.JdbcUserRepository jdbc) jdbc.saveAvatar(user,data);
        else { user.setAvatarData(data);userRepository.save(user); }
    }
}
