package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.RoundedPanel;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.validation.ValidationException;
import vn.edu.donga.unischedule.validation.Validator;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

public class ProfilePanel extends JPanel implements Refreshable {
    private final AppServices services;
    private final User user;
    private final JTextField fullNameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JPasswordField oldPasswordField = new JPasswordField();
    private final JPasswordField newPasswordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();
    private final JComboBox<String> themeBox = new JComboBox<>(new String[]{"Sáng", "Tối"});

    public ProfilePanel(AppServices services, User user) {
        this.services = services;
        this.user = user;
        setLayout(new BorderLayout(16, 16));
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        buildUi();
        refresh();
    }

    private void buildUi() {
        RoundedPanel profile = new RoundedPanel(8, Color.WHITE);
        profile.setLayout(new BorderLayout(0, 16));
        profile.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        profile.setPreferredSize(new java.awt.Dimension(240, 0));
        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT)); identity.setOpaque(false);
        identity.add(new vn.edu.donga.unischedule.ui.component.Avatar(user.getId(), user.getFullName(), 64));
        profile.add(identity, BorderLayout.NORTH);
        JPanel facts = new JPanel();
        facts.setLayout(new javax.swing.BoxLayout(facts, javax.swing.BoxLayout.Y_AXIS));
        facts.setOpaque(false);
        facts.add(new JLabel("Mã người dùng: U" + user.getId()));
        facts.add(javax.swing.Box.createVerticalStrut(16));
        facts.add(new JLabel("<html>Vai trò:<br><b>" + user.getRole().getDisplayName() + "</b></html>"));
        facts.add(javax.swing.Box.createVerticalStrut(16));
        facts.add(new JLabel("Username: " + user.getUsername()));
        profile.add(facts, BorderLayout.CENTER);
        add(profile, BorderLayout.WEST);

        JPanel forms = new JPanel(new GridLayout(2, 1, 0, 16));
        forms.setOpaque(false);
        forms.add(buildInfoCard());
        forms.add(buildPasswordCard());
        add(forms, BorderLayout.CENTER);
    }

    private RoundedPanel buildInfoCard() {
        RoundedPanel card = card("Thông tin cá nhân");
        GridBagConstraints gbc = constraints();
        addRow(card, gbc, "Họ tên", fullNameField);
        addRow(card, gbc, "Email", emailField);
        addRow(card, gbc, "Số điện thoại", phoneField);
        addRow(card, gbc, "Giao diện", themeBox);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        PrimaryButton save = new PrimaryButton("Cập nhật hồ sơ");
        save.addActionListener(event -> updateProfile());
        actions.add(save);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        card.add(actions, gbc);
        return card;
    }

    private RoundedPanel buildPasswordCard() {
        RoundedPanel card = card("Đổi mật khẩu");
        GridBagConstraints gbc = constraints();
        addRow(card, gbc, "Mật khẩu hiện tại", oldPasswordField);
        addRow(card, gbc, "Mật khẩu mới", newPasswordField);
        addRow(card, gbc, "Nhập lại mật khẩu", confirmPasswordField);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        SecondaryButton change = new SecondaryButton("Đổi mật khẩu");
        change.addActionListener(event -> changePassword());
        actions.add(change);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        card.add(actions, gbc);
        return card;
    }

    private RoundedPanel card(String title) {
        RoundedPanel card = new RoundedPanel(8, Color.WHITE);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
        GridBagConstraints heading = new GridBagConstraints();
        heading.gridx = 0; heading.gridy = 0; heading.gridwidth = 2;
        heading.weightx = 1; heading.fill = GridBagConstraints.HORIZONTAL;
        heading.insets = new Insets(0, 12, 12, 12);
        JLabel label = new JLabel(title); label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        card.add(label, heading);
        return card;
    }

    private GridBagConstraints constraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 12, 6, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy = 1;
        return gbc;
    }

    private void addRow(JPanel form, GridBagConstraints gbc, String label, java.awt.Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
        gbc.gridy++;
    }

    @Override
    public void refresh() {
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
    }

    private void updateProfile() {
        try {
            Validator.required(fullNameField.getText(), "Họ tên");
            Validator.email(emailField.getText());
            user.setFullName(fullNameField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setPhone(phoneField.getText().trim());
            services.users().save(user);
            if ("Tối".equals(themeBox.getSelectedItem())) {
                Dialogs.warning(this, "Chế độ tối được mô phỏng trong tuần 1 và sẽ lưu thật ở giai đoạn sau.");
            } else {
                Dialogs.success(this, "Đã cập nhật hồ sơ cá nhân.");
            }
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void changePassword() {
        String oldPassword = new String(oldPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());
        if (!user.getPassword().equals(oldPassword)) {
            Dialogs.error(this, "Mật khẩu hiện tại không đúng.");
            return;
        }
        if (newPassword.length() < 6) {
            Dialogs.error(this, "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }
        if (!newPassword.equals(confirm)) {
            Dialogs.error(this, "Mật khẩu nhập lại không khớp.");
            return;
        }
        user.setPassword(newPassword);
        services.users().save(user);
        oldPasswordField.setText("");
        newPasswordField.setText("");
        confirmPasswordField.setText("");
        Dialogs.success(this, "Đã đổi mật khẩu trong dữ liệu demo.");
    }

    private String initials(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
