package vn.edu.donga.unischedule;

import vn.edu.donga.unischedule.config.ThemeConfig;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.frame.LoginFrame;

import javax.swing.SwingUtilities;
import java.util.Arrays;

public class App {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--smoke")) {
            AppServices services = AppServices.createJdbc();
            services.auth().login("admin", "123456");
            services.auth().login("daotao", "123456");
            services.auth().login("giangvien", "123456");
            services.auth().login("sinhvien", "123456");
            System.out.println("UniSchedule smoke check passed.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            ThemeConfig.install();
            try {
                new LoginFrame(new vn.edu.donga.unischedule.controller.AppControllers(AppServices.createJdbc())).setVisible(true);
            } catch (vn.edu.donga.unischedule.validation.ValidationException ex) {
                javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage(), "Kết nối MySQL", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
