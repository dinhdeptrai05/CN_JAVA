package vn.edu.donga.unischedule.service;
import vn.edu.donga.unischedule.repository.jdbc.*;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.UserStatus;
import vn.edu.donga.unischedule.util.PasswordHasher;
import vn.edu.donga.unischedule.validation.ValidationException;
public final class JdbcAuthService implements AuthService {
    private final JdbcDatabase db;
    public JdbcAuthService(JdbcDatabase db) { this.db=db; }
    public User login(String username,String password) {
        db.setActor(null);
        User user=new JdbcUserRepository(db).findByUsername(username.trim()).orElseThrow(()->new ValidationException("Tên đăng nhập hoặc mật khẩu không đúng."));
        if(user.getStatus()!=UserStatus.ACTIVE || !PasswordHasher.verify(password,user.getPassword())) throw new ValidationException("Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã khóa.");
        db.setActor(user);
        try { db.transaction(c->{ db.update("UPDATE users SET last_login_at=UTC_TIMESTAMP() WHERE id=?",user.getId()); db.audit("LOGIN","users",user.getId()); return null; }); }
        catch(RuntimeException ex) { db.setActor(null); throw ex; }
        user.setLastLogin(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)); return user;
    }
}
