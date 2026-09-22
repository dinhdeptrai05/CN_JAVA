package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.RoomAvailability;
import vn.edu.donga.unischedule.model.Enums.ConflictType;
import vn.edu.donga.unischedule.model.Enums.Priority;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.RequestType;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.config.ThemeConfig;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.frame.LoginFrame;
import vn.edu.donga.unischedule.ui.frame.MainFrame;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoSmokeTest {
    @Test
    void allDemoAccountsCanLogin() {
        AppServices services = vn.edu.donga.unischedule.service.TestFixtures.createDemo();

        assertEquals(Role.ADMIN, services.auth().login("admin", "123456").getRole());
        assertEquals(Role.ACADEMIC, services.auth().login("daotao", "123456").getRole());
        assertEquals(Role.LECTURER, services.auth().login("giangvien", "123456").getRole());
        assertEquals(Role.STUDENT, services.auth().login("sinhvien", "123456").getRole());
    }

    @Test
    void mockDataMeetsWeekOneMinimums() {
        AppServices services = vn.edu.donga.unischedule.service.TestFixtures.createDemo();

        assertTrue(services.users().findAll().size() >= 4);
        assertEquals(3, services.catalog().getDepartments().size());
        assertTrue(services.catalog().getSemesters().size() >= 2);
        assertTrue(services.catalog().getCourses().size() >= 8);
        assertTrue(services.courseSections().findAll().size() >= 10);
        assertTrue(services.catalog().getLecturers().size() >= 6);
        assertTrue(services.rooms().findAll().size() >= 12);
        assertTrue(services.rooms().findAllEquipment().size() >= 15);
        assertTrue(services.schedules().findAll().size() >= 20);
        assertTrue(services.requests().findForUser(services.auth().login("admin", "123456")).size() >= 2);
        assertTrue(services.requests().findForUser(services.auth().login("daotao", "123456")).stream()
                .anyMatch(request -> request.getType() == RequestType.USE_ROOM));
    }

    @Test
    void conflictsContainRequiredTypes() {
        AppServices services = vn.edu.donga.unischedule.service.TestFixtures.createDemo();
        Set<ConflictType> types = services.conflicts().findAllConflicts().stream()
                .map(conflict -> conflict.getType())
                .collect(Collectors.toSet());

        assertTrue(types.contains(ConflictType.ROOM));
        assertTrue(types.contains(ConflictType.LECTURER));
        assertTrue(types.contains(ConflictType.COURSE_SECTION));
    }

    @Test
    void roomSearchAndLecturerRequestWorkInMemory() {
        AppServices services = vn.edu.donga.unischedule.service.TestFixtures.createDemo();
        User lecturer = services.auth().login("giangvien", "123456");
        var source = services.schedules().findByWeekForUser(LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)), lecturer).get(0);
        int before = services.requests().findForUser(lecturer).size();

        assertFalse(services.rooms().searchAvailableRooms(LocalDate.now(), services.catalog().getTimeSlots().get(0),
                "Tất cả", null, 30, "").isEmpty());

        ChangeRequest request = new ChangeRequest(null, lecturer, RequestType.USE_ROOM, source,
                services.rooms().findAll().get(0), LocalDate.now().plusDays(1), services.catalog().getTimeSlots().get(0),
                "", 0, "Yêu cầu kiểm tra luồng gửi mượn phòng demo.", Priority.NORMAL,
                RequestStatus.PENDING, LocalDateTime.now());
        services.requests().create(request);

        assertEquals(before + 1, services.requests().findForUser(lecturer).size());
    }

    @Test
    void roomSearchShowsScheduledStudentCountAndUnavailableRooms() {
        AppServices services = vn.edu.donga.unischedule.service.TestFixtures.createDemo();
        LocalDate monday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate tuesday = monday.plusDays(1);
        var firstSlot = services.catalog().getTimeSlots().get(0);
        var results = services.rooms().searchRoomAvailability(tuesday, firstSlot, null, null, 0, "");
        var occupied = results.stream().filter(result -> result.room().getCode().equals("A101")).findFirst().orElseThrow();
        assertEquals(RoomAvailability.Status.OCCUPIED, occupied.status());
        assertEquals(35, occupied.registeredStudents());
        assertTrue(occupied.classes().contains("IT101"));
        var maintenance = results.stream().filter(result -> result.room().getCode().equals("B204")).findFirst().orElseThrow();
        assertEquals(RoomAvailability.Status.MAINTENANCE, maintenance.status());
        var free = results.stream().filter(result -> result.room().getCode().equals("A303")).findFirst().orElseThrow();
        assertEquals(RoomAvailability.Status.FREE, free.status());
        assertEquals(0, free.registeredStudents());
        assertFalse(services.rooms().searchAvailableRooms(tuesday, firstSlot, null, null, 0, "")
                .stream().anyMatch(room -> room.getCode().equals("A101")));
    }

    @Test
    void swingFramesConstructForAllDemoRoles() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThemeConfig.install();
            AppServices services = vn.edu.donga.unischedule.service.TestFixtures.createDemo();
            LoginFrame loginFrame = new LoginFrame(new vn.edu.donga.unischedule.controller.AppControllers(services));
            loginFrame.dispose();
            for (String username : new String[]{"admin", "daotao", "giangvien", "sinhvien"}) {
                MainFrame frame = new MainFrame(new vn.edu.donga.unischedule.controller.AppControllers(services), services.auth().login(username, "123456"));
                frame.dispose();
            }
        });
    }
}
