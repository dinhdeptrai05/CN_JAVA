package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.controller.FormController;
import vn.edu.donga.unischedule.model.Classroom;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.TimeSlot;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.CourseSectionStatus;
import vn.edu.donga.unischedule.model.Enums.Priority;
import vn.edu.donga.unischedule.model.Enums.RequestType;
import vn.edu.donga.unischedule.model.Enums.ScheduleStatus;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.component.UiTasks;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;

/** One-day makeup lesson request. An existing teaching schedule identifies the assigned section. */
public final class MakeupClassDialog extends JDialog {
    private final AppControllers controllers;
    private final User lecturer;
    private final JComboBox<ScheduleEntry> classBox = new JComboBox<>();
    private final JComboBox<TimeSlot> slotBox = new JComboBox<>();
    private final JComboBox<Classroom> roomBox = new JComboBox<>();
    private final JTextField dateField = new JTextField(LocalDate.now().plusDays(1).toString());
    private final JTextArea reasonArea = new JTextArea(4, 28);
    private boolean sent;

    private MakeupClassDialog(JFrame owner, AppControllers controllers, User lecturer,
                              ScheduleEntry selectedClass, Classroom selectedRoom, LocalDate date, TimeSlot slot) {
        super(owner, "Đăng ký lịch học bù", true);
        this.controllers = controllers;
        this.lecturer = lecturer;
        var seen = new HashSet<Long>();
        controllers.schedules().findAll().stream()
                .filter(entry -> entry.getStatus() == ScheduleStatus.PUBLISHED)
                .filter(entry -> entry.getCourseSection().getLecturer().getId().equals(lecturer.getId()))
                .filter(entry -> entry.getCourseSection().getStatus() != CourseSectionStatus.CANCELLED
                        && entry.getCourseSection().getStatus() != CourseSectionStatus.CLOSED)
                .filter(entry -> seen.add(entry.getCourseSection().getId()))
                .forEach(classBox::addItem);
        classBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                                                                      int index, boolean selected, boolean focus) {
                ScheduleEntry entry = (ScheduleEntry) value;
                String label = entry == null ? "Chọn lớp học phần" : entry.getCourseSection().getCode()
                        + " - " + entry.getCourseSection().getCourse().getName();
                return super.getListCellRendererComponent(list, label, index, selected, focus);
            }
        });
        if (selectedClass != null) {
            for (int i = 0; i < classBox.getItemCount(); i++) {
                if (classBox.getItemAt(i).getCourseSection().getId().equals(selectedClass.getCourseSection().getId())) {
                    classBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        controllers.catalog().getTimeSlots().forEach(slotBox::addItem);
        if (date != null) dateField.setText(date.toString());
        if (slot != null) slotBox.setSelectedItem(slot);
        buildUi();
        reloadRooms(selectedRoom);
        setSize(590, 410);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static boolean showDialog(Component parent, AppControllers controllers, User lecturer,
                                     ScheduleEntry selectedClass, Classroom room, LocalDate date, TimeSlot slot) {
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(parent);
        var dialog = new MakeupClassDialog(owner, controllers, lecturer, selectedClass, room, date, slot);
        dialog.setVisible(true);
        return dialog.sent;
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        setContentPane(root);
        JLabel note = new JLabel("Chọn lớp, ngày và ca. Phòng đào tạo sẽ kiểm tra lại trước khi duyệt.");
        root.add(note, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        row(form, gbc, "Lớp học phần", classBox);
        row(form, gbc, "Ngày học bù (yyyy-MM-dd)", dateField);
        row(form, gbc, "Ca học", slotBox);
        JPanel roomSelection = new JPanel(new BorderLayout(8, 0));
        roomSelection.add(roomBox, BorderLayout.CENTER);
        SecondaryButton search = new SecondaryButton("Tìm phòng trống");
        search.addActionListener(event -> {
            try {
                LocalDate date = LocalDate.parse(dateField.getText().trim(), DateUtils.INPUT_FORMAT);
                TimeSlot slot = (TimeSlot) slotBox.getSelectedItem();
                ScheduleEntry selectedClass = (ScheduleEntry) classBox.getSelectedItem();
                int students = selectedClass == null ? 0 : selectedClass.getCourseSection().getStudentCount();
                UiTasks.run(this, "Đang tìm phòng trống…",
                        () -> controllers.rooms().searchAvailableRooms(date, slot, null, null, students, ""),
                        rooms -> setRooms(rooms, null), error -> Dialogs.error(this, UiTasks.message(error)));
            }
            catch (DateTimeParseException ex) { Dialogs.error(this, "Ngày học bù phải nhập theo định dạng yyyy-MM-dd."); }
        });
        roomSelection.add(search, BorderLayout.EAST);
        row(form, gbc, "Phòng", roomSelection);
        row(form, gbc, "Lý do học bù", new JScrollPane(reasonArea));
        root.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        SecondaryButton cancel = new SecondaryButton("Hủy");
        cancel.addActionListener(event -> dispose());
        PrimaryButton submit = new PrimaryButton("Gửi phòng đào tạo");
        submit.addActionListener(event -> submit());
        actions.add(cancel);
        actions.add(submit);
        root.add(actions, BorderLayout.SOUTH);
    }

    private void row(JPanel form, GridBagConstraints gbc, String label, Component input) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(input, gbc);
        gbc.gridy++;
    }

    private void reloadRooms(Classroom selectedRoom) {
        LocalDate date = LocalDate.parse(dateField.getText().trim(), DateUtils.INPUT_FORMAT);
        TimeSlot slot = (TimeSlot) slotBox.getSelectedItem();
        ScheduleEntry selectedClass = (ScheduleEntry) classBox.getSelectedItem();
        int students = selectedClass == null ? 0 : selectedClass.getCourseSection().getStudentCount();
        setRooms(controllers.rooms().searchAvailableRooms(date, slot, null, null, students, ""), selectedRoom);
    }

    private void setRooms(List<Classroom> rooms, Classroom selectedRoom) {
        roomBox.removeAllItems();
        rooms.forEach(roomBox::addItem);
        if (selectedRoom != null) {
            roomBox.setSelectedIndex(-1);
            for (int i = 0; i < roomBox.getItemCount(); i++) {
                if (roomBox.getItemAt(i).getId().equals(selectedRoom.getId())) {
                    roomBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void submit() {
        try {
            ScheduleEntry selectedClass = (ScheduleEntry) classBox.getSelectedItem();
            if (selectedClass == null) throw new ValidationException("Bạn chưa có lớp học phần đang dạy để đăng ký học bù.");
            LocalDate date = LocalDate.parse(dateField.getText().trim(), DateUtils.INPUT_FORMAT);
            TimeSlot slot = (TimeSlot) slotBox.getSelectedItem();
            Classroom room = (Classroom) roomBox.getSelectedItem();
            if (room == null) throw new ValidationException("Chưa có phòng trống phù hợp. Hãy tìm lại phòng.");
            String reason = reasonArea.getText();
            UiTasks.run(this, "Đang gửi yêu cầu học bù…", () -> {
                boolean stillFree = controllers.rooms().searchAvailableRooms(date, slot, null, null,
                        selectedClass.getCourseSection().getStudentCount(), "").stream()
                        .anyMatch(candidate -> candidate.getId().equals(room.getId()));
                if (!stillFree) throw new ValidationException("Phòng không còn trống cho ngày, ca và sĩ số đã chọn. Hãy tìm lại phòng.");
                var draft = FormController.request(lecturer, RequestType.USE_ROOM, selectedClass, room,
                        date.toString(), slot, "", "0", reason, Priority.NORMAL);
                return controllers.requests().create(draft);
            }, ignored -> { sent = true; dispose(); },
                    error -> Dialogs.error(this, UiTasks.message(error)));
        } catch (DateTimeParseException ex) {
            Dialogs.error(this, "Ngày học bù phải nhập theo định dạng yyyy-MM-dd.");
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
