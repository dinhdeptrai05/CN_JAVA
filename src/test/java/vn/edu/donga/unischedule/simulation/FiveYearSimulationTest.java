package vn.edu.donga.unischedule.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

class FiveYearSimulationTest {
    private SimulationData run(long seed) throws Exception {
        return new FiveYearSimulation(new SimulationBaseline(Path.of("simulation/baseline-2026-09-22.properties")),2027,seed).run();
    }
    @Test void reproducibleAndDifferentSeedsChangeActivity() throws Exception {
        var first=run(42); var second=run(42);
        assertEquals(SimulationExport.json(first.tables),SimulationExport.json(second.tables));
        assertEquals(first.annual,second.annual); assertEquals(first.monthly,second.monthly);
        assertNotEquals(SimulationExport.json(first.tables),SimulationExport.json(run(43).tables));
    }
    @Test void reconcilesAcrossSeedsAndLeapYears() throws Exception {
        for(long seed:new long[]{0,1,7,42,2027}) {
            var data=run(seed); SimulationValidator.validate(data);
            assertEquals(5,data.annual.size()); assertEquals(60,data.monthly.size());
            assertEquals(18,data.tables.size()); data.tables.forEach((table,rows)->assertFalse(rows.isEmpty(),table));
            assertTrue(data.rows("schedules").stream().anyMatch(s->s(s,"status").equals("CANCELLED")));
            assertEquals(5,data.rows("change_requests").stream().map(r->r.get("request_type")).distinct().count());
            assertTrue(data.monthly.stream().filter(m->n(m,"month")==7).allMatch(m->n(m,"sessions")==0));
        }
    }
    @Test void detectsCorruptAccountingEnrollmentAndSchedule() throws Exception {
        var accounting=run(42); accounting.annual.get(0).put("users_end",999);
        assertThrows(IllegalStateException.class,()->SimulationValidator.validate(accounting));
        var enrollment=run(42); enrollment.rows("course_sections").get(0).put("student_count",999);
        assertThrows(IllegalStateException.class,()->SimulationValidator.validate(enrollment));
        var collision=run(42); var copy=new java.util.LinkedHashMap<>(collision.rows("schedules").stream().filter(s->s(s,"status").equals("PUBLISHED")).findFirst().orElseThrow());
        copy.put("id",99999); collision.rows("schedules").add(copy);
        assertThrows(IllegalStateException.class,()->SimulationValidator.validate(collision));
        var brokenFk=run(42); brokenFk.rows("student_enrollments").get(0).put("student_id",99999);
        assertThrows(RuntimeException.class,()->SimulationValidator.validate(brokenFk));
    }
    @Test void exportIsDeterministicAndNeverOverwrites(@TempDir Path temp) throws Exception {
        var baseline=Path.of("simulation/baseline-2026-09-22.properties"); Path a=temp.resolve("a"),b=temp.resolve("b");
        SimulationExport.write(run(42),baseline,a,2027,42); SimulationExport.write(run(42),baseline,b,2027,42);
        try(var files=Files.list(a)) { for(var file:files.toList()) assertArrayEquals(Files.readAllBytes(file),Files.readAllBytes(b.resolve(file.getFileName()))); }
        byte[] previous=Files.readAllBytes(a.resolve("data.json"));
        assertThrows(FileAlreadyExistsException.class,()->SimulationExport.write(run(42),baseline,a,2027,42));
        assertArrayEquals(previous,Files.readAllBytes(a.resolve("data.json")));
        assertFalse(Files.readString(a.resolve("data.json")).contains("pbkdf2"));
        assertFalse(Files.readString(a.resolve("data.sql")).contains("USE unischedule"));
    }
}
