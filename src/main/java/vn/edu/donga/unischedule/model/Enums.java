package vn.edu.donga.unischedule.model;

public final class Enums {
    private Enums() {
    }

    public enum Role {
        ADMIN("Quản trị viên"),
        ACADEMIC("Phòng đào tạo"),
        LECTURER("Giảng viên"),
        STUDENT("Sinh viên");

        private final String displayName;

        Role(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum UserStatus {
        ACTIVE("Hoạt động"),
        LOCKED("Đã khóa"), INACTIVE("Ngừng hoạt động");

        private final String displayName;

        UserStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum RoomStatus {
        AVAILABLE("Sẵn sàng"),
        IN_USE("Đang sử dụng"),
        MAINTENANCE("Bảo trì"),
        INACTIVE("Ngừng sử dụng");

        private final String displayName;

        RoomStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ResourceStatus {
        ACTIVE("Hoạt động"),
        MAINTENANCE("Bảo trì"),
        BROKEN("Bị hỏng"),
        INACTIVE("Ngừng dùng");

        private final String displayName;

        ResourceStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum RoomType {
        THEORY("Lý thuyết"),
        COMPUTER("Máy tính"),
        LABORATORY("Thí nghiệm"),
        PRACTICE("Thực hành");

        private final String displayName;

        RoomType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ScheduleStatus {
        DRAFT("Nháp"),
        PUBLISHED("Đã công bố"),
        CANCELLED("Đã hủy");

        private final String displayName;

        ScheduleStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum CourseSectionStatus {
        UNSCHEDULED("Chưa xếp lịch"),
        SCHEDULED("Đã xếp lịch"),
        CONFLICTED("Có xung đột"), CLOSED("Đã đóng"), CANCELLED("Đã hủy");

        private final String displayName;

        CourseSectionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum RequestType {
        CHANGE_SCHEDULE("Đổi lịch"),
        CHANGE_ROOM("Đổi phòng"),
        BORROW_EQUIPMENT("Mượn thiết bị"),
        REPORT_DAMAGE("Báo hỏng"),
        USE_ROOM("Học bù / sử dụng phòng");

        private final String displayName;

        RequestType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum RequestStatus {
        PENDING("Chờ duyệt"),
        APPROVED("Đã duyệt"),
        REJECTED("Từ chối");

        private final String displayName;

        RequestStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Priority {
        LOW("Thấp"),
        NORMAL("Bình thường"),
        HIGH("Cao"),
        URGENT("Khẩn cấp");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ConflictType {
        ROOM("Trùng phòng"),
        LECTURER("Trùng giảng viên"),
        COURSE_SECTION("Trùng lớp học phần"),
        CAPACITY("Không đủ sức chứa");

        private final String displayName;

        ConflictType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ConflictStatus {
        OPEN("Chưa xử lý"),
        PROCESSING("Đang xử lý"),
        RESOLVED("Đã xử lý");

        private final String displayName;

        ConflictStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum NotificationType {
        SCHEDULE("Lịch học"),
        REQUEST("Yêu cầu"),
        ROOM("Phòng học"),
        SYSTEM("Hệ thống");

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
