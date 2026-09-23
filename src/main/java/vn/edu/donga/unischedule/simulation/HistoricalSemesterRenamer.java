package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Renames only the eleven completed historical-seed semesters; IDs and relationships stay intact. */
public final class HistoricalSemesterRenamer {
    public static final String MARKER = "RENAME_HISTORY_SEMESTERS_V1";
    private HistoricalSemesterRenamer() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("No options accepted");
        try (Connection connection = new ConnectionFactory().open()) {
            System.out.println(apply(connection) ? "Renamed 11 historical semesters." : "ALREADY_RENAMED: no rows changed.");
        }
    }

    public static boolean apply(Connection connection) throws SQLException {
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if (count(connection, HistoricalSeedImporter.MARKER) != 1)
                throw new SQLException("The completed history seed marker is required");
            if (count(connection, MARKER) > 0) {
                connection.rollback();
                return false;
            }
            List<Row> rows = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, code, name, start_date FROM semesters WHERE code LIKE 'XUAN-%' OR code LIKE 'THU-%' ORDER BY id FOR UPDATE");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) rows.add(new Row(result.getLong(1), result.getString(2), result.getString(3), result.getDate(4).toLocalDate()));
            }
            if (rows.size() != 11) throw new SQLException("Expected exactly 11 historical semesters; no changes made");
            for (Row row : rows) {
                String[] parts = row.code().split("-");
                if (parts.length != 2 || !(parts[0].equals("XUAN") || parts[0].equals("THU")))
                    throw new SQLException("Unexpected semester code: " + row.code());
                int year;
                try { year = Integer.parseInt(parts[1]); }
                catch (NumberFormatException ex) { throw new SQLException("Unexpected semester code: " + row.code(), ex); }
                int half = parts[0].equals("XUAN") ? 1 : 2;
                if (year != row.startDate().getYear() || (half == 1) != (row.startDate().getMonthValue() <= 6))
                    throw new SQLException("Semester code and dates differ: " + row.code());
                String oldName = "Học kỳ " + (half == 1 ? "xuân " : "thu ") + year;
                String newName = HistoricalDisplayNames.semesterName(year, half);
                if (!row.name().equals(oldName) && !row.name().equals(newName))
                    throw new SQLException("Semester was edited outside the seed: " + row.code());
                if (row.name().equals(oldName)) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE semesters SET name=? WHERE id=? AND code=? AND name=?")) {
                        update.setString(1, newName);
                        update.setLong(2, row.id());
                        update.setString(3, row.code());
                        update.setString(4, oldName);
                        if (update.executeUpdate() != 1) throw new SQLException("Semester changed during rename: " + row.code());
                    }
                }
            }
            try (PreparedStatement marker = connection.prepareStatement(
                    "INSERT INTO audit_logs(action, entity_type, details, created_at) VALUES (?, 'database', JSON_OBJECT('seed','HISTORY_5Y','purpose','semester labels'), UTC_TIMESTAMP())")) {
                marker.setString(1, MARKER);
                marker.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private static int count(Connection connection, String action) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action=?")) {
            statement.setString(1, action);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private record Row(long id, String code, String name, LocalDate startDate) { }
}
