package vn.edu.donga.unischedule.simulation;

import java.time.*;
import java.util.*;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

/** Independent reconciliation; fail before exporting if any invariant is violated. */
public final class SimulationValidator {
    private SimulationValidator() { }
    static void require(boolean valid,String message) { if(!valid) throw new IllegalStateException(message); }
    public static void validate(SimulationData d) {
        for(String table:TABLES) {
            Set<Object> ids=new HashSet<>();
            for(var r:d.rows(table)) if(r.containsKey("id")) require(ids.add(r.get("id")),"Duplicate id in "+table);
        }
        for(String spec:List.of("users:department_id:departments","user_roles:user_id:users","user_roles:role_id:roles","courses:department_id:departments",
                "course_sections:course_id:courses","course_sections:semester_id:semesters","lecturer_assignments:course_section_id:course_sections","lecturer_assignments:lecturer_id:users",
                "student_enrollments:course_section_id:course_sections","student_enrollments:student_id:users","classroom_equipment:classroom_id:classrooms","classroom_equipment:equipment_id:equipment",
                "schedules:course_section_id:course_sections","schedules:lecturer_assignment_id:lecturer_assignments","schedules:classroom_id:classrooms","schedules:start_slot_id:time_slots","schedules:end_slot_id:time_slots","schedules:created_by:users",
                "change_requests:requester_id:users","change_requests:schedule_id:schedules","change_requests:requested_room_id:classrooms","change_requests:requested_equipment_id:equipment","change_requests:requested_start_slot_id:time_slots","change_requests:requested_end_slot_id:time_slots","change_requests:reviewed_by:users",
                "notifications:user_id:users","audit_logs:user_id:users","maintenance_records:classroom_id:classrooms","maintenance_records:classroom_equipment_id:classroom_equipment","maintenance_records:reported_by:users")) {
            String[] parts=spec.split(":");
            for(var r:d.rows(parts[0])) if(r.get(parts[1])!=null) d.get(parts[2],r.get(parts[1]));
        }
        Map<Integer,String> roles=new HashMap<>();
        for(var r:d.rows("user_roles")) require(roles.put(n(r,"user_id"),s(d.get("roles",r.get("role_id")),"code"))==null,"Duplicate role membership");
        Set<String> pairs=new HashSet<>();
        Map<Integer,Set<Integer>> registrations=new HashMap<>();
        for(var section:d.rows("course_sections")) registrations.put(n(section,"id"),new HashSet<>());
        for(var e:d.rows("student_enrollments")) {
            require(roles.get(n(e,"student_id")).equals("STUDENT"),"Enrollment requires student");
            require(pairs.add(e.get("course_section_id")+"/"+e.get("student_id")),"Duplicate enrollment");
            require(activeOn(d,n(e,"student_id"),s(e,"enrolled_at").substring(0,10)),"Inactive student enrolled");
            if(s(e,"status").equals("ACTIVE")) registrations.get(n(e,"course_section_id")).add(n(e,"student_id"));
        }
        for(var section:d.rows("course_sections")) {
            require(n(section,"student_count")==registrations.get(n(section,"id")).size(),"Student count differs from enrollments");
            require(n(section,"student_count")<=n(section,"capacity"),"Section capacity exceeded");
        }
        for(var assignment:d.rows("lecturer_assignments")) require(roles.get(n(assignment,"lecturer_id")).equals("LECTURER"),"Assignment requires lecturer");
        var published=d.rows("schedules").stream().filter(r->s(r,"status").equals("PUBLISHED")).toList();
        Map<String,Integer> rooms=new HashMap<>(),teachers=new HashMap<>(),students=new HashMap<>(),sections=new HashMap<>();
        Map<Integer,Long> sessions=new HashMap<>(),minutes=new HashMap<>(),slots=new HashMap<>();
        for(var schedule:d.rows("schedules")) {
            var section=d.get("course_sections",schedule.get("course_section_id")); var semester=d.get("semesters",section.get("semester_id"));
            var assignment=d.get("lecturer_assignments",schedule.get("lecturer_assignment_id"));
            require(assignment.get("course_section_id").equals(section.get("id")),"Assignment/section mismatch");
            require(s(schedule,"start_date").compareTo(s(semester,"start_date"))>=0 && s(schedule,"end_date").compareTo(s(semester,"end_date"))<=0,"Schedule outside semester");
            require(n(schedule,"start_slot_id")<=n(schedule,"end_slot_id"),"Invalid slots");
        }
        for(var schedule:published) {
            var section=d.get("course_sections",schedule.get("course_section_id")); var room=d.get("classrooms",schedule.get("classroom_id"));
            var course=d.get("courses",section.get("course_id")); var assignment=d.get("lecturer_assignments",schedule.get("lecturer_assignment_id"));
            require(n(room,"capacity")>=n(section,"student_count") && s(room,"room_type").equals(s(course,"required_room_type")),"Invalid room type/capacity");
            for(var date:FiveYearSimulation.dates(schedule)) {
                require(activeOn(d,n(assignment,"lecturer_id"),date.toString()),"Inactive lecturer scheduled");
                sessions.merge(date.getYear(),1L,Long::sum);
                for(var m:d.rows("maintenance_records")) if(m.get("classroom_id").equals(room.get("id"))) require(date.toString().compareTo(s(m,"start_date"))<0 || date.toString().compareTo(s(m,"end_date"))>0,"Teaching during maintenance");
                for(int slot=n(schedule,"start_slot_id");slot<=n(schedule,"end_slot_id");slot++) {
                    String key=date+"/"+slot+"/";
                    require(rooms.put(key+room.get("id"),n(schedule,"id"))==null,"Room collision");
                    require(teachers.put(key+assignment.get("lecturer_id"),n(schedule,"id"))==null,"Lecturer collision");
                    require(sections.put(key+section.get("id"),n(schedule,"id"))==null,"Section collision");
                    for(int uid:registrations.get(n(section,"id"))) {
                        require(activeOn(d,uid,date.toString()),"Inactive student scheduled");
                        require(students.put(key+uid,n(schedule,"id"))==null,"Student collision");
                    }
                    var time=d.get("time_slots",slot); long duration=Duration.between(LocalTime.parse(s(time,"start_time")),LocalTime.parse(s(time,"end_time"))).toMinutes();
                    minutes.merge(date.getYear(),duration,Long::sum); slots.merge(date.getYear(),1L,Long::sum);
                }
            }
        }
        Map<String,Integer> borrowed=new HashMap<>();
        for(var r:d.rows("change_requests")) {
            boolean pending=s(r,"status").equals("PENDING");
            require(pending==!r.containsKey("reviewed_by"),"Review state mismatch");
            if(!pending) {
                String expected=List.of("REPORT_DAMAGE","BORROW_EQUIPMENT").contains(s(r,"request_type"))?"ADMIN":"ACADEMIC";
                require(roles.get(n(r,"reviewed_by")).equals(expected),"Wrong reviewer role");
                require(s(r,"reviewed_at").compareTo(s(r,"created_at"))>=0,"Review predates request");
            }
            if(s(r,"status").equals("APPROVED") && s(r,"request_type").equals("BORROW_EQUIPMENT")) {
                var eq=d.rows("classroom_equipment").stream().filter(e->e.get("classroom_id").equals(r.get("requested_room_id"))&&e.get("equipment_id").equals(r.get("requested_equipment_id"))).findFirst().orElseThrow();
                for(int slot=n(r,"requested_start_slot_id");slot<=n(r,"requested_end_slot_id");slot++) {
                    String key=r.get("requested_date")+"/"+slot+"/"+eq.get("id");
                    int quantity=borrowed.merge(key,n(r,"quantity"),Integer::sum); require(quantity<=n(eq,"quantity"),"Equipment overbooked");
                }
            }
            if(s(r,"status").equals("APPROVED") && List.of("CHANGE_ROOM","CHANGE_SCHEDULE").contains(s(r,"request_type"))) {
                var schedule=d.get("schedules",r.get("schedule_id"));
                require(schedule.get("classroom_id").equals(r.get("requested_room_id")) && schedule.get("day_of_week").equals(r.get("requested_day")) && schedule.get("start_slot_id").equals(r.get("requested_start_slot_id")),"Approved change not applied");
            }
        }
        int previous=-1;
        for(var y:d.annual) {
            int year=n(y,"year"); require(n(y,"users_end")==n(y,"users_start")+n(y,"users_new")-n(y,"users_left"),"User conservation failed");
            if(previous>=0) require(previous==n(y,"users_start"),"Year continuity failed"); previous=n(y,"users_end");
            for(String event:List.of("NEW","LEFT")) require(d.userEvents.stream().filter(e->s(e,"date").startsWith(""+year)&&s(e,"event").equals(event)).count()==n(y,event.equals("NEW")?"users_new":"users_left"),"User events differ from annual totals");
            require(d.rows("users").stream().filter(u->activeOn(d,n(u,"id"),year+"-12-31")).count()==n(y,"users_end"),"Year-end users differ from lifecycle");
            require(sessions.getOrDefault(year,0L)==n(y,"sessions") && minutes.getOrDefault(year,0L)==n(y,"teaching_minutes") && slots.getOrDefault(year,0L)==n(y,"used_room_slots"),"Teaching metrics mismatch");
            require(n(y,"requests")==n(y,"requests_approved")+n(y,"requests_rejected")+n(y,"requests_pending"),"Request totals mismatch");
            for(String key:List.of("sessions","teaching_minutes","used_room_slots","available_room_slots","requests")) require(d.monthly.stream().filter(m->n(m,"year")==year).mapToLong(m->((Number)m.get(key)).longValue()).sum()==((Number)y.get(key)).longValue(),"Monthly totals differ: "+key);
            require(n(y,"used_room_slots")<=n(y,"available_room_slots"),"Utilization exceeds capacity");
        }
        for(var n:d.rows("notifications")) { d.get(s(n,"reference_type"),n.get("reference_id")); require((n(n,"is_read")==1)==n.containsKey("read_at"),"Read receipt mismatch"); }
        for(var a:d.rows("audit_logs")) d.get(s(a,"entity_type"),a.get("entity_id"));
    }
    static boolean activeOn(SimulationData d,int uid,String date) {
        boolean active=false;
        for(var e:d.userEvents) if(n(e,"user_id")==uid && s(e,"date").compareTo(date)<=0) active=!s(e,"event").equals("LEFT");
        return active;
    }
}
