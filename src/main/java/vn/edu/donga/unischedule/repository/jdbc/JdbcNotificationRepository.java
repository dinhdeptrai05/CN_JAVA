package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.NotificationRepository;
import java.util.*;
public final class JdbcNotificationRepository implements NotificationRepository {
    private final JdbcDatabase db;
    public JdbcNotificationRepository(JdbcDatabase db) { this.db=db; }
    public List<Notification> findAll() {
        db.require(Role.values());var users=new JdbcUserRepository(db).findAll();
        return db.query("SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC",r->{long uid=r.getLong("user_id");return new Notification(r.getLong("id"),users.stream().filter(x->x.getId()==uid).findFirst().orElseThrow(),NotificationType.valueOf(r.getString("type")),r.getString("title"),r.getString("content"),r.getBoolean("is_read"),r.getTimestamp("created_at").toLocalDateTime(),r.getString("target_screen"));},db.actor().getId());
    }
    public Optional<Notification> findById(Long id) { return findAll().stream().filter(x->x.getId().equals(id)).findFirst(); }
    public Notification save(Notification n) { db.require(Role.values());db.update("UPDATE notifications SET is_read=?,read_at=CASE WHEN ? THEN UTC_TIMESTAMP() ELSE NULL END WHERE id=? AND user_id=?",n.isRead(),n.isRead(),n.getId(),db.actor().getId());return n; }
    public boolean deleteById(Long id) { db.require(Role.values());return db.update("DELETE FROM notifications WHERE id=? AND user_id=?",id,db.actor().getId())>0; }
}
