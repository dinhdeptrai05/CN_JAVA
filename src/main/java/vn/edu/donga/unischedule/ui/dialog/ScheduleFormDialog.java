package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.model.Enums.ScheduleStatus;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.validation.ValidationException;

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
import java.util.Optional;

public class ScheduleFormDialog extends JDialog {
    private final JComboBox<Semester> semesterBox = new JComboBox<>();
    private final JComboBox<CourseSection> sectionBox = new JComboBox<>();
    private final JComboBox<Lecturer> lecturerBox = new JComboBox<>();
    private final JComboBox<DayItem> dayBox = new JComboBox<>();
    private final JComboBox<TimeSlot> startSlotBox = new JComboBox<>();
    private final JComboBox<TimeSlot> endSlotBox = new JComboBox<>();
    private final JComboBox<Classroom> roomBox = new JComboBox<>();
    private final JTextField startDateField = new JTextField();
    private final JTextField endDateField = new JTextField();
    private final JTextArea noteArea = new JTextArea(3, 28);
    private ScheduleEntry result;
    private final ScheduleEntry editing;

    public ScheduleFormDialog(JFrame owner, AppServices services, ScheduleEntry editing) {
        super(owner, editing == null ? "Thêm lịch học" : "Sửa lịch học", true);
        this.editing = editing;
        services.catalog().getSemesters().forEach(semesterBox::addItem);
        services.courseSections().findAll().forEach(sectionBox::addItem);
        services.catalog().getLecturers().forEach(lecturerBox::addItem);
        services.catalog().getTimeSlots().forEach(slot -> {
            startSlotBox.addItem(slot);
            endSlotBox.addItem(slot);
        });
        services.rooms().findAll().forEach(roomBox::addItem);
        for (int day = 2; day <= 8; day++) {
            dayBox.addItem(new DayItem(day, DateUtils.dayName(day)));
        }
        buildUi();
        fill(editing);
        setSize(620, 620);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static Optional<ScheduleEntry> showDialog(Component parent, AppServices services, ScheduleEntry editing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        ScheduleFormDialog dialog = new ScheduleFormDialog(owner, services, editing);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        setContentPane(root);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        addRow(form, gbc, "Học kỳ", semesterBox);
        addRow(form, gbc, "Lớp học phần", sectionBox);
        addRow(form, gbc, "Giảng viên", lecturerBox);
        addRow(form, gbc, "Ngày trong tuần", dayBox);
        addRow(form, gbc, "Ca bắt đầu", startSlotBox);
        addRow(form, gbc, "Ca kết thúc", endSlotBox);
        addRow(form, gbc, "Phòng học", roomBox);
        addRow(form, gbc, "Ngày bắt đầu (yyyy-MM-dd)", startDateField);
        addRow(form, gbc, "Ngày kết thúc (yyyy-MM-dd)", endDateField);
        addRow(form, gbc, "Ghi chú", new JScrollPane(noteArea));
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        SecondaryButton cancel = new SecondaryButton("Hủy");
        PrimaryButton save = new PrimaryButton("Lưu");
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> save());
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
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

    private void fill(ScheduleEntry entry) {
        LocalDate start = DateUtils.currentWeekMonday();
        LocalDate end = start.plusWeeks(14);
        if (entry != null) {
            semesterBox.setSelectedItem(entry.getCourseSection().getSemester());
            sectionBox.setSelectedItem(entry.getCourseSection());
            lecturerBox.setSelectedItem(entry.getCourseSection().getLecturer());
            selectDay(entry.getDayOfWeek());
            startSlotBox.setSelectedItem(entry.getStartSlot());
            endSlotBox.setSelectedItem(entry.getEndSlot());
            roomBox.setSelectedItem(entry.getRoom());
            start = entry.getStartDate();
            end = entry.getEndDate();
            noteArea.setText(entry.getNote());
        } else if (endSlotBox.getItemCount() > 1) {
            endSlotBox.setSelectedIndex(1);
        }
        startDateField.setText(start.toString());
        endDateField.setText(end.toString());
    }

    private void selectDay(int day) {
        for (int i = 0; i < dayBox.getItemCount(); i++) {
            if (dayBox.getItemAt(i).value == day) {
                dayBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void save() {
        try {
            CourseSection section = (CourseSection) sectionBox.getSelectedItem();
            Lecturer lecturer = (Lecturer) lecturerBox.getSelectedItem();
            if (section != null && lecturer != null) {
                section.setLecturer(lecturer);
            }
            LocalDate start = LocalDate.parse(startDateField.getText().trim(), DateUtils.INPUT_FORMAT);
            LocalDate end = LocalDate.parse(endDateField.getText().trim(), DateUtils.INPUT_FORMAT);
            result = new ScheduleEntry(editing == null ? null : editing.getId(), section, (Classroom) roomBox.getSelectedItem(),
                    ((DayItem) dayBox.getSelectedItem()).value, (TimeSlot) startSlotBox.getSelectedItem(),
                    (TimeSlot) endSlotBox.getSelectedItem(), start, end,
                    editing == null ? ScheduleStatus.PUBLISHED : editing.getStatus(), noteArea.getText().trim());
            dispose();
        } catch (Exception ex) {
            throwOrShow(ex, "Ngày phải nhập theo định dạng yyyy-MM-dd.");
        }
    }

    private void throwOrShow(Exception ex, String fallback) {
        if (ex instanceof ValidationException validationException) {
            Dialogs.error(this, validationException.getMessage());
        } else {
            Dialogs.error(this, fallback);
        }
    }

    private record DayItem(int value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
