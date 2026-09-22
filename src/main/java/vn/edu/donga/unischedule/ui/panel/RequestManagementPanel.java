package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.RequestType;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.component.UiTasks;
import vn.edu.donga.unischedule.ui.dialog.RequestDetailDialog;
import vn.edu.donga.unischedule.ui.dialog.MakeupClassDialog;
import vn.edu.donga.unischedule.ui.dialog.RequestFormDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.ui.renderer.BadgeRenderer;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.util.TextUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class RequestManagementPanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final User user;
    private final SearchField searchField = new SearchField("Tìm người gửi, nội dung yêu cầu");
    private final JComboBox<String> typeBox = new JComboBox<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final GenericTableModel<ChangeRequest> tableModel;
    private final JTable table;

    public RequestManagementPanel(AppControllers controllers, User user) {
        this.controllers = controllers;
        this.user = user;
        tableModel = new GenericTableModel<>(
                new String[]{"Mã", "Người gửi", "Loại", "Nội dung", "Thời gian gửi", "Ưu tiên", "Trạng thái"},
                request -> "YC" + request.getId(),
                request -> request.getRequester().getFullName(),
                request -> request.getType().getDisplayName(),
                request -> request.getReason(),
                request -> request.getCreatedAt().toLocalDate().format(DateUtils.DATE_FORMAT),
                request -> request.getPriority().getDisplayName(),
                ChangeRequest::getStatus);
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
        typeBox.addItem("Tất cả loại yêu cầu");
        for (RequestType type : RequestType.values()) {
            typeBox.addItem(type.getDisplayName());
        }
        typeBox.addActionListener(event -> refresh());
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
        tabs.addTab("Tất cả", new JPanel());
        tabs.addTab("Chờ duyệt", new JPanel());
        tabs.addTab("Đã duyệt", new JPanel());
        tabs.addTab("Từ chối", new JPanel());
        tabs.setPreferredSize(new Dimension(0, 42));
        tabs.addChangeListener(event -> refresh());
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);
        top.add(tabs, BorderLayout.NORTH);
        JPanel filters = new JPanel(new GridLayout(1, 2, 10, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(typeBox);
        top.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        SecondaryButton detail = new SecondaryButton("Xem chi tiết");
        detail.addActionListener(event -> showDetail());
        actions.add(detail);
        if (user.getRole() == Role.LECTURER) {
            SecondaryButton makeup = new SecondaryButton("Đăng ký học bù");
            makeup.addActionListener(event -> {
                if (MakeupClassDialog.showDialog(this, controllers, user, null, null, null, null)) {
                    refresh();
                    Dialogs.success(this, "Yêu cầu học bù đã gửi và đang chờ phòng đào tạo duyệt.");
                }
            });
            actions.add(makeup);
            PrimaryButton create = new PrimaryButton("Gửi yêu cầu");
            create.addActionListener(event -> createRequest());
            actions.add(create);
        }
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ACADEMIC) {
            PrimaryButton approve = new PrimaryButton("Duyệt");
            SecondaryButton reject = new SecondaryButton("Từ chối");
            approve.addActionListener(event -> approveRequest());
            reject.addActionListener(event -> rejectRequest());
            actions.add(approve);
            actions.add(reject);
        }
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetail();
                }
            }
        });
    }

    @Override
    public void refresh() {
        String keyword = searchField.getText();
        String type = (String) typeBox.getSelectedItem();
        RequestStatus tabStatus = selectedTabStatus();
        List<ChangeRequest> rows = controllers.requests().search(user, keyword, type, tabStatus);
        tableModel.setRows(rows);
    }

    private RequestStatus selectedTabStatus() {
        return switch (tabs.getSelectedIndex()) {
            case 1 -> RequestStatus.PENDING;
            case 2 -> RequestStatus.APPROVED;
            case 3 -> RequestStatus.REJECTED;
            default -> null;
        };
    }

    private ChangeRequest selectedRequest() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một yêu cầu trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void showDetail() {
        try {
            RequestDetailDialog.showDialog(this, selectedRequest());
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void createRequest() {
        RequestFormDialog.showDialog(this, controllers, user).ifPresent(request -> {
            UiTasks.run(this, "Đang gửi yêu cầu…", () -> controllers.requests().create(request), () -> {
                refresh();
                Dialogs.success(this, "Yêu cầu đã được gửi và đang chờ duyệt.");
            });
        });
    }

    private void approveRequest() {
        try {
            ChangeRequest request = selectedRequest();
            UiTasks.run(this, "Đang duyệt yêu cầu…", () -> controllers.requests().approve(request, user), () -> {
                refresh();
                Dialogs.success(this, "Đã duyệt yêu cầu và cập nhật lịch nếu cần.");
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void rejectRequest() {
        try {
            ChangeRequest request = selectedRequest();
            String reason = JOptionPane.showInputDialog(this, "Nhập lý do từ chối:");
            if (reason == null) {
                return;
            }
            UiTasks.run(this, "Đang từ chối yêu cầu…", () -> controllers.requests().reject(request, user, reason), () -> {
                refresh();
                Dialogs.success(this, "Đã từ chối yêu cầu và thông báo cho người gửi.");
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
