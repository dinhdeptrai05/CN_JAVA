package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.repository.ReportRepository;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class ReportService {
    private final ReportRepository repository;
    public ReportService(ReportRepository repository) { this.repository=repository; }

    public Report generate(User user,Report.Filter filter) {
        if(user==null || (user.getRole()!=Role.ADMIN && user.getRole()!=Role.ACADEMIC)) throw new ValidationException("Chỉ Admin và Phòng đào tạo được xem báo cáo.");
        if(filter.from()==null || filter.to()==null || filter.to().isBefore(filter.from())) throw new ValidationException("Khoảng ngày không hợp lệ.");
        long days=ChronoUnit.DAYS.between(filter.from(),filter.to())+1;
        if(days>366) throw new ValidationException("Chọn khoảng báo cáo tối đa 366 ngày.");
        Report.Source source=repository.load(user);
        var sections=source.sections().stream().filter(s->matches(s,filter)).filter(s->!s.getSemester().getEndDate().isBefore(filter.from()) && !s.getSemester().getStartDate().isAfter(filter.to())).toList();
        var schedules=source.schedules().stream().filter(s->s.getStatus()==ScheduleStatus.PUBLISHED && matches(s.getCourseSection(),filter)).toList();
        Map<Long,Set<String>> occupied=new HashMap<>();
        Map<Long,Long> roomSessions=new HashMap<>(),sectionSessions=new HashMap<>();
        Map<Long,long[]> workloads=new LinkedHashMap<>();
        Map<Long,Lecturer> lecturers=new LinkedHashMap<>();
        Map<Long,Set<Long>> lecturerSections=new HashMap<>();
        for(var section:sections) if(section.getLecturer()!=null) {
            lecturers.put(section.getLecturer().getId(),section.getLecturer());workloads.putIfAbsent(section.getLecturer().getId(),new long[3]);
        }
        long sessions=0;
        for(var schedule:schedules) {
            var dates=occurrences(schedule,filter);
            if(dates.isEmpty()) continue;
            var slots=source.slots().stream().filter(t->t.getOrder()>=schedule.getStartSlot().getOrder() && t.getOrder()<=schedule.getEndSlot().getOrder()).toList();
            sessions+=dates.size();
            roomSessions.merge(schedule.getRoom().getId(),(long)dates.size(),Long::sum);
            sectionSessions.merge(schedule.getCourseSection().getId(),(long)dates.size(),Long::sum);
            var cells=occupied.computeIfAbsent(schedule.getRoom().getId(),key->new HashSet<>());
            for(var date:dates) for(var slot:slots) cells.add(date+"/"+slot.getId());
            var lecturer=schedule.getCourseSection().getLecturer();
            lecturers.put(lecturer.getId(),lecturer);
            var load=workloads.computeIfAbsent(lecturer.getId(),key->new long[3]);
            load[0]+=dates.size();load[1]+=(long)dates.size()*slots.size();
            load[2]+=(long)dates.size()*slots.stream().mapToLong(t->Duration.between(t.getStartTime(),t.getEndTime()).toMinutes()).sum();
            lecturerSections.computeIfAbsent(lecturer.getId(),key->new HashSet<>()).add(schedule.getCourseSection().getId());
        }
        List<List<Object>> roomRows=new ArrayList<>();
        for(var room:source.rooms()) {
            long used=occupied.getOrDefault(room.getId(),Set.of()).size(),possible=days*source.slots().size();
            roomRows.add(List.of(room.getCode(),room.getBuilding(),room.getCapacity(),room.getRoomStatus().getDisplayName(),roomSessions.getOrDefault(room.getId(),0L),used,possible,percent(used,possible)));
        }
        List<List<Object>> lecturerRows=new ArrayList<>();
        for(var lecturer:lecturers.values()) {
            var load=workloads.get(lecturer.getId());
            lecturerRows.add(List.of(lecturer.getLecturerCode()==null?lecturer.getUsername():lecturer.getLecturerCode(),lecturer.getFullName(),lecturerSections.getOrDefault(lecturer.getId(),Set.of()).size(),load[0],load[1],Math.round(load[2]/60.0*100)/100.0));
        }
        List<List<Object>> sectionRows=new ArrayList<>();
        long registrations=0;
        for(var section:sections) {
            registrations+=section.getStudentCount();
            sectionRows.add(List.of(section.getCode(),section.getCourse().getName(),section.getSemester().getName(),section.getStudentCount(),section.getCapacity(),percent(section.getStudentCount(),section.getCapacity()),sectionSessions.getOrDefault(section.getId(),0L),section.getStatus().getDisplayName()));
        }
        var requests=source.requests().stream().filter(r->!r.getCreatedAt().toLocalDate().isBefore(filter.from()) && !r.getCreatedAt().toLocalDate().isAfter(filter.to())).filter(r->requestMatches(r,filter)).toList();
        List<List<Object>> requestRows=new ArrayList<>();
        for(var type:RequestType.values()) {
            var group=requests.stream().filter(r->r.getType()==type).toList();
            long approved=group.stream().filter(r->r.getStatus()==RequestStatus.APPROVED).count(),rejected=group.stream().filter(r->r.getStatus()==RequestStatus.REJECTED).count();
            requestRows.add(List.of(type.getDisplayName(),group.size(),group.stream().filter(r->r.getStatus()==RequestStatus.PENDING).count(),approved,rejected,percent(approved+rejected,group.size())));
        }
        String scope="Học kỳ: "+(filter.semesterId()==null?"Tất cả":sections.stream().map(CourseSection::getSemester).filter(s->s.getId().equals(filter.semesterId())).map(Semester::getName).findFirst().orElse("#"+filter.semesterId()))
            +" • Khoa: "+(filter.departmentId()==null?"Tất cả":source.sections().stream().map(s->s.getCourse().getDepartment()).filter(d->d.getId().equals(filter.departmentId())).map(Department::getName).findFirst().orElse("#"+filter.departmentId()));
        return new Report(filter,LocalDateTime.now(ZoneOffset.UTC),scope,sessions,occupied.size(),registrations,requests.size(),List.of(
            new Report.Table("Sử dụng phòng","Chỉ lịch đã công bố. Ca đã dùng đếm duy nhất theo phòng/ngày/ca. Tỷ lệ = ca đã dùng / (số ngày × số ca); công suất lý thuyết gồm cả cuối tuần, chưa trừ bảo trì.",List.of("Phòng","Tòa nhà","Chỗ ngồi","Trạng thái hiện tại","Buổi học","Ca đã dùng","Ca lý thuyết","Sử dụng (%)"),roomRows),
            new Report.Table("Khối lượng giảng dạy","Đếm buổi thực tế trong khoảng ngày. Giờ dạy là tổng thời lượng từng ca, không tính giờ nghỉ giữa ca; lịch nháp/hủy không được tính.",List.of("Mã GV","Giảng viên","Lớp có lịch","Buổi dạy","Ca dạy","Giờ dạy"),lecturerRows),
            new Report.Table("Lớp học phần","Sĩ số, sức chứa và trạng thái là dữ liệu hiện tại của lớp thuộc học kỳ giao với khoảng ngày; số buổi tính trong khoảng đã chọn. Lượt đăng ký không phải số sinh viên duy nhất.",List.of("Mã lớp","Môn học","Học kỳ","Đăng ký","Sức chứa","Lấp đầy (%)","Buổi học","Trạng thái"),sectionRows),
            new Report.Table("Yêu cầu","Lọc theo ngày tạo (UTC), tổng hợp trạng thái hiện tại. Khi chọn khoa/học kỳ, chỉ tính yêu cầu liên kết với lịch của lớp tương ứng.",List.of("Loại yêu cầu","Tổng","Chờ duyệt","Đã duyệt","Từ chối","Đã xử lý (%)"),requestRows)));
    }
    private static boolean matches(CourseSection s,Report.Filter f) {
        return (f.semesterId()==null || f.semesterId().equals(s.getSemester().getId())) && (f.departmentId()==null || f.departmentId().equals(s.getCourse().getDepartment().getId()));
    }
    private static boolean requestMatches(ChangeRequest r,Report.Filter f) {
        return f.semesterId()==null && f.departmentId()==null || r.getScheduleEntry()!=null && matches(r.getScheduleEntry().getCourseSection(),f);
    }
    public static List<LocalDate> occurrences(ScheduleEntry s,Report.Filter f) {
        LocalDate from=s.getStartDate().isAfter(f.from())?s.getStartDate():f.from(),to=s.getEndDate().isBefore(f.to())?s.getEndDate():f.to();
        List<LocalDate> dates=new ArrayList<>();
        if(from.isAfter(to)) return dates;
        from=from.plusDays(Math.floorMod(s.getDayOfWeek()-1-from.getDayOfWeek().getValue(),7));
        for(var date=from;!date.isAfter(to);date=date.plusWeeks(1)) dates.add(date);
        return dates;
    }
    private static double percent(long count,long total) { return total==0?0:Math.round(count*10000.0/total)/100.0; }
}
