package vn.edu.donga.unischedule.repository.jdbc;
import java.sql.*;
import java.util.*;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.validation.ValidationException;
/** Nested repositories share the transaction connection on the calling thread. */
public final class JdbcDatabase {
    @FunctionalInterface public interface Work<T> { T run(Connection c) throws SQLException; }
    @FunctionalInterface public interface Mapper<T> { T map(ResultSet r) throws SQLException; }
    private final ConnectionFactory factory;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();
    private User actor;
    public JdbcDatabase(ConnectionFactory factory) { this.factory = factory; }
    public void setActor(User user) { actor = user; }
    public User actor() { return actor; }
    public boolean hasRole(Role requested) {
        return actor!=null && scalar("SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.id=? AND u.status='ACTIVE' AND r.code=?",actor.getId(),requested)>0;
    }
    public void require(Role... roles) {
        if (actor == null) throw new ValidationException("Vui lòng đăng nhập.");
        var role = query("SELECT r.code FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.id=? AND u.status='ACTIVE'", r -> r.getString(1), actor.getId());
        if (Arrays.stream(roles).noneMatch(r -> role.contains(r.name()))) throw new ValidationException("Không có quyền thực hiện thao tác này.");
    }
    public <T> T transaction(Work<T> work) {
        if (current.get() != null) {
            try { return work.run(current.get()); } catch (SQLException ex) { throw failure(ex); }
        }
        try (Connection c = factory.open()) {
            c.setAutoCommit(false); current.set(c);
            try { T value = work.run(c); c.commit(); return value; }
            catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
            finally { current.remove(); }
        } catch (SQLException ex) { throw failure(ex); }
    }
    public <T> List<T> query(String sql, Mapper<T> mapper, Object... args) {
        return transaction(c -> {
            try (var p = c.prepareStatement(sql)) {
                bind(p, args);
                try (var r = p.executeQuery()) { List<T> rows = new ArrayList<>(); while (r.next()) rows.add(mapper.map(r)); return rows; }
            }
        });
    }
    public int update(String sql, Object... args) {
        return transaction(c -> { try (var p = c.prepareStatement(sql)) { bind(p, args); return p.executeUpdate(); } });
    }
    public long insert(String sql, Object... args) {
        return transaction(c -> {
            try (var p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(p, args); p.executeUpdate();
                try (var keys = p.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Missing generated id"); return keys.getLong(1); }
            }
        });
    }
    public long scalar(String sql, Object... args) { return query(sql, r -> r.getLong(1), args).get(0); }
    public void audit(String action, String entity, Long id) {
        update("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,JSON_OBJECT('result','SUCCESS'),UTC_TIMESTAMP())", actor == null ? null : actor.getId(), action, entity, id);
    }
    /** A shared row serializes scheduling/resource changes, including empty calendars. */
    public void lockScheduling() { query("SELECT id FROM roles WHERE code='ACADEMIC' FOR UPDATE", r -> r.getLong(1)); }
    private static void bind(PreparedStatement p, Object[] args) throws SQLException {
        for (int i=0; i<args.length; i++) p.setObject(i+1, args[i] instanceof Enum<?> e ? e.name() : args[i]);
    }
    private static ValidationException failure(SQLException ex) {
        String message = ex.getSQLState() != null && ex.getSQLState().startsWith("23")
                ? "Dữ liệu trùng hoặc vi phạm ràng buộc liên kết."
                : "Không thể truy cập MySQL. Kiểm tra kết nối và cấu hình cơ sở dữ liệu (SQLState " + ex.getSQLState() + ").";
        var failure = new ValidationException(message); failure.initCause(ex); return failure;
    }
}
