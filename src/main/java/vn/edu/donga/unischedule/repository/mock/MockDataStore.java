package vn.edu.donga.unischedule.repository.mock;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class MockDataStore {
    private long userSeq = 1;
    private long roomSeq = 1;
    private long equipmentSeq = 1;
    private long courseSectionSeq = 1;
    private long scheduleSeq = 1;
    private long requestSeq = 1;
    private long notificationSeq = 1;

    private final List<User> users = new ArrayList<>();
    private final List<Department> departments = new ArrayList<>();
    private final List<Semester> semesters = new ArrayList<>();
    private final List<TimeSlot> timeSlots = new ArrayList<>();
    private final List<Course> courses = new ArrayList<>();
    private final List<CourseSection> courseSections = new ArrayList<>();
    private final List<Classroom> rooms = new ArrayList<>();
    private final List<Equipment> equipment = new ArrayList<>();
    private final List<ScheduleEntry> schedules = new ArrayList<>();
    private final List<ChangeRequest> requests = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();

    public MockDataStore() {
        seed();
    }

    public synchronized long nextUserId() {
        return userSeq++;
    }

    public synchronized long nextRoomId() {
        return roomSeq++;
    }

    public synchronized long nextEquipmentId() {
        return equipmentSeq++;
    }

    public synchronized long nextCourseSectionId() {
        return courseSectionSeq++;
    }

    public synchronized long nextScheduleId() {
        return scheduleSeq++;
    }

    public synchronized long nextRequestId() {
        return requestSeq++;
    }

    public synchronized long nextNotificationId() {
        return notificationSeq++;
    }

    public List<User> users() {
        return users;
    }

    public List<Department> departments() {
        return departments;
    }

    public List<Semester> semesters() {
        return semesters;
    }

    public List<TimeSlot> timeSlots() {
        return timeSlots;
    }

    public List<Course> courses() {
        return courses;
    }

    public List<CourseSection> courseSections() {
        return courseSections;
    }

    public List<Classroom> rooms() {
        return rooms;
    }

    public List<Equipment> equipment() {
        return equipment;
    }

    public List<ScheduleEntry> schedules() {
        return schedules;
    }

    public List<ChangeRequest> requests() {
        return requests;
    }

    public List<Notification> notifications() {
        return notifications;
    }

    private void seed() {
        departments.add(new Department(1L, "CNTT", "Công nghệ thông tin"));
        departments.add(new Department(2L, "KT", "Kinh tế"));
        departments.add(new Department(3L, "NN", "Ngôn ngữ"));

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        semesters.add(new Semester(1L, "Học kỳ 1 - 2026", monday.minusWeeks(2), monday.plusWeeks(16), "Đang học"));
        semesters.add(new Semester(2L, "Học kỳ 2 - 2026", monday.plusWeeks(18), monday.plusWeeks(36), "Dự kiến"));

        timeSlots.add(new TimeSlot(1L, "Ca 1", LocalTime.of(7, 0), LocalTime.of(8, 40), 1));
        timeSlots.add(new TimeSlot(2L, "Ca 2", LocalTime.of(8, 50), LocalTime.of(10, 30), 2));
        timeSlots.add(new TimeSlot(3L, "Ca 3", LocalTime.of(10, 40), LocalTime.of(12, 20), 3));
        timeSlots.add(new TimeSlot(4L, "Ca 4", LocalTime.of(13, 0), LocalTime.of(14, 40), 4));
        timeSlots.add(new TimeSlot(5L, "Ca 5", LocalTime.of(14, 50), LocalTime.of(16, 30), 5));
        timeSlots.add(new TimeSlot(6L, "Ca 6", LocalTime.of(18, 0), LocalTime.of(20, 30), 6));

        Administrator admin = new Administrator(nextUserId(), "admin", "123456", "Nguyễn Quản Trị",
                "admin@donga.edu.vn", "0901000001");
        AcademicStaff academic = new AcademicStaff(nextUserId(), "daotao", "123456", "Trần Thu Hà",
                "daotao@donga.edu.vn", "0901000002");
        Lecturer lecturerDemo = new Lecturer(nextUserId(), "giangvien", "123456", "Phạm Anh Tuấn",
                "tuan.pa@donga.edu.vn", "0901000003", departments.get(0), "GV001");
        Student student = new Student(nextUserId(), "sinhvien", "123456", "Nguyễn Hoàng Nam",
                "nam.nh@sv.donga.edu.vn", "0901000004", "SV220101", "CNTT22A");
        users.add(admin);
        users.add(academic);
        users.add(lecturerDemo);
        users.add(student);

        Lecturer lan = new Lecturer(nextUserId(), "lannt", "123456", "Nguyễn Thị Lan",
                "lan.nt@donga.edu.vn", "0902000001", departments.get(0), "GV002");
        Lecturer minh = new Lecturer(nextUserId(), "minhld", "123456", "Lê Đức Minh",
                "minh.ld@donga.edu.vn", "0902000002", departments.get(1), "GV003");
        Lecturer hoa = new Lecturer(nextUserId(), "hoapt", "123456", "Phan Thị Hoa",
                "hoa.pt@donga.edu.vn", "0902000003", departments.get(2), "GV004");
        Lecturer son = new Lecturer(nextUserId(), "sonvq", "123456", "Võ Quốc Sơn",
                "son.vq@donga.edu.vn", "0902000004", departments.get(0), "GV005");
        Lecturer thao = new Lecturer(nextUserId(), "thaodm", "123456", "Đặng Minh Thảo",
                "thao.dm@donga.edu.vn", "0902000005", departments.get(1), "GV006");
        users.add(lan);
        users.add(minh);
        users.add(hoa);
        users.add(son);
        users.add(thao);

        courses.add(new Course(1L, "IT101", "Nhập môn lập trình", 3, departments.get(0), RoomType.COMPUTER));
        courses.add(new Course(2L, "IT205", "Cấu trúc dữ liệu", 3, departments.get(0), RoomType.COMPUTER));
        courses.add(new Course(3L, "IT310", "Cơ sở dữ liệu", 3, departments.get(0), RoomType.COMPUTER));
        courses.add(new Course(4L, "IT420", "Phân tích thiết kế hệ thống", 3, departments.get(0), RoomType.THEORY));
        courses.add(new Course(5L, "ECO101", "Kinh tế vi mô", 3, departments.get(1), RoomType.THEORY));
        courses.add(new Course(6L, "MKT220", "Marketing căn bản", 3, departments.get(1), RoomType.THEORY));
        courses.add(new Course(7L, "ENG201", "Tiếng Anh học thuật", 2, departments.get(2), RoomType.PRACTICE));
        courses.add(new Course(8L, "JPN101", "Nhập môn tiếng Nhật", 2, departments.get(2), RoomType.PRACTICE));

        Lecturer[] lecturers = {lecturerDemo, lan, minh, hoa, son, thao};
        for (int i = 0; i < 10; i++) {
            Course course = courses.get(i % courses.size());
            courseSections.add(new CourseSection(nextCourseSectionId(),
                    course.getCode() + "-0" + (i + 1),
                    course,
                    semesters.get(0),
                    lecturers[i % lecturers.length],
                    45 + (i % 4) * 10,
                    35 + (i % 5) * 8,
                    i % 4 == 0 ? CourseSectionStatus.CONFLICTED : CourseSectionStatus.SCHEDULED));
        }

        rooms.add(new Classroom(nextRoomId(), "A101", "Phòng học A101", "Tòa A", 1, 60, RoomType.THEORY, RoomStatus.IN_USE, "Phòng lý thuyết tiêu chuẩn"));
        rooms.add(new Classroom(nextRoomId(), "A202", "Phòng máy A202", "Tòa A", 2, 45, RoomType.COMPUTER, RoomStatus.IN_USE, "Máy tính cấu hình học lập trình"));
        rooms.add(new Classroom(nextRoomId(), "A303", "Phòng seminar A303", "Tòa A", 3, 35, RoomType.THEORY, RoomStatus.AVAILABLE, "Phù hợp seminar nhóm"));
        rooms.add(new Classroom(nextRoomId(), "B101", "Phòng học B101", "Tòa B", 1, 80, RoomType.THEORY, RoomStatus.AVAILABLE, "Phòng lớn"));
        rooms.add(new Classroom(nextRoomId(), "B204", "Phòng lab B204", "Tòa B", 2, 40, RoomType.LABORATORY, RoomStatus.MAINTENANCE, "Đang kiểm tra hệ thống điện"));
        rooms.add(new Classroom(nextRoomId(), "B305", "Phòng thực hành B305", "Tòa B", 3, 50, RoomType.PRACTICE, RoomStatus.AVAILABLE, "Có loa và micro"));
        rooms.add(new Classroom(nextRoomId(), "C101", "Phòng máy C101", "Tòa C", 1, 55, RoomType.COMPUTER, RoomStatus.AVAILABLE, "Phòng máy mới"));
        rooms.add(new Classroom(nextRoomId(), "C203", "Phòng học C203", "Tòa C", 2, 65, RoomType.THEORY, RoomStatus.IN_USE, "Có bảng thông minh"));
        rooms.add(new Classroom(nextRoomId(), "C304", "Phòng ngoại ngữ C304", "Tòa C", 3, 32, RoomType.PRACTICE, RoomStatus.AVAILABLE, "Có tai nghe"));
        rooms.add(new Classroom(nextRoomId(), "D102", "Hội trường D102", "Tòa D", 1, 120, RoomType.THEORY, RoomStatus.AVAILABLE, "Dùng cho lớp đông"));
        rooms.add(new Classroom(nextRoomId(), "D205", "Phòng lab D205", "Tòa D", 2, 36, RoomType.LABORATORY, RoomStatus.AVAILABLE, "Thực hành phần cứng"));
        rooms.add(new Classroom(nextRoomId(), "D306", "Phòng học D306", "Tòa D", 3, 45, RoomType.THEORY, RoomStatus.INACTIVE, "Tạm ngừng sử dụng"));

        addEquipment("EQ001", "Máy chiếu", "Trình chiếu", 1, "Tốt", rooms.get(0), ResourceStatus.ACTIVE);
        addEquipment("EQ002", "Điều hòa", "Tiện nghi", 2, "Tốt", rooms.get(0), ResourceStatus.ACTIVE);
        addEquipment("EQ003", "Máy tính giảng viên", "Máy tính", 1, "Tốt", rooms.get(1), ResourceStatus.ACTIVE);
        addEquipment("EQ004", "Máy tính sinh viên", "Máy tính", 40, "Tốt", rooms.get(1), ResourceStatus.ACTIVE);
        addEquipment("EQ005", "Loa", "Âm thanh", 2, "Tốt", rooms.get(5), ResourceStatus.ACTIVE);
        addEquipment("EQ006", "Micro không dây", "Âm thanh", 2, "Tốt", rooms.get(5), ResourceStatus.ACTIVE);
        addEquipment("EQ007", "Bảng thông minh", "Trình chiếu", 1, "Tốt", rooms.get(7), ResourceStatus.ACTIVE);
        addEquipment("EQ008", "Tai nghe ngoại ngữ", "Ngoại ngữ", 32, "Tốt", rooms.get(8), ResourceStatus.ACTIVE);
        addEquipment("EQ009", "Bộ thí nghiệm điện", "Thí nghiệm", 12, "Đang kiểm tra", rooms.get(4), ResourceStatus.MAINTENANCE);
        addEquipment("EQ010", "Camera lớp học", "Ghi hình", 1, "Tốt", rooms.get(9), ResourceStatus.ACTIVE);
        addEquipment("EQ011", "Bút trình chiếu", "Trình chiếu", 1, "Tốt", rooms.get(2), ResourceStatus.ACTIVE);
        addEquipment("EQ012", "Bàn thực hành", "Thực hành", 20, "Tốt", rooms.get(10), ResourceStatus.ACTIVE);
        addEquipment("EQ013", "Máy chiếu dự phòng", "Trình chiếu", 1, "Bóng đèn yếu", rooms.get(3), ResourceStatus.BROKEN);
        addEquipment("EQ014", "Router phòng lab", "Mạng", 4, "Tốt", rooms.get(6), ResourceStatus.ACTIVE);
        addEquipment("EQ015", "Bảng trắng", "Dạy học", 1, "Tốt", rooms.get(11), ResourceStatus.ACTIVE);

        LocalDate start = monday.minusWeeks(1);
        LocalDate end = monday.plusWeeks(14);
        addSchedule(courseSections.get(0), rooms.get(0), 3, 1, 2, start, end, "Lịch tạo xung đột phòng");
        addSchedule(courseSections.get(1), rooms.get(0), 3, 2, 3, start, end, "Trùng A101 với lớp IT101");
        addSchedule(courseSections.get(2), rooms.get(1), 4, 3, 4, start, end, "Lịch tạo xung đột giảng viên");
        addSchedule(courseSections.get(3), rooms.get(2), 4, 3, 4, start, end, "Cùng giảng viên với lịch CSDL");
        courseSections.get(3).setLecturer(courseSections.get(2).getLecturer());
        addSchedule(courseSections.get(4), rooms.get(3), 5, 1, 2, start, end, "Lịch tạo xung đột lớp");
        addSchedule(courseSections.get(4), rooms.get(5), 5, 2, 3, start, end, "Cùng lớp học phần với lịch trước");
        addSchedule(courseSections.get(5), rooms.get(6), 2, 1, 2, start, end, "Thực hành phòng máy");
        addSchedule(courseSections.get(6), rooms.get(8), 2, 4, 5, start, end, "Thực hành ngoại ngữ");
        addSchedule(courseSections.get(7), rooms.get(5), 6, 1, 2, start, end, "Lớp kỹ năng giao tiếp");
        addSchedule(courseSections.get(8), rooms.get(7), 6, 3, 4, start, end, "Lớp phân tích hệ thống");
        addSchedule(courseSections.get(9), rooms.get(9), 3, 4, 5, start, end, "Lớp đông sinh viên");
        addSchedule(courseSections.get(0), rooms.get(6), 2, 5, 6, start, end, "Bổ sung thực hành");
        addSchedule(courseSections.get(1), rooms.get(10), 4, 1, 2, start, end, "Thí nghiệm phần cứng");
        addSchedule(courseSections.get(2), rooms.get(7), 5, 4, 5, start, end, "Lý thuyết CSDL");
        addSchedule(courseSections.get(3), rooms.get(3), 6, 5, 6, start, end, "Thảo luận nhóm");
        addSchedule(courseSections.get(4), rooms.get(9), 2, 3, 4, start, end, "Kinh tế ứng dụng");
        addSchedule(courseSections.get(5), rooms.get(0), 4, 5, 6, start, end, "Marketing dự án");
        addSchedule(courseSections.get(6), rooms.get(8), 5, 5, 6, start, end, "Luyện nghe nói");
        addSchedule(courseSections.get(7), rooms.get(2), 3, 5, 6, start, end, "Tiếng Nhật căn bản");
        addSchedule(courseSections.get(8), rooms.get(3), 2, 1, 2, start, end, "Lịch sáng thứ hai");

        requests.add(new ChangeRequest(nextRequestId(), lecturerDemo, RequestType.CHANGE_ROOM, schedules.get(0),
                rooms.get(3), monday.plusDays(2), timeSlots.get(1), "", 0,
                "Phòng hiện tại cần máy chiếu ổn định cho buổi demo nhóm.", Priority.HIGH, RequestStatus.PENDING, LocalDateTime.now().minusHours(3)));
        requests.add(new ChangeRequest(nextRequestId(), lecturerDemo, RequestType.BORROW_EQUIPMENT, schedules.get(1),
                rooms.get(0), monday.plusDays(2), timeSlots.get(1), "Micro không dây", 2,
                "Cần micro để sinh viên thuyết trình trong phòng lớn.", Priority.NORMAL, RequestStatus.PENDING, LocalDateTime.now().minusHours(8)));
        requests.add(new ChangeRequest(nextRequestId(), academic, RequestType.CHANGE_SCHEDULE, schedules.get(2),
                rooms.get(6), monday.plusDays(3), timeSlots.get(4), "", 0,
                "Điều chỉnh lịch để tránh trùng lịch hội đồng khoa.", Priority.URGENT, RequestStatus.APPROVED, LocalDateTime.now().minusDays(1)));
        requests.add(new ChangeRequest(nextRequestId(), lecturerDemo, RequestType.REPORT_DAMAGE, schedules.get(0),
                rooms.get(0), monday.plusDays(1), timeSlots.get(0), "Máy chiếu", 1,
                "Máy chiếu bị nhấp nháy liên tục trong giờ học.", Priority.HIGH, RequestStatus.PENDING, LocalDateTime.now().minusDays(2)));
        requests.add(new ChangeRequest(nextRequestId(), lan, RequestType.CHANGE_SCHEDULE, schedules.get(5),
                rooms.get(2), monday.plusDays(4), timeSlots.get(4), "", 0,
                "Sinh viên tham gia khảo sát doanh nghiệp nên cần đổi ca học.", Priority.NORMAL, RequestStatus.REJECTED, LocalDateTime.now().minusDays(3)));
        requests.add(new ChangeRequest(nextRequestId(), son, RequestType.USE_ROOM, null,
                rooms.get(9), monday.plusDays(5), timeSlots.get(2), "", 0,
                "Cần phòng lớn tổ chức seminar học thuật của khoa.", Priority.HIGH, RequestStatus.APPROVED, LocalDateTime.now().minusDays(4)));
        requests.add(new ChangeRequest(nextRequestId(), thao, RequestType.BORROW_EQUIPMENT, schedules.get(10),
                rooms.get(9), monday.plusDays(2), timeSlots.get(3), "Camera lớp học", 1,
                "Cần ghi hình phần trình bày để làm tư liệu học tập.", Priority.NORMAL, RequestStatus.PENDING, LocalDateTime.now().minusDays(5)));
        requests.add(new ChangeRequest(nextRequestId(), hoa, RequestType.CHANGE_ROOM, schedules.get(6),
                rooms.get(8), monday.plusDays(1), timeSlots.get(3), "", 0,
                "Phòng ngoại ngữ cần đủ tai nghe cho bài kiểm tra nghe.", Priority.HIGH, RequestStatus.REJECTED, LocalDateTime.now().minusDays(6)));

        addNotification(admin, NotificationType.REQUEST, "Có yêu cầu thiết bị chờ duyệt", "3 yêu cầu tài nguyên cần quản trị viên xử lý.", false, "requests");
        addNotification(admin, NotificationType.ROOM, "Phòng B204 đang bảo trì", "Bộ thí nghiệm điện đang được kiểm tra.", false, "rooms");
        addNotification(academic, NotificationType.SCHEDULE, "Phát hiện xung đột mới", "Hệ thống demo đang có đủ ba loại xung đột lịch.", false, "conflicts");
        addNotification(academic, NotificationType.REQUEST, "Yêu cầu đổi lịch mới", "Giảng viên gửi yêu cầu đổi lịch cần xử lý.", false, "requests");
        addNotification(lecturerDemo, NotificationType.SCHEDULE, "Lịch dạy tuần này", "Bạn có lịch dạy tại A101 và C101.", false, "timetable");
        addNotification(lecturerDemo, NotificationType.REQUEST, "Yêu cầu đang chờ duyệt", "Yêu cầu mượn thiết bị của bạn đang chờ xử lý.", false, "requests");
        addNotification(student, NotificationType.SCHEDULE, "Thông báo lịch học", "Lịch học Cơ sở dữ liệu đã cập nhật phòng học.", false, "timetable");
        addNotification(student, NotificationType.ROOM, "Tra cứu phòng trống", "Phòng C304 đang sẵn sàng cho tự học.", true, "roomSearch");
        addNotification(lecturerDemo, NotificationType.SYSTEM, "Chế độ demo", "Dữ liệu chỉ lưu trong bộ nhớ khi ứng dụng đang chạy.", true, "dashboard");
        addNotification(admin, NotificationType.SYSTEM, "Nhật ký hoạt động", "Các thao tác tuần 1 được mô phỏng để demo.", true, "audit");
    }

    private void addEquipment(String code, String name, String category, int quantity, String condition,
                              Classroom room, ResourceStatus status) {
        equipment.add(new Equipment(nextEquipmentId(), code, name, category, quantity, condition, room, status));
    }

    private void addSchedule(CourseSection section, Classroom room, int dayOfWeek, int startSlotIndex,
                             int endSlotIndex, LocalDate start, LocalDate end, String note) {
        schedules.add(new ScheduleEntry(nextScheduleId(), section, room, dayOfWeek, timeSlots.get(startSlotIndex - 1),
                timeSlots.get(endSlotIndex - 1), start, end, ScheduleStatus.PUBLISHED, note));
    }

    private void addNotification(User user, NotificationType type, String title, String content,
                                 boolean read, String targetScreen) {
        notifications.add(new Notification(nextNotificationId(), user, type, title, content, read,
                LocalDateTime.now().minusHours(notificationSeq * 2), targetScreen));
    }
}
