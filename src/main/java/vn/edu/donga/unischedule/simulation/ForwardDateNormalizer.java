package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Aligns dates of synthetic timetable rows with their 2026-2030 teaching periods. */
public final class ForwardDateNormalizer {
    private static final String MARKER = "FORWARD_DATES_2026_2030_V2";
    private ForwardDateNormalizer() { }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "preview" : args[0];
        if (!List.of("preview", "apply", "dry-run", "verify").contains(mode))
            throw new IllegalArgumentException("Use preview, apply, dry-run, or verify");
        try (Connection connection = new ConnectionFactory().open()) {
            if (mode.equals("verify")) verify(connection);
            else if (!mode.equals("preview")) {
                boolean changed = apply(connection, mode.equals("apply"));
                System.out.println(!changed ? "DATES_ALREADY_NORMALIZED" : mode.equals("apply")
                        ? "DATES_NORMALIZED" : "DATES_VALIDATED_AND_ROLLED_BACK");
            }
            print(connection);
        }
    }

    static boolean apply(Connection connection, boolean commit) throws Exception {
        boolean oldAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 0) {
                connection.rollback(); return false;
            }
            if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='FORWARD_FIVE_YEARS_V1'") != 1)
                throw new SQLException("Forward timetable seed is required first");
            update(connection, "UPDATE course_sections cs JOIN semesters se ON se.id=cs.semester_id "
                    + "SET cs.created_at=GREATEST(DATE_SUB(se.start_date,INTERVAL 14 DAY),MAKEDATE(YEAR(se.start_date),1)) "
                    + "WHERE cs.code LIKE 'LHD20%' AND YEAR(se.start_date) BETWEEN 2026 AND 2030");
            update(connection, "UPDATE lecturer_assignments la JOIN course_sections cs ON cs.id=la.course_section_id "
                    + "JOIN semesters se ON se.id=cs.semester_id SET la.assigned_at=GREATEST(DATE_SUB(se.start_date,INTERVAL 14 DAY),MAKEDATE(YEAR(se.start_date),1)) "
                    + "WHERE cs.code LIKE 'LHD20%' AND YEAR(se.start_date) BETWEEN 2026 AND 2030");
            update(connection, "UPDATE student_enrollments e JOIN course_sections cs ON cs.id=e.course_section_id "
                    + "JOIN semesters se ON se.id=cs.semester_id SET e.enrolled_at=DATE_SUB(se.start_date,INTERVAL 7 DAY) "
                    + "WHERE cs.code LIKE 'LHD20%' AND YEAR(se.start_date) BETWEEN 2026 AND 2030");
            update(connection, "UPDATE schedules s JOIN course_sections cs ON cs.id=s.course_section_id "
                    + "JOIN semesters se ON se.id=cs.semester_id SET s.created_at=DATE_SUB(se.start_date,INTERVAL 5 DAY),"
                    + "s.updated_at=DATE_SUB(se.start_date,INTERVAL 5 DAY) "
                    + "WHERE cs.code LIKE 'LHD20%' AND YEAR(se.start_date) BETWEEN 2026 AND 2030");
            long academic = scalar(connection, "SELECT id FROM users WHERE username='daotao'");
            try (var statement = connection.prepareStatement("INSERT INTO audit_logs(user_id,action,entity_type,details,created_at) VALUES (?,?,'database',CAST(? AS JSON),'2026-09-23 12:30:00')")) {
                statement.setLong(1, academic); statement.setString(2, MARKER);
                statement.setString(3, "{\"seed\":\"FORWARD_2026_2030\",\"purpose\":\"align simulated row dates with semester\"}");
                statement.executeUpdate();
            }
            verifyRows(connection);
            if (commit) connection.commit(); else connection.rollback();
            return true;
        } catch (Exception failure) {
            connection.rollback(); throw failure;
        } finally { connection.setAutoCommit(oldAutoCommit); }
    }

    static void verify(Connection connection) throws SQLException {
        if (scalar(connection, "SELECT COUNT(*) FROM audit_logs WHERE action='" + MARKER + "'") != 1)
            throw new SQLException("Forward date marker is missing");
        verifyRows(connection);
    }

    private static void verifyRows(Connection connection) throws SQLException {
        long invalid = scalar(connection, "SELECT COUNT(*) FROM course_sections cs JOIN semesters se ON se.id=cs.semester_id "
                + "WHERE cs.code LIKE 'LHD20%' AND YEAR(se.start_date) BETWEEN 2026 AND 2030 "
                + "AND DATE(cs.created_at)<>GREATEST(DATE_SUB(se.start_date,INTERVAL 14 DAY),MAKEDATE(YEAR(se.start_date),1))");
        if (invalid != 0) throw new SQLException("Synthetic section dates are not aligned with semesters");
        invalid = scalar(connection, "SELECT COUNT(*) FROM student_enrollments e JOIN course_sections cs ON cs.id=e.course_section_id "
                + "JOIN semesters se ON se.id=cs.semester_id WHERE cs.code LIKE 'LHD20%' "
                + "AND YEAR(se.start_date) BETWEEN 2026 AND 2030 AND DATE(e.enrolled_at)<>DATE_SUB(se.start_date,INTERVAL 7 DAY)");
        if (invalid != 0) throw new SQLException("Synthetic enrollment dates are not aligned with semesters");
    }

    private static void print(Connection connection) throws SQLException {
        System.out.println("Forward sections and enrollments by recorded year:");
        for (int year = 2026; year <= 2030; year++) System.out.println(year + " sections="
                + scalar(connection, "SELECT COUNT(*) FROM course_sections WHERE YEAR(created_at)=" + year)
                + " enrollments=" + scalar(connection, "SELECT COUNT(*) FROM student_enrollments WHERE YEAR(enrolled_at)=" + year));
    }
    private static long scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) { rows.next(); return rows.getLong(1); }
    }
    private static void update(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.executeUpdate(sql); }
    }
}
