package vn.edu.donga.unischedule.repository.jdbc;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.CourseSectionRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.util.*;
public final class JdbcCourseSectionRepository implements CourseSectionRepository {
    private final JdbcDatabase db;
    public JdbcCourseSectionRepository(JdbcDatabase db) { this.db=db; }
    public List<CourseSection> findAll() {
        var catalog=new JdbcCatalogRepository(db); var courses=catalog.getCourses(); var semesters=catalog.getSemesters(); var users=new JdbcUserRepository(db).findAll();
        var courseById=new HashMap<Long,Course>();for(var course:courses)courseById.put(course.getId(),course);
        var semesterById=new HashMap<Long,Semester>();for(var semester:semesters)semesterById.put(semester.getId(),semester);
        var lecturerById=new HashMap<Long,Lecturer>();for(var user:users)if(user instanceof Lecturer lecturer)lecturerById.put(user.getId(),lecturer);
        return db.query("SELECT cs.*, (SELECT lecturer_id FROM lecturer_assignments WHERE course_section_id=cs.id ORDER BY id LIMIT 1) lecturer_id FROM course_sections cs ORDER BY cs.code",r->{
            long courseId=r.getLong("course_id"),semesterId=r.getLong("semester_id"),lecturerId=r.getLong("lecturer_id");
            return new CourseSection(r.getLong("id"),r.getString("code"),Objects.requireNonNull(courseById.get(courseId)),Objects.requireNonNull(semesterById.get(semesterId)),lecturerById.get(lecturerId),r.getInt("capacity"),r.getInt("student_count"),CourseSectionStatus.valueOf(r.getString("status")));
        });
    }
    public Optional<CourseSection> findById(Long id) { return findAll().stream().filter(x->x.getId().equals(id)).findFirst(); }
    public List<CourseSection> findForUser(User user) {
        if(user.getRole()==Role.STUDENT) {
            var ids=db.query("SELECT course_section_id FROM student_enrollments WHERE student_id=? AND status='ACTIVE'",r->r.getLong(1),user.getId());
            return findAll().stream().filter(s->ids.contains(s.getId())).toList();
        }
        if(user.getRole()==Role.LECTURER) {
            var ids=db.query("SELECT course_section_id FROM lecturer_assignments WHERE lecturer_id=?",r->r.getLong(1),user.getId());
            return findAll().stream().filter(s->ids.contains(s.getId())).toList();
        }
        return findAll();
    }
    public CourseSection save(CourseSection s) {
        long[] result=db.transaction(c->{ db.require(Role.ACADEMIC); db.lockScheduling();
            if(s.getCourse()==null || s.getSemester()==null || s.getLecturer()==null) throw new ValidationException("Chọn môn học, học kỳ và giảng viên.");
            if(db.scalar("SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.id=? AND u.status='ACTIVE' AND r.code='LECTURER'",s.getLecturer().getId())==0) throw new ValidationException("Giảng viên không hoạt động.");
            long count=s.getId()==null?0:db.scalar("SELECT COUNT(*) FROM student_enrollments WHERE course_section_id=? AND status='ACTIVE'",s.getId());
            if(s.getCapacity()<count) throw new ValidationException("Sức chứa nhỏ hơn số sinh viên đã đăng ký.");
            Long id=s.getId();
            if(id!=null && db.scalar("SELECT COUNT(*) FROM schedules WHERE course_section_id=? AND status<>'CANCELLED'",id)>0) {
                var old=findById(id).orElseThrow();
                if(!old.getLecturer().getId().equals(s.getLecturer().getId()) || !old.getSemester().getId().equals(s.getSemester().getId()) || !old.getCourse().getId().equals(s.getCourse().getId()) || s.getStatus()==CourseSectionStatus.CLOSED || s.getStatus()==CourseSectionStatus.CANCELLED) throw new ValidationException("Hủy các lịch hiện có trước khi thay đổi phân công, học kỳ hoặc đóng lớp.");
            }
            if(id==null) id=db.insert("INSERT INTO course_sections(course_id,semester_id,code,capacity,student_count,status) VALUES (?,?,?,?,?,?)",s.getCourse().getId(),s.getSemester().getId(),s.getCode(),s.getCapacity(),count,s.getStatus());
            else db.update("UPDATE course_sections SET course_id=?,semester_id=?,code=?,capacity=?,student_count=?,status=? WHERE id=?",s.getCourse().getId(),s.getSemester().getId(),s.getCode(),s.getCapacity(),count,s.getStatus(),id);
            var assignments=db.query("SELECT id FROM lecturer_assignments WHERE course_section_id=? ORDER BY id",r->r.getLong(1),id);
            if(assignments.isEmpty()) db.update("INSERT INTO lecturer_assignments(course_section_id,lecturer_id) VALUES (?,?)",id,s.getLecturer().getId());
            else db.update("UPDATE lecturer_assignments SET lecturer_id=? WHERE id=?",s.getLecturer().getId(),assignments.get(0));
            db.audit(s.getId()==null?"CREATE_SECTION":s.getStatus()==CourseSectionStatus.CANCELLED?"CANCEL_SECTION":"UPDATE_SECTION","course_sections",id); return new long[]{id,count};
        }); s.setId(result[0]);s.setStudentCount((int)result[1]);return s;
    }
    public boolean deleteById(Long id) { var item=findById(id); if(item.isEmpty()) return false; item.get().setStatus(CourseSectionStatus.CANCELLED); save(item.get()); return true; }
    public void enroll(Long sectionId,Long studentId,boolean active) {
        db.transaction(c->{ db.require(Role.ACADEMIC);db.lockScheduling();
            db.query("SELECT id FROM course_sections WHERE id=? FOR UPDATE",r->r.getLong(1),sectionId);
            CourseSection s=findById(sectionId).orElseThrow();
            if(db.scalar("SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.id=? AND u.status='ACTIVE' AND r.code='STUDENT'",studentId)==0) throw new ValidationException("Tài khoản không phải sinh viên hoạt động.");
            boolean enrolled=db.scalar("SELECT COUNT(*) FROM student_enrollments WHERE course_section_id=? AND student_id=? AND status='ACTIVE'",sectionId,studentId)>0;
            if(active && !enrolled && (s.getStudentCount()>=s.getCapacity() || s.getStatus()==CourseSectionStatus.CLOSED || s.getStatus()==CourseSectionStatus.CANCELLED)) throw new ValidationException("Lớp đã đóng hoặc đã đủ sinh viên.");
            if(active && !enrolled && db.scalar("SELECT COUNT(*) FROM schedules sc JOIN classrooms r ON r.id=sc.classroom_id WHERE sc.course_section_id=? AND sc.status<>'CANCELLED' AND r.capacity<?",sectionId,s.getStudentCount()+1)>0) throw new ValidationException("Phòng học không đủ chỗ cho đăng ký mới.");
            db.update("INSERT INTO student_enrollments(course_section_id,student_id,status) VALUES (?,?,?) ON DUPLICATE KEY UPDATE status=?",sectionId,studentId,active?"ACTIVE":"CANCELLED",active?"ACTIVE":"CANCELLED");
            db.update("UPDATE course_sections SET student_count=(SELECT COUNT(*) FROM student_enrollments WHERE course_section_id=? AND status='ACTIVE') WHERE id=?",sectionId,sectionId);
            db.audit(active?"ENROLL":"CANCEL_ENROLLMENT","course_sections",sectionId); return null;
        });
    }
}
