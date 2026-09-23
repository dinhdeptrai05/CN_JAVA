package vn.edu.donga.unischedule.simulation;

import vn.edu.donga.unischedule.config.DatabaseConfig;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

/** Captures aggregates/catalogs only. No names, emails, hashes or avatars of real users. */
public final class SimulationBaseline {
    static final List<String> CATALOGS = List.of("roles", "departments", "time_slots", "courses", "classrooms", "equipment", "classroom_equipment");
    final Properties properties;
    public SimulationBaseline(Path path) throws IOException {
        properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
    }
    public int count(String role) { return Integer.parseInt(properties.getProperty("active." + role)); }
    public void populate(SimulationData data) {
        for (String table : CATALOGS) {
            int count = Integer.parseInt(properties.getProperty(table + ".size"));
            for (int i = 0; i < count; i++) {
                var row = new LinkedHashMap<String, Object>();
                String prefix = table + "." + i + ".";
                for (String key : properties.getProperty(table + ".columns").split(",")) {
                    String value = properties.getProperty(prefix + key);
                    if (value != null) row.put(key, key.equals("id") || key.endsWith("_id") || List.of("capacity", "floor", "credits", "quantity", "sort_order").contains(key) ? Integer.valueOf(value) : value);
                }
                data.rows(table).add(row);
            }
        }
    }
    public static void capture(Path output) throws Exception {
        var p = new TreeMap<String, String>();
        try (Connection c = DriverManager.getConnection(DatabaseConfig.url(), DatabaseConfig.username(), DatabaseConfig.password())) {
            c.setReadOnly(true); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            p.put("source", "READ_ONLY_MYSQL_AGGREGATES_AND_CATALOGS");
            p.put("captured_date", java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString());
            for (String table : TABLES) {
                try (var st = c.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next(); p.put("existing." + table, rs.getString(1));
                }
            }
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT r.code,COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.status='ACTIVE' GROUP BY r.code")) {
                while (rs.next()) p.put("active." + rs.getString(1), rs.getString(2));
            }
            for (String role : List.of("ADMIN", "ACADEMIC", "LECTURER", "STUDENT"))
                if (Integer.parseInt(p.getOrDefault("active." + role, "0")) < 1) throw new IllegalStateException("Baseline requires active " + role);
            // A synthetic person has exactly one role. Reject ambiguous aggregate baselines.
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM (SELECT u.id FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.id WHERE u.status='ACTIVE' GROUP BY u.id HAVING COUNT(ur.role_id)<>1) x")) {
                rs.next(); if (rs.getInt(1) != 0) throw new IllegalStateException("Multiple/missing roles need an explicit baseline mapping");
            }
            for (String table : CATALOGS) {
                try (var st = c.createStatement(); var rs = st.executeQuery("SELECT * FROM " + table + " ORDER BY id")) {
                    var md = rs.getMetaData(); var columns = new ArrayList<String>();
                    for (int k = 1; k <= md.getColumnCount(); k++) {
                        String col = md.getColumnName(k);
                        if (!List.of("created_at", "updated_at", "description", "condition_note").contains(col)) columns.add(col);
                    }
                    p.put(table + ".columns", String.join(",", columns)); int i = 0;
                    while (rs.next()) {
                        for (String col : columns) if (rs.getString(col) != null) p.put(table + "." + i + "." + col, rs.getString(col));
                        i++;
                    }
                    p.put(table + ".size", "" + i);
                }
            }
            c.rollback();
        }
        // CREATE_NEW: even a baseline capture never overwrites a file.
        try (var out = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            for (var entry : p.entrySet()) out.write(entry.getKey() + "=" + entry.getValue().replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r") + "\n");
        }
    }
}
