package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.service.ReportService;
import vn.edu.donga.unischedule.util.ReportExport;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    @TempDir Path directory;
    private final User admin=new Administrator(1L,"admin","","Admin","a@example.com","");
    private final Report.Filter filter=new Report.Filter(LocalDate.of(2026,9,7),LocalDate.of(2026,9,21),null,null);
    private Report.Source source() {
        var department=new Department(1L,"IT","CNTT");
        var lecturer=new Lecturer(3L,"lecturer","","Giảng viên","l@example.com","",department,"GV1");
        var course=new Course(1L,"IT1","<script>alert(1)</script>",3,department,RoomType.THEORY);
        var semester=new Semester(1L,"HK1",LocalDate.of(2026,9,1),LocalDate.of(2026,12,1),"ACTIVE");
        var section=new CourseSection(1L,"=SUM(1,2)",course,semester,lecturer,40,10,CourseSectionStatus.SCHEDULED);
        var room=new Classroom(1L,"A101","A101","A",1,40,RoomType.THEORY,RoomStatus.AVAILABLE,"");
        var first=new TimeSlot(1L,"Ca 1",LocalTime.of(7,0),LocalTime.of(8,40),1);
        var second=new TimeSlot(2L,"Ca 2",LocalTime.of(8,50),LocalTime.of(10,30),2);
        List<ScheduleEntry> schedules=new ArrayList<>();
        for(int i=1;i<=3;i++)schedules.add(new ScheduleEntry((long)i,section,room,2,first,second,semester.getStartDate(),semester.getEndDate(),i==3?ScheduleStatus.CANCELLED:ScheduleStatus.PUBLISHED,""));
        return new Report.Source(schedules,List.of(room),List.of(first,second),List.of(section),List.of());
    }
    @Test void countsActualOccurrencesAndDeduplicatesRoomSlots() {
        var result=new ReportService(u->source()).generate(admin,filter);
        assertEquals(6,result.sessions());assertEquals(1,result.usedRooms());assertEquals(10,result.registrations());
        var room=result.tables().get(0).rows().get(0);assertEquals(6L,room.get(5));assertEquals(30L,room.get(6));assertEquals(20.0,room.get(7));
        var lecturer=result.tables().get(1).rows().get(0);assertEquals(6L,lecturer.get(3));assertEquals(12L,lecturer.get(4));assertEquals(20.0,lecturer.get(5));
    }
    @Test void dateBoundsEmptyScopeAndInvalidFilters() {
        var service=new ReportService(u->source());
        assertEquals(0,service.generate(admin,new Report.Filter(LocalDate.of(2026,9,8),LocalDate.of(2026,9,8),null,null)).sessions());
        assertEquals(2,service.generate(admin,new Report.Filter(filter.from(),filter.from(),null,null)).sessions());
        assertTrue(service.generate(admin,new Report.Filter(filter.from(),filter.to(),null,99L)).tables().get(2).rows().isEmpty());
        assertThrows(ValidationException.class,()->service.generate(admin,new Report.Filter(filter.to(),filter.from(),null,null)));
        assertThrows(ValidationException.class,()->service.generate(admin,new Report.Filter(filter.from(),filter.from().plusDays(366),null,null)));
        var empty=new ReportService(u->new Report.Source(List.of(),List.of(),List.of(),List.of(),List.of())).generate(admin,filter);
        assertEquals(0,empty.sessions());assertEquals(0,empty.tables().get(0).rows().size());
    }
    @Test void studentsCannotLoadReports() {
        var service=new ReportService(u->{throw new AssertionError("Must authorize before reading");});
        assertThrows(ValidationException.class,()->service.generate(new Student(4L,"s","","S","s@example.com","","SV1","IT1"),filter));
    }
    @Test void requestDatesStatusesAndLinkedScopeAreRespected() {
        var base=source();var schedule=base.schedules().get(0);
        List<ChangeRequest> requests=new ArrayList<>();
        for(int i=0;i<4;i++)requests.add(new ChangeRequest((long)i+1,admin,RequestType.CHANGE_ROOM,i==2?null:schedule,base.rooms().get(0),filter.from(),base.slots().get(0),"",0,"Lý do yêu cầu",Priority.NORMAL,
            i==0?RequestStatus.PENDING:i==1?RequestStatus.APPROVED:RequestStatus.REJECTED,(i==3?filter.from().minusDays(1):filter.from()).atStartOfDay()));
        var service=new ReportService(u->new Report.Source(base.schedules(),base.rooms(),base.slots(),base.sections(),requests));
        var report=service.generate(admin,filter);assertEquals(3,report.requests());
        var row=report.tables().get(3).rows().stream().filter(r->r.get(0).equals(RequestType.CHANGE_ROOM.getDisplayName())).findFirst().orElseThrow();
        assertEquals(List.of("Đổi phòng",3,1L,1L,1L,66.67),row);
        assertEquals(2,service.generate(admin,new Report.Filter(filter.from(),filter.to(),1L,null)).requests());
    }
    @Test void exportsUnicodeAndEscapesUntrustedCells() throws Exception {
        var result=new ReportService(u->source()).generate(admin,filter);
        Path csv=directory.resolve("report.csv"),html=directory.resolve("report.html");
        ReportExport.csv(result,2,csv);ReportExport.html(result,html);
        String text=Files.readString(csv);assertTrue(text.startsWith("\uFEFF"));assertTrue(text.contains("\"'=SUM(1,2)\""));assertTrue(text.contains("Môn học"));
        String page=Files.readString(html);assertFalse(page.contains("<script>"));assertTrue(page.contains("&lt;script&gt;"));assertTrue(page.contains("window.print()"));
        assertEquals("\"a,\"\"b\"\"\"",ReportExport.csvCell("a,\"b\""));
    }
}
