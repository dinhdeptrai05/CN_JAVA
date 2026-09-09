package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.Notification;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.NotificationType;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
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

public class NotificationPanel extends JPanel implements Refreshable {
    private final AppServices services;
    private final User user;
    private final ScreenNavigator navigator;
    private final SearchField searchField = new SearchField("Tìm thông báo");
    private final JComboBox<String> typeBox = new JComboBox<>();
    private final JComboBox<String> readBox = new JComboBox<>(new String[]{"Tất cả", "Chưa đọc", "Đã đọc"});
    private final GenericTableModel<Notification> tableModel;
    private final JTable table;

    public NotificationPanel(AppServices services, User user, ScreenNavigator navigator) {
        this.services = services;
        this.user = user;
        this.navigator = navigator;
        tableModel = new GenericTableModel<>(
                new String[]{"Loại", "Tiêu đề", "Nội dung", "Thời gian", "Trạng thái"},
                notification -> notification.getType().getDisplayName(),
                Notification::getTitle,
                Notification::getContent,
                notification -> notification.getCreatedAt().toLocalDate().toString(),
                notification -> notification.isRead() ? "Đã đọc" : "Mới");
        table = new JTable(tableModel);
        TableUtils.style(table);
        setLayout(new BorderLayout(0, 14));
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        buildFilters();
        buildUi();
        refresh();
    }

    private void buildFilters() {
        typeBox.addItem("Tất cả loại");
        for (NotificationType type : NotificationType.values()) {
            typeBox.addItem(type.getDisplayName());
        }
        typeBox.addActionListener(event -> refresh());
        readBox.addActionListener(event -> refresh());
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
        filters.add(typeBox);
        filters.add(readBox);
        top.add(filters, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        SecondaryButton open = new SecondaryButton("Mở liên quan");
        SecondaryButton markOne = new SecondaryButton("Đánh dấu đã đọc");
        PrimaryButton markAll = new PrimaryButton("Đánh dấu tất cả");
        open.addActionListener(event -> openTarget());
        markOne.addActionListener(event -> markSelectedRead());
        markAll.addActionListener(event -> {
            services.notifications().markAllRead(user);
            refresh();
            Dialogs.success(this, "Đã đánh dấu tất cả thông báo là đã đọc.");
        });
        actions.add(open);
        actions.add(markOne);
        actions.add(markAll);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openTarget();
                }
            }
        });
    }

    @Override
    public void refresh() {
        String keyword = searchField.getText();
        String type = (String) typeBox.getSelectedItem();
        String read = (String) readBox.getSelectedItem();
        List<Notification> rows = services.notifications().findForUser(user).stream()
                .filter(notification -> type == null || type.startsWith("Tất cả") || notification.getType().getDisplayName().equals(type))
                .filter(notification -> read == null || read.equals("Tất cả")
                        || (read.equals("Chưa đọc") && !notification.isRead())
                        || (read.equals("Đã đọc") && notification.isRead()))
                .filter(notification -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(notification.getTitle(), keyword)
                        || TextUtils.containsIgnoreAccent(notification.getContent(), keyword))
                .toList();
        tableModel.setRows(rows);
    }

    private Notification selectedNotification() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một thông báo trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void markSelectedRead() {
        try {
            services.notifications().markRead(selectedNotification());
            refresh();
            Dialogs.success(this, "Đã đánh dấu thông báo là đã đọc.");
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void openTarget() {
        try {
            Notification notification = selectedNotification();
            services.notifications().markRead(notification);
            refresh();
            navigator.showScreen(notification.getTargetScreen());
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
