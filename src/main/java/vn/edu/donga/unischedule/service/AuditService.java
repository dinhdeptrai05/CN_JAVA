package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.AuditEntry;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only persisted audit history. */
public final class AuditService {
    private static final String VISIBLE="action NOT IN ('SEED_HISTORY_5Y_V1','REPAIR_HISTORY_PENDING_ROOMS','NORMALIZE_HISTORY_DISPLAY_V1','REPAIR_HISTORY_LOGIN_TIMELINE_V1','REPAIR_HISTORY_LOGIN_TIMELINE_V2')";
    private final List<AuditEntry> entries;
    private vn.edu.donga.unischedule.repository.jdbc.JdbcDatabase database;
    public AuditService(vn.edu.donga.unischedule.repository.jdbc.JdbcDatabase database) { this.entries=List.of();this.database=database; }
    public AuditService() {
        entries = List.of();
    }
    public List<AuditEntry> findAll() {
        if(database==null) return entries;
        database.require(vn.edu.donga.unischedule.model.Enums.Role.ADMIN,vn.edu.donga.unischedule.model.Enums.Role.ACADEMIC);
        return database.query("SELECT a.*,u.username FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id WHERE a."+VISIBLE+" ORDER BY a.created_at DESC,a.id DESC",r->new AuditEntry(r.getTimestamp("created_at").toLocalDateTime(),r.getString("username"),r.getString("action"),r.getString("entity_type")+" #"+r.getLong("entity_id"),"Thành công"));
    }
    public long count() {
        if(database==null)return entries.size();
        database.require(vn.edu.donga.unischedule.model.Enums.Role.ADMIN,vn.edu.donga.unischedule.model.Enums.Role.ACADEMIC);
        return database.scalar("SELECT COUNT(*) FROM audit_logs WHERE "+VISIBLE);
    }
    public List<AuditEntry> findPage(int page,int size) {
        if(page<0||size<1||size>500)throw new IllegalArgumentException("Invalid audit page");
        if(database==null)return entries.stream().skip((long)page*size).limit(size).toList();
        database.require(vn.edu.donga.unischedule.model.Enums.Role.ADMIN,vn.edu.donga.unischedule.model.Enums.Role.ACADEMIC);
        return database.query("SELECT a.*,u.username FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id WHERE a."+VISIBLE+" ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?",
                r->new AuditEntry(r.getTimestamp("created_at").toLocalDateTime(),r.getString("username"),r.getString("action"),r.getString("entity_type")+" #"+r.getLong("entity_id"),"Thành công"),size,page*size);
    }
}
