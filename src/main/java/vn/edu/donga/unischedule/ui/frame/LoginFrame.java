package vn.edu.donga.unischedule.ui.frame;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.RoundedPanel;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginFrame extends JFrame {
    private final AppServices services;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ");
    private final JComboBox<String> demoCombo = new JComboBox<>();
    private final Map<String, String[]> demoAccounts = new LinkedHashMap<>();

    public LoginFrame(AppServices services) {
        super("Đăng nhập - " + AppConfig.APP_NAME);
        this.services = services;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setSize(1080, 700);
        setLocationRelativeTo(null);
        buildDemoAccounts();
        buildUi();
    }

    private void buildDemoAccounts() {
        demoAccounts.put("Quản trị viên - Nguyễn Quản Trị", new String[]{"admin", "123456"});
        demoAccounts.put("Phòng đào tạo - Trần Thu Hà", new String[]{"daotao", "123456"});
        demoAccounts.put("Giảng viên - Phạm Anh Tuấn", new String[]{"giangvien", "123456"});
        demoAccounts.put("Sinh viên - Nguyễn Hoàng Nam", new String[]{"sinhvien", "123456"});
        demoAccounts.keySet().forEach(demoCombo::addItem);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppConfig.BACKGROUND);
        setContentPane(root);

        JPanel hero = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 30, 22));
        hero.setBackground(Color.WHITE);
        JLabel brand = new JLabel("UniSchedule");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 22f));
        brand.setIcon(vn.edu.donga.unischedule.ui.component.HeroIcons.of("academic-cap", 30, AppConfig.PRIMARY));
        brand.setIconTextGap(12);
        hero.add(brand);
        root.add(hero, BorderLayout.NORTH);

        JPanel formHost = new JPanel(new GridBagLayout());
        formHost.setOpaque(false);
        root.add(formHost, BorderLayout.CENTER);

        RoundedPanel card = new RoundedPanel(12, Color.WHITE);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        card.setPreferredSize(new Dimension(470, 550));
        formHost.add(card);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        JLabel title = new JLabel("Đăng nhập");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(AppConfig.TEXT);
        card.add(title, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("Chào mừng trở lại UniSchedule");
        subtitle.setForeground(AppConfig.MUTED);
        card.add(subtitle, gbc);

        gbc.gridy++;
        card.add(label("Tài khoản demo"), gbc);
        gbc.gridy++;
        demoCombo.addActionListener(event -> fillSelectedDemo());
        card.add(demoCombo, gbc);

        gbc.gridy++;
        card.add(label("Tên đăng nhập"), gbc);
        gbc.gridy++;
        usernameField.putClientProperty("JTextField.placeholderText", "admin");
        card.add(usernameField, gbc);

        gbc.gridy++;
        card.add(label("Mật khẩu"), gbc);
        gbc.gridy++;
        JPanel passwordPanel = new JPanel(new BorderLayout(8, 0));
        passwordPanel.setOpaque(false);
        passwordField.putClientProperty("JTextField.placeholderText", "123456");
        SecondaryButton toggleButton = new SecondaryButton("Hiện");
        toggleButton.addActionListener(event -> togglePassword(toggleButton));
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(toggleButton, BorderLayout.EAST);
        card.add(passwordPanel, gbc);

        gbc.gridy++;
        JCheckBox rememberBox = new JCheckBox("Ghi nhớ đăng nhập");
        rememberBox.setOpaque(false);
        card.add(rememberBox, gbc);

        gbc.gridy++;
        errorLabel.setForeground(AppConfig.DANGER);
        card.add(errorLabel, gbc);

        gbc.gridy++;
        PrimaryButton loginButton = new PrimaryButton("Đăng nhập");
        loginButton.addActionListener(event -> login());
        card.add(loginButton, gbc);
        getRootPane().setDefaultButton(loginButton);

        fillSelectedDemo();
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppConfig.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private void fillSelectedDemo() {
        String key = (String) demoCombo.getSelectedItem();
        if (key == null) {
            return;
        }
        String[] account = demoAccounts.get(key);
        usernameField.setText(account[0]);
        passwordField.setText(account[1]);
        errorLabel.setText(" ");
    }

    private void togglePassword(SecondaryButton button) {
        if (passwordField.getEchoChar() == 0) {
            passwordField.setEchoChar('•');
            button.setText("Hiện");
        } else {
            passwordField.setEchoChar((char) 0);
            button.setText("Ẩn");
        }
    }

    private void login() {
        try {
            User user = services.auth().login(usernameField.getText(), new String(passwordField.getPassword()));
            MainFrame mainFrame = new MainFrame(services, user);
            mainFrame.setVisible(true);
            dispose();
        } catch (ValidationException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }
}
