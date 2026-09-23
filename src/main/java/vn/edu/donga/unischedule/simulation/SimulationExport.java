package vn.edu.donga.unischedule.simulation;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static vn.edu.donga.unischedule.simulation.SimulationData.*;

public final class SimulationExport {
    private SimulationExport() { }
    public static void write(SimulationData d,Path baseline,Path output,int startYear,long seed) throws Exception {
        SimulationValidator.validate(d);
        if(output.toAbsolutePath().getParent()!=null) Files.createDirectories(output.toAbsolutePath().getParent());
        Files.createDirectory(output); // Atomic refusal of an existing destination.
        String schema="-- SIMULATION ONLY. Import into a NEW, EMPTY simulation database, never unischedule.\n";
        for(String name:List.of("01_create_identity_tables.sql","02_create_academic_tables.sql","03_create_facility_tables.sql","04_create_schedule_tables.sql","05_create_request_tables.sql")) {
            try(var in=SimulationExport.class.getResourceAsStream("/db/"+name)) { schema+=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8)+"\n"; }
        }
        var metadata=row("data_kind","SIMULATION_ONLY","model_version","1.0","seed",seed,"start_year",startYear,"years",5,
                "baseline_date",new SimulationBaseline(baseline).properties.getProperty("captured_date"),
                "baseline_sha256",sha(Files.readAllBytes(baseline)),"schema_sha256",sha(schema.getBytes(StandardCharsets.UTF_8)),"checks","PASSED",
                "baseline_source","Read-only aggregate/catalog snapshot; all people and future activity are synthetic", "revenue","NOT_APPLICABLE");
        Files.copy(baseline,output.resolve("baseline.properties"));
        writeFile(output,"manifest.json",json(metadata));
        writeFile(output,"data.json",json(row("metadata",metadata,"tables",d.tables,"user_events",d.userEvents)));
        writeFile(output,"summary.json",json(row("metadata",metadata,"annual",d.annual,"monthly",d.monthly,"total",totals(d))));
        writeFile(output,"schema.sql",schema);
        var sql=new StringBuilder("-- SIMULATION ONLY. Synthetic accounts cannot log in. No CREATE DATABASE, USE, DROP, DELETE or TRUNCATE.\nSET NAMES utf8mb4;\nSTART TRANSACTION;\n");
        for(String table:TABLES) for(var row:d.rows(table)) {
            var keys=new ArrayList<>(row.keySet());
            sql.append("INSERT INTO `").append(table).append("` (`").append(String.join("`,`",keys)).append("`) VALUES (");
            sql.append(String.join(",",keys.stream().map(k->sqlValue(row.get(k))).toList())).append(");\n");
        }
        sql.append("COMMIT;\n"); writeFile(output,"data.sql",sql.toString());
        writeFile(output,"report.html",report(d,metadata));
        var hashes=new TreeMap<String,String>();
        for(String file:List.of("baseline.properties","manifest.json","data.json","summary.json","schema.sql","data.sql","report.html")) hashes.put(file,sha(Files.readAllBytes(output.resolve(file))));
        writeFile(output,"checksums.json",json(hashes));
    }
    private static void writeFile(Path dir,String name,String value) throws Exception { Files.writeString(dir.resolve(name),value+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW); }
    static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String sqlValue(Object value) {
        if(value==null) return "NULL"; if(value instanceof Number) return value.toString();
        // Hex UTF-8 literals avoid dependence on MySQL backslash escaping modes.
        return "CONVERT(X'"+HexFormat.of().formatHex(value.toString().getBytes(StandardCharsets.UTF_8))+"' USING utf8mb4)";
    }
    public static String json(Object value) {
        if(value==null) return "null";
        if(value instanceof Number || value instanceof Boolean) return value.toString();
        if(value instanceof Map<?,?> map) return "{"+String.join(",",map.entrySet().stream().map(e->json(e.getKey().toString())+":"+json(e.getValue())).toList())+"}";
        if(value instanceof Collection<?> list) return "["+String.join(",",list.stream().map(SimulationExport::json).toList())+"]";
        var out=new StringBuilder("\"");
        for(char c:value.toString().toCharArray()) {
            if(c=='"' || c=='\\') out.append('\\').append(c);
            else if(c<32) out.append(String.format(Locale.ROOT,"\\u%04x",(int)c)); else out.append(c);
        }
        return out.append('"').toString();
    }
    static Map<String,Object> totals(SimulationData d) {
        var total=new LinkedHashMap<String,Object>();
        for(String key:List.of("users_new","users_left","sections","schedules","cancelled_schedules","enrollments","cancelled_enrollments","sessions","teaching_minutes","used_room_slots","available_room_slots","requests","requests_approved","requests_rejected","requests_pending","maintenance","notifications","audit_logs"))
            total.put(key,d.annual.stream().mapToLong(y->((Number)y.get(key)).longValue()).sum());
        total.put("users_start",d.annual.get(0).get("users_start")); total.put("users_end",d.annual.get(d.annual.size()-1).get("users_end"));
        total.put("utilization_pct",Math.round(((Number)total.get("used_room_slots")).doubleValue()*10000/((Number)total.get("available_room_slots")).doubleValue())/100.0);
        return total;
    }
    private static String html(Object text) { return text.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    private static String table(List<Map<String,Object>> rows,String[][] columns) {
        var out=new StringBuilder("<div class='scroll'><table><thead><tr>");
        for(var col:columns) out.append("<th>").append(html(col[1])).append("</th>"); out.append("</tr></thead><tbody>");
        for(var row:rows) { out.append("<tr>"); for(var col:columns) out.append("<td>").append(html(row.get(col[0]))).append("</td>"); out.append("</tr>"); }
        return out.append("</tbody></table></div>").toString();
    }
    private static String report(SimulationData d,Map<String,Object> metadata) {
        var total=totals(d); int first=n(d.annual.get(0),"year"),last=first+4;
        var out=new StringBuilder("<!doctype html><html lang='vi'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>UniSchedule · Mô phỏng 5 năm</title><style>body{font:16px/1.6 system-ui,sans-serif;color:#17243b;background:#f4f6fb;margin:0}main{max-width:1180px;margin:auto;padding:32px}h1{font-size:36px;line-height:1.2}h2{margin-top:36px}header,.card{background:white;padding:24px;border-radius:16px;margin-bottom:20px}.badge{color:#8b3500;background:#fff0d5;padding:6px 12px;border-radius:8px;font-weight:bold}.scroll{overflow:auto}table{border-collapse:collapse;width:100%;background:white;font-size:14px}th,td{padding:10px 12px;border-bottom:1px solid #dce1ec;text-align:right;white-space:nowrap}th{background:#e9edfa}th:first-child,td:first-child{text-align:left}.bar{height:24px;background:#5850ec;border-radius:4px;min-width:2px}.chartrow{display:grid;grid-template-columns:65px 1fr 75px;gap:16px;align-items:center;margin:10px 0}small{color:#516078}a{color:#4338ca}@media print{body{background:white}main{padding:0}.card{break-inside:avoid}table{font-size:10px}th,td{padding:4px}}</style><main>");
        out.append("<header><span class='badge'>DỮ LIỆU MÔ PHỎNG — KHÔNG PHẢI DỰ BÁO</span><h1>UniSchedule · ").append(first).append("–").append(last).append("</h1><p>Mô phỏng quản lý đào tạo, thời khóa biểu và tài nguyên phòng học. Seed <b>").append(metadata.get("seed")).append("</b> · 18 bảng nghiệp vụ · Kiểm tra nhất quán: <b>ĐẠT</b>.</p><small>Mốc dữ liệu: bản chụp MySQL tổng hợp ngày ").append(html(metadata.get("baseline_date"))).append(". Danh tính và toàn bộ hoạt động tương lai đều được tạo mới.</small></header>");
        out.append("<div class='card'><h2>Kết quả sau 5 năm</h2><p>Người dùng hoạt động: <b>").append(total.get("users_start")).append(" + ").append(total.get("users_new")).append(" − ").append(total.get("users_left")).append(" = ").append(total.get("users_end")).append("</b>. Tổng <b>").append(total.get("sessions")).append(" buổi học</b>, ").append(total.get("requests")).append(" yêu cầu và ").append(total.get("maintenance")).append(" đợt bảo trì. Sử dụng phòng cả giai đoạn: <b>").append(total.get("utilization_pct")).append("%</b>.</p><p>Không có nghiệp vụ thanh toán trong dự án: doanh thu không áp dụng. Số người dùng cuối năm là số dư, không cộng dồn 5 năm.</p></div>");
        out.append("<h2>Người dùng từng năm</h2>").append(table(d.annual,new String[][]{{"year","Năm"},{"users_start","Đầu năm"},{"users_new","Mới"},{"users_left","Rời đi"},{"users_end","Cuối năm"},{"students_end","Sinh viên"},{"lecturers_end","Giảng viên"}}));
        out.append("<div class='card'><h2>Tăng trưởng người dùng hoạt động</h2>"); int max=d.annual.stream().mapToInt(y->n(y,"users_end")).max().orElse(1);
        for(var y:d.annual) out.append("<div class='chartrow'><b>").append(y.get("year")).append("</b><div class='bar' style='width:").append(n(y,"users_end")*100/max).append("%'></div><span>").append(y.get("users_end")).append(" người</span></div>");
        out.append("</div><h2>Đào tạo và công suất</h2>").append(table(d.annual,new String[][]{{"year","Năm"},{"sections","Lớp HP"},{"enrollments","Đăng ký"},{"cancelled_enrollments","ĐK hủy"},{"schedules","Lịch"},{"cancelled_schedules","Lịch hủy"},{"sessions","Buổi học"},{"teaching_minutes","Phút dạy"},{"utilization_pct","Phòng (%)"}}));
        out.append("<h2>Yêu cầu và vận hành</h2>").append(table(d.annual,new String[][]{{"year","Năm"},{"requests","Yêu cầu"},{"requests_approved","Duyệt"},{"requests_rejected","Từ chối"},{"requests_pending","Chờ"},{"maintenance","Bảo trì"},{"notifications","Thông báo"},{"audit_logs","Nhật ký"}}));
        out.append("<h2>Mùa vụ theo tháng</h2><p>Hai học kỳ 16 tuần; nghỉ hè làm công suất giảm. Yêu cầu tập trung trước học kỳ. Bảng đủ 60 tháng, gồm cả tháng không phát sinh.</p><details><summary>Mở bảng 60 tháng</summary>").append(table(d.monthly,new String[][]{{"year","Năm"},{"month","Tháng"},{"sessions","Buổi học"},{"requests","Yêu cầu"},{"used_room_slots","Ca phòng dùng"},{"available_room_slots","Ca khả dụng"}})).append("</details>");
        out.append("<div class='card'><h2>Giả định và công thức</h2><ul><li>Tuyển mới sinh viên = max(4, làm tròn 35% số sinh viên đầu năm); rời đi = max(1, làm tròn 15%). Rời đi ngày 01/07; tuyển mới ngày 20/08. Tuyển giảng viên để đủ ít nhất 6 người và 1 GV/10 SV.</li><li>Mỗi sinh viên đăng ký 8 môn/học kỳ; tách nhóm tối đa 25 người. Hủy đăng ký với xác suất 6%; hủy toàn bộ lịch lớp trước khi học với xác suất 4%. Sĩ số chỉ đếm đăng ký ACTIVE.</li><li>Mỗi lớp có 1 buổi/tuần gồm 2 ca, trong 16 tuần. Mượn phòng được duyệt có thể thêm 1 buổi. Ca tối dài hơn ca ngày; phút dạy lấy đúng time_slots, không tính giờ nghỉ.</li><li>70% lịch được công bố phát sinh một yêu cầu. Dự kiến 65% duyệt, 23% từ chối, 12% chờ; yêu cầu không khả thi chuyển thành từ chối. Có đủ 5 loại yêu cầu.</li><li>30% phòng hoạt động được bảo trì hè mỗi năm; cộng sửa lỗi ban đầu và yêu cầu báo hỏng. Tránh mọi lịch trùng phòng, giảng viên, sinh viên, lớp hoặc thời gian bảo trì.</li><li>Sử dụng phòng = ca phòng thực dùng / ca phòng khả dụng × 100. Mẫu số gồm thứ Hai–thứ Bảy cả năm, 6 ca/ngày, loại phòng INACTIVE và ngày bảo trì. Khác mẫu số lý thuyết 7 ngày của màn hình báo cáo hiện tại.</li></ul><p>Đây là kịch bản quy mô demo với cơ sở vật chất giữ nguyên, không phải ước lượng cho toàn trường. Không có dữ liệu lịch sử nhiều năm để hiệu chỉnh xác suất. Trạng thái đăng ký học kỳ cũ được giữ nguyên để thống kê lịch sử; người rời đi không có lịch mới.</p></div>");
        out.append("<h2>Dữ liệu kiểm tra và chạy lại</h2><p><a href='summary.json'>Tổng hợp JSON</a> · <a href='data.json'>18 bảng và sự kiện người dùng</a> · <a href='manifest.json'>Seed &amp; nguồn dữ liệu</a> · <a href='checksums.json'>SHA-256</a> · <a href='data.sql'>SQL chi tiết</a></p><p>Hướng dẫn: docs/SIMULATION.md trong dự án. Cùng baseline, seed và phiên bản mô hình cho cùng nội dung báo cáo. Thư mục đầu ra đã tồn tại sẽ bị từ chối để bảo toàn dữ liệu.</p></main></html>");
        return out.toString();
    }
}
