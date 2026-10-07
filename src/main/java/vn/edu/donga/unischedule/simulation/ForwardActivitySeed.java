package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Insert-only operational activity for the forward 2026-2030 application scenario. */
public final class ForwardActivitySeed {
    private static final String MARKER = "FORWARD_ACTIVITY_2026_2030_V1";
    private static final String DETAILS = "{\"seed\":\"FORWARD_ACTIVITY_2026_2030\",\"random_seed\":20260923}";
    private static final String[] FAMILY = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Võ", "Đặng", "Bùi"};
    private static final String[] GIVEN = {"Minh Anh", "Gia Huy", "Thu Hà", "Hoàng Nam", "Thanh Trúc", "Đức Minh", "Ngọc Linh", "Quốc Bảo"};

    private ForwardActivitySeed() { }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "preview" : args[0];
        if (!List.of("preview", "apply", "dry-run", "verify").contains(mode))
            throw new IllegalArgumentException("Use preview, apply, dry-run, or verify");
        try (Connection connection = new ConnectionFactory().open()) {
            if (mode.equals("verify")) verify(connection);
            else if (!mode.equals("preview")) {
                boolean inserted = apply(connection, mode.equals("apply"));
                System.out.println(!inserted ? "ACTIVITY_ALREADY_APPLIED" : mode.equals("apply")
                        ? "ACTIVITY_APPLIED" : "ACTIVITY_VALIDATED_AND_ROLLED_BACK");
            }
            printSummary(connection);
        }
    }

    static boolean apply(Connection connection, boolean commit) throws Exception {
        if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='FORWARD_FIVE_YEARS_V1'") != 1)
            throw new SQLException("Forward timetable seed is required first");
        String lockName = "forward_activity_" + connection.getCatalog();
        try (PreparedStatement lock = connection.prepareStatement("SELECT GET_LOCK(?,15)")) {
            lock.setString(1, lockName);
            try (ResultSet rows = lock.executeQuery()) { rows.next(); if (rows.getInt(1) != 1) throw new SQLException("Another forward seed is running"); }
        }
        boolean oldAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 0) {
                connection.rollback(); return false;
            }
            long academic = scalar(connection, "SELECT id FROM users WHERE username='daotao'");
            long lecturerRole = scalar(connection, "SELECT id FROM roles WHERE code='LECTURER'");
            long studentRole = scalar(connection, "SELECT id FROM roles WHERE code='STUDENT'");
            List<Long> departments = longs(connection, "SELECT id FROM departments ORDER BY id");
            List<Long> rooms = longs(connection, "SELECT id FROM classrooms WHERE status='AVAILABLE' ORDER BY id");
            int users = 0, requests = 0, maintenance = 0, notifications = 0;
            for (int year = 2027; year <= 2030; year++) {
                List<Long> lecturers = new ArrayList<>();
                for (int index = 0; index < 60; index++) {
                    boolean lecturer = index < 6;
                    long department = departments.get(index % departments.size());
                    String username = (lecturer ? "gv" : "sv") + "f" + year + String.format(Locale.ROOT, "%03d", index + 1);
                    LocalDate created = LocalDate.of(year, lecturer ? 1 : 8, lecturer ? 8 : 20);
                    LocalDate login = LocalDate.of(year, 12, 15).minusDays(index % 45);
                    String name = FAMILY[(index + year) % FAMILY.length] + " " + GIVEN[(index * 3 + year) % GIVEN.length];
                    long user = insert(connection,
                            "INSERT INTO users(department_id,username,password_hash,full_name,email,phone,lecturer_code,student_code,class_code,status,last_login_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                            department, username, HistoricalSeedGenerator.DEMO_HASH, name,
                            username + (lecturer ? "@donga.edu.vn" : "@sv.donga.edu.vn"),
                            String.format(Locale.ROOT, "08%08d", (year - 2026) * 1000 + index),
                            lecturer ? username.toUpperCase(Locale.ROOT) : null,
                            lecturer ? null : username.toUpperCase(Locale.ROOT),
                            lecturer ? null : "K" + (year % 100) + "-" + (index % 3 + 1),
                            "ACTIVE", stamp(login), stamp(created), stamp(created));
                    execute(connection, "INSERT INTO user_roles(user_id,role_id) VALUES (?,?)", user, lecturer ? lecturerRole : studentRole);
                    audit(connection, academic, "CREATE_USER", "users", user, created);
                    insert(connection, "INSERT INTO notifications(user_id,title,content,type,target_screen,is_read,created_at,read_at) VALUES (?,?,?,?,?,?,?,?)",
                            user, "Tài khoản đã được kích hoạt", "Thông tin học tập và lịch biểu đã sẵn sàng.",
                            "SYSTEM", "dashboard", true, stamp(created.plusDays(1)), stamp(created.plusDays(2)));
                    notifications++; users++;
                    if (lecturer) lecturers.add(user);
                }
                for (int index = 0; index < 48; index++) {
                    long requester = lecturers.get(index % lecturers.size());
                    long room = rooms.get(Math.floorMod(year * 31 + index * 17, rooms.size()));
                    LocalDate created = LocalDate.of(year, index < 24 ? 2 : 9, 3 + index % 20);
                    String status = index % 10 == 0 ? "PENDING" : index % 5 == 0 ? "REJECTED" : "APPROVED";
                    boolean reviewed = !status.equals("PENDING");
                    long request = insert(connection,
                            "INSERT INTO change_requests(requester_id,requested_room_id,request_type,reason,priority,status,reviewed_by,review_reason,reviewed_at,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                            requester, room, "USE_ROOM", "Đăng ký sử dụng phòng cho hoạt động học thuật của khoa.",
                            index % 8 == 0 ? "HIGH" : "NORMAL", status, reviewed ? academic : null,
                            reviewed ? status.equals("APPROVED") ? "Đã kiểm tra và bố trí phòng phù hợp." : "Phòng đã có kế hoạch sử dụng khác." : null,
                            reviewed ? stamp(created.plusDays(1)) : null, stamp(created));
                    audit(connection, requester, "CREATE_REQUEST", "change_requests", request, created);
                    insert(connection, "INSERT INTO notifications(user_id,title,content,type,reference_type,reference_id,target_screen,is_read,created_at,read_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                            requester, status.equals("PENDING") ? "Yêu cầu đang chờ xử lý" : "Yêu cầu phòng đã được cập nhật",
                            status.equals("APPROVED") ? "Phòng đã được xác nhận theo kế hoạch." : status.equals("REJECTED") ? "Vui lòng chọn thời gian hoặc phòng khác." : "Phòng đào tạo sẽ phản hồi yêu cầu.",
                            "REQUEST", "change_requests", request, "requests", reviewed, stamp(created.plusDays(1)), reviewed ? stamp(created.plusDays(2)) : null);
                    requests++; notifications++;
                }
                for (int index = 0; index < 36; index++) {
                    long room = rooms.get(Math.floorMod(year * 43 + index * 23, rooms.size()));
                    LocalDate start = LocalDate.of(year, 6 + index % 2, 2 + index % 24);
                    long record = insert(connection,
                            "INSERT INTO maintenance_records(classroom_id,reported_by,description,start_date,end_date,status,created_at) VALUES (?,?,?,?,?,?,?)",
                            room, lecturers.get(index % lecturers.size()),
                            index % 3 == 0 ? "Kiểm tra máy chiếu và hệ thống âm thanh." : index % 3 == 1 ? "Bảo dưỡng điều hòa và nguồn điện phòng học." : "Kiểm tra bàn ghế và thiết bị kết nối.",
                            start, start.plusDays(1 + index % 3), "COMPLETED", stamp(start.minusDays(2)));
                    audit(connection, academic, "COMPLETE_MAINTENANCE", "maintenance_records", record, start.plusDays(1 + index % 3));
                    maintenance++;
                }
            }
            insert(connection, "INSERT INTO audit_logs(user_id,action,entity_type,details,created_at) VALUES (?,?,?,?,?)",
                    academic, MARKER, "database", "{\"seed\":\"FORWARD_ACTIVITY_2026_2030\",\"users\":" + users
                            + ",\"requests\":" + requests + ",\"maintenance\":" + maintenance
                            + ",\"notifications\":" + notifications + "}", "2026-09-23 12:00:00");
            verifyRows(connection);
            if (commit) connection.commit(); else connection.rollback();
            return true;
        } catch (Exception failure) {
            connection.rollback(); throw failure;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
            try (PreparedStatement release = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) { release.setString(1, lockName); release.execute(); }
        }
    }

    static void verify(Connection connection) throws SQLException {
        if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 1)
            throw new SQLException("Forward activity marker is missing");
        verifyRows(connection);
    }

    private static void verifyRows(Connection connection) throws SQLException {
        for (int year = 2027; year <= 2030; year++) {
            if (scalar(connection, "SELECT COUNT(*) FROM users WHERE username LIKE '%f" + year + "%' AND YEAR(created_at)=" + year) != 60)
                throw new SQLException("Unexpected future user count for " + year);
            if (scalar(connection, "SELECT COUNT(*) FROM change_requests WHERE YEAR(created_at)=" + year) < 48)
                throw new SQLException("Future requests are missing for " + year);
            if (scalar(connection, "SELECT COUNT(*) FROM maintenance_records WHERE YEAR(start_date)=" + year) < 36)
                throw new SQLException("Future maintenance is missing for " + year);
        }
    }

    private static void printSummary(Connection connection) throws SQLException {
        System.out.println("Forward operational rows by year:");
        for (int year = 2026; year <= 2030; year++) System.out.println(year + " users="
                + scalar(connection, "SELECT COUNT(*) FROM users WHERE YEAR(created_at)=" + year)
                + " requests=" + scalar(connection, "SELECT COUNT(*) FROM change_requests WHERE YEAR(created_at)=" + year)
                + " maintenance=" + scalar(connection, "SELECT COUNT(*) FROM maintenance_records WHERE YEAR(start_date)=" + year));
    }

    private static List<Long> longs(Connection connection, String sql) throws SQLException {
        List<Long> result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) { while (rows.next()) result.add(rows.getLong(1)); }
        return result;
    }
    private static long scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) { rows.next(); return rows.getLong(1); }
    }
    private static long insert(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, values); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Generated key is missing"); return keys.getLong(1); }
        }
    }
    private static void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) { bind(statement, values); statement.executeUpdate(); }
    }
    private static void audit(Connection connection, long user, String action, String entity, long id, LocalDate date) throws SQLException {
        insert(connection, "INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                user, action, entity, id, DETAILS, stamp(date));
    }
    private static void bind(PreparedStatement statement, Object[] values) throws SQLException {
        for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
    }
    private static String stamp(LocalDate date) { return date + " 08:00:00"; }
}
