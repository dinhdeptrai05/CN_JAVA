package vn.edu.donga.unischedule.controller;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Converts form input into detached drafts; persistence happens only on Save. */
public final class FormController {
    private FormController() { }

    public static Classroom room(Classroom editing, String code, String name, String building, String floor,
                                 String capacity, RoomType type, RoomStatus status, String description) {
        return new Classroom(editing == null ? null : editing.getId(), code.trim(), name.trim(), building.trim(),
                Integer.parseInt(floor.trim()), Integer.parseInt(capacity.trim()), type, status, description.trim());
    }
    public static Equipment equipment(Equipment editing, String code, String name, String category, String quantity,
                                      String condition, Classroom room, ResourceStatus status) {
        Equipment result = new Equipment(editing == null ? null : editing.getId(), code.trim(), name.trim(), category.trim(),
                Integer.parseInt(quantity.trim()), condition.trim(), room, status);
        if(editing!=null) result.setPlacementId(editing.getPlacementId());return result;
    }
    public static CourseSection section(CourseSection editing, String code, Course course, Semester semester,
                                        Lecturer lecturer, String capacity, String studentCount, CourseSectionStatus status) {
        return new CourseSection(editing == null ? null : editing.getId(), code.trim(), course, semester, lecturer,
                Integer.parseInt(capacity.trim()), Integer.parseInt(studentCount.trim()), status);
    }
    public static ChangeRequest request(User user, RequestType type, ScheduleEntry schedule, Classroom room,
                                        String date, TimeSlot slot, String equipment, String quantity, String reason, Priority priority) {
        int count = quantity.isBlank() ? 0 : Integer.parseInt(quantity.trim());
        return new ChangeRequest(null, user, type, schedule, room, LocalDate.parse(date.trim()), slot,
                equipment.trim(), count, reason.trim(), priority, RequestStatus.PENDING, LocalDateTime.now());
    }
    public static ScheduleEntry schedule(ScheduleEntry editing, CourseSection section, Lecturer lecturer, Classroom room,
                                         int day, TimeSlot startSlot, TimeSlot endSlot, String startDate, String endDate, String note) {
        if (section != null && lecturer != null && !lecturer.getId().equals(section.getLecturer().getId()))
            throw new ValidationException("Vui lòng phân công giảng viên tại màn hình lớp học phần trước khi xếp lịch.");
        return new ScheduleEntry(editing == null ? null : editing.getId(), section, room, day, startSlot, endSlot,
                LocalDate.parse(startDate.trim()), LocalDate.parse(endDate.trim()),
                editing == null ? ScheduleStatus.PUBLISHED : editing.getStatus(), note.trim());
    }
}
