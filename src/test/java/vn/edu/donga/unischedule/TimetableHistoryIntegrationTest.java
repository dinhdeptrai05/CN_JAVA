package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.edu.donga.unischedule.config.ThemeConfig;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.model.TimetablePeriod;
import vn.edu.donga.unischedule.model.Semester;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.panel.ReportPanel;
import vn.edu.donga.unischedule.ui.panel.RoomManagementPanel;
import vn.edu.donga.unischedule.ui.panel.RoomSearchPanel;
import vn.edu.donga.unischedule.ui.frame.MainFrame;
import vn.edu.donga.unischedule.ui.panel.TimetablePanel;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, read-only timetable check against the seeded local database. */
class TimetableHistoryIntegrationTest {
    @Test void roomSearchOpensWithoutBlockingAndPaginatesCards() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("UNISCHEDULE_HISTORY_UI_TEST")));
        AppServices services = AppServices.createJdbc();
        AppControllers controllers = new AppControllers(services);
        var admin = services.users().findAll().stream().filter(user -> user.getUsername().equals("admin")).findFirst().orElseThrow();
        AtomicReference<RoomSearchPanel> panel = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panel.set(new RoomSearchPanel(controllers, admin)));
        JTable table = find(panel.get(), JTable.class);
        assertNotNull(table);
        for (int attempt = 0; attempt < 150 && table.getRowCount() < 900; attempt++) {
            Thread.sleep(100);
            SwingUtilities.invokeAndWait(() -> { });
        }
        assertTrue(table.getRowCount() >= 900, "The asynchronous room search should finish");
        var cards = find(panel.get(), vn.edu.donga.unischedule.ui.component.ResponsiveCardGrid.class);
        assertNotNull(cards);
        assertTrue(cards.getComponentCount() <= 48, "Only one page of room cards should be built");
    }

    @Test void switchingToRoomSearchDoesNotBlockOtherTabs() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("UNISCHEDULE_HISTORY_UI_TEST")));
        AppServices services = AppServices.createJdbc();
        AppControllers controllers = new AppControllers(services);
        var admin = services.auth().login("admin", "123456");
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame(controllers, admin);
            try {
                frame.addNotify();
                long start = System.nanoTime();
                frame.showScreen("roomSearch");
                frame.showScreen("rooms");
                frame.showScreen("roomSearch");
                assertTrue((System.nanoTime() - start) / 1_000_000 < 10_000,
                        "Switching tabs must not wait for the full room search");
            } finally { frame.dispose(); }
        });
    }

    @Test void roomManagementLoadsExpandedTimetable() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("UNISCHEDULE_HISTORY_UI_TEST")));
        AppServices services = AppServices.createJdbc();
        AppControllers controllers = new AppControllers(services);
        var admin = services.users().findAll().stream().filter(user -> user.getUsername().equals("admin")).findFirst().orElseThrow();
        assertEquals(998, controllers.rooms().countRoomsWithTimetable(LocalDate.of(2026, 9, 23)));
        var availability = controllers.rooms().searchRoomAvailability(LocalDate.of(2026, 9, 23),
                services.catalog().getTimeSlots().get(0), "Tất cả", null, 0, "");
        assertEquals(1000, availability.size());
        assertTrue(availability.stream().anyMatch(item -> item.status() == vn.edu.donga.unischedule.model.RoomAvailability.Status.OCCUPIED));
        SwingUtilities.invokeAndWait(() -> {
            RoomManagementPanel panel = new RoomManagementPanel(controllers, admin);
            JTable table = find(panel, JTable.class);
            assertNotNull(table);
            assertEquals(1000, table.getRowCount());
            assertNotNull(table.getValueAt(0, 6));
        });
    }

    @Test void semesterLabelsAndFiltersUseAcademicNumbers() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("UNISCHEDULE_HISTORY_UI_TEST")));
        AppServices services = AppServices.createJdbc();
        AppControllers controllers = new AppControllers(services);
        var semesters = services.catalog().getSemesters();
        assertTrue(semesters.stream().allMatch(semester -> semester.getName().matches("Học kỳ [12] - 20\\d{2}")));
        var first2026 = semesters.stream().filter(semester -> semester.getName().equals("Học kỳ 1 - 2026")).toList();
        assertEquals(2, first2026.size());
        assertNotEquals(first2026.get(0).toString(), first2026.get(1).toString());
        var admin = services.users().findAll().stream().filter(user -> user.getUsername().equals("admin")).findFirst().orElseThrow();
        for (Semester semester : first2026)
            assertFalse(controllers.courseSections().search(admin, "", semester.toString(), null, null).isEmpty());
        SwingUtilities.invokeAndWait(() -> {
            ReportPanel panel = new ReportPanel(controllers, admin);
            JComboBox<?> semesterBox = findBox(panel, Semester.class);
            assertNotNull(semesterBox);
            assertEquals(semesters.size() + 1, semesterBox.getItemCount());
            assertTrue(contains(semesterBox, first2026.get(0)));
            assertTrue(contains(semesterBox, first2026.get(1)));
        });
    }

    @Test void realSchedulesAndRoleSpecificCalendarRender() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("UNISCHEDULE_HISTORY_UI_TEST")));
        AppServices services = AppServices.createJdbc();
        AppControllers controllers = new AppControllers(services);
        var accounts = services.users().findAll();
        var admin = accounts.stream().filter(user -> user.getUsername().equals("admin")).findFirst().orElseThrow();
        var academic = accounts.stream().filter(user -> user.getUsername().equals("daotao")).findFirst().orElseThrow();
        var lecturer = accounts.stream().filter(user -> user.getUsername().equals("giangvien")).findFirst().orElseThrow();
        var student = accounts.stream().filter(user -> user.getUsername().equals("sinhvien")).findFirst().orElseThrow();
        LocalDate fall2022 = LocalDate.of(2022, 9, 12);
        LocalDate fall2024 = LocalDate.of(2024, 9, 9);
        assertFalse(services.schedules().findByWeekForUser(fall2022, admin).isEmpty());
        assertFalse(services.schedules().findByWeekForUser(fall2022, academic).isEmpty());
        assertTrue(services.schedules().findPublishedByWeekForUser(fall2022, lecturer).isEmpty());
        assertTrue(services.schedules().findPublishedByWeekForUser(fall2022, student).isEmpty());
        assertFalse(services.schedules().findPublishedByWeekForUser(fall2024, lecturer).isEmpty());
        assertFalse(services.schedules().findPublishedByWeekForUser(fall2024, student).isEmpty());
        var semesters = services.catalog().getSemesters();
        var staffPeriods = semesters.stream().filter(semester -> TimetablePeriod.visibleTo(semester, admin.getRole(), LocalDate.now()))
                .map(TimetablePeriod::of).distinct().toList();
        var studentPeriods = semesters.stream().filter(semester -> TimetablePeriod.visibleTo(semester, student.getRole(), LocalDate.now()))
                .map(TimetablePeriod::of).distinct().toList();
        assertEquals(11, staffPeriods.size()); // autumn 2021 + two terms in each following calendar year
        assertEquals(6, studentPeriods.size()); // spring and autumn, 2024–2026
        for (var period : staffPeriods) {
            LocalDate week = period.firstDay(semesters).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            assertFalse(services.schedules().findByWeekForUser(week, admin).stream()
                    .filter(entry -> period.includes(entry.getCourseSection().getSemester())).toList().isEmpty(), period.toString() + period.year());
        }
        for (var period : studentPeriods) {
            LocalDate week = period.firstDay(semesters).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            assertFalse(services.schedules().findPublishedByWeekForUser(week, student).stream()
                    .filter(entry -> period.includes(entry.getCourseSection().getSemester())).toList().isEmpty(), period.toString() + period.year());
        }

        Path output = Path.of("target", "timetable-ui-previews");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> ThemeConfig.install());
        renderRole(controllers, admin, 2022, output.resolve("admin-2022.png"), true);
        renderRole(controllers, academic, 2024, output.resolve("academic-2024.png"), true);
        renderRole(controllers, lecturer, 2024, output.resolve("lecturer-2024.png"), false);
        renderRole(controllers, student, 2024, output.resolve("student-2024.png"), false);
        renderRole(controllers, admin, 2026, output.resolve("admin-2026.png"), true);
        renderRole(controllers, student, 2026, output.resolve("student-2026-personal.png"), false);
    }

    private void renderRole(AppControllers controllers, vn.edu.donga.unischedule.model.User user,
                            int year, Path target, boolean staff) throws Exception {
        final BufferedImage image = new BufferedImage(1380, 820, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            TimetablePanel panel = new TimetablePanel(controllers, user);
            JComboBox<?> yearBox = findBox(panel, Integer.class);
            JComboBox<?> periodBox = findBox(panel, TimetablePeriod.class);
            assertNotNull(yearBox);
            assertNotNull(periodBox);
            assertEquals(staff, contains(yearBox, 2022));
            assertEquals(staff ? 6 : 3, yearBox.getItemCount());
            assertTrue(contains(yearBox, 2024));
            assertTrue(contains(yearBox, 2025));
            assertTrue(contains(yearBox, 2026));
            yearBox.setSelectedItem(year);
            periodBox.setSelectedItem(new TimetablePeriod(year, 2));
            JTable table = find(panel, JTable.class);
            assertNotNull(table);
            assertTrue(table.getRowCount() > 0, user.getUsername() + " should see a populated historical week");
            panel.setSize(1380, 820);
            layout(panel);
            var graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
        });
        ImageIO.write(image, "png", target.toFile());
    }

    private static boolean contains(JComboBox<?> box, Object value) {
        for (int index = 0; index < box.getItemCount(); index++)
            if (value.equals(box.getItemAt(index))) return true;
        return false;
    }

    private static JComboBox<?> findBox(Container root, Class<?> itemType) {
        for (Component child : root.getComponents()) {
            if (child instanceof JComboBox<?> box) {
                for (int index = 0; index < box.getItemCount(); index++)
                    if (itemType.isInstance(box.getItemAt(index))) return box;
            }
            if (child instanceof Container nested) {
                JComboBox<?> found = findBox(nested, itemType);
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked") private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return (T) child;
            if (child instanceof Container nested) {
                T found = find(nested, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) if (child instanceof Container nested) layout(nested);
    }
}
