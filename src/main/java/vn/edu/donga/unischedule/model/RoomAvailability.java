package vn.edu.donga.unischedule.model;

/** Scheduled room occupancy for one date and time slot. */
public record RoomAvailability(Classroom room, Status status, Integer registeredStudents, String classes) {
    public enum Status {
        FREE("Trống"), OCCUPIED("Có lịch học"), MAINTENANCE("Bảo trì"), UNAVAILABLE("Không sử dụng được");

        private final String label;

        Status(String label) { this.label = label; }

        public String label() { return label; }
    }
}
