package vn.edu.donga.unischedule;

import vn.edu.donga.unischedule.config.ThemeConfig;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.frame.LoginFrame;

import javax.swing.SwingUtilities;
import java.util.Arrays;

public class App {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--smoke")) {
            AppServices services = AppServices.createDemo();
            services.auth().login("admin", "123456");
            services.auth().login("daotao", "123456");
            services.auth().login("giangvien", "123456");
            services.auth().login("sinhvien", "123456");
            System.out.println("UniSchedule smoke check passed.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            ThemeConfig.install();
            new LoginFrame(AppServices.createDemo()).setVisible(true);
        });
    }
}
