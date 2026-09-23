package vn.edu.donga.unischedule.simulation;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import vn.edu.donga.unischedule.model.Report;
import vn.edu.donga.unischedule.repository.jdbc.*;
import vn.edu.donga.unischedule.service.*;
import vn.edu.donga.unischedule.util.PasswordHasher;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

public final class HistoricalSeedMain {
    private HistoricalSeedMain() { }
    public static void main(String[] args) throws Exception {
        var options=new HashMap<String,String>();boolean apply=false,verify=false;
        for(int i=0;i<args.length;i++) {
            if(args[i].equals("--apply")){apply=true;continue;}
            if(args[i].equals("--verify")){verify=true;continue;}
            if(!List.of("--as-of","--seed","--baseline","--out").contains(args[i])||i+1==args.length)throw new IllegalArgumentException("--apply | --verify; --as-of YYYY-MM-DD --seed N --baseline FILE --out NEW_DIR");
            options.put(args[i],args[++i]);
        }
        LocalDate asOf=LocalDate.parse(options.getOrDefault("--as-of",LocalDate.now().toString()));long seed=Long.parseLong(options.getOrDefault("--seed","20260923"));
        if(verify){verifyApplication(asOf);return;}
        if(apply)try(var c=new ConnectionFactory().open()) {if(HistoricalSeedImporter.alreadyApplied(c)){System.out.println("ALREADY_APPLIED: no rows added or changed. Use --verify to inspect.");return;}}
        var baseline=new SimulationBaseline(Path.of(options.getOrDefault("--baseline","simulation/baseline-2026-09-22.properties")));
        var data=new HistoricalSeedGenerator(baseline,asOf,seed).generate();
        var counts=new LinkedHashMap<String,Object>();for(String table:TABLES)counts.put(table,HistoricalSeedImporter.SHARED.contains(table)?0:data.rows(table).size());
        var years=new ArrayList<Map<String,Object>>();
        for(int year=asOf.getYear()-5;year<=asOf.getYear();year++) {
            String prefix=""+year;long sessions=0;
            for(var schedule:data.rows("schedules"))if(s(schedule,"status").equals("PUBLISHED"))sessions+=FiveYearSimulation.dates(schedule).stream().filter(day->day.getYear()==Integer.parseInt(prefix)&&!day.isAfter(asOf)).count();
            years.add(row("year",year,"sections",data.rows("course_sections").stream().filter(r->s(r,"created_at").startsWith(prefix)).count(),"elapsed_sessions",sessions,
                    "enrollments",data.rows("student_enrollments").stream().filter(r->s(r,"enrolled_at").startsWith(prefix)).count(),"requests",data.rows("change_requests").stream().filter(r->s(r,"created_at").startsWith(prefix)).count()));
        }
        Path out=Path.of(options.getOrDefault("--out","target/history-seed-"+System.currentTimeMillis()));Files.createDirectories(out.toAbsolutePath().getParent());Files.createDirectory(out);
        var plan=row("kind","SYNTHETIC_HISTORY","as_of",asOf.toString(),"seed",seed,"to_insert",counts,"yearly",years,"active_students",data.rows("users").stream().filter(u->u.containsKey("student_code")&&s(u,"status").equals("ACTIVE")).count());
        Files.writeString(out.resolve("plan.json"),SimulationExport.json(plan));
        System.out.println("Historical seed plan: "+SimulationExport.json(plan));
        if(!apply){System.out.println("PREVIEW ONLY: no database writes. Add --apply to seed.");return;}
        try(var c=new ConnectionFactory().open()) {
            var result=HistoricalSeedImporter.apply(c,data,asOf,seed);
            System.out.println(result.inserted()?"COMMITTED: historical rows inserted; every pre-existing row fingerprint unchanged.":"ALREADY_APPLIED: no rows changed.");
            Files.writeString(out.resolve("receipt.json"),SimulationExport.json(row("committed",result.inserted(),"added",result.counts(),"original_rows_sha256",result.originals())));
        }
        verifyApplication(asOf);
    }
    /** Read data through the SAME repositories/services used by Swing, without login side effects. */
    static void verifyApplication(LocalDate asOf) throws Exception {
        var factory=new ConnectionFactory(){@Override public java.sql.Connection open() throws java.sql.SQLException {var c=super.open();c.setReadOnly(true);return c;}};
        var db=new JdbcDatabase(factory);var users=new JdbcUserRepository(db);var admin=users.findByUsername("qt_hethong").orElseThrow();db.setActor(admin);
        if(!PasswordHasher.verify("123456",admin.getPassword()))throw new IllegalStateException("Seed login hash invalid");
        var allUsers=users.findAll();var schedules=new JdbcScheduleRepository(db);var allSchedules=schedules.findAll();
        var allRequests=new JdbcRequestRepository(db).findAll();var equipment=new JdbcRoomRepository(db).findAllEquipment();
        var seedSchedules=new HashSet<>(db.query("SELECT DISTINCT entity_id FROM audit_logs WHERE entity_type='schedules' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.seed'))='HISTORY_5Y' AND entity_id IS NOT NULL",r->r.getLong(1)));
        long conflicts=new ConflictService(schedules).findAllConflicts().stream().filter(c->seedSchedules.contains(c.getFirstSchedule().getId())||c.getSecondSchedule()!=null&&seedSchedules.contains(c.getSecondSchedule().getId())).count();
        if(conflicts!=0)throw new IllegalStateException("Seed schedule conflicts: "+conflicts);
        var lecturer=users.findByUsername("gv0001").orElseThrow();
        var student=allUsers.stream().filter(u->u.getUsername().matches("sv20[0-9]{6}")&&u.getStatus()==vn.edu.donga.unischedule.model.Enums.UserStatus.ACTIVE).reduce((a,b)->b).orElseThrow();
        LocalDate week=asOf.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int studentLessons=schedules.findForUser(week,student).size(),teacherLessons=schedules.findForUser(week,lecturer).size();
        if(studentLessons==0||teacherLessons==0)throw new IllegalStateException("Current demo student/lecturer has no timetable");
        var service=new ReportService(new JdbcReportRepository(db));
        for(int year=asOf.getYear()-5;year<=asOf.getYear();year++) {
            LocalDate end=LocalDate.of(year,12,31);if(end.isAfter(asOf))end=asOf;
            var report=service.generate(admin,new Report.Filter(LocalDate.of(year,1,1),end,null,null));
            System.out.println("Native report "+year+": "+report.sessions()+" sessions, "+report.registrations()+" registrations, "+report.requests()+" requests");
        }
        System.out.println("NATIVE_READ_OK: users="+allUsers.size()+", schedules="+allSchedules.size()+", requests="+allRequests.size()+", equipment="+equipment.size()+", seed conflicts=0");
        System.out.println("DEMO: qt_hethong / pdt_01 / gv0001 / "+student.getUsername()+"; password=123456. Current week lessons: lecturer="+teacherLessons+", student="+studentLessons);
    }
}
