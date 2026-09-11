package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.UserStatus;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

public class UserFormDialog extends JDialog {
    private final JComboBox<Role> roleBox = new JComboBox<>(Role.values());
    private final JTextField usernameField = new JTextField();
    private final JTextField fullNameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JComboBox<UserStatus> statusBox = new JComboBox<>(UserStatus.values());
    private final AppControllers controllers;
    private final User editing;
    private User result;

    public UserFormDialog(JFrame owner, AppControllers controllers, User editing) {
        super(owner, editing == null ? "Thêm tài khoản" : "Sửa tài khoản", true);
        this.controllers = controllers;
        this.editing = editing;
        buildUi();
        fill(editing);
        setSize(500, 410);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static Optional<User> showDialog(Component parent, AppControllers controllers, User editing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        UserFormDialog dialog = new UserFormDialog(owner, controllers, editing);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        setContentPane(root);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy = 0;
        addRow(form, gbc, "Vai trò", roleBox);
        addRow(form, gbc, "Username", usernameField);
        addRow(form, gbc, "Họ tên", fullNameField);
        addRow(form, gbc, "Email", emailField);
        addRow(form, gbc, "Số điện thoại", phoneField);
        addRow(form, gbc, "Trạng thái", statusBox);
        root.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        SecondaryButton cancel = new SecondaryButton("Hủy");
        PrimaryButton save = new PrimaryButton("Lưu");
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> save());
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
    }

    private void addRow(JPanel form, GridBagConstraints gbc, String label, Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
        gbc.gridy++;
    }

    private void fill(User user) {
        if (user == null) {
            return;
        }
        roleBox.setSelectedItem(user.getRole());
        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        statusBox.setSelectedItem(user.getStatus());
    }

    private void save() {
        Role role = (Role) roleBox.getSelectedItem();
        result = controllers.users().prepareUser(editing, role, usernameField.getText().trim(),
                fullNameField.getText().trim(), emailField.getText().trim(), phoneField.getText().trim(),
                (UserStatus) statusBox.getSelectedItem());
        dispose();
    }
}
