package vn.edu.donga.unischedule.simulation;

import java.util.Locale;

/** Human-facing identifiers for the seeded campus history. Provenance stays in audit metadata. */
public final class HistoricalDisplayNames {
    private HistoricalDisplayNames() { }

    public static String username(String old, int year) {
        if (old.equals("hist5_admin")) return "qt_hethong";
        if (old.equals("hist5_daotao")) return "pdt_01";
        if (old.startsWith("hist5_gv")) return "gv" + String.format(Locale.ROOT, "%04d", Integer.parseInt(old.substring(8)));
        if (old.startsWith("hist5_sv")) return "sv" + year + String.format(Locale.ROOT, "%04d", Integer.parseInt(old.substring(8)));
        throw new IllegalArgumentException("Unexpected historical username: " + old);
    }

    public static String semesterCode(int year, int half) { return (half == 1 ? "XUAN-" : "THU-") + year; }
    public static String semesterName(int year, int half) {
        return half == 1 ? "Học kỳ 2 - " + (year - 1) : "Học kỳ 1 - " + year;
    }
    public static String courseCode(int department, int subject) {
        return new String[]{"CNTT", "QTKD", "NN"}[department] + (201 + subject);
    }
    public static String sectionCode(String courseCode, int year, int half, int index) {
        return courseCode + "-" + year + (half == 1 ? "X" : "T") + "-" + String.format(Locale.ROOT, "%03d", index);
    }
    public static String roomCode(int oneBasedIndex) {
        int group=(oneBasedIndex-1)/6, within=(oneBasedIndex-1)%6;
        return "EFGH".charAt(group) + String.valueOf(100*(within/3+1)+within%3+1);
    }
    public static String building(int oneBasedIndex) { return "Tòa " + "EFGH".charAt((oneBasedIndex-1)/6); }
    public static String equipmentCode(String roomCode, int oneBasedIndex) {
        return "TB-" + roomCode + "-" + String.format(Locale.ROOT,"%02d",oneBasedIndex);
    }
    public static String email(String username, boolean student) {
        return username + (student ? "@sv.donga.edu.vn" : "@donga.edu.vn");
    }
    public static String classCode(String departmentCode, int year) {
        return departmentCode + String.format(Locale.ROOT,"%02d",year%100) + "B";
    }
}
