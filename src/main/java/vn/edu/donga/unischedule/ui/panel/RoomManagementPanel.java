package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.RoomStatus;
import vn.edu.donga.unischedule.model.Enums.RoomType;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.component.StatCard;
import vn.edu.donga.unischedule.ui.dialog.RoomDetailDialog;
import vn.edu.donga.unischedule.ui.dialog.RoomFormDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.ui.renderer.BadgeRenderer;
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
import java.util.stream.Collectors;

public class RoomManagementPanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final User user;
    private final JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
    private final SearchField searchField = new SearchField("Tìm mã, tên phòng hoặc thiết bị");
    private final JComboBox<String> buildingBox = new JComboBox<>();
    private final JComboBox<String> typeBox = new JComboBox<>();
    private final JComboBox<String> capacityBox = new JComboBox<>(new String[]{"Mọi sức chứa", ">= 40", ">= 60", ">= 80"});
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final GenericTableModel<Classroom> tableModel;
    private final JTable table;

    public RoomManagementPanel(AppControllers controllers, User user) {
        this.controllers = controllers;
        this.user = user;
        tableModel = new GenericTableModel<>(
                new String[]{"Mã", "Tên phòng", "Tòa", "Tầng", "Sức chứa", "Loại", "Thiết bị", "Trạng thái"},
                Classroom::getCode,
                Classroom::getName,
                Classroom::getBuilding,
                Classroom::getFloor,
                Classroom::getCapacity,
                room -> room.getRoomType().getDisplayName(),
                this::equipmentSummary,
                Classroom::getRoomStatus);
        table = new JTable(tableModel);
        TableUtils.style(table);
        table.getColumnModel().getColumn(7).setCellRenderer(new BadgeRenderer());
        setLayout(new BorderLayout(0, 14));
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        buildFilters();
        buildUi();
        refresh();
    }

    private void buildFilters() {
        statsPanel.setOpaque(false);
        buildingBox.addItem("Tất cả tòa nhà");
        controllers.rooms().buildings().forEach(buildingBox::addItem);
        typeBox.addItem("Tất cả loại phòng");
        for (RoomType type : RoomType.values()) {
            typeBox.addItem(type.getDisplayName());
        }
        statusBox.addItem("Tất cả trạng thái");
        for (RoomStatus status : RoomStatus.values()) {
            statusBox.addItem(status.getDisplayName());
        }
        buildingBox.addActionListener(event -> refresh());
        typeBox.addActionListener(event -> refresh());
        capacityBox.addActionListener(event -> refresh());
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
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(statsPanel, BorderLayout.NORTH);
        JPanel filters = new JPanel(new GridLayout(1, 5, 10, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(buildingBox);
        filters.add(typeBox);
        filters.add(capacityBox);
        filters.add(statusBox);
        top.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        SecondaryButton detail = new SecondaryButton("Xem chi tiết");
        detail.addActionListener(event -> showDetail());
        actions.add(detail);
        if (user.getRole() == Role.ADMIN) {
            PrimaryButton add = new PrimaryButton("Thêm phòng");
            SecondaryButton edit = new SecondaryButton("Sửa");
            SecondaryButton maintenance = new SecondaryButton("Bảo trì / mở lại");
            add.addActionListener(event -> addRoom());
            edit.addActionListener(event -> editRoom());
            maintenance.addActionListener(event -> toggleMaintenance());
            actions.add(add);
            actions.add(edit);
            actions.add(maintenance);
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
        rebuildStats();
        String keyword = searchField.getText();
        String building = (String) buildingBox.getSelectedItem();
        String type = (String) typeBox.getSelectedItem();
        String capacity = (String) capacityBox.getSelectedItem();
        String status = (String) statusBox.getSelectedItem();
        int minCapacity = capacity == null || capacity.startsWith("Mọi") ? 0 : Integer.parseInt(capacity.replaceAll("\\D+", ""));
        List<Classroom> rows = controllers.rooms().search(keyword, building, type, minCapacity, status);
        tableModel.setRows(rows);
    }

    private void rebuildStats() {
        statsPanel.removeAll();
        List<Classroom> rooms = controllers.rooms().findAll();
        statsPanel.add(new StatCard("PH", "Tổng số phòng", String.valueOf(rooms.size()), "Trong hệ thống", AppConfig.PRIMARY));
        statsPanel.add(new StatCard("TR", "Phòng trống", String.valueOf(count(RoomStatus.AVAILABLE)), "Sẵn sàng", AppConfig.SUCCESS));
        statsPanel.add(new StatCard("SD", "Đang sử dụng", String.valueOf(count(RoomStatus.IN_USE)), "Có lịch học", AppConfig.WARNING));
        statsPanel.add(new StatCard("BT", "Bảo trì", String.valueOf(count(RoomStatus.MAINTENANCE)), "Cần xử lý", AppConfig.DANGER));
        statsPanel.revalidate();
        statsPanel.repaint();
    }

    private long count(RoomStatus status) {
        return controllers.rooms().count(status);
    }

    private String equipmentSummary(Classroom room) {
        return controllers.rooms().equipmentSummary(room);
    }

    private Classroom selectedRoom() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một phòng trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void addRoom() {
        RoomFormDialog.showDialog(this, null).ifPresent(room -> {
            try {
                controllers.rooms().save(room);
                refresh();
                Dialogs.success(this, "Đã thêm phòng học.");
            } catch (ValidationException ex) {
                Dialogs.error(this, ex.getMessage());
            }
        });
    }

    private void editRoom() {
        try {
            RoomFormDialog.showDialog(this, selectedRoom()).ifPresent(room -> {
                try {
                    controllers.rooms().save(room);
                    refresh();
                    Dialogs.success(this, "Đã cập nhật phòng học.");
                } catch (ValidationException ex) {
                    Dialogs.error(this, ex.getMessage());
                }
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void showDetail() {
        try {
            RoomDetailDialog.showDialog(this, controllers, selectedRoom(), user);
            refresh();
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void toggleMaintenance() {
        try {
            Classroom room = selectedRoom();
            if (Dialogs.confirm(this, "Đổi trạng thái bảo trì của phòng " + room.getCode() + "?")) {
                controllers.rooms().toggleMaintenance(room);
                refresh();
                Dialogs.success(this, "Đã cập nhật trạng thái phòng.");
            }
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
