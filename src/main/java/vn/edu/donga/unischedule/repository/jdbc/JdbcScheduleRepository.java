package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.ScheduleRepository;
import vn.edu.donga.unischedule.service.*;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.time.*;
import java.util.*;
public final class JdbcScheduleRepository implements ScheduleRepository {
    private final JdbcDatabase db;
    public JdbcScheduleRepository(JdbcDatabase db) { this.db=db; }
    public List<ScheduleEntry> findAll() {
        var sections=new JdbcCourseSectionRepository(db).findAll();var rooms=new JdbcRoomRepository(db).findAll();var slots=new JdbcCatalogRepository(db).getTimeSlots();var users=new JdbcUserRepository(db).findAll();
        return db.query("SELECT s.*,la.lecturer_id FROM schedules s JOIN lecturer_assignments la ON la.id=s.lecturer_assignment_id ORDER BY s.id",r->{
            long sectionId=r.getLong("course_section_id"),roomId=r.getLong("classroom_id"),startId=r.getLong("start_slot_id"),endId=r.getLong("end_slot_id"),lecturerId=r.getLong("lecturer_id");
            var base=sections.stream().filter(x->x.getId()==sectionId).findFirst().orElseThrow();
            var section=new CourseSection(base.getId(),base.getCode(),base.getCourse(),base.getSemester(),(Lecturer)users.stream().filter(x->x.getId()==lecturerId).findFirst().orElseThrow(),base.getCapacity(),base.getStudentCount(),base.getStatus());
            return new ScheduleEntry(r.getLong("id"),section,rooms.stream().filter(x->x.getId()==roomId).findFirst().orElseThrow(),r.getInt("day_of_week"),slots.stream().filter(x->x.getId()==startId).findFirst().orElseThrow(),slots.stream().filter(x->x.getId()==endId).findFirst().orElseThrow(),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),ScheduleStatus.valueOf(r.getString("status")),r.getString("note"));
        });
    }
    public Optional<ScheduleEntry> findById(Long id) { return findAll().stream().filter(x->x.getId().equals(id)).findFirst(); }
    public List<ScheduleEntry> findByWeek(LocalDate start) { return findAll().stream().filter(s->s.getStatus()!=ScheduleStatus.CANCELLED && !s.getStartDate().isAfter(start.plusDays(6)) && !s.getEndDate().isBefore(start)).toList(); }
    public List<ScheduleEntry> findForUser(LocalDate start,User user) {
        var rows=findByWeek(start);
        if(user.getRole()==Role.ADMIN || user.getRole()==Role.ACADEMIC) return rows;
        if(user.getRole()==Role.LECTURER) return rows.stream().filter(s->s.getStatus()==ScheduleStatus.PUBLISHED && s.getCourseSection().getLecturer().getId().equals(user.getId())).toList();
        var enrolled=db.query("SELECT course_section_id FROM student_enrollments WHERE student_id=? AND status='ACTIVE'",r->r.getLong(1),user.getId());
        return rows.stream().filter(s->s.getStatus()==ScheduleStatus.PUBLISHED && enrolled.contains(s.getCourseSection().getId())).toList();
    }
    public ScheduleEntry save(ScheduleEntry entry) {
        long id=db.transaction(c->{ db.require(Role.ACADEMIC);return persist(entry); });entry.setId(id);return entry;
    }
    /** Called within the locked request transaction after reviewer authorization. */
    long persist(ScheduleEntry entry) {
        db.lockScheduling();
        var section=new JdbcCourseSectionRepository(db).findById(entry.getCourseSection().getId()).orElseThrow();
        var room=new JdbcRoomRepository(db).findById(entry.getRoom().getId()).orElseThrow();
        var slots=new JdbcCatalogRepository(db).getTimeSlots();
        var start=slots.stream().filter(x->x.getId().equals(entry.getStartSlot().getId())).findFirst().orElseThrow();
        var end=slots.stream().filter(x->x.getId().equals(entry.getEndSlot().getId())).findFirst().orElseThrow();
        var fresh=new ScheduleEntry(entry.getId(),section,room,entry.getDayOfWeek(),start,end,entry.getStartDate(),entry.getEndDate(),entry.getStatus(),entry.getNote());
        if(entry.getStatus()!=ScheduleStatus.CANCELLED) {
            if(section.getStatus()==CourseSectionStatus.CLOSED || section.getStatus()==CourseSectionStatus.CANCELLED) throw new ValidationException("Lớp học phần đã đóng.");
            if(section.getLecturer()==null || section.getLecturer().getStatus()!=UserStatus.ACTIVE) throw new ValidationException("Giảng viên không hoạt động.");
            if(entry.getStartDate().isBefore(section.getSemester().getStartDate()) || entry.getEndDate().isAfter(section.getSemester().getEndDate())) throw new ValidationException("Lịch phải nằm trong thời gian học kỳ.");
            new ScheduleService(this,new ConflictService(this)).validate(fresh);
            if(db.scalar("SELECT COUNT(*) FROM maintenance_records WHERE classroom_id=? AND status IN ('REPORTED','IN_PROGRESS') AND start_date<=? AND (end_date IS NULL OR end_date>=?)",room.getId(),entry.getEndDate(),entry.getStartDate())>0) throw new ValidationException("Phòng có bảo trì trong khoảng ngày được chọn.");
        }
        long assignment=db.scalar("SELECT id FROM lecturer_assignments WHERE course_section_id=? AND lecturer_id=?",section.getId(),section.getLecturer().getId());
        Long id=entry.getId();
        if(id==null) id=db.insert("INSERT INTO schedules(course_section_id,lecturer_assignment_id,classroom_id,start_slot_id,end_slot_id,day_of_week,start_date,end_date,status,note,created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?)",section.getId(),assignment,room.getId(),start.getId(),end.getId(),entry.getDayOfWeek(),entry.getStartDate(),entry.getEndDate(),entry.getStatus(),entry.getNote(),db.actor().getId());
        else db.update("UPDATE schedules SET course_section_id=?,lecturer_assignment_id=?,classroom_id=?,start_slot_id=?,end_slot_id=?,day_of_week=?,start_date=?,end_date=?,status=?,note=? WHERE id=?",section.getId(),assignment,room.getId(),start.getId(),end.getId(),entry.getDayOfWeek(),entry.getStartDate(),entry.getEndDate(),entry.getStatus(),entry.getNote(),id);
        db.audit("SAVE_SCHEDULE","schedules",id);return id;
    }
    public boolean deleteById(Long id) { return db.transaction(c->{db.require(Role.ACADEMIC);db.lockScheduling();int count=db.update("UPDATE schedules SET status='CANCELLED' WHERE id=? AND status<>'CANCELLED'",id);if(count>0)db.audit("CANCEL_SCHEDULE","schedules",id);return count>0;}); }
}
