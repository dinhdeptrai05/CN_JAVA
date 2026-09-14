package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.UserRepository;
import vn.edu.donga.unischedule.util.PasswordHasher;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.util.*;
public final class JdbcUserRepository implements UserRepository {
    private final JdbcDatabase db;
    public JdbcUserRepository(JdbcDatabase db) { this.db=db; }
    private List<User> select(String suffix,Object... args) {
        var departments=new JdbcCatalogRepository(db).getDepartments();
        return db.query("SELECT u.*,r.code role_code FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id " + suffix, r -> {
            Long id=r.getLong("id"); String username=r.getString("username"), password=r.getString("password_hash"),name=r.getString("full_name"),email=r.getString("email"),phone=r.getString("phone");
            User u=switch(Role.valueOf(r.getString("role_code"))) {
                case ADMIN -> new Administrator(id,username,password,name,email,phone);
                case ACADEMIC -> new AcademicStaff(id,username,password,name,email,phone);
                case LECTURER -> new Lecturer(id,username,password,name,email,phone,JdbcCatalogRepository.department(departments,r.getLong("department_id")),r.getString("lecturer_code"));
                case STUDENT -> new Student(id,username,password,name,email,phone,r.getString("student_code"),r.getString("class_code"));
            };
            u.setStatus(UserStatus.valueOf(r.getString("status"))); u.setAvatarData(r.getBytes("avatar_data"));
            u.setLastLogin(r.getTimestamp("last_login_at")==null?null:r.getTimestamp("last_login_at").toLocalDateTime()); return u;
        },args);
    }
    public List<User> findAll() { return select("ORDER BY u.id,r.id"); }
    public Optional<User> findById(Long id) { return select("WHERE u.id=? ORDER BY r.id",id).stream().findFirst(); }
    public Optional<User> findByUsername(String username) { return select("WHERE u.username=? ORDER BY r.id",username).stream().findFirst(); }
    public User save(User u) {
        String hash=PasswordHasher.isHash(u.getPassword())?u.getPassword():PasswordHasher.hash(u.getPassword());
        long id=db.transaction(c->{
            db.require(Role.values());
            boolean admin=db.hasRole(Role.ADMIN);
            if(u.getId()!=null) {
                User old=findById(u.getId()).orElseThrow();
                if(old.getRole()!=u.getRole() && db.scalar("SELECT (SELECT COUNT(*) FROM lecturer_assignments WHERE lecturer_id=?)+(SELECT COUNT(*) FROM student_enrollments WHERE student_id=?)",u.getId(),u.getId())>0) throw new ValidationException("Tài khoản đã có phân công hoặc đăng ký học; không thể đổi vai trò.");
            }
            if (!admin && !Objects.equals(db.actor().getId(),u.getId())) throw new ValidationException("Chỉ được cập nhật hồ sơ của mình.");
            if (!admin) {
                User old=findById(u.getId()).orElseThrow();
                if(old.getRole()!=u.getRole() || old.getStatus()!=u.getStatus() || !old.getUsername().equals(u.getUsername())) throw new ValidationException("Không được thay đổi quyền tài khoản.");
            }
            Long department=u instanceof Lecturer l && l.getDepartment()!=null?l.getDepartment().getId():null;
            Long key=u.getId();
            if(key==null) key=db.insert("INSERT INTO users(username,password_hash,full_name,email,phone,department_id,lecturer_code,student_code,class_code,status,avatar_data) VALUES (?,?,?,?,?,?,?,?,?,?,?)",u.getUsername(),hash,u.getFullName(),u.getEmail(),u.getPhone(),department,u instanceof Lecturer l?l.getLecturerCode():null,u instanceof Student s?s.getStudentCode():null,u instanceof Student s?s.getClassCode():null,u.getStatus(),u.getAvatarData());
            else db.update("UPDATE users SET username=?,password_hash=?,full_name=?,email=?,phone=?,department_id=COALESCE(?,department_id),lecturer_code=?,student_code=?,class_code=?,status=?,avatar_data=? WHERE id=?",u.getUsername(),hash,u.getFullName(),u.getEmail(),u.getPhone(),department,u instanceof Lecturer l?l.getLecturerCode():null,u instanceof Student s?s.getStudentCode():null,u instanceof Student s?s.getClassCode():null,u.getStatus(),u.getAvatarData(),key);
            if(admin) {
                db.update("DELETE FROM user_roles WHERE user_id=?",key);
                db.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code=?",key,u.getRole());
            }
            db.audit("SAVE_USER","users",key); return key;
        });
        u.setId(id); u.setPassword(hash); return u;
    }
    public boolean deleteById(Long id) { db.require(Role.ADMIN); return db.transaction(c->{ int changed=db.update("UPDATE users SET status='INACTIVE' WHERE id=?",id); db.audit("DEACTIVATE_USER","users",id); return changed>0; }); }
    public void saveAvatar(User u,byte[] bytes) {
        db.transaction(c->{ db.require(Role.values()); if(!db.actor().getId().equals(u.getId())) throw new ValidationException("Chỉ được đổi ảnh của mình."); db.update("UPDATE users SET avatar_data=? WHERE id=?",bytes,u.getId()); db.audit("UPDATE_AVATAR","users",u.getId()); return null; });
        u.setAvatarData(bytes);
    }
    public void updateProfile(User user,String fullName,String email,String phone) {
        db.transaction(c->{db.require(Role.values());if(!db.actor().getId().equals(user.getId()))throw new ValidationException("Chỉ được cập nhật hồ sơ của mình.");
            db.update("UPDATE users SET full_name=?,email=?,phone=? WHERE id=?",fullName,email,phone,user.getId());db.audit("UPDATE_PROFILE","users",user.getId());return null;});
        user.setFullName(fullName);user.setEmail(email);user.setPhone(phone);
    }
}
