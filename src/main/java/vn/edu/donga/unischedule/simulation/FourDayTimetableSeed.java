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
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Deterministic, insert-only teaching timetable expansion for every usable room and term. */
public final class FourDayTimetableSeed {
    private static final String MARKER = "ROOM_FOUR_DAYS_V1";
    private static final String SOURCE = "ROOM_FOUR_DAYS";
    private static final String DETAILS = "{\"seed\":\"" + SOURCE + "\"}";
    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 23);
    private static final long SEED = 20260923L;
    private static final int DAYS_PER_ROOM = 4;
    private static final int STUDENTS_PER_SECTION = 6;
    private static final String[] FAMILY = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Võ", "Đặng", "Bùi"};
    private static final String[] GIVEN = {"Minh Anh", "Gia Huy", "Thu Hà", "Hoàng Nam", "Thanh Trúc", "Đức Minh", "Ngọc Linh", "Quốc Bảo", "Anh Tuấn", "Thảo Vy"};

    private FourDayTimetableSeed() { }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || (args.length == 1 && !List.of("--apply", "--dry-run", "--verify").contains(args[0])))
            throw new IllegalArgumentException("Use --apply, --dry-run, --verify, or no argument for preview");
        try (Connection connection = new ConnectionFactory().open()) {
            if (args.length == 0) printCoverage(connection);
            else if (args[0].equals("--verify")) { verify(connection); printCoverage(connection); }
            else {
                boolean inserted = apply(connection, args[0].equals("--apply"));
                System.out.println(!inserted ? "ALREADY_APPLIED" : args[0].equals("--apply") ? "APPLIED" : "VALIDATED_AND_ROLLED_BACK");
                printCoverage(connection);
            }
        }
    }

    public static boolean apply(Connection connection, boolean commit) throws Exception {
        if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='ROOM_OCCUPANCY_60_V1'") != 1)
            throw new SQLException("The room occupancy seed is required first");
        String lockName = "room_four_days_" + connection.getCatalog();
        try (PreparedStatement lock = connection.prepareStatement("SELECT GET_LOCK(?,15)")) {
            lock.setString(1, lockName);
            try (ResultSet result = lock.executeQuery()) {
                result.next();
                if (result.getInt(1) != 1) throw new SQLException("Another timetable import is running");
            }
        }
        boolean oldAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            try (Statement lockScheduling = connection.createStatement();
                 ResultSet ignored = lockScheduling.executeQuery("SELECT id FROM roles WHERE code='ACADEMIC' FOR UPDATE")) {
                while (ignored.next()) { /* App scheduling lock. */ }
            }
            if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 0) {
                connection.rollback();
                return false;
            }
            List<Period> periods = periods(connection);
            if (periods.size() != 12) throw new SQLException("Expected twelve academic periods from 2021 to 2026");
            List<Room> rooms = rooms(connection);
            if (rooms.size() < 990) throw new SQLException("Unexpected number of usable rooms");
            List<Long> slots = slots(connection);
            long academic = scalar(connection, "SELECT id FROM users WHERE username='daotao'");
            long lecturerRole = scalar(connection, "SELECT id FROM roles WHERE code='LECTURER'");
            long studentRole = scalar(connection, "SELECT id FROM roles WHERE code='STUDENT'");
            List<Long> departmentIds = departments(connection);
            int addedSections = 0, addedSchedules = 0;
            try (Writer writer = new Writer(connection)) {
                addLaboratoryCourses(connection, writer, departmentIds, academic);
                Map<String, List<Course>> courses = courses(connection);
                addPeople(writer, departmentIds, lecturerRole, studentRole, academic);
                Map<Long, List<Long>> teachers = teachers(connection);
                Map<Long, List<Student>> students = students(connection);
                indexExisting(connection, periods);
                for (Period period : periods) {
                    List<Room> roomOrder = new ArrayList<>(rooms);
                    Collections.shuffle(roomOrder, new Random(SEED ^ period.key.hashCode()));
                    int ordinal = 0;
                    for (Room room : roomOrder) {
                        Set<Integer> roomDays = period.roomDays.computeIfAbsent(room.id, ignored -> new HashSet<>());
                        List<Course> matches = courses.get(room.type);
                        if (matches == null || matches.isEmpty()) throw new SQLException("No matching course for room " + room.code);
                        Course course = matches.get(Math.floorMod((int) room.id + ordinal, matches.size()));
                        List<Long> departmentTeachers = teachers.get(course.departmentId);
                        List<Student> departmentStudents = students.get(course.departmentId);
                        if (departmentTeachers == null || departmentStudents == null) throw new SQLException("Missing people for department " + course.departmentId);
                        while (roomDays.size() < DAYS_PER_ROOM) {
                            Placement placement = place(period, room, departmentTeachers, departmentStudents,
                                    Math.min(2, DAYS_PER_ROOM - roomDays.size()), ordinal);
                            if (placement == null) throw new SQLException("Cannot fill four weekdays for " + room.code + " in " + period.key);
                            LocalDate created = beforeStart(period.start, 14);
                            String sectionCode = "LHD" + period.year + "-" + period.term + "-" + room.code + "-"
                                    + placement.days.get(0).day + (placement.days.size() == 2 ? placement.days.get(1).day : "");
                            long section = writer.insert("INSERT INTO course_sections(course_id,semester_id,code,capacity,student_count,status,created_at) VALUES (?,?,?,?,?,?,?)",
                                    course.id, period.semesterId, sectionCode, room.capacity, placement.students.size(),
                                    period.end.isBefore(AS_OF) ? "CLOSED" : "SCHEDULED", stamp(created));
                            long assignment = writer.insert("INSERT INTO lecturer_assignments(course_section_id,lecturer_id,assigned_at) VALUES (?,?,?)",
                                    section, placement.teacher, stamp(created));
                            for (long student : placement.students)
                                writer.batch("INSERT INTO student_enrollments(course_section_id,student_id,enrolled_at,status) VALUES (?,?,?,?)",
                                        section, student, stamp(beforeStart(period.start, 7)), "ACTIVE");
                            for (DayBlock day : placement.days) {
                                long schedule = writer.insert("INSERT INTO schedules(course_section_id,lecturer_assignment_id,classroom_id,start_slot_id,end_slot_id,day_of_week,start_date,end_date,status,note,created_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                        section, assignment, room.id, slots.get(day.block * 2), slots.get(day.block * 2 + 1), day.day,
                                        period.start.toString(), period.end.toString(), "PUBLISHED", "Lịch học chính khóa",
                                        academic, stamp(beforeStart(period.start, 5)), stamp(beforeStart(period.start, 5)));
                                writer.batch("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                                        academic, "PUBLISH_SCHEDULE", "schedules", schedule, DETAILS, stamp(beforeStart(period.start, 5)));
                                roomDays.add(day.day);
                                period.teacherBusy.add(key(placement.teacher, day.day, day.block));
                                for (long student : placement.students) period.studentBusy.add(key(student, day.day, day.block));
                                period.slotCounts[day.day - 2][day.block]++;
                                addedSchedules++;
                            }
                            writer.batch("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                                    academic, "CREATE_SECTION", "course_sections", section, DETAILS, stamp(created));
                            addedSections++;
                            ordinal++;
                        }
                    }
                }
                writer.flush();
                writer.insert("INSERT INTO audit_logs(user_id,action,entity_type,details,created_at) VALUES (?,?,?,?,?)",
                        academic, MARKER, "database", "{\"seed\":\"" + SOURCE + "\",\"random_seed\":" + SEED
                                + ",\"new_sections\":" + addedSections + ",\"new_schedules\":" + addedSchedules + "}", stamp(AS_OF));
            }
            verifyCoverage(connection, rooms, periods);
            verifyNewSections(connection);
            verifyNewConflicts(connection);
            if (commit) connection.commit(); else connection.rollback();
            return true;
        } catch (Exception failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
            try (PreparedStatement release = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                release.setString(1, lockName);
                release.execute();
            }
        }
    }

    public static void verify(Connection connection) throws SQLException {
        if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 1)
            throw new SQLException("Four-day timetable marker is missing");
        verifyCoverage(connection, rooms(connection), periods(connection));
        verifyNewSections(connection);
        verifyNewConflicts(connection);
    }

    private static void addLaboratoryCourses(Connection connection, Writer writer, List<Long> departments, long academic) throws SQLException {
        if (scalar(connection, "SELECT COUNT(*) FROM courses WHERE required_room_type='LABORATORY' AND status='ACTIVE'") > 0) return;
        String[] names = {"Thực hành nghiên cứu số", "Thực hành phân tích ứng dụng", "Thực hành ngôn ngữ"};
        for (int index = 0; index < departments.size(); index++) {
            long course = writer.insert("INSERT INTO courses(department_id,code,name,credits,required_room_type,status) VALUES (?,?,?,?,?,?)",
                    departments.get(index), "LAB" + (210 + index), names[index], 3, "LABORATORY", "ACTIVE");
            writer.batch("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                    academic, "CREATE_COURSE", "courses", course, DETAILS, stamp(LocalDate.of(2021, 9, 24)));
        }
        writer.flush();
    }

    private static void addPeople(Writer writer, List<Long> departments, long lecturerRole, long studentRole, long academic) throws SQLException {
        for (int index = 0; index < 225; index++) {
            long department = departments.get(index % departments.size());
            String username = "gv" + (4001 + index);
            LocalDate created = LocalDate.of(2021, 9, 24);
            long user = writer.insert("INSERT INTO users(department_id,username,password_hash,full_name,email,phone,lecturer_code,status,last_login_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    department, username, HistoricalSeedGenerator.DEMO_HASH, name(index), username + "@donga.edu.vn",
                    phone(index), username.toUpperCase(Locale.ROOT), "ACTIVE", stamp(AS_OF.minusDays(index % 91)), stamp(created), stamp(created));
            writer.batch("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)", user, lecturerRole);
            writer.batch("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                    academic, "CREATE_USER", "users", user, DETAILS, stamp(created));
        }
        for (int year = 2021; year <= 2026; year++) {
            int size = year == 2021 ? 1800 : 450;
            for (int index = 0; index < size; index++) {
                long department = departments.get(index % departments.size());
                String username = "sv" + year + "w" + String.format(Locale.ROOT, "%04d", index + 1);
                LocalDate created = year == 2021 ? LocalDate.of(2021, 9, 24) : LocalDate.of(year, 8, 25);
                LocalDate left = year <= 2022 ? LocalDate.of(year + 4, 7, 15) : null;
                LocalDate login = left == null ? AS_OF.minusDays(index % 250) : left.minusDays(1 + index % 180);
                if (login.isBefore(created)) login = created.plusDays(1);
                long user = writer.insert("INSERT INTO users(department_id,username,password_hash,full_name,email,phone,student_code,class_code,status,last_login_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                        department, username, HistoricalSeedGenerator.DEMO_HASH, name(index + year), username + "@sv.donga.edu.vn",
                        phone(1000 + (year - 2021) * 1800 + index), username.toUpperCase(Locale.ROOT),
                        "K" + (year % 100) + "-" + (index % 3 + 1), left == null ? "ACTIVE" : "INACTIVE",
                        stamp(login), stamp(created), stamp(left == null ? created : left));
                writer.batch("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)", user, studentRole);
                writer.batch("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details,created_at) VALUES (?,?,?,?,?,?)",
                        academic, "CREATE_USER", "users", user, DETAILS, stamp(created));
            }
        }
        writer.flush();
    }

    private static Placement place(Period period, Room room, List<Long> teachers, List<Student> students, int desired, int ordinal) {
        List<Integer> possibleDays = new ArrayList<>();
        for (int day = 2; day <= 7; day++) if (!period.roomDays.get(room.id).contains(day)) possibleDays.add(day);
        Collections.shuffle(possibleDays, new Random(SEED ^ room.id ^ period.key.hashCode() ^ ordinal));
        possibleDays.sort(Comparator.comparingInt(day -> period.dayCount[day - 2]));
        for (int firstIndex = 0; firstIndex < possibleDays.size(); firstIndex++) {
            int first = possibleDays.get(firstIndex);
            int secondLimit = desired == 1 ? firstIndex + 1 : possibleDays.size();
            for (int secondIndex = firstIndex + 1; secondIndex <= secondLimit; secondIndex++) {
                if (desired == 2 && secondIndex >= possibleDays.size()) break;
                int second = desired == 1 ? -1 : possibleDays.get(secondIndex);
                Placement candidate = placeOnDays(period, room, teachers, students, first, second, ordinal);
                if (candidate != null) {
                    period.dayCount[first - 2]++;
                    if (second != -1) period.dayCount[second - 2]++;
                    return candidate;
                }
            }
        }
        return desired == 2 ? place(period, room, teachers, students, 1, ordinal) : null;
    }

    private static Placement placeOnDays(Period period, Room room, List<Long> teachers, List<Student> students,
                                         int first, int second, int ordinal) {
        List<int[]> blockPairs = new ArrayList<>();
        for (int a = 0; a < 3; a++) for (int b = 0; b < (second == -1 ? 1 : 3); b++) blockPairs.add(new int[]{a, b});
        blockPairs.sort(Comparator.comparingInt(pair -> period.slotCounts[first - 2][pair[0]]
                + (second == -1 ? 0 : period.slotCounts[second - 2][pair[1]])));
        for (int[] blocks : blockPairs) {
            Long teacher = null;
            for (int offset = 0; offset < teachers.size(); offset++) {
                long candidate = teachers.get(Math.floorMod(ordinal + offset, teachers.size()));
                if (!period.teacherBusy.contains(key(candidate, first, blocks[0]))
                        && (second == -1 || !period.teacherBusy.contains(key(candidate, second, blocks[1])))) {
                    teacher = candidate; break;
                }
            }
            if (teacher == null) continue;
            List<Long> chosen = new ArrayList<>();
            for (int offset = 0; offset < students.size() && chosen.size() < STUDENTS_PER_SECTION; offset++) {
                Student student = students.get(Math.floorMod((int) room.id + ordinal * 11 + offset, students.size()));
                if (student.created.isAfter(period.start) || student.left != null && student.left.isBefore(period.start)) continue;
                if (period.studentBusy.contains(key(student.id, first, blocks[0]))
                        || second != -1 && period.studentBusy.contains(key(student.id, second, blocks[1]))) continue;
                chosen.add(student.id);
            }
            if (chosen.size() == STUDENTS_PER_SECTION) {
                List<DayBlock> days = second == -1 ? List.of(new DayBlock(first, blocks[0]))
                        : List.of(new DayBlock(first, blocks[0]), new DayBlock(second, blocks[1]));
                return new Placement(days, teacher, chosen);
            }
        }
        return null;
    }

    private static void verifyCoverage(Connection connection, List<Room> rooms, List<Period> periods) throws SQLException {
        indexExisting(connection, periods);
        for (Period period : periods) for (Room room : rooms)
            if (period.roomDays.getOrDefault(room.id, Set.of()).size() < DAYS_PER_ROOM)
                throw new SQLException("Room " + room.code + " has fewer than four weekdays in " + period.key);
    }

    private static void verifyNewSections(Connection connection) throws SQLException {
        if (scalar(connection, "SELECT COUNT(*) FROM course_sections cs WHERE cs.code LIKE 'LHD20%' AND cs.student_count<>(SELECT COUNT(*) FROM student_enrollments e WHERE e.course_section_id=cs.id AND e.status='ACTIVE')") != 0)
            throw new SQLException("New section student totals do not match active enrollments");
        if (scalar(connection, "SELECT COUNT(*) FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id WHERE cs.code LIKE 'LHD20%' AND (s.status<>'PUBLISHED' OR s.created_by<>(SELECT id FROM users WHERE username='daotao'))") != 0)
            throw new SQLException("New timetables are not published by academic staff");
    }

    private static void verifyNewConflicts(Connection connection) throws SQLException {
        Map<Long, String> bySemester = new HashMap<>();
        for (Period period : periods(connection)) for (long semester : period.semesterIds) bySemester.put(semester, period.key);
        Map<String, Boolean> rooms = new HashMap<>(), teachers = new HashMap<>(), students = new HashMap<>();
        String schedules = "SELECT cs.semester_id,s.classroom_id,la.lecturer_id,s.day_of_week,ss.sort_order,es.sort_order,cs.code "
                + "FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id "
                + "JOIN lecturer_assignments la ON la.id=s.lecturer_assignment_id "
                + "JOIN time_slots ss ON ss.id=s.start_slot_id JOIN time_slots es ON es.id=s.end_slot_id WHERE s.status<>'CANCELLED'";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(schedules)) {
            while (rows.next()) {
                String period = bySemester.get(rows.getLong(1));
                if (period == null) continue;
                boolean fresh = rows.getString(7).startsWith("LHD20");
                for (int block = 0; block < 3; block++) if (rows.getInt(5) <= block * 2 + 2 && rows.getInt(6) >= block * 2 + 1) {
                    checked(rooms, period + "/" + key(rows.getLong(2), rows.getInt(4), block), fresh, "room");
                    checked(teachers, period + "/" + key(rows.getLong(3), rows.getInt(4), block), fresh, "lecturer");
                }
            }
        }
        String enrollments = "SELECT cs.semester_id,e.student_id,s.day_of_week,ss.sort_order,es.sort_order,cs.code "
                + "FROM student_enrollments e JOIN schedules s ON s.course_section_id=e.course_section_id "
                + "JOIN course_sections cs ON cs.id=e.course_section_id "
                + "JOIN time_slots ss ON ss.id=s.start_slot_id JOIN time_slots es ON es.id=s.end_slot_id "
                + "WHERE e.status='ACTIVE' AND s.status<>'CANCELLED'";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(enrollments)) {
            while (rows.next()) {
                String period = bySemester.get(rows.getLong(1));
                if (period == null) continue;
                boolean fresh = rows.getString(6).startsWith("LHD20");
                for (int block = 0; block < 3; block++) if (rows.getInt(4) <= block * 2 + 2 && rows.getInt(5) >= block * 2 + 1)
                    checked(students, period + "/" + key(rows.getLong(2), rows.getInt(3), block), fresh, "student");
            }
        }
    }

    private static void checked(Map<String, Boolean> seen, String key, boolean fresh, String type) throws SQLException {
        Boolean prior = seen.putIfAbsent(key, fresh);
        if (prior != null && (prior || fresh)) throw new SQLException("New " + type + " schedule conflict: " + key);
    }

    private static void printCoverage(Connection connection) throws SQLException {
        List<Period> periods = periods(connection);
        List<Room> rooms = rooms(connection);
        indexExisting(connection, periods);
        System.out.println("Usable rooms with at least four scheduled weekdays (target " + rooms.size() + "):");
        for (Period period : periods) {
            long complete = rooms.stream().filter(room -> period.roomDays.getOrDefault(room.id, Set.of()).size() >= DAYS_PER_ROOM).count();
            System.out.println(period.key + "  " + complete + "/" + rooms.size());
        }
    }

    private static void indexExisting(Connection connection, List<Period> periods) throws SQLException {
        Map<Long, Period> bySemester = new HashMap<>();
        for (Period period : periods) {
            period.roomDays.clear(); period.teacherBusy.clear(); period.studentBusy.clear();
            for (int[] counts : period.slotCounts) java.util.Arrays.fill(counts, 0);
            java.util.Arrays.fill(period.dayCount, 0);
            for (long semester : period.semesterIds) bySemester.put(semester, period);
        }
        String schedules = "SELECT cs.semester_id,s.classroom_id,la.lecturer_id,s.day_of_week,ss.sort_order,es.sort_order "
                + "FROM schedules s JOIN course_sections cs ON cs.id=s.course_section_id "
                + "JOIN lecturer_assignments la ON la.id=s.lecturer_assignment_id "
                + "JOIN time_slots ss ON ss.id=s.start_slot_id JOIN time_slots es ON es.id=s.end_slot_id WHERE s.status<>'CANCELLED'";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(schedules)) {
            while (rows.next()) {
                Period period = bySemester.get(rows.getLong(1));
                if (period == null) continue;
                int day = rows.getInt(4);
                period.roomDays.computeIfAbsent(rows.getLong(2), ignored -> new HashSet<>()).add(day);
                if (day >= 2 && day <= 7) period.dayCount[day - 2]++;
                for (int block = 0; block < 3; block++) if (rows.getInt(5) <= block * 2 + 2 && rows.getInt(6) >= block * 2 + 1) {
                    period.teacherBusy.add(key(rows.getLong(3), day, block));
                    if (day >= 2 && day <= 7) period.slotCounts[day - 2][block]++;
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

    private static List<Long> departments(Connection connection) throws SQLException {
        List<Long> result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id FROM departments ORDER BY id")) {
            while (rows.next()) result.add(rows.getLong(1));
        }
        if (result.size() != 3) throw new SQLException("Expected three existing departments");
        return result;
    }

    private static Map<Long, List<Long>> teachers(Connection connection) throws SQLException {
        Map<Long, List<Long>> result = new HashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,department_id FROM users WHERE (username REGEXP '^gv1[0-9]{3}$' OR username REGEXP '^gv4[0-9]{3}$') AND status='ACTIVE' ORDER BY id")) {
            while (rows.next()) result.computeIfAbsent(rows.getLong(2), ignored -> new ArrayList<>()).add(rows.getLong(1));
        }
        return result;
    }

    private static Map<Long, List<Student>> students(Connection connection) throws SQLException {
        Map<Long, List<Student>> result = new HashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,department_id,created_at,updated_at,status FROM users WHERE username REGEXP '^sv20[0-9]{2}[cw][0-9]{4}$' ORDER BY id")) {
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
        if (result.size() != 6) throw new SQLException("Expected six teaching slots");
        return result;
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new SQLException("Required database row is missing");
            return rows.getLong(1);
        }
    }
    private static String key(long user, int day, int block) { return user + "/" + day + "/" + block; }
    private static String name(int index) { return FAMILY[index % FAMILY.length] + " " + GIVEN[(index / FAMILY.length) % GIVEN.length]; }
    private static String phone(int index) { return String.format(Locale.ROOT, "09%08d", 50000000 + index); }
    private static LocalDate beforeStart(LocalDate start, int days) {
        LocalDate planned = start.minusDays(days);
        return planned.isAfter(AS_OF) ? AS_OF : planned;
    }
    private static String stamp(LocalDate day) { return day + " 08:00:00"; }

    private static final class Period {
        final String key;
        final int year, term;
        final List<Long> semesterIds = new ArrayList<>();
        final Map<Long, Set<Integer>> roomDays = new HashMap<>();
        final Set<String> teacherBusy = new HashSet<>(), studentBusy = new HashSet<>();
        final int[][] slotCounts = new int[6][3];
        final int[] dayCount = new int[6];
        long semesterId;
        LocalDate start, end;
        Period(String key, int year, int term) { this.key = key; this.year = year; this.term = term; }
    }
    private record Room(long id, String code, String type, int capacity) { }
    private record Course(long id, long departmentId) { }
    private record Student(long id, LocalDate created, LocalDate left) { }
    private record DayBlock(int day, int block) { }
    private record Placement(List<DayBlock> days, long teacher, List<Long> students) { }

    private static final class Writer implements AutoCloseable {
        private final Connection connection;
        private final Map<String, PreparedStatement> inserts = new HashMap<>(), batches = new HashMap<>();
        private final Map<String, Integer> batchCounts = new HashMap<>();
        Writer(Connection connection) { this.connection = connection; }
        long insert(String sql, Object... values) throws SQLException {
            PreparedStatement statement = inserts.get(sql);
            if (statement == null) { statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); inserts.put(sql, statement); }
            bind(statement, values);
            if (statement.executeUpdate() != 1) throw new SQLException("Insert did not affect exactly one row");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Generated key is missing");
                return keys.getLong(1);
            }
        }
        void batch(String sql, Object... values) throws SQLException {
            PreparedStatement statement = batches.get(sql);
            if (statement == null) { statement = connection.prepareStatement(sql); batches.put(sql, statement); }
            bind(statement, values);
            statement.addBatch();
            int count = batchCounts.merge(sql, 1, Integer::sum);
            if (count >= 300) { statement.executeBatch(); batchCounts.put(sql, 0); }
        }
        void flush() throws SQLException {
            for (var item : batches.entrySet()) if (batchCounts.getOrDefault(item.getKey(), 0) > 0) {
                item.getValue().executeBatch(); batchCounts.put(item.getKey(), 0);
            }
        }
        private static void bind(PreparedStatement statement, Object[] values) throws SQLException {
            statement.clearParameters();
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
        }
        @Override public void close() throws SQLException {
            flush();
            for (PreparedStatement statement : inserts.values()) statement.close();
            for (PreparedStatement statement : batches.values()) statement.close();
        }
    }
}
