package vn.edu.donga.unischedule.simulation;

import java.util.*;

/** Ordered, synthetic relational rows; deliberately independent of application repositories. */
public final class SimulationData {
    public static final List<String> TABLES = List.of("roles", "departments", "users", "user_roles", "semesters",
            "time_slots", "courses", "course_sections", "lecturer_assignments", "student_enrollments",
            "classrooms", "equipment", "classroom_equipment", "schedules", "change_requests", "notifications",
            "audit_logs", "maintenance_records");
    public final Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
    public final List<Map<String, Object>> userEvents = new ArrayList<>();
    public final List<Map<String, Object>> annual = new ArrayList<>(), monthly = new ArrayList<>();
    public SimulationData() { TABLES.forEach(t -> tables.put(t, new ArrayList<>())); }
    public static Map<String, Object> row(Object... pairs) {
        var row = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) row.put((String)pairs[i], pairs[i + 1]);
        return row;
    }
    public Map<String, Object> add(String table, Object... pairs) {
        var row = row(pairs);
        if (!table.equals("user_roles") && !row.containsKey("id")) row.put("id", rows(table).size() + 1);
        rows(table).add(row); return row;
    }
    public List<Map<String, Object>> rows(String table) { return tables.get(table); }
    public Map<String, Object> get(String table, Object id) {
        return rows(table).stream().filter(r -> Objects.equals(r.get("id"), id)).findFirst().orElseThrow();
    }
    public static int n(Map<String, Object> row, String key) { return ((Number)row.get(key)).intValue(); }
    public static String s(Map<String, Object> row, String key) { return Objects.toString(row.get(key), ""); }
}
