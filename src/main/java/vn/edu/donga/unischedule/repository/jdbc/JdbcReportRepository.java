package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.repository.ReportRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
public final class JdbcReportRepository implements ReportRepository {
    private final JdbcDatabase db;
    public JdbcReportRepository(JdbcDatabase db) { this.db=db; }
    public Report.Source load(User user) {
        return db.transaction(c -> {
            c.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
            db.require(Role.ADMIN,Role.ACADEMIC);
            if(!db.actor().getId().equals(user.getId())) throw new ValidationException("Phiên đăng nhập không khớp.");
            return new Report.Source(new JdbcScheduleRepository(db).findAll(), new JdbcRoomRepository(db).findAll(),
                new JdbcCatalogRepository(db).getTimeSlots(),new JdbcCourseSectionRepository(db).findAll(),new JdbcRequestRepository(db).findAll());
        });
    }
}
