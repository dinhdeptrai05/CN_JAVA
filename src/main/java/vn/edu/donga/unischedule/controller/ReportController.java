package vn.edu.donga.unischedule.controller;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.service.ReportService;
import vn.edu.donga.unischedule.util.ReportExport;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.time.LocalDate;
import java.nio.file.Path;
public final class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) { this.service=service; }
    public Report generate(User user,String from,String to,Long semester,Long department) {
        try {return service.generate(user,new Report.Filter(LocalDate.parse(from.trim()),LocalDate.parse(to.trim()),semester,department));}
        catch(java.time.format.DateTimeParseException ex){throw new ValidationException("Ngày phải có dạng yyyy-MM-dd.");}
    }
    public void export(Report report,int table,Path file,boolean html) {
        try {if(html)ReportExport.html(report,file);else ReportExport.csv(report,table,file);}
        catch(java.io.IOException ex){throw new ValidationException("Không ghi được báo cáo. Kiểm tra đường dẫn và tệp có đang mở không.");}
    }
}
