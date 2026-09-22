package vn.edu.donga.unischedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.repository.jdbc.*;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.controller.ReportController.ExportFormat;
import vn.edu.donga.unischedule.ui.panel.ReportPanel;
import vn.edu.donga.unischedule.validation.ValidationException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="UNISCHEDULE_DB_URL",matches=".*[/]unischedule_test[?].*")
class ReportIntegrationTest {
    @Test void reportMatchesIndependentMysqlCalendarQueryAndRenders() throws Exception {
        var app=AppServices.createJdbc();var admin=app.auth().login("admin","123456");
        var semester=app.catalog().getSemesters().get(0);var from=semester.getStartDate();var to=from.plusDays(13);
        var filter=new Report.Filter(from,to,semester.getId(),null);var result=app.reports().generate(admin,filter);
        var db=new JdbcDatabase(new ConnectionFactory());
        long expected=db.scalar("WITH RECURSIVE days AS (SELECT CAST(? AS DATE) d UNION ALL SELECT DATE_ADD(d,INTERVAL 1 DAY) FROM days WHERE d<?) SELECT COUNT(*) FROM days JOIN schedules s ON days.d BETWEEN s.start_date AND s.end_date AND s.day_of_week=WEEKDAY(days.d)+2 JOIN course_sections cs ON cs.id=s.course_section_id WHERE s.status='PUBLISHED' AND cs.semester_id=?",from,to,semester.getId());
        assertEquals(expected,result.sessions());assertTrue(result.sessions()>0);
        var academic=app.auth().login("daotao","123456");assertEquals(expected,app.reports().generate(academic,filter).sessions());
        var student=app.auth().login("sinhvien","123456");assertThrows(ValidationException.class,()->app.reports().generate(student,filter));
        assertThrows(ValidationException.class,()->app.reports().generate(admin,filter));
        app.auth().login("admin","123456");
        var controllers=new AppControllers(app);
        javax.swing.SwingUtilities.invokeAndWait(()->{
            vn.edu.donga.unischedule.config.ThemeConfig.install();
            var panel=new ReportPanel(controllers,admin);panel.display(result);
            var frame=new javax.swing.JFrame();frame.setContentPane(panel);
            try {frame.addNotify();frame.setSize(1180,760);frame.validate();layout(frame.getRootPane());
                var picture=new java.awt.image.BufferedImage(1180,760,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=picture.createGraphics();try{frame.getRootPane().printAll(g);}finally{g.dispose();}
                try{Path path=Path.of("target","jdbc-previews","reports.png");Files.createDirectories(path.getParent());javax.imageio.ImageIO.write(picture,"png",path.toFile());}catch(java.io.IOException e){throw new AssertionError(e);}
            }finally{frame.dispose();}
        });
        Path export=Path.of("target","jdbc-previews","report.pdf");controllers.reports().export(result,export,ExportFormat.PDF);assertTrue(Files.size(export)>1000);
    }
    private static void layout(java.awt.Container c){c.doLayout();for(var child:c.getComponents())if(child instanceof java.awt.Container nested)layout(nested);}
}
