package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Additive, deterministic expansion of the five-year teaching history. */
public final class HistoricalThousandSeed {
    public static final String MARKER = "SCALE_HISTORY_1000_V1";
    private static final String SOURCE = "SCALE_1K_5Y";
    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 23);
    private static final long SEED = 20260923L;
    private static final String AUDIT_DETAILS = "{\"seed\":\"" + SOURCE + "\"}";
    private static final List<String> OPERATIONAL = List.of("users", "user_roles", "courses", "course_sections",
            "lecturer_assignments", "student_enrollments", "classrooms", "equipment", "classroom_equipment",
            "schedules", "change_requests", "notifications", "audit_logs", "maintenance_records");
    private static final List<String> CATALOGS = List.of("roles", "departments", "semesters", "time_slots");
    private static final String[][] SUBJECTS = {
            {"Dữ liệu phân tán", "Phát triển web", "An toàn thông tin", "Trí tuệ nhân tạo", "Điện toán đám mây",
                    "Lập trình di động", "Mạng doanh nghiệp", "Kiểm thử hệ thống", "Phân tích dữ liệu", "Thiết kế phần mềm",
                    "Tương tác người máy", "Hệ thống nhúng"},
            {"Kế toán quản trị", "Tài chính doanh nghiệp", "Marketing số", "Quản trị chuỗi cung ứng", "Thương mại quốc tế",
                    "Phân tích thị trường", "Quản trị nhân sự", "Khởi nghiệp", "Kiểm toán", "Kinh tế phát triển",
                    "Quản trị dự án", "Thống kê kinh doanh"},
            {"Ngôn ngữ học ứng dụng", "Dịch thuật chuyên ngành", "Giao tiếp học thuật", "Văn hóa giao thoa", "Biên tập tiếng Anh",
                    "Ngữ âm thực hành", "Viết học thuật", "Tiếng Anh thương mại", "Phương pháp giảng dạy", "Đọc hiểu chuyên sâu",
                    "Kỹ năng thuyết trình", "Nghiên cứu ngôn ngữ"}
    };
    private static final String[] VARIANTS = {"cơ bản", "ứng dụng", "nâng cao", "thực hành", "chuyên sâu", "theo dự án"};

    private HistoricalThousandSeed() { }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || (args.length == 1 && !List.of("--apply", "--dry-run", "--verify").contains(args[0])))
            throw new IllegalArgumentException("Use --apply, --dry-run or --verify; no argument previews current counts");
        try (Connection connection = new ConnectionFactory().open()) {
            if (args.length == 0) {
                System.out.println("Current counts: " + counts(connection));
                System.out.println("Run scripts/seed-thousand-history.ps1 to add the five-year operational data.");
            } else if (args[0].equals("--verify")) {
                verify(connection);
                System.out.println("VERIFIED: " + counts(connection));
            } else {
                boolean commit = args[0].equals("--apply");
                boolean inserted = apply(connection, commit);
                System.out.println(!inserted ? "ALREADY_APPLIED: " + counts(connection)
                        : commit ? "Added five-year operational records: " + counts(connection)
                        : "VALIDATED_AND_ROLLED_BACK: " + counts(connection));
            }
        }
    }

    public static boolean apply(Connection connection) throws Exception {
        return apply(connection, true);
    }

    public static boolean apply(Connection connection, boolean commit) throws Exception {
        String lockName = "scale_history_1000_" + connection.getCatalog();
        try (PreparedStatement lock = connection.prepareStatement("SELECT GET_LOCK(?,15)")) {
            lock.setString(1, lockName);
            try (ResultSet result = lock.executeQuery()) {
                result.next();
                if (result.getInt(1) != 1) throw new SQLException("Another history import is running");
            }
        }
        boolean oldAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            if (countAction(connection, HistoricalSeedImporter.MARKER) != 1)
                throw new SQLException("The completed five-year base seed is required");
            if (countAction(connection, MARKER) > 0) {
                connection.rollback();
                return false;
            }
            Map<String, Long> before = counts(connection);
            int units = 0;
            for (String table : List.of("courses", "course_sections", "lecturer_assignments", "classrooms",
                    "equipment", "classroom_equipment", "schedules", "change_requests", "maintenance_records"))
                units = Math.max(units, (int) Math.max(0, 1000 - before.get(table)));
            if (units > 3000) throw new SQLException("Unexpected database size; refusing an oversized import");
            List<Department> departments = departments(connection);
            List<Term> terms = terms(connection);
            List<Long> slots = slots(connection);
            if (departments.size() != 3 || terms.size() != 11 || slots.size() != 6)
                throw new SQLException("The project's three departments, eleven historical terms and six slots are required");
            long lecturerRole = role(connection, "LECTURER"), studentRole = role(connection, "STUDENT");
            long adminId = userId(connection, "admin"), academicId = userId(connection, "daotao");
            Random random = new Random(SEED);
            Map<Long, List<Long>> lecturers = new HashMap<>();
            Map<Long, List<Student>> students = new HashMap<>();
            for (Department department : departments) {
                lecturers.put(department.id(), new ArrayList<>());
                students.put(department.id(), new ArrayList<>());
            }
            Set<String> teacherBusy = new HashSet<>(), studentBusy = new HashSet<>();
            int requestNeed = (int) Math.max(0, 1000 - before.get("change_requests"));
            List<Long> insertedSections = new ArrayList<>();
            try (Writer writer = new Writer(connection)) {
                for (int index = 0; index < 60; index++) {
                    Department department = departments.get(index % 3);
                    String login = "gv" + (1001 + index);
                    LocalDate created = LocalDate.of(2021, 9, 24);
                    long id = writer.insert("INSERT INTO users(department_id,username,password_hash,full_name,email,phone,lecturer_code,status,last_login_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                            department.id(), login, HistoricalSeedGenerator.DEMO_HASH, fullName(index), login + "@donga.edu.vn",
                            phone(index), login.toUpperCase(Locale.ROOT), "ACTIVE", stamp(AS_OF.minusDays(index % 90)), stamp(created), stamp(created));
                    writer.execute("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)", id, lecturerRole);
                    lecturers.get(department.id()).add(id);
                    audit(writer, academicId, "CREATE_USER", "users", id, created);
                    notification(writer, id, "Thông tin phân công giảng dạy đã được cập nhật", "SYSTEM", "dashboard", created);
                }
                for (int year = 2021; year <= 2026; year++) for (int index = 0; index < 200; index++) {
                    Department department = departments.get(index % 3);
                    int serial = (year - 2021) * 200 + index + 1;
                    String login = "sv" + year + "c" + String.format(Locale.ROOT, "%04d", serial);
                    LocalDate created = year == 2021 ? LocalDate.of(2021, 9, 24) : LocalDate.of(year, 8, 25);
                    LocalDate left = year <= 2022 ? LocalDate.of(year + 4, 7, 15) : null;
                    LocalDate loginDay = left == null ? AS_OF.minusDays(random.nextInt(180)) : left.minusDays(1 + random.nextInt(60));
                    if (loginDay.isBefore(created)) loginDay = created.plusDays(1);
                    String status = left == null ? "ACTIVE" : "INACTIVE";
                    long id = writer.insert("INSERT INTO users(department_id,username,password_hash,full_name,email,phone,student_code,class_code,status,last_login_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                            department.id(), login, HistoricalSeedGenerator.DEMO_HASH, fullName(serial), login + "@sv.donga.edu.vn",
                            phone(serial + 100), login.toUpperCase(Locale.ROOT), department.code() + (year % 100) + "C",
                            status, stamp(loginDay), stamp(created), stamp(left == null ? created : left));
                    writer.execute("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)", id, studentRole);
                    students.get(department.id()).add(new Student(id, created, left));
                    audit(writer, academicId, "CREATE_USER", "users", id, created);
                    notification(writer, id, "Đã kích hoạt tài khoản sinh viên", "SYSTEM", "dashboard", created);
                }
                for (int index = 0; index < units; index++) {
                    Term term = terms.get(index % terms.size());
                    Department department = departments.get(index % 3);
                    String code = department.code() + (term.start().getYear() % 100) + "C" + String.format(Locale.ROOT, "%04d", index + 1);
                    String roomCode = "CS" + (index / 300 + 1) + "-" + String.format(Locale.ROOT, "%03d", index + 1);
                    String roomType = switch (department.code()) { case "CNTT" -> "COMPUTER"; case "KT" -> "THEORY"; default -> "PRACTICE"; };
                    String courseName = SUBJECTS[index % 3][(index / 3) % SUBJECTS[0].length] + " " + VARIANTS[(index / 36) % VARIANTS.length];
                    long courseId = writer.insert("INSERT INTO courses(department_id,code,name,credits,required_room_type,status) VALUES (?,?,?,?,?,?)",
                            department.id(), code, courseName, 3, roomType, "ACTIVE");
                    long roomId = writer.insert("INSERT INTO classrooms(code,name,building,floor,capacity,room_type,status,description) VALUES (?,?,?,?,?,?,?,?)",
                            roomCode, "Phòng " + roomCode, "Cơ sở " + (index / 300 + 1), 1 + (index % 300) / 60,
                            40, roomType, "AVAILABLE", "Phòng giảng dạy và thực hành");
                    String equipmentName = switch (index % 3) { case 0 -> "Máy chiếu"; case 1 -> "Bộ âm thanh"; default -> "Máy tính giảng viên"; };
                    long equipmentId = writer.insert("INSERT INTO equipment(code,name,category,status) VALUES (?,?,?,?)",
                            "TB-" + roomCode, equipmentName, "Thiết bị giảng dạy", "ACTIVE");
                    long placementId = writer.insert("INSERT INTO classroom_equipment(classroom_id,equipment_id,quantity,condition_status,condition_note,updated_at) VALUES (?,?,?,?,?,?)",
                            roomId, equipmentId, 1, "GOOD", "Đã kiểm tra và sử dụng tốt", stamp(term.start().minusDays(8)));
                    writer.insert("INSERT INTO maintenance_records(classroom_id,classroom_equipment_id,reported_by,description,start_date,end_date,status,created_at) VALUES (?,?,?,?,?,?,?,?)",
                            roomId, placementId, adminId, "Kiểm tra thiết bị trước học kỳ", term.start().minusDays(10).toString(),
                            term.start().minusDays(8).toString(), "COMPLETED", stamp(term.start().minusDays(10)));
                    long teacher = lecturers.get(department.id()).get((index / 3) % 20);
                    DaySlot daySlot = daySlot(index, term.id(), teacher, teacherBusy);
                    List<Student> eligible = students.get(department.id()).stream()
                            .filter(student -> !student.created().isAfter(term.start())
                                    && (student.left() == null || !student.left().isBefore(term.start())))
                            .toList();
                    List<Student> enrolled = chooseStudents(index, term.id(), daySlot, eligible, studentBusy);
                    long sectionId = writer.insert("INSERT INTO course_sections(course_id,semester_id,code,capacity,student_count,status,created_at) VALUES (?,?,?,?,?,?,?)",
                            courseId, term.id(), code + "-01", 30, enrolled.size(), term.end().isBefore(AS_OF) ? "CLOSED" : "SCHEDULED",
                            stamp(term.start().minusDays(7)));
                    insertedSections.add(sectionId);
                    long assignmentId = writer.insert("INSERT INTO lecturer_assignments(course_section_id,lecturer_id,assigned_at) VALUES (?,?,?)",
                            sectionId, teacher, stamp(term.start().minusDays(7)));
                    for (Student student : enrolled)
                        writer.insert("INSERT INTO student_enrollments(course_section_id,student_id,enrolled_at,status) VALUES (?,?,?,?)",
                                sectionId, student.id(), stamp(term.start().minusDays(5)), "ACTIVE");
                    if (index % 9 == 0) {
                        Set<Long> selected = new HashSet<>();
                        for (Student student : enrolled) selected.add(student.id());
                        eligible.stream().filter(student -> !selected.contains(student.id())).findFirst().ifPresent(student -> {
                            try { writer.insert("INSERT INTO student_enrollments(course_section_id,student_id,enrolled_at,status) VALUES (?,?,?,?)",
                                    sectionId, student.id(), stamp(term.start().minusDays(5)), "CANCELLED"); }
                            catch (SQLException ex) { throw new IllegalStateException(ex); }
                        });
                    }
                    long scheduleId = writer.insert("INSERT INTO schedules(course_section_id,lecturer_assignment_id,classroom_id,start_slot_id,end_slot_id,day_of_week,start_date,end_date,status,note,created_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                            sectionId, assignmentId, roomId, slots.get(daySlot.slot() * 2), slots.get(daySlot.slot() * 2 + 1),
                            daySlot.day(), term.start().toString(), term.end().toString(), "PUBLISHED", "Lịch học chính khóa",
                            academicId, stamp(term.start().minusDays(4)), stamp(term.start().minusDays(4)));
                    notification(writer, teacher, "Đã công bố lịch học " + code, "SCHEDULE", "timetable", term.start().minusDays(2));
                    audit(writer, academicId, "CREATE_SECTION", "course_sections", sectionId, term.start().minusDays(7));
                    audit(writer, academicId, "PUBLISH_SCHEDULE", "schedules", scheduleId, term.start().minusDays(4));
                    if (index < requestNeed)
                        request(writer, index, teacher, scheduleId, roomId, equipmentId, equipmentName, daySlot,
                                slots, term.start(), academicId);
                }
                Map<String, Long> after = counts(connection);
                for (String table : OPERATIONAL)
                    if (after.get(table) < 1000) throw new SQLException("Operational table below 1000: " + table);
                for (String table : CATALOGS)
                    if (!after.get(table).equals(before.get(table))) throw new SQLException("Catalog was changed: " + table);
                if (!insertedSections.isEmpty()) {
                    try (PreparedStatement check = connection.prepareStatement(
                            "SELECT COUNT(*) FROM course_sections cs WHERE cs.id>=? AND cs.id<=? AND cs.student_count<>(SELECT COUNT(*) FROM student_enrollments e WHERE e.course_section_id=cs.id AND e.status='ACTIVE')")) {
                        check.setLong(1, insertedSections.get(0));
                        check.setLong(2, insertedSections.get(insertedSections.size() - 1));
                        try (ResultSet result = check.executeQuery()) {
                            result.next();
                            if (result.getLong(1) != 0) throw new SQLException("Student counts and enrollments differ");
                        }
                    }
                }
                String details = SimulationExport.json(Map.of("seed", SOURCE, "as_of", AS_OF.toString(), "random_seed", SEED,
                        "before", before, "after", after));
                writer.insert("INSERT INTO audit_logs(user_id,action,entity_type,details,created_at) VALUES (?,?,?,?,?)",
                        academicId, MARKER, "database", details, stamp(AS_OF));
            }
            if (commit) connection.commit(); else connection.rollback();
            return true;
        } catch (Exception ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
            try (PreparedStatement release = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                release.setString(1, lockName);
                release.execute();
            }
        }
    }

    public static void verify(Connection connection) throws SQLException {
        if (countAction(connection, MARKER) != 1) throw new SQLException("Completed 1000-row seed marker is missing");
        Map<String, Long> current = counts(connection);
        for (String table : OPERATIONAL) if (current.get(table) < 1000) throw new SQLException("Below 1000: " + table);
        for (String table : CATALOGS) if (current.get(table) >= 1000) throw new SQLException("Fixed catalog was inflated: " + table);
        try (PreparedStatement check = connection.prepareStatement("SELECT COUNT(*) FROM course_sections cs WHERE cs.student_count<>(SELECT COUNT(*) FROM student_enrollments e WHERE e.course_section_id=cs.id AND e.status='ACTIVE')");
             ResultSet result = check.executeQuery()) {
            result.next();
            if (result.getLong(1) != 0) throw new SQLException("Student counts and enrollments differ");
        }
        assertZero(connection, "New student timetable conflicts",
                "SELECT COUNT(*) FROM (SELECT e.student_id, cs.semester_id, s.day_of_week, s.start_slot_id "
                        + "FROM student_enrollments e JOIN users u ON u.id=e.student_id "
                        + "JOIN course_sections cs ON cs.id=e.course_section_id "
                        + "JOIN schedules s ON s.course_section_id=cs.id "
                        + "WHERE e.status='ACTIVE' AND u.username REGEXP '^sv20[0-9]{2}c[0-9]{4}$' "
                        + "GROUP BY e.student_id, cs.semester_id, s.day_of_week, s.start_slot_id HAVING COUNT(*)>1) conflicts");
        assertZero(connection, "New lecturer timetable conflicts",
                "SELECT COUNT(*) FROM (SELECT a.lecturer_id, cs.semester_id, s.day_of_week, s.start_slot_id "
                        + "FROM lecturer_assignments a JOIN users u ON u.id=a.lecturer_id "
                        + "JOIN course_sections cs ON cs.id=a.course_section_id "
                        + "JOIN schedules s ON s.course_section_id=cs.id "
                        + "WHERE u.username REGEXP '^gv1[0-9]{3}$' "
                        + "GROUP BY a.lecturer_id, cs.semester_id, s.day_of_week, s.start_slot_id HAVING COUNT(*)>1) conflicts");
        assertZero(connection, "Invalid simulated login dates",
                "SELECT COUNT(*) FROM users WHERE username REGEXP '^(sv20[0-9]{2}c[0-9]{4}|gv1[0-9]{3})$' "
                        + "AND (last_login_at IS NULL OR last_login_at<created_at OR last_login_at>'2026-09-23 23:59:59')");
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                "SELECT COUNT(DISTINCT cs.semester_id) FROM schedules s "
                        + "JOIN course_sections cs ON cs.id=s.course_section_id "
                        + "JOIN courses c ON c.id=cs.course_id "
                        + "WHERE c.code REGEXP '^[A-Z]+[0-9]{2}C[0-9]{4}$'")) {
            result.next();
            if (result.getInt(1) != 11) throw new SQLException("New schedules do not cover all eleven historical terms");
        }
    }

    private static void assertZero(Connection connection, String label, String query) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            result.next();
            if (result.getLong(1) != 0) throw new SQLException(label + ": " + result.getLong(1));
        }
    }

    private static DaySlot daySlot(int index, long termId, long teacher, Set<String> busy) throws SQLException {
        for (int attempt = 0; attempt < 18; attempt++) {
            int candidate = (index * 7 + attempt) % 18;
            int day = 2 + candidate / 3, slot = candidate % 3;
            String key = termId + "/" + teacher + "/" + day + "/" + slot;
            if (busy.add(key)) return new DaySlot(day, slot);
        }
        throw new SQLException("No free lecturer slot");
    }

    private static List<Student> chooseStudents(int index, long termId, DaySlot daySlot, List<Student> pool, Set<String> busy) throws SQLException {
        List<Student> selected = new ArrayList<>();
        if (pool.isEmpty()) throw new SQLException("No students for historical term");
        for (int offset = 0; offset < pool.size() && selected.size() < 12; offset++) {
            Student student = pool.get((index * 13 + offset) % pool.size());
            String key = termId + "/" + student.id() + "/" + daySlot.day() + "/" + daySlot.slot();
            if (busy.add(key)) selected.add(student);
        }
        if (selected.size() != 12) throw new SQLException("Not enough non-conflicting students");
        return selected;
    }

    private static void request(Writer writer, int index, long teacher, long schedule, long room, long equipment,
                                String equipmentName, DaySlot daySlot, List<Long> slots, LocalDate start, long reviewer) throws SQLException {
        String type = switch (index % 5) {
            case 0, 1, 2 -> "BORROW_EQUIPMENT";
            case 3 -> "REPORT_DAMAGE";
            default -> "USE_ROOM";
        };
        boolean approved = type.equals("BORROW_EQUIPMENT") && index % 7 != 0;
        LocalDate classDate = start.plusDays(daySlot.day() - 2L);
        String reason = switch (type) {
            case "BORROW_EQUIPMENT" -> "Cần thiết bị hỗ trợ bài giảng";
            case "REPORT_DAMAGE" -> "Đề nghị kiểm tra thiết bị trong phòng";
            default -> "Đề nghị bố trí phòng cho buổi bổ sung";
        };
        String review = approved ? "Thiết bị sẵn sàng và đã được duyệt"
                : type.equals("REPORT_DAMAGE") ? "Kiểm tra chưa phát hiện hỏng hóc"
                : "Chưa bố trí được tài nguyên phù hợp";
        long id = writer.insert("INSERT INTO change_requests(requester_id,schedule_id,requested_room_id,requested_equipment_id,requested_date,requested_day,requested_start_slot_id,requested_end_slot_id,equipment_name,quantity,request_type,reason,priority,status,reviewed_by,review_reason,reviewed_at,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                teacher, schedule, room, type.equals("USE_ROOM") ? null : equipment, classDate.toString(), daySlot.day(),
                slots.get(daySlot.slot() * 2), slots.get(daySlot.slot() * 2 + 1),
                type.equals("USE_ROOM") ? null : equipmentName, type.equals("USE_ROOM") ? null : 1, type, reason,
                index % 9 == 0 ? "HIGH" : "NORMAL", approved ? "APPROVED" : "REJECTED", reviewer, review,
                stamp(start.minusDays(2)), stamp(start.minusDays(4)));
        audit(writer, reviewer, approved ? "APPROVE_REQUEST" : "REJECT_REQUEST", "change_requests", id, start.minusDays(2));
        notification(writer, teacher, "Yêu cầu giảng dạy đã được xử lý", "REQUEST", "requests", start.minusDays(2));
    }

    private static void audit(Writer writer, long actor, String action, String entity, long id, LocalDate date) throws SQLException {
        writer.insert("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                actor, action, entity, id, AUDIT_DETAILS, stamp(date));
    }

    private static void notification(Writer writer, long user, String title, String type, String target, LocalDate date) throws SQLException {
        writer.insert("INSERT INTO notifications(user_id,title,content,type,target_screen,is_read,created_at,read_at) VALUES (?,?,?,?,?,?,?,?)",
                user, title, title + ".", type, target, date.isBefore(AS_OF.minusDays(7)) ? 1 : 0,
                stamp(date), date.isBefore(AS_OF.minusDays(7)) ? stamp(date.plusDays(1)) : null);
    }

    private static String fullName(int index) {
        String[] family = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Võ", "Đặng", "Bùi"};
        String[] given = {"Minh Anh", "Gia Huy", "Thu Hà", "Hoàng Nam", "Thanh Trúc", "Đức Minh", "Ngọc Linh", "Quốc Bảo", "Anh Tuấn", "Thảo Vy"};
        return family[index % family.length] + " " + given[(index / family.length) % given.length];
    }

    private static String phone(int index) { return String.format(Locale.ROOT, "09%08d", 30000000 + index); }
    private static String stamp(LocalDate date) { return date + " 08:00:00"; }

    private static List<Department> departments(Connection connection) throws SQLException {
        List<Department> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id,code FROM departments ORDER BY id");
             ResultSet rows = statement.executeQuery())
            { while (rows.next()) result.add(new Department(rows.getLong(1), rows.getString(2))); }
        return result;
    }

    private static List<Term> terms(Connection connection) throws SQLException {
        List<Term> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id,code,start_date,end_date FROM semesters WHERE code LIKE 'XUAN-%' OR code LIKE 'THU-%' ORDER BY start_date");
             ResultSet rows = statement.executeQuery())
            { while (rows.next()) result.add(new Term(rows.getLong(1), rows.getString(2), rows.getDate(3).toLocalDate(), rows.getDate(4).toLocalDate())); }
        return result;
    }

    private static List<Long> slots(Connection connection) throws SQLException {
        List<Long> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM time_slots ORDER BY sort_order");
             ResultSet rows = statement.executeQuery())
            { while (rows.next()) result.add(rows.getLong(1)); }
        return result;
    }

    private static long role(Connection connection, String code) throws SQLException { return idFor(connection, "SELECT id FROM roles WHERE code=?", code); }
    private static long userId(Connection connection, String username) throws SQLException { return idFor(connection, "SELECT id FROM users WHERE username=?", username); }
    private static long idFor(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Missing required catalog or account: " + value);
                return rows.getLong(1);
            }
        }
    }

    private static int countAction(Connection connection, String action) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action=?")) {
            statement.setString(1, action);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getInt(1); }
        }
    }

    private static Map<String, Long> counts(Connection connection) throws SQLException {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String table : SimulationData.TABLES) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM `" + table + "`");
                 ResultSet rows = statement.executeQuery()) { rows.next(); result.put(table, rows.getLong(1)); }
        }
        return result;
    }

    private record Department(long id, String code) { }
    private record Term(long id, String code, LocalDate start, LocalDate end) { }
    private record Student(long id, LocalDate created, LocalDate left) { }
    private record DaySlot(int day, int slot) { }

    private static final class Writer implements AutoCloseable {
        private final Connection connection;
        private final Map<String, PreparedStatement> statements = new HashMap<>();
        Writer(Connection connection) { this.connection = connection; }
        long insert(String sql, Object... values) throws SQLException {
            PreparedStatement statement = statements.computeIfAbsent(sql, key -> {
                try { return connection.prepareStatement(key, Statement.RETURN_GENERATED_KEYS); }
                catch (SQLException ex) { throw new IllegalStateException(ex); }
            });
            bind(statement, values);
            if (statement.executeUpdate() != 1) throw new SQLException("Insert did not affect one row");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                return 0;
            }
        }
        void execute(String sql, Object... values) throws SQLException {
            PreparedStatement statement = statements.computeIfAbsent(sql, key -> {
                try { return connection.prepareStatement(key); }
                catch (SQLException ex) { throw new IllegalStateException(ex); }
            });
            bind(statement, values);
            if (statement.executeUpdate() != 1) throw new SQLException("Insert did not affect one row");
        }
        private static void bind(PreparedStatement statement, Object[] values) throws SQLException {
            statement.clearParameters();
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
        }
        @Override public void close() throws SQLException {
            SQLException failure = null;
            for (PreparedStatement statement : statements.values()) try { statement.close(); }
            catch (SQLException ex) { if (failure == null) failure = ex; else failure.addSuppressed(ex); }
            if (failure != null) throw failure;
        }
    }
}
