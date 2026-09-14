package vn.edu.donga.unischedule.util;
import vn.edu.donga.unischedule.model.Report;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
public final class ReportExport {
    private ReportExport() { }
    public static void csv(Report report,int index,Path path) throws IOException {
        var table=report.tables().get(index);var out=new StringBuilder("\uFEFF");
        out.append(csvCell(table.title())).append("\r\n").append(csvCell(report.filter().from()+" - "+report.filter().to())).append("\r\n").append(csvCell(report.scope())).append("\r\n").append(csvCell(table.note())).append("\r\n");
        out.append(table.columns().stream().map(ReportExport::csvCell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        for(var row:table.rows()) out.append(row.stream().map(v->csvCell(String.valueOf(v))).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        write(path,out.toString());
    }
    public static String csvCell(String value) {
        String trimmed=value.stripLeading();
        if(!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0))>=0) value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
    public static void html(Report report,Path path) throws IOException {
        var out=new StringBuilder("<!doctype html><html lang='vi'><meta charset='utf-8'><title>Báo cáo UniSchedule</title><style>body{font:14px Arial,sans-serif;color:#172033;margin:36px}h1,h2{color:#5850ec}table{border-collapse:collapse;width:100%;margin:16px 0 28px}td,th{padding:9px;border:1px solid #ddd;text-align:left}th{background:#f1f3ff}small{color:#555}tr{break-inside:avoid}@media print{button{display:none}thead{display:table-header-group}body{margin:10mm}}</style><button onclick='window.print()'>In / Lưu PDF</button><h1>Báo cáo thống kê UniSchedule</h1>");
        out.append("<p>").append(report.filter().from()).append(" — ").append(report.filter().to()).append("</p><p>").append(escape(report.scope())).append("</p><small>Tạo lúc ").append(report.generatedAt()).append(" UTC</small>");
        out.append("<p>Buổi học: ").append(report.sessions()).append(" · Phòng có lịch: ").append(report.usedRooms()).append(" · Lượt đăng ký hiện tại: ").append(report.registrations()).append(" · Yêu cầu: ").append(report.requests()).append("</p>");
        for(var table:report.tables()) {
            out.append("<h2>").append(escape(table.title())).append("</h2><p>").append(escape(table.note())).append("</p><table><thead><tr>");
            for(var column:table.columns())out.append("<th>").append(escape(column)).append("</th>");out.append("</tr></thead><tbody>");
            for(var row:table.rows()){out.append("<tr>");for(var cell:row)out.append("<td>").append(escape(String.valueOf(cell))).append("</td>");out.append("</tr>");}
            if(table.rows().isEmpty())out.append("<tr><td colspan='").append(table.columns().size()).append("'>Không có dữ liệu</td></tr>");
            out.append("</tbody></table>");
        }
        write(path,out.append("</html>").toString());
    }
    private static String escape(String s) {return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static void write(Path path,String content) throws IOException {
        Path target=path.toAbsolutePath(),temp=Files.createTempFile(target.getParent(),"report-",".tmp");
        try {Files.writeString(temp,content,StandardCharsets.UTF_8);Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
    }
}
