package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.UserStatus;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.dialog.UserFormDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.ui.renderer.BadgeRenderer;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.util.TextUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class UserManagementPanel extends JPanel implements Refreshable {
    private final AppServices services;
    private final SearchField searchField = new SearchField("Tìm username, họ tên hoặc email");
    private final JComboBox<String> roleBox = new JComboBox<>();
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final GenericTableModel<User> tableModel;
    private final JTable table;

    public UserManagementPanel(AppServices services) {
        this.services = services;
        tableModel = new GenericTableModel<>(
                new String[]{"Mã", "Họ tên", "Username", "Email", "Vai trò", "Lần đăng nhập cuối", "Trạng thái"},
                user -> "U" + user.getId(),
                User::getFullName,
                User::getUsername,
                User::getEmail,
                user -> user.getRole().getDisplayName(),
                user -> user.getLastLogin().toLocalDate().format(DateUtils.DATE_FORMAT),
                User::getStatus);
        table = new JTable(tableModel);
        TableUtils.style(table);
        table.getColumnModel().getColumn(6).setCellRenderer(new BadgeRenderer());
        setLayout(new BorderLayout(0, 14));
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        buildFilters();
        buildUi();
        refresh();
    }

    private void buildFilters() {
        roleBox.addItem("Tất cả vai trò");
        for (Role role : Role.values()) {
            roleBox.addItem(role.getDisplayName());
        }
        statusBox.addItem("Tất cả trạng thái");
        for (UserStatus status : UserStatus.values()) {
            statusBox.addItem(status.getDisplayName());
        }
        roleBox.addActionListener(event -> refresh());
        statusBox.addActionListener(event -> refresh());
        searchField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);
        JPanel filters = new JPanel(new GridLayout(1, 3, 10, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(roleBox);
        filters.add(statusBox);
        top.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        PrimaryButton add = new PrimaryButton("Thêm tài khoản");
        SecondaryButton edit = new SecondaryButton("Sửa");
        SecondaryButton toggle = new SecondaryButton("Khóa / mở khóa");
        SecondaryButton reset = new SecondaryButton("Đặt lại mật khẩu");
        add.addActionListener(event -> addUser());
        edit.addActionListener(event -> editUser());
        toggle.addActionListener(event -> toggleLock());
        reset.addActionListener(event -> resetPassword());
        actions.add(add);
        actions.add(edit);
        actions.add(toggle);
        actions.add(reset);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        String keyword = searchField.getText();
        String role = (String) roleBox.getSelectedItem();
        String status = (String) statusBox.getSelectedItem();
        List<User> rows = services.users().findAll().stream()
                .filter(user -> role == null || role.startsWith("Tất cả") || user.getRole().getDisplayName().equals(role))
                .filter(user -> status == null || status.startsWith("Tất cả") || user.getStatus().getDisplayName().equals(status))
                .filter(user -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(user.getUsername(), keyword)
                        || TextUtils.containsIgnoreAccent(user.getFullName(), keyword)
                        || TextUtils.containsIgnoreAccent(user.getEmail(), keyword))
                .toList();
        tableModel.setRows(rows);
    }

    private User selectedUser() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một tài khoản trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void addUser() {
        UserFormDialog.showDialog(this, services, null).ifPresent(user -> {
            try {
                services.users().save(user);
                refresh();
                Dialogs.success(this, "Đã thêm tài khoản demo với mật khẩu 123456.");
            } catch (ValidationException ex) {
                Dialogs.error(this, ex.getMessage());
            }
        });
    }

    private void editUser() {
        try {
            UserFormDialog.showDialog(this, services, selectedUser()).ifPresent(user -> {
                try {
                    services.users().save(user);
                    refresh();
                    Dialogs.success(this, "Đã cập nhật tài khoản.");
                } catch (ValidationException ex) {
                    Dialogs.error(this, ex.getMessage());
                }
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void toggleLock() {
        try {
            User user = selectedUser();
            services.users().toggleLock(user);
            refresh();
            Dialogs.success(this, "Đã cập nhật trạng thái tài khoản.");
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void resetPassword() {
        try {
            User user = selectedUser();
            services.users().resetPassword(user);
            Dialogs.success(this, "Mật khẩu demo đã được đặt lại thành 123456.");
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
