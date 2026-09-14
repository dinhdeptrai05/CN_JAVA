package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import vn.edu.donga.unischedule.controller.*;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.util.DateUtils;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {
    private AppControllers controllers() { return new AppControllers(vn.edu.donga.unischedule.service.TestFixtures.createDemo()); }

    @Test void invalidProfileAndPasswordDoNotMutateUser() {
        var controllers = controllers();
        User user = controllers.auth().login("daotao", "123456");
        String name = user.getFullName(), email = user.getEmail();
        assertThrows(ValidationException.class, () -> controllers.users().updateProfile(user, "Tên mới", "invalid-email", "123"));
        assertEquals(name, user.getFullName()); assertEquals(email, user.getEmail());
        assertThrows(ValidationException.class, () -> controllers.users().changePassword(user, "wrong", "654321", "654321"));
        assertThrows(ValidationException.class, () -> controllers.users().changePassword(user, "123456", "654321", "different"));
        assertEquals("123456", user.getPassword());
        controllers.users().changePassword(user, "123456", "654321", "654321");
        assertEquals(user.getId(), controllers.auth().login("daotao", "654321").getId());
    }

    @Test void invalidEquipmentQuantityDoesNotLeakIntoRepository() {
        var controllers = controllers();
        Equipment equipment = controllers.rooms().findAllEquipment().get(0);
        int before = equipment.getQuantity();
        assertThrows(ValidationException.class, () -> controllers.rooms().updateEquipmentQuantity(equipment, "-2"));
        assertEquals(before, equipment.getQuantity());
        assertThrows(NumberFormatException.class, () -> controllers.rooms().updateEquipmentQuantity(equipment, "abc"));
        assertEquals(before, equipment.getQuantity());
        controllers.rooms().updateEquipmentQuantity(equipment, "9");
        assertEquals(9, equipment.getQuantity());
        controllers.rooms().markEquipmentBroken(equipment);
        assertEquals(ResourceStatus.BROKEN, equipment.getStatus());
    }

    @Test void scheduleDraftCannotChangeSharedLecturerOnFailureOrCancel() {
        var controllers = controllers();
        CourseSection section = controllers.courseSections().findAll().get(0);
        Lecturer original = section.getLecturer();
        Lecturer other = controllers.catalog().getLecturers().stream().filter(l -> !l.getId().equals(original.getId())).findFirst().orElseThrow();
        Classroom room = controllers.rooms().findAll().get(0);
        var slots = controllers.catalog().getTimeSlots();
        assertThrows(ValidationException.class, () -> FormController.schedule(null, section, other, room, 2,
                slots.get(0), slots.get(1), "2026-09-07", "2026-12-14", ""));
        assertSame(original, section.getLecturer());
        assertThrows(java.time.format.DateTimeParseException.class, () -> FormController.schedule(null, section, original, room, 2,
                slots.get(0), slots.get(1), "bad date", "2026-12-14", ""));
        assertSame(original, section.getLecturer());
        int count = controllers.schedules().findAll().size();
        ScheduleEntry draft = FormController.schedule(null, section, original, room, 2, slots.get(0), slots.get(1),
                "2026-09-07", "2026-12-14", "Draft only");
        assertNull(draft.getId()); assertEquals(count, controllers.schedules().findAll().size());
        assertSame(original, section.getLecturer());
    }

    @Test void queriesRespectLecturerScopeAndSearchCriteria() {
        var controllers = controllers();
        User lecturer = controllers.auth().login("giangvien", "123456");
        var sections = controllers.courseSections().search(lecturer, "", null, null, null);
        assertFalse(sections.isEmpty());
        assertTrue(sections.stream().allMatch(section -> section.getLecturer().getId().equals(lecturer.getId())));
        assertTrue(controllers.courseSections().search(lecturer, "does-not-exist", null, null, null).isEmpty());
        var entries = controllers.schedules().search(DateUtils.currentWeekMonday(), lecturer, "", null, null, null, null);
        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().allMatch(entry -> entry.getCourseSection().getLecturer().getId().equals(lecturer.getId())));
        assertTrue(controllers.rooms().search("", "Tòa B", null, 50, null).stream()
                .allMatch(room -> room.getBuilding().equals("Tòa B") && room.getCapacity() >= 50));
    }

    @Test void requestControllerPreservesApprovalRules() {
        var controllers = controllers();
        User lecturer = controllers.auth().login("giangvien", "123456");
        var draft = FormController.request(lecturer, RequestType.USE_ROOM, null, controllers.rooms().findAll().get(0),
                LocalDate.now().plusDays(1).toString(), controllers.catalog().getTimeSlots().get(0), "", "0", "Mượn phòng cho buổi thảo luận", Priority.NORMAL);
        controllers.requests().create(draft);
        assertThrows(ValidationException.class, () -> controllers.requests().approve(draft, lecturer));
        assertEquals(RequestStatus.PENDING, draft.getStatus());
        controllers.requests().approve(draft, controllers.auth().login("admin", "123456"));
        assertEquals(RequestStatus.APPROVED, draft.getStatus());
    }

    @Test void userDraftPreservesLecturerMetadataWithoutMutatingOriginal() {
        var controllers = controllers();
        Lecturer lecturer = (Lecturer) controllers.auth().login("giangvien", "123456");
        String oldName = lecturer.getFullName();
        Lecturer draft = (Lecturer) controllers.users().prepareUser(lecturer, Role.LECTURER, lecturer.getUsername(),
                "Tên mới", lecturer.getEmail(), lecturer.getPhone(), UserStatus.ACTIVE);
        assertNotSame(lecturer, draft); assertEquals(oldName, lecturer.getFullName());
        assertEquals(lecturer.getLecturerCode(), draft.getLecturerCode());
        assertSame(lecturer.getDepartment(), draft.getDepartment());
        assertEquals(lecturer.getPassword(), draft.getPassword());
    }
}
