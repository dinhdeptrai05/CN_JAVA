package vn.edu.donga.unischedule.simulation;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

class HistoricalSeedTest {
    private SimulationData generate(long seed) throws Exception {
        return new HistoricalSeedGenerator(new SimulationBaseline(Path.of("simulation/baseline-2026-09-22.properties")),LocalDate.of(2026,9,23),seed).generate();
    }
    @Test void largeHistoryIsReproducibleAndSafeForTheExistingUi() throws Exception {
        var a=generate(20260923);var b=generate(20260923);
        assertEquals(SimulationExport.json(a.tables),SimulationExport.json(b.tables));
        assertTrue(a.rows("users").size()>500);assertTrue(a.rows("student_enrollments").size()>9000);
        assertEquals(11,a.rows("semesters").size());
        assertTrue(a.rows("course_sections").stream().anyMatch(r->s(r,"status").equals("CLOSED")));
        assertTrue(a.rows("course_sections").stream().anyMatch(r->s(r,"status").equals("SCHEDULED")));
        assertTrue(a.rows("users").stream().anyMatch(r->s(r,"status").equals("INACTIVE")));
        assertTrue(a.rows("change_requests").stream().anyMatch(r->s(r,"status").equals("PENDING")));
        assertTrue(a.rows("change_requests").stream().filter(r->s(r,"status").equals("PENDING")).allMatch(r->s(r,"created_at").startsWith("2026-09")));
        assertEquals(5,a.rows("change_requests").stream().map(r->r.get("request_type")).distinct().count());
        assertTrue(a.rows("users").stream().noneMatch(r->s(r,"username").contains("hist5")||s(r,"full_name").contains("Mẫu 5N")));
        assertTrue(a.rows("courses").stream().noneMatch(r->s(r,"code").startsWith("HS5-")||s(r,"name").contains("Mẫu 5N")));
        assertTrue(a.rows("notifications").stream().noneMatch(r->s(r,"title").contains("Mẫu 5N")||s(r,"content").contains("seed lịch sử")));
        assertEquals(a.rows("users").size(),a.rows("users").stream().map(r->r.get("username")).distinct().count());
        assertTrue(a.rows("users").stream().allMatch(r->r.containsKey("last_login_at")
                &&s(r,"last_login_at").compareTo(s(r,"created_at"))>=0
                &&s(r,"last_login_at").compareTo("2026-09-24")<0));
        assertTrue(a.rows("users").stream().map(r->s(r,"last_login_at").substring(0,4)).distinct().count()>=4);
        for(var user:a.rows("users"))if(s(user,"status").equals("INACTIVE")) {
            String left=a.userEvents.stream().filter(e->e.get("user_id").equals(user.get("id"))&&s(e,"event").equals("LEFT"))
                    .map(e->s(e,"date")).findFirst().orElseThrow();
            assertTrue(s(user,"last_login_at").substring(0,10).compareTo(left)<=0);
        }
        assertTrue(vn.edu.donga.unischedule.util.PasswordHasher.verify("123456",HistoricalSeedGenerator.DEMO_HASH));
        SimulationValidator.validate(a);
    }
    @Test void anotherSeedStillRespectsDatesAndRelationalConstraints() throws Exception {
        var d=generate(7);
        SimulationValidator.validate(d);
        for(var r:d.rows("notifications")) assertTrue(s(r,"created_at").compareTo("2026-09-24")<0);
        for(var r:d.rows("maintenance_records")) assertTrue(s(r,"end_date").compareTo("2026-09-23")<=0);
    }
}
