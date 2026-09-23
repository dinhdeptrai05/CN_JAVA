package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import vn.edu.donga.unischedule.model.Semester;
import vn.edu.donga.unischedule.model.TimetablePeriod;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.simulation.HistoricalDisplayNames;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimetablePeriodTest {
    @Test void groupsOverlappingCatalogSemestersAndLimitsYearsByRole() {
        assertEquals("Học kỳ 1 - 2026", HistoricalDisplayNames.semesterName(2026, 2));
        assertEquals("Học kỳ 2 - 2025", HistoricalDisplayNames.semesterName(2026, 1));
        LocalDate today = LocalDate.of(2026, 9, 23);
        Semester spring = new Semester(12L, "Học kỳ 2 - 2025", LocalDate.of(2026, 1, 12), LocalDate.of(2026, 5, 3), "COMPLETED");
        Semester originalFall = new Semester(1L, "Học kỳ 1 - 2026", LocalDate.of(2026, 8, 28), LocalDate.of(2027, 1, 1), "ACTIVE");
        Semester seededFall = new Semester(13L, "Học kỳ 1 - 2026", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 12, 27), "ACTIVE");
        Semester old = new Semester(3L, "Học kỳ 1 - 2021", LocalDate.of(2021, 10, 4), LocalDate.of(2021, 12, 31), "COMPLETED");
        Semester next = new Semester(2L, "Học kỳ 2 - 2026", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 5, 21), "PLANNED");
        assertEquals(2, List.of(spring, originalFall, seededFall).stream().map(TimetablePeriod::of).distinct().count());
        TimetablePeriod fall = TimetablePeriod.of(originalFall);
        assertTrue(fall.includes(seededFall));
        assertEquals("Học kỳ 1", fall.toString());
        assertEquals("Học kỳ 2", TimetablePeriod.of(spring).toString());
        assertNotEquals(originalFall.toString(), seededFall.toString());
        assertEquals(originalFall.getStartDate(), fall.firstDay(List.of(spring, originalFall, seededFall)));
        assertEquals(originalFall.getEndDate(), fall.lastDay(List.of(spring, originalFall, seededFall)));
        assertTrue(TimetablePeriod.visibleTo(old, Role.ADMIN, today));
        assertTrue(TimetablePeriod.visibleTo(old, Role.ACADEMIC, today));
        assertFalse(TimetablePeriod.visibleTo(old, Role.LECTURER, today));
        assertFalse(TimetablePeriod.visibleTo(old, Role.STUDENT, today));
        assertTrue(TimetablePeriod.visibleTo(spring, Role.STUDENT, today));
        assertFalse(TimetablePeriod.visibleTo(next, Role.ADMIN, today));
    }
}
