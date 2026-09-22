package vn.edu.donga.unischedule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.jdbc.*;
import vn.edu.donga.unischedule.service.*;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Run only against a freshly seeded, disposable database named unischedule_test. */
@EnabledIfEnvironmentVariable(named="UNISCHEDULE_DB_URL",matches=".*[/]unischedule_test[?].*")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcIntegrationTest {
    private AppServices app;
    private JdbcDatabase db;
    @BeforeEach void open() {app=AppServices.createJdbc();db=new JdbcDatabase(new ConnectionFactory());}
    private User login(String username) {var user=app.auth().login(username,"123456");db.setActor(user);return user;}
    private ScheduleEntry copy(ScheduleEntry old,Classroom room,int day,TimeSlot start,TimeSlot end) {
        return new ScheduleEntry(null,old.getCourseSection(),room,day,start,end,old.getStartDate(),old.getEndDate(),ScheduleStatus.PUBLISHED,"Integration test");
    }
    @Test @Order(1) void schemaSeedAndFourRoles() {
        assertEquals(18,db.scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'"));
        assertEquals(4,db.scalar("SELECT COUNT(*) FROM roles"));
        for(String account:List.of("admin","daotao","giangvien","sinhvien")) {
            var user=login(account);assertTrue(user.getPassword().startsWith("pbkdf2-sha256$"));
            assertFalse(app.notifications().findForUser(user).isEmpty());
        }
        assertThrows(ValidationException.class,()->app.auth().login("admin' OR 1=1 --","123456"));
        assertThrows(ValidationException.class,()->app.auth().login("admin","wrong"));
        assertEquals(20,db.scalar("SELECT COUNT(*) FROM schedules"));
        assertEquals(0,app.conflicts().findAllConflicts().size());
        assertEquals(0,db.scalar("SELECT COUNT(*) FROM course_sections cs WHERE student_count<>(SELECT COUNT(*) FROM student_enrollments e WHERE e.course_section_id=cs.id AND e.status='ACTIVE')"));
        assertThrows(ValidationException.class,()->db.update("INSERT INTO departments(code,name) VALUES (?,?)","CNTT","Duplicate"));
        assertThrows(ValidationException.class,()->db.update("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)",999999,1));
    }
    @Test @Order(2) void profileAvatarPasswordSurviveNewConnection() throws Exception {
        User user=login("sinhvien");
        Path photo=Path.of("target","profile-input.png");var image=new java.awt.image.BufferedImage(40,20,java.awt.image.BufferedImage.TYPE_INT_RGB);javax.imageio.ImageIO.write(image,"png",photo.toFile());
        app.users().updateAvatar(user,photo);
        var reread=new JdbcUserRepository(db).findById(user.getId()).orElseThrow();assertArrayEquals(user.getAvatarData(),reread.getAvatarData());
        assertEquals(512,javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(reread.getAvatarData())).getWidth());
        app.users().updateProfile(user,user.getFullName(),"profile-test@donga.edu.vn","0900000999");
        assertEquals("profile-test@donga.edu.vn",new JdbcUserRepository(db).findById(user.getId()).orElseThrow().getEmail());
        app.users().changePassword(user,"123456","new-password","new-password");
        assertThrows(ValidationException.class,()->AppServices.createJdbc().auth().login("sinhvien","123456"));
        var fresh=AppServices.createJdbc();var logged=fresh.auth().login("sinhvien","new-password");
        fresh.users().changePassword(logged,"new-password","123456","123456");fresh.users().updateAvatar(logged,null);
        assertNull(new JdbcUserRepository(db).findById(user.getId()).orElseThrow().getAvatarData());
    }
    @Test @Order(3) void enrollmentControlsStudentTimetableAndCounts() {
        var student=login("sinhvien");var week=LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var enrolled=db.query("SELECT course_section_id FROM student_enrollments WHERE student_id=? AND status='ACTIVE'",r->r.getLong(1),student.getId());
        assertTrue(app.schedules().findByWeekForUser(week,student).stream().allMatch(s->enrolled.contains(s.getCourseSection().getId())));
        login("daotao");var sections=new JdbcCourseSectionRepository(db);long id=10L;long before=db.scalar("SELECT student_count FROM course_sections WHERE id=?",id);
        boolean wasEnrolled=db.scalar("SELECT COUNT(*) FROM student_enrollments WHERE course_section_id=? AND student_id=? AND status='ACTIVE'",id,student.getId())>0;
        sections.enroll(id,student.getId(),true);sections.enroll(id,student.getId(),true);
        assertEquals(before+(wasEnrolled?0:1),db.scalar("SELECT student_count FROM course_sections WHERE id=?",id));
        sections.enroll(id,student.getId(),wasEnrolled);assertEquals(before,db.scalar("SELECT student_count FROM course_sections WHERE id=?",id));
        long logs=db.scalar("SELECT COUNT(*) FROM audit_logs");assertThrows(ValidationException.class,()->sections.enroll(id,1L,true));assertEquals(logs,db.scalar("SELECT COUNT(*) FROM audit_logs"));
    }
    @Test @Order(4) void conflictsPermissionsAndSoftCancellation() {
        login("daotao");var base=app.schedules().findAll().get(0);var slots=app.catalog().getTimeSlots();var room=app.rooms().findAll().stream().filter(r->r.getId()==4L).findFirst().orElseThrow();
        long before=db.scalar("SELECT COUNT(*) FROM audit_logs");
        assertThrows(ValidationException.class,()->app.schedules().save(copy(base,base.getRoom(),base.getDayOfWeek(),base.getStartSlot(),base.getEndSlot())));
        assertThrows(ValidationException.class,()->app.schedules().save(copy(base,room,base.getDayOfWeek(),base.getStartSlot(),base.getEndSlot())));
        var candidate=copy(base,room,8,slots.get(4),slots.get(4));app.schedules().save(candidate);assertNotNull(candidate.getId());
        assertEquals(1,db.scalar("SELECT COUNT(*) FROM audit_logs WHERE action='CREATE_SCHEDULE' AND entity_id=? AND user_id=?",candidate.getId(),db.actor().getId()));
        app.schedules().save(candidate); // Excludes itself when updating.
        assertEquals(1,db.scalar("SELECT COUNT(*) FROM audit_logs WHERE action='UPDATE_SCHEDULE' AND entity_id=? AND user_id=?",candidate.getId(),db.actor().getId()));
        assertTrue(db.scalar("SELECT COUNT(*) FROM audit_logs")>before);
        assertTrue(app.schedules().delete(candidate.getId()));assertEquals("CANCELLED",db.query("SELECT status FROM schedules WHERE id=?",r->r.getString(1),candidate.getId()).get(0));
        login("sinhvien");assertThrows(ValidationException.class,()->app.schedules().save(copy(base,room,8,slots.get(4),slots.get(4))));
    }
    @Test @Order(5) void requestTransactionNotifiesAndRollsBackOnConflict() {
        var lecturer=login("giangvien");var schedule=app.schedules().findAll().get(0);var slots=app.catalog().getTimeSlots();var room=app.rooms().findAll().stream().filter(r->r.getId()==4L).findFirst().orElseThrow();
        var req=new ChangeRequest(null,lecturer,RequestType.CHANGE_ROOM,schedule,room,LocalDate.now(),slots.get(0),"",0,"Đổi sang phòng phù hợp để giảng dạy.",Priority.NORMAL,RequestStatus.PENDING,LocalDateTime.now());app.requests().create(req);
        assertEquals(1,db.scalar("SELECT COUNT(*) FROM audit_logs WHERE action='CREATE_REQUEST' AND entity_id=? AND user_id=?",req.getId(),lecturer.getId()));
        var admin=login("admin");assertFalse(app.requests().canProcess(req,admin));assertThrows(ValidationException.class,()->app.requests().approve(req,admin));
        var academic=login("daotao");long notifications=db.scalar("SELECT COUNT(*) FROM notifications");app.requests().approve(req,academic);
        assertEquals(RequestStatus.APPROVED,req.getStatus());assertTrue(db.scalar("SELECT COUNT(*) FROM notifications")>notifications);
        assertEquals(room.getId().longValue(),db.scalar("SELECT classroom_id FROM schedules WHERE id=?",schedule.getId()));
        var firstReviewer=academic;assertThrows(ValidationException.class,()->app.requests().approve(req,firstReviewer));
        lecturer=login("giangvien");var conflicting=new ChangeRequest(null,lecturer,RequestType.CHANGE_SCHEDULE,schedule,room,LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY)),slots.get(2),"",0,"Đổi lịch để tham gia cuộc họp khoa.",Priority.NORMAL,RequestStatus.PENDING,LocalDateTime.now());app.requests().create(conflicting);
        // Place another section in the requested room/time to force an approval failure.
        academic=login("daotao");var other=app.schedules().findAll().stream().filter(s->s.getCourseSection().getId()==2L).findFirst().orElseThrow();
        var blocker=copy(other,room,5,slots.get(2),slots.get(3));app.schedules().save(blocker);
        long audits=db.scalar("SELECT COUNT(*) FROM audit_logs");long notes=db.scalar("SELECT COUNT(*) FROM notifications");var actor=academic;
        assertThrows(ValidationException.class,()->app.requests().approve(conflicting,actor));
        assertEquals("PENDING",db.query("SELECT status FROM change_requests WHERE id=?",r->r.getString(1),conflicting.getId()).get(0));assertEquals(audits,db.scalar("SELECT COUNT(*) FROM audit_logs"));assertEquals(notes,db.scalar("SELECT COUNT(*) FROM notifications"));app.schedules().delete(blocker.getId());
    }
    @Test @Order(6) void notificationAndRoomEquipmentPersistence() {
        var admin=login("admin");var notification=app.notifications().findForUser(admin).get(0);app.notifications().markRead(notification);
        assertEquals(1,db.scalar("SELECT is_read FROM notifications WHERE id=?",notification.getId()));
        var room=new Classroom(null,"TEST901","Phòng kiểm thử","Tòa T",1,30,RoomType.THEORY,RoomStatus.AVAILABLE,"");app.rooms().save(room);
        var e=new Equipment(null,"TESTEQ","Thiết bị kiểm thử","Kiểm thử",2,"Tốt",room,ResourceStatus.ACTIVE);app.rooms().saveEquipment(e);app.rooms().updateEquipmentQuantity(e,3);
        assertEquals(3,db.scalar("SELECT quantity FROM classroom_equipment WHERE id=?",e.getPlacementId()));
        app.rooms().toggleMaintenance(room);assertTrue(new JdbcRoomRepository(db).hasMaintenance(room.getId(),LocalDate.now()));
        app.rooms().toggleMaintenance(room);assertFalse(new JdbcRoomRepository(db).hasMaintenance(room.getId(),LocalDate.now()));
    }
    @Test @Order(7) void fourJdbcRolesRenderProfileAndDashboard() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            vn.edu.donga.unischedule.config.ThemeConfig.install();
            for(String account:List.of("admin","daotao","giangvien","sinhvien")) {
                var services=AppServices.createJdbc();var user=services.auth().login(account,"123456");
                var frame=new vn.edu.donga.unischedule.ui.frame.MainFrame(new vn.edu.donga.unischedule.controller.AppControllers(services),user);
                try {
                    frame.addNotify();frame.setSize(1440,900);
                    for(String screen:List.of("dashboard","profile")) {
                        frame.showScreen(screen);frame.validate();layout(frame.getRootPane());
                        var output=new java.awt.image.BufferedImage(1440,900,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=output.createGraphics();
                        try{frame.getRootPane().printAll(g);}finally{g.dispose();}
                        try{Path dir=Path.of("target","jdbc-previews");Files.createDirectories(dir);javax.imageio.ImageIO.write(output,"png",dir.resolve(account+"-"+screen+".png").toFile());}catch(java.io.IOException ex){throw new AssertionError(ex);}
                    }
                }finally{frame.dispose();}
            }
        });
    }
    private static void layout(java.awt.Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof java.awt.Container nested)layout(nested);}
    @Test @Order(8) void concurrentRoomBookingCommitsOnlyOne() throws Exception {
        var first=AppServices.createJdbc();first.auth().login("daotao","123456");
        var second=AppServices.createJdbc();second.auth().login("daotao","123456");
        var a=first.schedules().findAll().stream().filter(s->s.getCourseSection().getId()==1L).findFirst().orElseThrow();
        var b=second.schedules().findAll().stream().filter(s->s.getCourseSection().getId()==2L).findFirst().orElseThrow();
        var room=first.rooms().findAll().stream().filter(r->r.getId()==4L).findFirst().orElseThrow();var slot=first.catalog().getTimeSlots().get(4);
        var gate=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var jobs=List.of(pool.submit(()->bookAtGate(first,copy(a,room,8,slot,slot),gate)),pool.submit(()->bookAtGate(second,copy(b,room,8,slot,slot),gate)));
            gate.countDown();var outcomes=new ArrayList<Long>();for(var job:jobs)outcomes.add(job.get(30,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,outcomes.stream().filter(Objects::nonNull).count());
            first.schedules().delete(outcomes.stream().filter(Objects::nonNull).findFirst().orElseThrow());
        }finally{pool.shutdownNow();}
    }
    @Test @Order(9) void adminSeesOtherThreeRolesAndNotificationChangesAreAtomic() {
        var academic=login("daotao");
        var sections=new JdbcCourseSectionRepository(db);
        var base=sections.findAll().get(0);
        var section=new CourseSection(null,"AUDIT-SECTION",base.getCourse(),base.getSemester(),base.getLecturer(),30,0,CourseSectionStatus.UNSCHEDULED);
        sections.save(section);
        sections.save(section);
        assertTrue(sections.deleteById(section.getId()));
        for(String action:List.of("CREATE_SECTION","UPDATE_SECTION","CANCEL_SECTION")) {
            assertEquals(1,db.scalar("SELECT COUNT(*) FROM audit_logs WHERE action=? AND entity_type='course_sections' AND entity_id=? AND user_id=?",action,section.getId(),academic.getId()));
        }
        for(String account:List.of("daotao","giangvien","sinhvien")) {
            var actor=login(account);
            app.users().updateProfile(actor,actor.getFullName(),actor.getEmail(),actor.getPhone());
            var repository=new JdbcNotificationRepository(db);
            var note=app.notifications().findForUser(actor).get(0);
            db.update("UPDATE notifications SET is_read=0,read_at=NULL WHERE id=?",note.getId());
            long before=db.scalar("SELECT COUNT(*) FROM audit_logs");
            assertThrows(IllegalStateException.class,()->db.transaction(c->{
                note.setRead(true);
                repository.save(note);
                throw new IllegalStateException("Rollback audit together with the change");
            }));
            assertEquals(before,db.scalar("SELECT COUNT(*) FROM audit_logs"));
            assertEquals(0,db.scalar("SELECT is_read FROM notifications WHERE id=?",note.getId()));
            app.notifications().markRead(note);
            assertEquals(before+1,db.scalar("SELECT COUNT(*) FROM audit_logs"));
            app.notifications().markRead(note);
            assertEquals(before+1,db.scalar("SELECT COUNT(*) FROM audit_logs"));
            assertFalse(repository.deleteById(-1L));
            assertEquals(before+1,db.scalar("SELECT COUNT(*) FROM audit_logs"));
            assertTrue(repository.deleteById(note.getId()));
            assertEquals(before+2,db.scalar("SELECT COUNT(*) FROM audit_logs"));
        }
        login("admin");
        var history=app.audit().findAll();
        for(String account:List.of("daotao","giangvien","sinhvien")) {
            for(String action:List.of("UPDATE_PROFILE","READ_NOTIFICATION","DELETE_NOTIFICATION")) {
                assertTrue(history.stream().anyMatch(e->account.equals(e.user()) && action.equals(e.action())),account+" / "+action);
            }
        }
        for(String account:List.of("giangvien","sinhvien")) {
            login(account);
            assertThrows(ValidationException.class,()->app.audit().findAll());
        }
    }
    @Test @Order(10) void lecturerMakeupRequestNeedsClassAndAcademicApprovalCreatesOneDaySchedule() {
        var lecturer=login("giangvien");
        var source=app.schedules().findAll().stream()
                .filter(s->s.getStatus()==ScheduleStatus.PUBLISHED
                        && s.getCourseSection().getLecturer().getId().equals(lecturer.getId()))
                .findFirst().orElseThrow();
        ScheduleEntry makeup=null;
        LocalDate first=LocalDate.now().plusDays(1);
        if(first.isBefore(source.getCourseSection().getSemester().getStartDate())) first=source.getCourseSection().getSemester().getStartDate();
        for(int day=0;day<14 && makeup==null;day++) {
            LocalDate date=first.plusDays(day);
            if(date.isAfter(source.getCourseSection().getSemester().getEndDate())) break;
            for(var slot:app.catalog().getTimeSlots()) {
                for(var room:app.rooms().searchAvailableRooms(date,slot,null,null,source.getCourseSection().getStudentCount(),"")) {
                    var candidate=new ScheduleEntry(null,source.getCourseSection(),room,date.getDayOfWeek().getValue()+1,
                            slot,slot,date,date,ScheduleStatus.PUBLISHED,"Integration test makeup");
                    try { app.schedules().validate(candidate);makeup=candidate;break; }
                    catch(ValidationException ignored) { }
                }
                if(makeup!=null) break;
            }
        }
        assertNotNull(makeup,"Test data should have an available makeup slot");
        var invalid=new ChangeRequest(null,lecturer,RequestType.USE_ROOM,null,makeup.getRoom(),makeup.getStartDate(),
                makeup.getStartSlot(),"",0,"Học bù do nghỉ lễ.",Priority.NORMAL,RequestStatus.PENDING,LocalDateTime.now());
        long auditBefore=db.scalar("SELECT COUNT(*) FROM audit_logs");
        assertThrows(ValidationException.class,()->app.requests().create(invalid));
        assertEquals(auditBefore,db.scalar("SELECT COUNT(*) FROM audit_logs"));
        var request=new ChangeRequest(null,lecturer,RequestType.USE_ROOM,source,makeup.getRoom(),makeup.getStartDate(),
                makeup.getStartSlot(),"",0,"Học bù do nghỉ lễ.",Priority.NORMAL,RequestStatus.PENDING,LocalDateTime.now());
        app.requests().create(request);
        long academicId=db.scalar("SELECT id FROM users WHERE username='daotao'");
        assertEquals(1,db.scalar("SELECT COUNT(*) FROM notifications WHERE user_id=? AND reference_type='change_requests' AND reference_id=?",academicId,request.getId()));
        var admin=login("admin");
        assertFalse(app.requests().canProcess(request,admin));
        assertThrows(ValidationException.class,()->app.requests().approve(request,admin));
        var academic=login("daotao");
        long schedulesBefore=db.scalar("SELECT COUNT(*) FROM schedules");
        app.requests().approve(request,academic);
        assertEquals(schedulesBefore+1,db.scalar("SELECT COUNT(*) FROM schedules"));
        assertEquals(1,db.scalar("SELECT COUNT(*) FROM schedules WHERE course_section_id=? AND classroom_id=? AND start_date=? AND end_date=? AND start_slot_id=? AND status='PUBLISHED'",
                source.getCourseSection().getId(),makeup.getRoom().getId(),makeup.getStartDate(),makeup.getStartDate(),makeup.getStartSlot().getId()));
        long students=db.scalar("SELECT COUNT(*) FROM student_enrollments WHERE course_section_id=? AND status='ACTIVE'",source.getCourseSection().getId());
        assertEquals(students,db.scalar("SELECT COUNT(*) FROM notifications WHERE reference_type='change_requests' AND reference_id=? AND target_screen='timetable'",request.getId()));
        assertEquals(1,db.scalar("SELECT COUNT(*) FROM audit_logs WHERE action='APPROVE_REQUEST' AND entity_id=? AND user_id=?",request.getId(),academic.getId()));
        login("giangvien");
        var duplicate=new ChangeRequest(null,lecturer,RequestType.USE_ROOM,source,makeup.getRoom(),makeup.getStartDate(),
                makeup.getStartSlot(),"",0,"Thử đăng ký trùng ca.",Priority.NORMAL,RequestStatus.PENDING,LocalDateTime.now());
        app.requests().create(duplicate);
        academic=login("daotao");
        long logs=db.scalar("SELECT COUNT(*) FROM audit_logs"),notes=db.scalar("SELECT COUNT(*) FROM notifications");
        var reviewer=academic;
        assertThrows(ValidationException.class,()->app.requests().approve(duplicate,reviewer));
        assertEquals("PENDING",db.query("SELECT status FROM change_requests WHERE id=?",r->r.getString(1),duplicate.getId()).get(0));
        assertEquals(schedulesBefore+1,db.scalar("SELECT COUNT(*) FROM schedules"));
        assertEquals(logs,db.scalar("SELECT COUNT(*) FROM audit_logs"));
        assertEquals(notes,db.scalar("SELECT COUNT(*) FROM notifications"));
    }
    private static Long bookAtGate(AppServices app,ScheduleEntry s,java.util.concurrent.CountDownLatch gate) throws InterruptedException {
        gate.await();try{return app.schedules().save(s).getId();}catch(ValidationException expected){return null;}
    }
}
