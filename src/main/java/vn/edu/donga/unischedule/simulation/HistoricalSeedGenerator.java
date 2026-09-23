package vn.edu.donga.unischedule.simulation;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

/** Additive teaching dataset: five years of past activity plus the current semester. */
public final class HistoricalSeedGenerator {
    // Same published demo credential as the project's original seed, NOT a production credential.
    public static final String DEMO_HASH="pbkdf2-sha256$600000$XUH+tGikkFPZSVDGlL3jkw==$3KcSrx8Puai3Y8rrZXubrAPNJavM7wlz9IxY3VBEUx0=";
    final SimulationData d=new SimulationData();
    final LocalDate asOf,from;
    final Random random;
    final Map<Integer,Set<Integer>> members=new HashMap<>();
    final Map<Integer,Integer> admitted=new LinkedHashMap<>();
    final Map<String,Set<Integer>> busyRooms=new HashMap<>(),busyTeachers=new HashMap<>(),busyStudents=new HashMap<>();
    final List<Map<String,Object>> yearly=new ArrayList<>();
    int studentSequence;
    public HistoricalSeedGenerator(SimulationBaseline baseline,LocalDate asOf,long seed) {
        this.asOf=asOf; this.from=asOf.minusYears(5); random=new Random(seed); baseline.populate(d);
        // Only these three catalogs are shared with the existing application. Everything else is NEW.
        for(String table:List.of("courses","classrooms","equipment","classroom_equipment")) d.rows(table).clear();
        if(d.rows("departments").size()<3 || d.rows("time_slots").size()!=6) throw new IllegalArgumentException("Need 3 departments and the project's 6 slots");
        addUser("ADMIN",0,from,"qt_hethong"); addUser("ACADEMIC",0,from,"pdt_01");
        for(int i=0;i<24;i++) addUser("LECTURER",i%3,from,String.format(Locale.ROOT,"gv%04d",i+1));
        catalogs();
    }
    public SimulationData generate() {
        for(int year=from.getYear();year<=asOf.getYear();year++) {
            int before=activeStudents().size();
            if(year>from.getYear() && !LocalDate.of(year,7,15).isAfter(asOf)) {
                for(var user:new ArrayList<>(activeStudents())) if(year-admitted.get(n(user,"id"))>=4 || random.nextDouble()<.035) {
                    var date=LocalDate.of(year,7,15); user.put("status","INACTIVE");user.put("updated_at",stamp(date));
                    d.userEvents.add(row("user_id",user.get("id"),"role","STUDENT","date",date.toString(),"event","LEFT"));
                    audit(1,"DEACTIVATE_USER","users",n(user,"id"),date);
                }
            }
            // The second semester uses students still active before the July exits.
            if(year>from.getYear()) term(year,1);
            LocalDate intake=year==from.getYear()?from:LocalDate.of(year,8,25);
            if(!intake.isAfter(asOf)) {
                for(int i=0;i<60+(year-from.getYear())*10;i++) {
                    int uid=addUser("STUDENT",i%3,intake,"sv"+year+String.format(Locale.ROOT,"%04d",++studentSequence)); admitted.put(uid,year);
                }
                term(year,2);
            }
            if(year>from.getYear() && !LocalDate.of(year,7,20).isAfter(asOf)) for(var room:d.rows("classrooms")) if(random.nextDouble()<.35)
                maintenance(room,LocalDate.of(year,7,20),LocalDate.of(year,7,24));
            yearly.add(row("year",year,"students_before",before,"students_after",activeStudents().size()));
        }
        // A few recent actionable requests and notices make the current UI useful as well as historical.
        var current=d.rows("schedules").stream().filter(s->s(s,"status").equals("PUBLISHED") && !LocalDate.parse(s(s,"end_date")).isBefore(asOf)).limit(5).toList();
        for(int i=0;i<current.size();i++) {
            var schedule=current.get(i);
            int type=i==2?0:2;
            request(schedule,type,asOf.minusDays(1),true);
        }
        for(var u:d.rows("users")) {
            int id=n(u,"id");
            LocalDate created=LocalDate.parse(s(u,"created_at").substring(0,10));
            LocalDate left=d.userEvents.stream().filter(event->n(event,"user_id")==id&&s(event,"event").equals("LEFT"))
                    .map(event->LocalDate.parse(s(event,"date"))).findFirst().orElse(null);
            LocalDate login=HistoricalLoginTimeline.lastLogin(s(u,"username"),created,left,asOf,u.containsKey("student_code"));
            u.put("last_login_at",stamp(login));
            audit(id,"LOGIN","users",id,login);
        }
        // Validator reads lifecycle dates rather than present-day ACTIVE for historical attendance.
        SimulationValidator.validate(d);
        validateTimestamps();
        return d;
    }
    private List<Map<String,Object>> activeStudents() { return d.rows("users").stream().filter(u->admitted.containsKey(n(u,"id"))&&s(u,"status").equals("ACTIVE")).toList(); }
    private String stamp(LocalDate date) { return date+" 08:00:00"; }
    private int addUser(String role,int department,LocalDate date,String username) {
        int id=d.rows("users").size()+1;
        String[] surnames={"Nguyễn","Trần","Lê","Phạm","Hoàng","Đặng","Võ","Bùi"},names={"Minh Anh","Hoàng Nam","Thu Hà","Gia Huy","Ngọc Linh","Quốc Bảo","Thanh Trúc","Đức Minh","Thảo Vy","Anh Tuấn"};
        int dep=n(d.rows("departments").get(department),"id");
        String login=username;
        var u=d.add("users","username",login,"password_hash",DEMO_HASH,"full_name",surnames[random.nextInt(surnames.length)]+" "+names[random.nextInt(names.length)],
                "email",HistoricalDisplayNames.email(login,role.equals("STUDENT")),"phone","","department_id",dep,"status","ACTIVE","created_at",stamp(date),"updated_at",stamp(date));
        if(role.equals("STUDENT")) {u.put("student_code",login.toUpperCase(Locale.ROOT));u.put("class_code",HistoricalDisplayNames.classCode(s(d.rows("departments").get(department),"code"),date.getYear()));}
        if(role.equals("LECTURER")) u.put("lecturer_code",login.toUpperCase(Locale.ROOT));
        var r=d.rows("roles").stream().filter(x->s(x,"code").equals(role)).findFirst().orElseThrow();
        d.add("user_roles","user_id",id,"role_id",r.get("id"));
        d.userEvents.add(row("user_id",id,"role",role,"event",date.equals(from)?"BASELINE":"NEW","date",date.toString()));
        audit(1,"CREATE_USER","users",id,date); return id;
    }
    private void catalogs() {
        String[][] names={{"Lập trình hướng đối tượng","Cấu trúc dữ liệu nâng cao","Cơ sở dữ liệu ứng dụng","Phát triển ứng dụng Java","Mạng máy tính","Kỹ thuật phần mềm","Kiểm thử phần mềm","Phân tích hệ thống"},
                {"Quản trị học","Kinh tế vĩ mô","Nguyên lý kế toán","Thống kê kinh doanh","Quản trị marketing","Quản trị nhân lực","Thương mại điện tử","Quản trị dự án"},
                {"Ngữ âm tiếng Anh","Kỹ năng nghe nói","Đọc hiểu học thuật","Viết học thuật","Biên dịch cơ bản","Giao tiếp liên văn hóa","Tiếng Anh thương mại","Phương pháp nghiên cứu"}};
        for(int dep=0;dep<3;dep++) for(int i=0;i<8;i++) d.add("courses","department_id",d.rows("departments").get(dep).get("id"),"code",HistoricalDisplayNames.courseCode(dep,i),"name",names[dep][i],"credits",3,"required_room_type",dep==0?(i<5?"COMPUTER":"THEORY"):dep==1?"THEORY":"PRACTICE","status","ACTIVE");
        for(String type:List.of("THEORY","COMPUTER","PRACTICE","LABORATORY")) for(int i=0;i<(type.equals("LABORATORY")?2:6);i++) {
            int id=d.rows("classrooms").size()+1;
            String roomCode=HistoricalDisplayNames.roomCode(id);
            var room=d.add("classrooms","code",roomCode,"name","Phòng "+roomCode,"building",HistoricalDisplayNames.building(id),"floor",i/3+1,"capacity",type.equals("THEORY")?60:40,"room_type",type,"status","AVAILABLE","description","Phòng học phục vụ giảng dạy và thực hành");
            for(int k=0;k<3;k++) {
                String eqName=k==0?"Máy chiếu":k==1?"Điều hòa":type.equals("COMPUTER")?"Máy tính sinh viên":"Loa phòng học";
                var eq=d.add("equipment","code",HistoricalDisplayNames.equipmentCode(roomCode,k+1),"name",eqName,"category","Thiết bị phòng học","status","ACTIVE");
                d.add("classroom_equipment","classroom_id",room.get("id"),"equipment_id",eq.get("id"),"quantity",k==2&&type.equals("COMPUTER")?40:k==1?2:1,"condition_status","GOOD","condition_note","Đã kiểm tra","updated_at",stamp(from));
            }
        }
    }
    private void term(int year,int half) {
        LocalDate start=LocalDate.of(year,half==1?1:9,half==1?12:7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if(year==from.getYear()) start=from.plusDays(7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if(start.isAfter(asOf) || start.minusDays(7).isBefore(from)) return;
        LocalDate end=start.plusWeeks(16).minusDays(1);
        if(half==2 && end.getYear()>year)end=LocalDate.of(year,12,31);
        boolean historical=end.isBefore(asOf);
        var sem=d.add("semesters","code",HistoricalDisplayNames.semesterCode(year,half),"name",HistoricalDisplayNames.semesterName(year,half),"start_date",start.toString(),"end_date",end.toString(),"status",historical?"COMPLETED":"ACTIVE");
        busyRooms.clear();busyTeachers.clear();busyStudents.clear();
        List<Map<String,Object>> students=d.rows("users").stream().filter(u->admitted.containsKey(n(u,"id"))&&SimulationValidator.activeOn(d,n(u,"id"),s(sem,"start_date"))).toList();
        int index=0;
        for(int dep=0;dep<3;dep++) {
            int depId=n(d.rows("departments").get(dep),"id");
            var departmentStudents=students.stream().filter(u->n(u,"department_id")==depId).toList();
            var courses=d.rows("courses").stream().filter(c->n(c,"department_id")==depId).toList();
            var teachers=d.rows("users").stream().filter(u->u.containsKey("lecturer_code")&&n(u,"department_id")==depId).toList();
            for(int offset=0;offset<departmentStudents.size();offset+=25) for(int subject=0;subject<4;subject++) {
                var group=departmentStudents.subList(offset,Math.min(offset+25,departmentStudents.size()));
                var course=courses.get((half==1?4:0)+subject);
                var section=d.add("course_sections","course_id",course.get("id"),"semester_id",sem.get("id"),"code",HistoricalDisplayNames.sectionCode(s(course,"code"),year,half,++index),"capacity",25,"student_count",0,"status",historical?"CLOSED":"SCHEDULED","created_at",stamp(start.minusDays(7)));
                var memberSet=new LinkedHashSet<Integer>();members.put(n(section,"id"),memberSet);
                boolean cancelled=random.nextDouble()<.025;
                for(var u:group) {
                    boolean cancel=cancelled||random.nextDouble()<.045;
                    var en=d.add("student_enrollments","course_section_id",section.get("id"),"student_id",u.get("id"),"status",cancel?"CANCELLED":"ACTIVE","enrolled_at",stamp(start.minusDays(5)));
                    if(!cancel) memberSet.add(n(u,"id"));
                    audit(2,cancel?"CANCEL_ENROLLMENT":"ENROLL","student_enrollments",n(en,"id"),start.minusDays(5));
                }
                section.put("student_count",memberSet.size()); if(memberSet.isEmpty()) cancelled=true;
                if(cancelled) section.put("status","CANCELLED");
                var lecturer=teachers.get((index+offset/25)%teachers.size());
                var assignment=d.add("lecturer_assignments","course_section_id",section.get("id"),"lecturer_id",lecturer.get("id"),"assigned_at",stamp(start.minusDays(7)));
                var schedule=d.add("schedules","course_section_id",section.get("id"),"lecturer_assignment_id",assignment.get("id"),"start_date",start.toString(),"end_date",end.toString(),"status",cancelled?"CANCELLED":"PUBLISHED","note","Lịch học chính khóa","created_by",2,"created_at",stamp(start.minusDays(4)),"updated_at",stamp(start.minusDays(4)));
                if(!place(schedule,null,false)) throw new IllegalStateException("No capacity for "+section.get("code"));
                if(!cancelled) reserve(schedule);
                audit(2,cancelled?"CANCEL_SCHEDULE":"PUBLISH_SCHEDULE","schedules",n(schedule,"id"),start.minusDays(4));
                if(!cancelled && random.nextDouble()<.75) request(schedule,index%5,start.minusDays(3),false);
                for(var u:group) notify(n(u,"id"),"SCHEDULE","course_sections",n(section,"id"),start.minusDays(2),"Thời khóa biểu "+section.get("code")+" đã công bố");
            }
        }
        notify(1,"SYSTEM","semesters",n(sem,"id"),start.minusDays(1),"Đã hoàn tất chuẩn bị học kỳ "+sem.get("name"));
        notify(2,"SCHEDULE","semesters",n(sem,"id"),start.minusDays(1),"Đã công bố "+index+" lớp học phần");
    }
    private int teacher(Map<String,Object> schedule) {return n(d.get("lecturer_assignments",schedule.get("lecturer_assignment_id")),"lecturer_id");}
    private boolean place(Map<String,Object> schedule,Integer excludedRoom,boolean changeTime) {
        var original=new LinkedHashMap<>(schedule);var section=d.get("course_sections",schedule.get("course_section_id"));var course=d.get("courses",section.get("course_id"));
        for(int day=2;day<=7;day++) for(int slot=1;slot<=5;slot+=2) for(var room:d.rows("classrooms")) {
            if(!s(room,"room_type").equals(s(course,"required_room_type")) || (excludedRoom!=null&&n(room,"id")==excludedRoom)) continue;
            if(excludedRoom!=null && (!Objects.equals(original.get("day_of_week"),day)||!Objects.equals(original.get("start_slot_id"),slot))) continue;
            if(changeTime&&Objects.equals(original.get("day_of_week"),day)&&Objects.equals(original.get("start_slot_id"),slot)) continue;
            String key=day+"/"+slot;
            if(busyRooms.getOrDefault(key,Set.of()).contains(n(room,"id"))||busyTeachers.getOrDefault(key,Set.of()).contains(teacher(schedule))||!Collections.disjoint(busyStudents.getOrDefault(key,Set.of()),members.get(n(section,"id")))) continue;
            schedule.putAll(row("day_of_week",day,"start_slot_id",slot,"end_slot_id",slot+1,"classroom_id",room.get("id")));return true;
        }
        schedule.clear();schedule.putAll(original);return false;
    }
    private void reserve(Map<String,Object> s) {
        String key=s.get("day_of_week")+"/"+s.get("start_slot_id");
        busyRooms.computeIfAbsent(key,k->new HashSet<>()).add(n(s,"classroom_id"));
        busyTeachers.computeIfAbsent(key,k->new HashSet<>()).add(teacher(s));
        busyStudents.computeIfAbsent(key,k->new HashSet<>()).addAll(members.get(n(s,"course_section_id")));
    }
    private void release(Map<String,Object> s) {
        String key=s.get("day_of_week")+"/"+s.get("start_slot_id");
        busyRooms.get(key).remove(n(s,"classroom_id"));busyTeachers.get(key).remove(teacher(s));
        busyStudents.get(key).removeAll(members.get(n(s,"course_section_id")));
    }
    private void request(Map<String,Object> schedule,int type,LocalDate date,boolean pending) {
        String requestType=List.of("CHANGE_ROOM","CHANGE_SCHEDULE","BORROW_EQUIPMENT","REPORT_DAMAGE","USE_ROOM").get(type);
        boolean approve=!pending&&random.nextDouble()<.8;
        var r=d.add("change_requests","requester_id",teacher(schedule),"schedule_id",schedule.get("id"),"request_type",requestType,"reason",List.of("Cần phòng phù hợp bài thực hành","Điều chỉnh giờ giảng dạy","Mượn thiết bị hỗ trợ giảng dạy","Thiết bị cần kiểm tra định kỳ","Đăng ký buổi học bổ sung").get(type),"priority",type==3?"HIGH":"NORMAL","created_at",stamp(date));
        if(approve&&(type==0||type==1)) {
            release(schedule);
            approve=place(schedule,type==0?n(schedule,"classroom_id"):null,type==1);
            reserve(schedule);
            if(approve) schedule.put("updated_at",stamp(date.plusDays(1)));
        }
        Map<String,Object> desired=schedule;
        if(approve&&type==4) {
            var extra=new LinkedHashMap<>(schedule);extra.put("id",d.rows("schedules").size()+1);
            approve=place(extra,null,true);
            if(approve) {
                LocalDate day=FiveYearSimulation.dates(extra).get(0).plusWeeks(2);
                extra.put("start_date",day.toString());extra.put("end_date",day.toString());extra.put("created_at",stamp(date.plusDays(1)));extra.put("updated_at",stamp(date.plusDays(1)));extra.put("note","Buổi học bổ sung");
                d.rows("schedules").add(extra);reserve(extra);desired=extra;
                audit(2,"CREATE_MAKEUP","schedules",n(extra,"id"),date.plusDays(1));
            }
        }
        LocalDate desiredDate=FiveYearSimulation.dates(desired).stream().filter(x->!x.isBefore(date)).findFirst().orElse(LocalDate.parse(s(desired,"start_date")));
        r.putAll(row("requested_room_id",desired.get("classroom_id"),"requested_date",desiredDate.toString(),"requested_day",desired.get("day_of_week"),"requested_start_slot_id",desired.get("start_slot_id"),"requested_end_slot_id",desired.get("end_slot_id")));
        if(pending&&type==0) {
            var originalRoom=d.get("classrooms",schedule.get("classroom_id"));
            String key=schedule.get("day_of_week")+"/"+schedule.get("start_slot_id");
            d.rows("classrooms").stream().filter(room->!room.get("id").equals(originalRoom.get("id"))&&room.get("room_type").equals(originalRoom.get("room_type"))
                    &&!busyRooms.getOrDefault(key,Set.of()).contains(n(room,"id"))).findFirst().ifPresent(room->r.put("requested_room_id",room.get("id")));
        }
        if(type==2||type==3) {
            var eq=d.rows("classroom_equipment").stream().filter(e->e.get("classroom_id").equals(schedule.get("classroom_id"))).findFirst().orElseThrow();
            r.putAll(row("requested_equipment_id",eq.get("equipment_id"),"equipment_name",d.get("equipment",eq.get("equipment_id")).get("name"),"quantity",1));
            if(type==3&&approve) {
                // Repair before the first teaching day, so its room is never in use during maintenance.
                maintenance(d.get("classrooms",schedule.get("classroom_id")),date.plusDays(1),date.plusDays(2));
            }
        }
        r.put("status",pending?"PENDING":approve?"APPROVED":"REJECTED");
        if(!pending) r.putAll(row("reviewed_by",type==2||type==3?1:2,"reviewed_at",stamp(date.plusDays(1)),"review_reason",approve?"Đã duyệt và áp dụng":"Không bố trí được tài nguyên phù hợp"));
        audit(type==2||type==3?1:2,pending?"SUBMIT_REQUEST":approve?"APPROVE_REQUEST":"REJECT_REQUEST","change_requests",n(r,"id"),pending?date:date.plusDays(1));
        notify(pending?(type==2?1:2):teacher(schedule),"REQUEST","change_requests",n(r,"id"),pending?date:date.plusDays(1),pending?"Có yêu cầu cần xử lý":"Yêu cầu "+requestType+" đã được xử lý");
    }
    private void maintenance(Map<String,Object> room,LocalDate start,LocalDate end) {
        var eq=d.rows("classroom_equipment").stream().filter(e->e.get("classroom_id").equals(room.get("id"))).findFirst().orElseThrow();
        var m=d.add("maintenance_records","classroom_id",room.get("id"),"classroom_equipment_id",eq.get("id"),"reported_by",1,"description","Kiểm tra và thay linh kiện thiết bị","start_date",start.toString(),"end_date",end.toString(),"status","COMPLETED","created_at",stamp(start));
        if(s(eq,"updated_at").compareTo(stamp(end))<0)eq.put("updated_at",stamp(end));
        audit(1,"COMPLETE_MAINTENANCE","maintenance_records",n(m,"id"),end);
    }
    private void notify(int uid,String type,String table,int id,LocalDate date,String content) {
        boolean read=date.isBefore(asOf.minusDays(7))||random.nextDouble()<.65;
        var n=d.add("notifications","user_id",uid,"title",content,"content",content+".","type",type,"reference_type",table,"reference_id",id,"target_screen",type.equals("SCHEDULE")?"timetable":type.equals("REQUEST")?"requests":"dashboard","is_read",read?1:0,"created_at",stamp(date));
        if(read)n.put("read_at",stamp(date.plusDays(1).isAfter(asOf)?asOf:date.plusDays(1)));
    }
    private void audit(int uid,String action,String table,int id,LocalDate date) {
        d.add("audit_logs","user_id",uid,"action",action,"entity_type",table,"entity_id",id,"details","{\"seed\":\"HISTORY_5Y\"}","created_at",stamp(date));
    }
    private void validateTimestamps() {
        for(var table:d.tables.values()) for(var r:table) for(String key:List.of("created_at","updated_at","reviewed_at","read_at","enrolled_at","assigned_at","last_login_at")) if(r.containsKey(key)) {
            LocalDate date=LocalDate.parse(s(r,key).substring(0,10));
            if(date.isBefore(from)||date.isAfter(asOf)) throw new IllegalStateException("Historical timestamp outside window: "+key+" "+date);
        }
    }
}
