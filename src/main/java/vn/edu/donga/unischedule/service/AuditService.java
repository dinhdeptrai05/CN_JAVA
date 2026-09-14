package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.AuditEntry;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only persisted audit history. */
public final class AuditService {
    private final List<AuditEntry> entries;
    private vn.edu.donga.unischedule.repository.jdbc.JdbcDatabase database;
    public AuditService(vn.edu.donga.unischedule.repository.jdbc.JdbcDatabase database) { this.entries=List.of();this.database=database; }
    public AuditService() {
        entries = List.of();
    }
    public List<AuditEntry> findAll() {
        if(database==null) return entries;
        database.require(vn.edu.donga.unischedule.model.Enums.Role.ADMIN,vn.edu.donga.unischedule.model.Enums.Role.ACADEMIC);
        return database.query("SELECT a.*,u.username FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id ORDER BY a.created_at DESC,a.id DESC",r->new AuditEntry(r.getTimestamp("created_at").toLocalDateTime(),r.getString("username"),r.getString("action"),r.getString("entity_type")+" #"+r.getLong("entity_id"),"Thành công"));
    }
}
