package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import vn.edu.donga.unischedule.config.ThemeConfig;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.frame.*;
import vn.edu.donga.unischedule.ui.dialog.*;
import vn.edu.donga.unischedule.ui.component.HeroIcons;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class UiRenderTest {
    @Test void renderScreensAndForms() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThemeConfig.install();
            assertEquals("Poppins", UIManager.getFont("defaultFont").getFamily());
            assertTrue(UIManager.getFont("defaultFont").canDisplayUpTo("Thời khóa biểu Nguyễn") < 0);
            var icon = (com.formdev.flatlaf.extras.FlatSVGIcon) HeroIcons.of("calendar-days", 24, Color.BLACK);
            assertTrue(icon.hasFound());
            AppServices services = AppServices.createDemo();
            MainFrame frame = new MainFrame(services, services.auth().login("daotao", "123456"));
            try {
                frame.addNotify();
                for (int width : new int[]{1180, 1440}) {
                    frame.setSize(width, 900);
                    for (String screen : new String[]{"dashboard", "timetable", "rooms", "requests", "roomSearch", "courseSections", "conflicts", "notifications", "profile"}) {
                        frame.showScreen(screen); render(frame, screen + "-" + width);
                    }
                }
                JDialog[] forms = {new ScheduleFormDialog(frame, services, null), new RoomFormDialog(frame, null),
                        new EquipmentDialog(frame, services.rooms().findAll().get(0), null),
                        new CourseSectionDialog(frame, services, null), new UserFormDialog(frame, services, null),
                        new RequestFormDialog(frame, services, services.auth().login("giangvien", "123456")),
                        new RoomDetailDialog(frame, services, services.rooms().findAll().get(0), services.auth().login("admin", "123456"))};
                for (JDialog form : forms) {
                    try { form.addNotify(); render(form, form.getClass().getSimpleName()); }
                    finally { form.dispose(); }
                }
                LoginFrame login = new LoginFrame(services);
                try { login.addNotify(); render(login, "login"); } finally { login.dispose(); }
                for (String account : new String[]{"admin", "giangvien", "sinhvien"}) {
                    MainFrame roleFrame = new MainFrame(services, services.auth().login(account, "123456"));
                    try {
                        roleFrame.addNotify(); render(roleFrame, account + "-dashboard");
                        if (account.equals("admin")) for (String screen : new String[]{"users", "audit"}) {
                            roleFrame.showScreen(screen); render(roleFrame, screen);
                        }
                    } finally { roleFrame.dispose(); }
                }
            } finally { frame.dispose(); }
        });
    }
    private static void render(Window window, String name) {
        window.validate();
        JRootPane root = ((RootPaneContainer) window).getRootPane();
        root.setSize(window.getWidth(), window.getHeight());
        layout(root);
        BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        root.printAll(graphics); graphics.dispose();
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 5)
            for (int x = 0; x < image.getWidth(); x += 5) colors.add(image.getRGB(x, y));
        assertTrue(colors.size() > 30, "Screen must contain rendered content: " + name);
        try {
            Path directory = Path.of("target", "ui-previews"); Files.createDirectories(directory);
            ImageIO.write(image, "png", directory.resolve(name + ".png").toFile());
        } catch (java.io.IOException ex) { throw new AssertionError(ex); }
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }
}
