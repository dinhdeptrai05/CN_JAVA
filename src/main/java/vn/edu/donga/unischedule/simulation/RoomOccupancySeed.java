package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Adds real course sections until roughly 60% of rooms have a timetable in every academic period. */
public final class RoomOccupancySeed {
    private static final String MARKER = "ROOM_OCCUPANCY_60_V1";
    private static final String SOURCE = "ROOM_OCCUPANCY_60";
    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 23);
    private static final long SEED = 20260923L;
    private static final int TARGET_PERCENT = 60;
    private static final String DETAILS = "{\"seed\":\"" + SOURCE + "\"}";

    private RoomOccupancySeed() { }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || (args.length == 1 && !List.of("--apply", "--dry-run", "--verify").contains(args[0])))
            throw new IllegalArgumentException("Use --apply, --dry-run, --verify, or no argument for preview");
        try (Connection connection = new ConnectionFactory().open()) {
            if (args.length == 0) printOccupancy(connection);
            else if (args[0].equals("--verify")) { verify(connection); printOccupancy(connection); }
            else {
                boolean applied = apply(connection, args[0].equals("--apply"));
                System.out.println(applied ? args[0].equals("--apply") ? "APPLIED" : "VALIDATED_AND_ROLLED_BACK" : "ALREADY_APPLIED");
                printOccupancy(connection);
            }
        }
    }

    public static boolean apply(Connection connection, boolean commit) throws Exception {
        if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + HistoricalThousandSeed.MARKER + "'") != 1)
            throw new SQLException("Run the 1000-row historical seed first");
        String lockName = "room_occupancy_60_" + connection.getCatalog();
        try (PreparedStatement lock = connection.prepareStatement("SELECT GET_LOCK(?,15)")) {
            lock.setString(1, lockName);
            try (ResultSet result = lock.executeQuery()) {
                result.next();
                if (result.getInt(1) != 1) throw new SQLException("Another room timetable seed is running");
            }
        }
        boolean previousAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            try (Statement lockScheduling = connection.createStatement();
                 ResultSet ignored = lockScheduling.executeQuery("SELECT id FROM roles WHERE code='ACADEMIC' FOR UPDATE")) {
                while (ignored.next()) { /* Same scheduling lock as the application. */ }
            }
            if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 0) {
                connection.rollback();
                return false;
            }
            List<Period> periods = periods(connection);
            if (periods.size() != 12) throw new SQLException("Expected eleven historical periods and the planned second 2026 term");
            List<Room> rooms = rooms(connection);
            int target = (int) Math.round(scalar(connection, "SELECT COUNT(*) FROM classrooms") * TARGET_PERCENT / 100.0);
            Map<String, List<Course>> courses = courses(connection);
            Map<Long, List<Long>> teachers = teachers(connection);
            Map<Long, List<Student>> students = students(connection);
            List<Long> slots = slots(connection);
            long academic = id(connection, "SELECT id FROM users WHERE username='daotao'");
            indexExisting(connection, periods);
            int added = 0;
            try (Writer writer = new Writer(connection)) {
                for (Period period : periods) {
                    List<Room> shuffled = new ArrayList<>(rooms);
                    Collections.shuffle(shuffled, new Random(SEED ^ period.key.hashCode()));
                    int withinPeriod = 0;
                    for (Room room : shuffled) {
                        if (period.roomIds.size() >= target) break;
                        if (period.roomIds.contains(room.id)) continue;
                        List<Course> matchingCourses = courses.get(room.type);
                        if (matchingCourses == null || matchingCourses.isEmpty()) continue;
                        Course course = matchingCourses.get(Math.floorMod((int) (room.id + period.key.hashCode()), matchingCourses.size()));
                        List<Long> departmentTeachers = teachers.get(course.departmentId);
                        List<Student> departmentStudents = students.get(course.departmentId);
                        if (departmentTeachers == null || departmentStudents == null) continue;
                        int classSize = period.academicYear == 2021 ? 4 : period.academicYear == 2022 ? 6
                                : period.academicYear == 2023 ? 8 : 10;
                        Placement placement = place(period, room, departmentTeachers, departmentStudents, classSize, withinPeriod);
                        if (placement == null) continue;
                        String sectionCode = "LHP" + period.academicYear + "-" + period.term + "-" + room.code;
                        LocalDate created = dateBeforeStart(period.start, 14);
                        long section = writer.insert("INSERT INTO course_sections(course_id,semester_id,code,capacity,student_count,status,created_at) VALUES (?,?,?,?,?,?,?)",
                                course.id, period.semesterId, sectionCode, room.capacity, placement.studentIds.size(),
                                period.end.isBefore(AS_OF) ? "CLOSED" : "SCHEDULED", stamp(created));
                        long assignment = writer.insert("INSERT INTO lecturer_assignments(course_section_id,lecturer_id,assigned_at) VALUES (?,?,?)",
                                section, placement.teacherId, stamp(created));
                        for (long student : placement.studentIds)
                            writer.insert("INSERT INTO student_enrollments(course_section_id,student_id,enrolled_at,status) VALUES (?,?,?,?)",
                                    section, student, stamp(dateBeforeStart(period.start, 7)), "ACTIVE");
                        long schedule = writer.insert("INSERT INTO schedules(course_section_id,lecturer_assignment_id,classroom_id,start_slot_id,end_slot_id,day_of_week,start_date,end_date,status,note,created_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                section, assignment, room.id, slots.get(placement.block * 2), slots.get(placement.block * 2 + 1),
                                placement.day, period.start.toString(), period.end.toString(), "PUBLISHED", "Lịch học chính khóa",
                                academic, stamp(dateBeforeStart(period.start, 5)), stamp(dateBeforeStart(period.start, 5)));
                        audit(writer, academic, "CREATE_SECTION", "course_sections", section, created);
                        audit(writer, academic, "PUBLISH_SCHEDULE", "schedules", schedule, dateBeforeStart(period.start, 5));
                        period.roomIds.add(room.id);
                        period.teacherBusy.add(key(placement.teacherId, placement.day, placement.block));
                        for (long student : placement.studentIds) period.studentBusy.add(key(student, placement.day, placement.block));
                        period.slotCounts[placement.day - 2][placement.block]++;
                        added++;
                        withinPeriod++;
                    }
                    if (period.roomIds.size() != target)
                        throw new SQLException("Could not schedule " + period.key + ": " + period.roomIds.size() + "/" + target + " rooms");
                }
                writer.insert("INSERT INTO audit_logs(user_id,action,entity_type,details,created_at) VALUES (?,?,?,?,?)",
                        academic, MARKER, "database", "{\"seed\":\"" + SOURCE + "\",\"random_seed\":" + SEED
                                + ",\"target_rooms\":" + target + ",\"added_schedules\":" + added + "}", stamp(AS_OF));
            }
            verifyOccupancy(connection, periods, target);
            verifyNoNewConflicts(connection);
            if (commit) connection.commit(); else connection.rollback();
            return true;
        } catch (Exception failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
            try (PreparedStatement release = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                release.setString(1, lockName);
                release.execute();
            }
        }
    }

    public static void verify(Connection connection) throws SQLException {
        if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 1)
            throw new SQLException("Room timetable seed marker is missing");
        int target = (int) Math.round(scalar(connection, "SELECT COUNT(*) FROM classrooms") * TARGET_PERCENT / 100.0);
        List<Period> periods = periods(connection);
        indexExisting(connection, periods);
        verifyOccupancy(connection, periods, target);
        if (scalar(connection, "SELECT COUNT(*) FROM course_sections cs WHERE cs.code LIKE 'LHP20%' AND cs.student_count<>(SELECT COUNT(*) FROM student_enrollments e WHERE e.course_section_id=cs.id AND e.status='ACTIVE')") != 0)
            throw new SQLException("New section enrollment totals do not match");
        if (scalar(connection, "SELECT COUNT(*) FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id WHERE cs.code LIKE 'LHP20%' AND s.created_by<>(SELECT id FROM users WHERE username='daotao')") != 0)
            throw new SQLException("New schedules were not created by academic staff");
        verifyNoNewConflicts(connection);
    }

    private static void verifyNoNewConflicts(Connection connection) throws SQLException {
        Map<Long, String> periodBySemester = new HashMap<>();
        for (Period period : periods(connection)) for (long semester : period.semesterIds)
            periodBySemester.put(semester, period.key);
        Map<String, Boolean> roomSlots = new HashMap<>(), teacherSlots = new HashMap<>(), studentSlots = new HashMap<>();
        String scheduleSql = "SELECT cs.semester_id,s.classroom_id,la.lecturer_id,s.day_of_week,ss.sort_order,es.sort_order,cs.code "
                + "FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id "
                + "JOIN lecturer_assignments la ON la.id=s.lecturer_assignment_id "
                + "JOIN time_slots ss ON ss.id=s.start_slot_id JOIN time_slots es ON es.id=s.end_slot_id "
                + "WHERE s.status<>'CANCELLED'";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(scheduleSql)) {
            while (rows.next()) {
                String period = periodBySemester.get(rows.getLong(1));
                if (period == null) continue;
                boolean added = rows.getString(7).startsWith("LHP20");
                for (int block = 0; block < 3; block++) if (rows.getInt(5) <= block * 2 + 2 && rows.getInt(6) >= block * 2 + 1) {
                    checkedSlot(roomSlots, period + "/" + key(rows.getLong(2), rows.getInt(4), block), added, "room");
                    checkedSlot(teacherSlots, period + "/" + key(rows.getLong(3), rows.getInt(4), block), added, "lecturer");
                }
            }
        }
        String studentSql = "SELECT cs.semester_id,e.student_id,s.day_of_week,ss.sort_order,es.sort_order,cs.code "
                + "FROM student_enrollments e JOIN schedules s ON s.course_section_id=e.course_section_id "
                + "JOIN course_sections cs ON cs.id=e.course_section_id "
                + "JOIN time_slots ss ON ss.id=s.start_slot_id JOIN time_slots es ON es.id=s.end_slot_id "
                + "WHERE e.status='ACTIVE' AND s.status<>'CANCELLED'";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(studentSql)) {
            while (rows.next()) {
                String period = periodBySemester.get(rows.getLong(1));
                if (period == null) continue;
                boolean added = rows.getString(6).startsWith("LHP20");
                for (int block = 0; block < 3; block++) if (rows.getInt(4) <= block * 2 + 2 && rows.getInt(5) >= block * 2 + 1)
                    checkedSlot(studentSlots, period + "/" + key(rows.getLong(2), rows.getInt(3), block), added, "student");
            }
        }
    }

    private static void checkedSlot(Map<String, Boolean> seen, String key, boolean added, String type) throws SQLException {
        Boolean previous = seen.putIfAbsent(key, added);
        if (previous != null && (previous || added)) throw new SQLException("New " + type + " timetable conflict: " + key);
    }

    private static void verifyOccupancy(Connection connection, List<Period> periods, int target) throws SQLException {
        indexExisting(connection, periods);
        for (Period period : periods)
            if (period.roomIds.size() < target) throw new SQLException("Room occupancy fell below the original target in " + period.key + ": " + period.roomIds.size() + "/" + target);
    }

    private static void printOccupancy(Connection connection) throws SQLException {
        List<Period> periods = periods(connection);
        indexExisting(connection, periods);
        long total = scalar(connection, "SELECT COUNT(*) FROM classrooms");
        System.out.println("Rooms with a published timetable / all rooms (target " + TARGET_PERCENT + "%):");
        for (Period period : periods)
            System.out.println(period.key + "  " + period.roomIds.size() + "/" + total + " used; " + (total - period.roomIds.size()) + " free");
    }

    private static Placement place(Period period, Room room, List<Long> teachers, List<Student> students,
                                   int classSize, int ordinal) {
        List<int[]> dayBlocks = new ArrayList<>();
        for (int day = 2; day <= 7; day++) for (int block = 0; block < 3; block++) dayBlocks.add(new int[]{day, block});
        dayBlocks.sort(Comparator.comparingInt((int[] value) -> period.slotCounts[value[0] - 2][value[1]])
                .thenComparingInt(value -> Math.floorMod(value[0] * 7 + value[1] + ordinal, 19)));
        for (int[] dayBlock : dayBlocks) {
            int day = dayBlock[0], block = dayBlock[1];
            Long teacher = null;
            for (int offset = 0; offset < teachers.size(); offset++) {
                long candidate = teachers.get(Math.floorMod(ordinal + offset, teachers.size()));
                if (!period.teacherBusy.contains(key(candidate, day, block))) { teacher = candidate; break; }
            }
            if (teacher == null) continue;
            List<Long> chosen = new ArrayList<>();
            for (int offset = 0; offset < students.size() && chosen.size() < classSize; offset++) {
                Student student = students.get(Math.floorMod((int) room.id + ordinal * 13 + offset, students.size()));
                if (student.created.isAfter(period.start) || student.left != null && student.left.isBefore(period.start)) continue;
                if (!period.studentBusy.contains(key(student.id, day, block))) chosen.add(student.id);
            }
            if (chosen.size() == classSize) return new Placement(day, block, teacher, chosen);
        }
        return null;
    }

    private static void indexExisting(Connection connection, List<Period> periods) throws SQLException {
        Map<Long, Period> bySemester = new HashMap<>();
        for (Period period : periods) {
            period.roomIds.clear(); period.teacherBusy.clear(); period.studentBusy.clear();
            for (int[] counts : period.slotCounts) java.util.Arrays.fill(counts, 0);
            for (long id : period.semesterIds) bySemester.put(id, period);
        }
        String schedules = "SELECT s.id,cs.semester_id,s.classroom_id,la.lecturer_id,s.day_of_week,ss.sort_order,es.sort_order "
                + "FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id "
                + "JOIN lecturer_assignments la ON la.id=s.lecturer_assignment_id "
                + "JOIN time_slots ss ON ss.id=s.start_slot_id JOIN time_slots es ON es.id=s.end_slot_id WHERE s.status<>'CANCELLED'";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(schedules)) {
            while (rows.next()) {
                Period period = bySemester.get(rows.getLong(2));
                if (period == null) continue;
                period.roomIds.add(rows.getLong(3));
                for (int block = 0; block < 3; block++) if (rows.getInt(6) <= block * 2 + 2 && rows.getInt(7) >= block * 2 + 1) {
                    period.teacherBusy.add(key(rows.getLong(4), rows.getInt(5), block));
                    if (rows.getInt(5) >= 2 && rows.getInt(5) <= 7) period.slotCounts[rows.getInt(5) - 2][block]++;
                }
            }
        }
        String enrollments = "SELECT cs.semester_id,e.student_id,s.day_of_week,ss.sort_order,es.sort_order "
                + "FROM student_enrollments e JOIN schedules s ON s.course_section_id=e.course_section_id "
                + "JOIN course_sections cs ON cs.id=e.course_section_id "
                + "JOIN time_slots ss ON ss.id=s.start_slot_id JOIN time_slots es ON es.id=s.end_slot_id "
                + "WHERE e.status='ACTIVE' AND s.status<>'CANCELLED'";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(enrollments)) {
            while (rows.next()) {
                Period period = bySemester.get(rows.getLong(1));
                if (period == null) continue;
                for (int block = 0; block < 3; block++) if (rows.getInt(4) <= block * 2 + 2 && rows.getInt(5) >= block * 2 + 1)
                    period.studentBusy.add(key(rows.getLong(2), rows.getInt(3), block));
            }
        }
    }

    private static List<Period> periods(Connection connection) throws SQLException {
        Map<String, Period> grouped = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,start_date,end_date FROM semesters ORDER BY start_date")) {
            while (rows.next()) {
                LocalDate start = rows.getDate(2).toLocalDate(), end = rows.getDate(3).toLocalDate();
                int year = start.getMonthValue() >= 8 ? start.getYear() : start.getYear() - 1;
                int term = start.getMonthValue() >= 8 ? 1 : 2;
                if (year < 2021 || year > 2026) continue;
                String key = year + "-" + term;
                Period period = grouped.computeIfAbsent(key, ignored -> new Period(key, year, term));
                period.semesterIds.add(rows.getLong(1));
                if (period.start == null || start.isAfter(period.start)) {
                    period.start = start; period.end = end; period.semesterId = rows.getLong(1);
                }
            }
        }
        return new ArrayList<>(grouped.values());
    }

    private static List<Room> rooms(Connection connection) throws SQLException {
        List<Room> result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,code,room_type,capacity FROM classrooms WHERE status='AVAILABLE' ORDER BY id")) {
            while (rows.next()) result.add(new Room(rows.getLong(1), rows.getString(2), rows.getString(3), rows.getInt(4)));
        }
        return result;
    }

    private static Map<String, List<Course>> courses(Connection connection) throws SQLException {
        Map<String, List<Course>> result = new HashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,department_id,required_room_type FROM courses WHERE status='ACTIVE' ORDER BY id")) {
            while (rows.next()) result.computeIfAbsent(rows.getString(3), ignored -> new ArrayList<>()).add(new Course(rows.getLong(1), rows.getLong(2)));
        }
        return result;
    }

    private static Map<Long, List<Long>> teachers(Connection connection) throws SQLException {
        Map<Long, List<Long>> result = new HashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,department_id FROM users WHERE username REGEXP '^gv1[0-9]{3}$' AND status='ACTIVE' ORDER BY id")) {
            while (rows.next()) result.computeIfAbsent(rows.getLong(2), ignored -> new ArrayList<>()).add(rows.getLong(1));
        }
        return result;
    }

    private static Map<Long, List<Student>> students(Connection connection) throws SQLException {
        Map<Long, List<Student>> result = new HashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,department_id,created_at,updated_at,status FROM users WHERE username REGEXP '^sv20[0-9]{2}c[0-9]{4}$' ORDER BY id")) {
            while (rows.next()) result.computeIfAbsent(rows.getLong(2), ignored -> new ArrayList<>()).add(new Student(rows.getLong(1),
                    rows.getTimestamp(3).toLocalDateTime().toLocalDate(),
                    rows.getString(5).equals("INACTIVE") ? rows.getTimestamp(4).toLocalDateTime().toLocalDate() : null));
        }
        return result;
    }

    private static List<Long> slots(Connection connection) throws SQLException {
        List<Long> result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id FROM time_slots ORDER BY sort_order")) {
            while (rows.next()) result.add(rows.getLong(1));
        }
        if (result.size() != 6) throw new SQLException("Expected exactly six teaching slots");
        return result;
    }

    private static long id(Connection connection, String sql) throws SQLException { return scalar(connection, sql); }
    private static long scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new SQLException("Required database row is missing");
            return rows.getLong(1);
        }
    }
    private static String key(long user, int day, int block) { return user + "/" + day + "/" + block; }
    private static LocalDate dateBeforeStart(LocalDate start, int days) {
        LocalDate planned = start.minusDays(days);
        return planned.isAfter(AS_OF) ? AS_OF : planned;
    }
    private static String stamp(LocalDate date) { return date + " 08:00:00"; }
    private static void audit(Writer writer, long actor, String action, String entity, long id, LocalDate day) throws SQLException {
        writer.insert("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                actor, action, entity, id, DETAILS, stamp(day));
    }

    private static final class Period {
        final String key;
        final int academicYear, term;
        final List<Long> semesterIds = new ArrayList<>();
        final Set<Long> roomIds = new HashSet<>();
        final Set<String> teacherBusy = new HashSet<>(), studentBusy = new HashSet<>();
        final int[][] slotCounts = new int[6][3];
        long semesterId;
        LocalDate start, end;
        Period(String key, int academicYear, int term) { this.key = key; this.academicYear = academicYear; this.term = term; }
    }
    private record Room(long id, String code, String type, int capacity) { }
    private record Course(long id, long departmentId) { }
    private record Student(long id, LocalDate created, LocalDate left) { }
    private record Placement(int day, int block, long teacherId, List<Long> studentIds) { }

    private static final class Writer implements AutoCloseable {
        private final Connection connection;
        private final Map<String, PreparedStatement> statements = new HashMap<>();
        Writer(Connection connection) { this.connection = connection; }
        long insert(String sql, Object... values) throws SQLException {
            PreparedStatement statement = statements.get(sql);
            if (statement == null) {
                statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                statements.put(sql, statement);
            }
            statement.clearParameters();
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            if (statement.executeUpdate() != 1) throw new SQLException("Insert did not affect exactly one row");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Generated key is missing");
                return keys.getLong(1);
            }
        }
        @Override public void close() throws SQLException {
            SQLException failure = null;
            for (PreparedStatement statement : statements.values()) try { statement.close(); }
            catch (SQLException ex) { if (failure == null) failure = ex; else failure.addSuppressed(ex); }
            if (failure != null) throw failure;
        }
    }
}
