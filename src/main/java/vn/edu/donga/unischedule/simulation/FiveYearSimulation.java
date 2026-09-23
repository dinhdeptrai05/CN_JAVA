package vn.edu.donga.unischedule.simulation;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

/** Scenario engine: one isolated in-memory dataset, no database connection or application writes. */
public final class FiveYearSimulation {
    public static final int YEARS = 5;
    final SimulationData d = new SimulationData();
    final Random random;
    final int startYear;
    final Map<Integer, String> roleByUser = new LinkedHashMap<>();
    final Map<Integer, Set<Integer>> studentsBySection = new HashMap<>();
    public FiveYearSimulation(SimulationBaseline baseline, int startYear, long seed) {
        if (startYear < 2000 || startYear > 2090) throw new IllegalArgumentException("Start year must be 2000..2090");
        this.startYear = startYear; random = new Random(seed); baseline.populate(d);
        for(var equipment:d.rows("classroom_equipment")) equipment.put("updated_at",startYear+"-01-01 00:00:00");
        if (d.rows("time_slots").size() != 6) throw new IllegalArgumentException("Scenario requires the project's six time slots");
        for (String role : List.of("ADMIN", "ACADEMIC", "LECTURER", "STUDENT"))
            for (int i = 0; i < baseline.count(role); i++) user(role, LocalDate.of(startYear, 1, 1), "BASELINE");
        // Baseline repairs complete before teaching starts; inactive rooms stay inactive.
        for (var room : d.rows("classrooms")) if (s(room, "status").equals("MAINTENANCE")) {
            maintenance(room, null, LocalDate.of(startYear, 1, 1), LocalDate.of(startYear, 1, 3)); room.put("status", "AVAILABLE");
        }
        for (var eq : d.rows("classroom_equipment")) if (!s(eq, "condition_status").equals("GOOD")) {
            maintenance(d.get("classrooms", eq.get("classroom_id")), eq, LocalDate.of(startYear, 1, 1), LocalDate.of(startYear, 1, 3)); eq.put("condition_status", "GOOD");
        }
    }
    public SimulationData run() {
        for (int year = startYear; year < startYear + YEARS; year++) simulateYear(year);
        SimulationValidator.validate(d);
        return d;
    }
    private int actor(String role) { return active(role).get(0); }
    private List<Integer> active(String role) {
        return d.rows("users").stream().filter(u -> s(u,"status").equals("ACTIVE") && roleByUser.get(n(u,"id")).equals(role)).map(u -> n(u,"id")).toList();
    }
    private int activeCount() { return (int)d.rows("users").stream().filter(u -> s(u,"status").equals("ACTIVE")).count(); }
    private void user(String role, LocalDate date, String event) {
        int id = d.rows("users").size()+1;
        var u = d.add("users", "username", "sim_user_"+id, "password_hash", "SIMULATION_DISABLED_LOGIN",
                "full_name", "SIM "+role+" "+id, "email", "sim"+id+"@example.invalid", "status", "ACTIVE",
                "department_id", d.rows("departments").get((id-1)%d.rows("departments").size()).get("id"),
                "created_at", date+" 00:00:00", "updated_at", date+" 00:00:00");
        if (role.equals("STUDENT")) { u.put("student_code", "SIM-SV"+id); u.put("class_code", "SIM-"+date.getYear()); }
        if (role.equals("LECTURER")) u.put("lecturer_code", "SIM-GV"+id);
        int roleId = n(d.rows("roles").stream().filter(r->s(r,"code").equals(role)).findFirst().orElseThrow(),"id");
        d.add("user_roles", "user_id", id, "role_id", roleId); roleByUser.put(id, role);
        d.userEvents.add(row("user_id",id,"date",date.toString(),"event",event,"role",role));
        audit(id,"USER_"+event,"users",id,date);
    }
    private void simulateYear(int year) {
        int beginning = activeCount(), studentStart = active("STUDENT").size();
        int admitted = Math.max(4, (int)Math.round(studentStart * .35));
        int left = Math.max(1, (int)Math.round(studentStart * .15));
        // Admissions and exits occur before semester 2, so semester 1 keeps its historical enrollments.
        semester(year, 1);
        var departing = new ArrayList<>(active("STUDENT")); Collections.shuffle(departing, random);
        for (int id : departing.subList(0, Math.min(left, departing.size()))) {
            var u=d.get("users",id); u.put("status","INACTIVE"); u.put("updated_at",year+"-07-01 00:00:00");
            d.userEvents.add(row("user_id",id,"date",year+"-07-01","event","LEFT","role","STUDENT"));
            audit(actor("ADMIN"),"USER_LEFT","users",id,LocalDate.of(year,7,1));
        }
        for (int i=0;i<admitted;i++) user("STUDENT",LocalDate.of(year,8,20),"NEW");
        int hired=0;
        while (active("LECTURER").size()<Math.max(6,(active("STUDENT").size()+9)/10)) { user("LECTURER",LocalDate.of(year,8,20),"NEW"); hired++; }
        // Summer maintenance occupies actual room dates, never teaching dates.
        for (var room:d.rows("classrooms")) if (!s(room,"status").equals("INACTIVE") && random.nextDouble()<.30) {
            var eq=d.rows("classroom_equipment").stream().filter(e->e.get("classroom_id").equals(room.get("id"))).findFirst().orElse(null);
            maintenance(room,eq,LocalDate.of(year,7,10),LocalDate.of(year,7,16));
        }
        semester(year,2);
        var summary=row("year",year,"users_start",beginning,"users_new",admitted+hired,"users_left",left,"users_end",activeCount(),
                "students_end",active("STUDENT").size(),"lecturers_end",active("LECTURER").size());
        summarize(year,summary); d.annual.add(summary);
    }
    private void semester(int year,int term) {
        LocalDate start=LocalDate.of(year,term==1?1:9,term==1?11:4).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate end=start.plusWeeks(16).minusDays(1);
        var sem=d.add("semesters","code","SIM-"+year+"-"+term,"name","SIM Học kỳ "+term+" / "+year,"start_date",start.toString(),"end_date",end.toString(),"status","COMPLETED");
        var students=active("STUDENT"); int courseIndex=0;
        for (var course:d.rows("courses")) {
            for (int from=0;from<students.size();from+=25) {
                var cohort=students.subList(from,Math.min(from+25,students.size()));
                var section=d.add("course_sections","course_id",course.get("id"),"semester_id",sem.get("id"),"code","SIM-"+year+"-"+term+"-"+(courseIndex+1)+"-"+from/25,
                        "capacity",25,"student_count",0,"status","SCHEDULED","created_at",start.minusDays(7)+" 08:00:00");
                int sid=n(section,"id"); var registered=new LinkedHashSet<Integer>();
                for (int uid:cohort) {
                    boolean cancel=random.nextDouble()<.06;
                    d.add("student_enrollments","course_section_id",sid,"student_id",uid,"status",cancel?"CANCELLED":"ACTIVE","enrolled_at",start.minusDays(7)+" 09:00:00");
                    if(!cancel) registered.add(uid);
                }
                studentsBySection.put(sid,registered); section.put("student_count",registered.size());
                var lecturers=active("LECTURER"); int lid=lecturers.get((courseIndex+from/25)%lecturers.size());
                var assignment=d.add("lecturer_assignments","course_section_id",sid,"lecturer_id",lid,"assigned_at",start.minusDays(7)+" 08:00:00");
                var schedule=d.add("schedules","course_section_id",sid,"lecturer_assignment_id",assignment.get("id"),"start_date",start.toString(),"end_date",end.toString(),
                        "status","DRAFT","note","SIMULATION","created_by",actor("ACADEMIC"),"created_at",start.minusDays(6)+" 08:00:00","updated_at",start.minusDays(6)+" 08:00:00");
                boolean placed=place(schedule,course,null,false);
                if(!placed) throw new IllegalStateException("No feasible slot for "+section.get("code")+"; reduce demand or add capacity");
                if(registered.isEmpty() || random.nextDouble()<.04) {
                    schedule.put("status","CANCELLED"); section.put("status","CANCELLED"); section.put("student_count",0); registered.clear();
                    d.rows("student_enrollments").stream().filter(e->e.get("course_section_id").equals(sid)).forEach(e->e.put("status","CANCELLED"));
                } else schedule.put("status","PUBLISHED");
                audit(actor("ACADEMIC"),"SCHEDULE_"+schedule.get("status"),"schedules",n(schedule,"id"),start.minusDays(6));
                // Five request types across each semester; probability yields opening-week peaks.
                if(s(schedule,"status").equals("PUBLISHED") && random.nextDouble()<.70) request(schedule,course,courseIndex%5,start);
            }
            courseIndex++;
        }
        for(int uid:students) notifyUser(uid,"SCHEDULE","semesters",n(sem,"id"),start.minusDays(2));
    }
    private boolean place(Map<String,Object> schedule,Map<String,Object> course,Integer excludedRoom,boolean changedTime) {
        var old=new LinkedHashMap<>(schedule);
        for(int day=2;day<=7;day++) for(int slot=1;slot<=5;slot+=2) for(var room:d.rows("classrooms")) {
            if(!s(room,"status").equals("AVAILABLE") || !s(room,"room_type").equals(s(course,"required_room_type")) || n(room,"capacity")<25) continue;
            if(excludedRoom!=null && n(room,"id")==excludedRoom) continue;
            if(changedTime && Objects.equals(old.get("day_of_week"),day) && Objects.equals(old.get("start_slot_id"),slot)) continue;
            schedule.putAll(row("classroom_id",room.get("id"),"day_of_week",day,"start_slot_id",slot,"end_slot_id",slot+1));
            if(feasible(schedule)) return true;
        }
        schedule.clear(); schedule.putAll(old); return false;
    }
    private boolean feasible(Map<String,Object> candidate) {
        for(var other:d.rows("schedules")) {
            if(other==candidate || !s(other,"status").equals("PUBLISHED") || !overlaps(candidate,other)) continue;
            if(Objects.equals(candidate.get("classroom_id"),other.get("classroom_id")) || lecturer(candidate)==lecturer(other)
                    || Objects.equals(candidate.get("course_section_id"),other.get("course_section_id"))) return false;
            if(!Collections.disjoint(studentsBySection.get(n(candidate,"course_section_id")),studentsBySection.get(n(other,"course_section_id")))) return false;
        }
        for(var maintenance:d.rows("maintenance_records")) if(Objects.equals(candidate.get("classroom_id"),maintenance.get("classroom_id")))
            for(var date:dates(candidate)) if(!date.isBefore(LocalDate.parse(s(maintenance,"start_date"))) && !date.isAfter(LocalDate.parse(s(maintenance,"end_date")))) return false;
        return true;
    }
    int lecturer(Map<String,Object> schedule) { return n(d.get("lecturer_assignments",schedule.get("lecturer_assignment_id")),"lecturer_id"); }
    static boolean overlaps(Map<String,Object> a,Map<String,Object>b) {
        return Objects.equals(a.get("day_of_week"),b.get("day_of_week")) && n(a,"start_slot_id")<=n(b,"end_slot_id") && n(b,"start_slot_id")<=n(a,"end_slot_id")
                && s(a,"start_date").compareTo(s(b,"end_date"))<=0 && s(b,"start_date").compareTo(s(a,"end_date"))<=0;
    }
    static List<LocalDate> dates(Map<String,Object> schedule) {
        var result=new ArrayList<LocalDate>(); LocalDate start=LocalDate.parse(s(schedule,"start_date")),end=LocalDate.parse(s(schedule,"end_date"));
        start=start.plusDays(Math.floorMod(n(schedule,"day_of_week")-1-start.getDayOfWeek().getValue(),7));
        for(var day=start;!day.isAfter(end);day=day.plusWeeks(1)) result.add(day);
        return result;
    }
    private void request(Map<String,Object> schedule,Map<String,Object> course,int typeIndex,LocalDate start) {
        String type=List.of("CHANGE_ROOM","CHANGE_SCHEDULE","BORROW_EQUIPMENT","REPORT_DAMAGE","USE_ROOM").get(typeIndex);
        double draw=random.nextDouble(); String status=draw<.65?"APPROVED":draw<.88?"REJECTED":"PENDING";
        int reviewer=actor(typeIndex==2 || typeIndex==3?"ADMIN":"ACADEMIC");
        var request=d.add("change_requests","requester_id",lecturer(schedule),"schedule_id",schedule.get("id"),"request_type",type,
                "reason","SIM: nhu cầu học kỳ","priority","NORMAL","status",status,"created_at",start.minusDays(5)+" 08:00:00");
        var before=new LinkedHashMap<>(schedule);
        if(status.equals("APPROVED")) {
            if(typeIndex<2) {
                if(!place(schedule,course,typeIndex==0?n(schedule,"classroom_id"):null,typeIndex==1)) status="REJECTED";
            } else if(typeIndex==4) {
                var extra=new LinkedHashMap<>(schedule); extra.put("id",d.rows("schedules").size()+1);
                extra.put("start_date",start.plusWeeks(2).toString()); extra.put("end_date",start.plusWeeks(2).plusDays(5).toString());
                if(place(extra,course,null,false)) {
                    LocalDate date=dates(extra).get(0); extra.put("start_date",date.toString()); extra.put("end_date",date.toString());
                    extra.put("note","SIM USE_ROOM request "+request.get("id")); d.rows("schedules").add(extra);
                    request.put("review_reason","created_schedule_id="+extra.get("id"));
                    request.put("requested_date",date.toString()); request.put("requested_day",extra.get("day_of_week"));
                    request.put("requested_room_id",extra.get("classroom_id")); request.put("requested_start_slot_id",extra.get("start_slot_id")); request.put("requested_end_slot_id",extra.get("end_slot_id"));
                } else status="REJECTED";
            }
        }
        request.putIfAbsent("requested_room_id",schedule.get("classroom_id"));
        request.putIfAbsent("requested_date",dates(schedule).get(0).toString()); request.putIfAbsent("requested_day",schedule.get("day_of_week"));
        request.putIfAbsent("requested_start_slot_id",schedule.get("start_slot_id")); request.putIfAbsent("requested_end_slot_id",schedule.get("end_slot_id"));
        if(typeIndex==2 || typeIndex==3) {
            var eq=d.rows("classroom_equipment").stream().filter(e->e.get("classroom_id").equals(schedule.get("classroom_id")) && n(e,"quantity")>0).findFirst().orElse(null);
            if(eq==null) status="REJECTED";
            else {
                request.put("requested_equipment_id",eq.get("equipment_id")); request.put("quantity",1);
                request.put("equipment_name",d.get("equipment",eq.get("equipment_id")).get("name"));
                if(typeIndex==3 && status.equals("APPROVED")) {
                    // Planned repair in the summer break, after this request and outside both terms.
                    LocalDate date=start.getMonthValue()==1?LocalDate.of(start.getYear(),5,15):LocalDate.of(start.getYear(),12,31);
                    maintenance(d.get("classrooms",schedule.get("classroom_id")),eq,date,date);
                }
            }
        }
        request.put("status",status);
        if(!status.equals("PENDING")) { request.put("reviewed_by",reviewer); request.put("reviewed_at",start.minusDays(4)+" 08:00:00"); }
        if(!before.equals(schedule)) { schedule.put("updated_at",start.minusDays(4)+" 08:00:00"); request.put("review_reason","SIM: áp dụng trước khi học kỳ bắt đầu"); }
        audit(reviewer,"REQUEST_"+status,"change_requests",n(request,"id"),start.minusDays(4));
        notifyUser(lecturer(schedule),"REQUEST","change_requests",n(request,"id"),start.minusDays(4));
    }
    private void maintenance(Map<String,Object> room,Map<String,Object> equipment,LocalDate start,LocalDate end) {
        var m=d.add("maintenance_records","classroom_id",room.get("id"),"reported_by",actor("ADMIN"),"description","SIM: bảo trì dự phòng/khắc phục",
                "start_date",start.toString(),"end_date",end.toString(),"status","COMPLETED","created_at",start+" 00:00:00");
        if(equipment!=null) m.put("classroom_equipment_id",equipment.get("id"));
        audit(actor("ADMIN"),"MAINTENANCE_COMPLETED","maintenance_records",n(m,"id"),end);
    }
    private void audit(int uid,String action,String table,int id,LocalDate date) {
        d.add("audit_logs","user_id",uid,"action",action,"entity_type",table,"entity_id",id,"details","{\"simulation\":true}","created_at",date+" 10:00:00");
    }
    private void notifyUser(int uid,String type,String table,int id,LocalDate date) {
        boolean read=random.nextDouble()<.8;
        var n=d.add("notifications","user_id",uid,"title","SIM "+type,"content","Dữ liệu mô phỏng phục vụ bài tập", "type",type,"reference_type",table,"reference_id",id,
                "target_screen",type.equals("SCHEDULE")?"timetable":"requests","is_read",read?1:0,"created_at",date+" 10:00:00");
        if(read) n.put("read_at",date.plusDays(1)+" 10:00:00");
    }
    private long yearCount(String table,String date,int year) { return d.rows(table).stream().filter(r->s(r,date).startsWith(""+year)).count(); }
    private void summarize(int year,Map<String,Object> result) {
        var schedules=d.rows("schedules").stream().filter(s->s(s,"start_date").startsWith(""+year)).toList();
        long sessions=0,minutes=0,used=0,available=0;
        for(int month=1;month<=12;month++) {
            long ms=0,mm=0,mu=0,ma=0; YearMonth ym=YearMonth.of(year,month);
            for(var schedule:schedules) if(s(schedule,"status").equals("PUBLISHED")) for(var date:dates(schedule)) if(date.getMonthValue()==month) {
                ms++; for(int slot=n(schedule,"start_slot_id");slot<=n(schedule,"end_slot_id");slot++) {
                    var time=d.get("time_slots",slot); mm+=Duration.between(LocalTime.parse(s(time,"start_time")),LocalTime.parse(s(time,"end_time"))).toMinutes(); mu++;
                }
            }
            for(int day=1;day<=ym.lengthOfMonth();day++) {
                LocalDate date=ym.atDay(day); if(date.getDayOfWeek()==DayOfWeek.SUNDAY) continue;
                for(var room:d.rows("classrooms")) if(!s(room,"status").equals("INACTIVE")) {
                    boolean unavailable=d.rows("maintenance_records").stream().anyMatch(m->m.get("classroom_id").equals(room.get("id")) && !date.isBefore(LocalDate.parse(s(m,"start_date"))) && !date.isAfter(LocalDate.parse(s(m,"end_date"))));
                    if(!unavailable) ma+=6;
                }
            }
            String prefix=ym.toString(); long requests=d.rows("change_requests").stream().filter(r->s(r,"created_at").startsWith(prefix)).count();
            d.monthly.add(row("year",year,"month",month,"sessions",ms,"teaching_minutes",mm,"used_room_slots",mu,"available_room_slots",ma,"requests",requests));
            sessions+=ms;minutes+=mm;used+=mu;available+=ma;
        }
        result.putAll(row("sections",yearCount("course_sections","created_at",year),"schedules",schedules.size(),"cancelled_schedules",schedules.stream().filter(s->s(s,"status").equals("CANCELLED")).count(),
                "enrollments",yearCount("student_enrollments","enrolled_at",year),"cancelled_enrollments",d.rows("student_enrollments").stream().filter(e->s(e,"enrolled_at").startsWith(""+year)&&s(e,"status").equals("CANCELLED")).count(),
                "sessions",sessions,"teaching_minutes",minutes,"used_room_slots",used,"available_room_slots",available,"utilization_pct",Math.round(used*10000.0/available)/100.0,
                "requests",yearCount("change_requests","created_at",year),"maintenance",yearCount("maintenance_records","start_date",year),"notifications",yearCount("notifications","created_at",year),"audit_logs",yearCount("audit_logs","created_at",year)));
        for(String status:List.of("APPROVED","REJECTED","PENDING")) result.put("requests_"+status.toLowerCase(Locale.ROOT),d.rows("change_requests").stream().filter(r->s(r,"created_at").startsWith(""+year)&&s(r,"status").equals(status)).count());
    }
}
