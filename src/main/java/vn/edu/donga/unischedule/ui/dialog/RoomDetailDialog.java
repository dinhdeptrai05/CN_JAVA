package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.Equipment;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.ResourceStatus;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.component.UiTasks;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class RoomDetailDialog extends JDialog {
    private final AppControllers controllers;
    private final Classroom room;
    private final User user;
    private final GenericTableModel<Equipment> equipmentModel;
    private final JTable equipmentTable;

    public RoomDetailDialog(JFrame owner, AppControllers controllers, Classroom room, User user) {
        super(owner, "Chi tiết phòng " + room.getCode(), true);
        this.controllers = controllers;
        this.room = room;
        this.user = user;
        equipmentModel = new GenericTableModel<>(
                new String[]{"Mã", "Thiết bị", "Nhóm", "Số lượng", "Tình trạng", "Trạng thái"},
                Equipment::getCode,
                Equipment::getName,
                Equipment::getCategory,
                Equipment::getQuantity,
                Equipment::getCondition,
                item -> item.getStatus().getDisplayName());
        equipmentTable = new JTable(equipmentModel);
        TableUtils.style(equipmentTable);
        buildUi();
        refreshEquipment();
        setSize(760, 560);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static void showDialog(Component parent, AppControllers controllers, Classroom room, User user) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        new RoomDetailDialog(owner, controllers, room, user).setVisible(true);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        setContentPane(root);
        JPanel info = new JPanel(new GridLayout(0, 2, 12, 8));
        info.add(new JLabel("Tên phòng: " + room.getName()));
        info.add(new JLabel("Tòa nhà: " + room.getBuilding()));
        info.add(new JLabel("Tầng: " + room.getFloor()));
        info.add(new JLabel("Sức chứa: " + room.getCapacity()));
        info.add(new JLabel("Loại phòng: " + room.getRoomType().getDisplayName()));
        info.add(new JLabel("Trạng thái: " + room.getRoomStatus().getDisplayName()));
        root.add(info, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Thiết bị", new JScrollPane(equipmentTable));
        tabs.addTab("Lịch sử dụng", new JScrollPane(buildScheduleTable()));
        root.add(tabs, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        SecondaryButton close = new SecondaryButton("Đóng");
        close.addActionListener(event -> dispose());
        actions.add(close);
        if (user.getRole() == Role.ADMIN) {
            PrimaryButton add = new PrimaryButton("Thêm thiết bị");
            SecondaryButton update = new SecondaryButton("Cập nhật số lượng");
            SecondaryButton broken = new SecondaryButton("Báo hỏng");
            add.addActionListener(event -> addEquipment());
            update.addActionListener(event -> updateQuantity());
            broken.addActionListener(event -> markBroken());
            actions.add(add);
            actions.add(update);
            actions.add(broken);
        }
        root.add(actions, BorderLayout.SOUTH);
    }

    private JTable buildScheduleTable() {
        GenericTableModel<ScheduleEntry> model = new GenericTableModel<>(
                new String[]{"Thứ", "Lớp", "Môn", "Giảng viên", "Ca"},
                entry -> DateUtils.dayName(entry.getDayOfWeek()),
                entry -> entry.getCourseSection().getCode(),
                entry -> entry.getCourseSection().getCourse().getName(),
                entry -> entry.getCourseSection().getLecturer().getFullName(),
                entry -> entry.getStartSlot().getName() + " - " + entry.getEndSlot().getName());
        model.setRows(controllers.schedules().forRoom(room.getId()));
        JTable table = new JTable(model);
        TableUtils.style(table);
        return table;
    }

    private void refreshEquipment() {
        equipmentModel.setRows(controllers.rooms().findEquipmentByRoom(room.getId()));
    }

    private Equipment selectedEquipment() {
        int row = equipmentTable.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một thiết bị trong bảng.");
        }
        return equipmentModel.getRowAt(equipmentTable.convertRowIndexToModel(row));
    }

    private void addEquipment() {
        EquipmentDialog.showDialog(this, room, null).ifPresent(item -> {
            UiTasks.run(this, "Đang thêm thiết bị…", () -> controllers.rooms().saveEquipment(item), () -> {
                refreshEquipment();
                Dialogs.success(this, "Đã thêm thiết bị vào phòng.");
            });
        });
    }

    private void updateQuantity() {
        try {
            Equipment item = selectedEquipment();
            String input = JOptionPane.showInputDialog(this, "Nhập số lượng mới:", item.getQuantity());
            if (input == null) {
                return;
            }
            Integer.parseInt(input.trim());
            UiTasks.run(this, "Đang cập nhật thiết bị…", () -> controllers.rooms().updateEquipmentQuantity(item, input), () -> {
                refreshEquipment();
                Dialogs.success(this, "Đã cập nhật số lượng thiết bị.");
            });
        } catch (NumberFormatException ex) {
            Dialogs.error(this, "Số lượng phải là số nguyên dương.");
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void markBroken() {
        try {
            Equipment item = selectedEquipment();
            UiTasks.run(this, "Đang báo hỏng thiết bị…", () -> controllers.rooms().markEquipmentBroken(item), () -> {
                refreshEquipment();
                Dialogs.success(this, "Đã đánh dấu thiết bị bị hỏng.");
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
