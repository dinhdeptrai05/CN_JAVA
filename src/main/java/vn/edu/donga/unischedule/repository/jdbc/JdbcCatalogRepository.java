package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.CatalogRepository;
import java.util.*;
public final class JdbcCatalogRepository implements CatalogRepository {
    private final JdbcDatabase db;
    public JdbcCatalogRepository(JdbcDatabase db) { this.db=db; }
    public List<Department> getDepartments() { return db.query("SELECT * FROM departments ORDER BY id", r -> new Department(r.getLong("id"),r.getString("code"),r.getString("name"))); }
    public List<Semester> getSemesters() { return db.query("SELECT * FROM semesters ORDER BY start_date", r -> new Semester(r.getLong("id"),r.getString("name"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getString("status"))); }
    public List<TimeSlot> getTimeSlots() { return db.query("SELECT * FROM time_slots ORDER BY sort_order", r -> new TimeSlot(r.getLong("id"),r.getString("name"),r.getTime("start_time").toLocalTime(),r.getTime("end_time").toLocalTime(),r.getInt("sort_order"))); }
    public List<Course> getCourses() {
        var departments=getDepartments();
        return db.query("SELECT * FROM courses WHERE status='ACTIVE' ORDER BY code", r -> new Course(r.getLong("id"),r.getString("code"),r.getString("name"),r.getInt("credits"),department(departments,r.getLong("department_id")),RoomType.valueOf(r.getString("required_room_type"))));
    }
    static Department department(List<Department> all,long id) { return all.stream().filter(x->x.getId()==id).findFirst().orElse(null); }
    public List<Lecturer> getLecturers() { return new JdbcUserRepository(db).findAll().stream().filter(u->u instanceof Lecturer && u.getStatus()==UserStatus.ACTIVE).map(u->(Lecturer)u).toList(); }
}
