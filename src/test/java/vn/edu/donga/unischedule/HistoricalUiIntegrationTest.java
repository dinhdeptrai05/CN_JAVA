package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.config.ThemeConfig;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.frame.MainFrame;
import vn.edu.donga.unischedule.ui.panel.HistoryPanel;
import vn.edu.donga.unischedule.ui.panel.UserManagementPanel;
import vn.edu.donga.unischedule.ui.component.SearchField;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in read/render check against the already seeded project DB. Login uses a synthetic account. */
class HistoricalUiIntegrationTest {
    @Test void originalAccountsHaveRoleScopedHistory() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("UNISCHEDULE_HISTORY_UI_TEST")));
        for(var account:new String[]{"admin","daotao","giangvien","sinhvien"}) {
            var factory=new vn.edu.donga.unischedule.repository.jdbc.ConnectionFactory(){
                @Override public java.sql.Connection open() throws java.sql.SQLException {
                    var connection=super.open();connection.setReadOnly(true);return connection;
                }
            };
            var db=new vn.edu.donga.unischedule.repository.jdbc.JdbcDatabase(factory);
            var user=new vn.edu.donga.unischedule.repository.jdbc.JdbcUserRepository(db).findByUsername(account).orElseThrow();
            db.setActor(user);
            var history=new vn.edu.donga.unischedule.repository.jdbc.JdbcHistoryRepository(db).load();
            int visibleYears=account.equals("admin")||account.equals("daotao")?5:3;
            assertEquals(java.time.LocalDate.now().minusYears(visibleYears),history.from(),account);
            assertEquals(java.time.LocalDate.now(),history.to(),account);
            assertEquals(visibleYears+1,history.years().size(),account);
            assertTrue(history.years().stream().mapToLong(y->y.sessions()).sum()>1000,account);
        }
    }
    @Test void realFiveYearScreenAndReportsRender() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("UNISCHEDULE_HISTORY_UI_TEST")));
        AppServices services=AppServices.createJdbc();
        var admin=services.auth().login("qt_hethong","123456");
        var history=services.history().load();
        assertEquals(6,history.years().size());
        assertTrue(history.years().stream().mapToLong(y->y.sessions()).sum()>5000);
        assertTrue(history.years().stream().anyMatch(y->y.year()==2026&&y.sections()>100));
        assertTrue(services.reports().generate(admin,new vn.edu.donga.unischedule.model.Report.Filter(
                java.time.LocalDate.of(2026,1,1),java.time.LocalDate.now(),null,null)).sessions()>1000);
        final MainFrame[] frame=new MainFrame[1];
        SwingUtilities.invokeAndWait(()->{
            ThemeConfig.install();
            frame[0]=new MainFrame(new AppControllers(services),admin);
            frame[0].addNotify();frame[0].setSize(1440,900);
            frame[0].showScreen("history");
            HistoryPanel panel=find(frame[0],HistoryPanel.class);
            assertNotNull(panel);panel.display(history);
        });
        try {
            BufferedImage image=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);
            SwingUtilities.invokeAndWait(()->{
                var root=frame[0].getRootPane();root.setSize(1440,900);layout(root);
                var graphics=image.createGraphics();root.printAll(graphics);graphics.dispose();
            });
            Path dir=Path.of("target","history-ui-previews");Files.createDirectories(dir);
            ImageIO.write(image,"png",dir.resolve("history-five-years.png").toFile());
            java.util.Set<Integer> colors=new java.util.HashSet<>();
            for(int y=0;y<900;y+=7)for(int x=0;x<1440;x+=7)colors.add(image.getRGB(x,y));
            assertTrue(colors.size()>30,"History screen should render as a full app view");
            BufferedImage allYearsImage=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);
            BufferedImage usersImage=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);
            SwingUtilities.invokeAndWait(()->{
                frame[0].showScreen("users");
                var users=find(frame[0],UserManagementPanel.class);assertNotNull(users);
                var root=frame[0].getRootPane();root.setSize(1440,900);layout(root);
                var allGraphics=allYearsImage.createGraphics();root.printAll(allGraphics);allGraphics.dispose();
                var search=find(users,SearchField.class);assertNotNull(search);search.getTextField().setText("sv2026");
                layout(root);
                var graphics=usersImage.createGraphics();root.printAll(graphics);graphics.dispose();
            });
            ImageIO.write(allYearsImage,"png",dir.resolve("users-all-years.png").toFile());
            ImageIO.write(usersImage,"png",dir.resolve("users-natural-history.png").toFile());
            BufferedImage earlierUsers=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);
            SwingUtilities.invokeAndWait(()->{
                var users=find(frame[0],UserManagementPanel.class);
                find(users,SearchField.class).getTextField().setText("");
                var year=findLoginYearBox(users);assertNotNull(year);year.setSelectedItem("2024");
                var root=frame[0].getRootPane();root.setSize(1440,900);layout(root);
                var graphics=earlierUsers.createGraphics();root.printAll(graphics);graphics.dispose();
            });
            ImageIO.write(earlierUsers,"png",dir.resolve("users-login-2024.png").toFile());
        } finally {SwingUtilities.invokeAndWait(frame[0]::dispose);}
    }
    @SuppressWarnings("unchecked") private static <T extends Component> T find(Container root,Class<T> type) {
        for(Component child:root.getComponents()) {
            if(type.isInstance(child))return (T)child;
            if(child instanceof Container nested) {T found=find(nested,type);if(found!=null)return found;}
        }
        return null;
    }
    private static void layout(Container container) {
        container.doLayout();
        for(Component child:container.getComponents())if(child instanceof Container nested)layout(nested);
    }
    private static JComboBox<?> findLoginYearBox(Container root) {
        for(Component child:root.getComponents()) {
            if(child instanceof JComboBox<?> box&&box.getItemCount()>0&&"Tất cả năm đăng nhập".equals(box.getItemAt(0)))return box;
            if(child instanceof Container nested) {var found=findLoginYearBox(nested);if(found!=null)return found;}
        }
        return null;
    }
}
