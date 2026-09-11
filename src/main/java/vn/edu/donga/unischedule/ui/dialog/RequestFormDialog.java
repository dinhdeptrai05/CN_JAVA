package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.Priority;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.RequestType;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class RequestFormDialog extends JDialog {
    private final JComboBox<RequestType> typeBox = new JComboBox<>(new RequestType[]{
            RequestType.CHANGE_SCHEDULE, RequestType.CHANGE_ROOM, RequestType.BORROW_EQUIPMENT, RequestType.USE_ROOM
    });
    private final JComboBox<ScheduleEntry> scheduleBox = new JComboBox<>();
    private final JComboBox<Classroom> roomBox = new JComboBox<>();
    private final JComboBox<TimeSlot> slotBox = new JComboBox<>();
    private final JTextField dateField = new JTextField(LocalDate.now().plusDays(2).toString());
    private final JTextField equipmentField = new JTextField();
    private final JTextField quantityField = new JTextField("1");
    private final JTextArea reasonArea = new JTextArea(4, 28);
    private final JComboBox<Priority> priorityBox = new JComboBox<>(Priority.values());
    private final User user;
    private ChangeRequest result;

    public RequestFormDialog(JFrame owner, AppControllers controllers, User user) {
        super(owner, "Gửi yêu cầu", true);
        this.user = user;
        controllers.schedules().findByWeekForUser(DateUtils.currentWeekMonday(), user).forEach(scheduleBox::addItem);
        controllers.rooms().findAll().forEach(roomBox::addItem);
        controllers.catalog().getTimeSlots().forEach(slotBox::addItem);
        buildUi();
        setSize(620, 560);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static Optional<ChangeRequest> showDialog(Component parent, AppControllers controllers, User user) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        RequestFormDialog dialog = new RequestFormDialog(owner, controllers, user);
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
        addRow(form, gbc, "Loại yêu cầu", typeBox);
        addRow(form, gbc, "Lịch học liên quan", scheduleBox);
        addRow(form, gbc, "Phòng mong muốn", roomBox);
        addRow(form, gbc, "Ngày mong muốn (yyyy-MM-dd)", dateField);
        addRow(form, gbc, "Ca mong muốn", slotBox);
        addRow(form, gbc, "Thiết bị", equipmentField);
        addRow(form, gbc, "Số lượng thiết bị", quantityField);
        addRow(form, gbc, "Mức ưu tiên", priorityBox);
        addRow(form, gbc, "Lý do", new JScrollPane(reasonArea));
        root.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        SecondaryButton cancel = new SecondaryButton("Hủy");
        PrimaryButton send = new PrimaryButton("Gửi yêu cầu");
        cancel.addActionListener(event -> dispose());
        send.addActionListener(event -> save());
        actions.add(cancel);
        actions.add(send);
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

    private void save() {
        try {
            result = vn.edu.donga.unischedule.controller.FormController.request(user, (RequestType) typeBox.getSelectedItem(),
                    (ScheduleEntry) scheduleBox.getSelectedItem(), (Classroom) roomBox.getSelectedItem(),
                    dateField.getText(), (TimeSlot) slotBox.getSelectedItem(), equipmentField.getText(), quantityField.getText(),
                    reasonArea.getText(), (Priority) priorityBox.getSelectedItem());
            dispose();
        } catch (NumberFormatException ex) {
            Dialogs.error(this, "Số lượng thiết bị phải là số nguyên.");
        } catch (Exception ex) {
            Dialogs.error(this, "Ngày mong muốn phải nhập theo định dạng yyyy-MM-dd.");
        }
    }
}
