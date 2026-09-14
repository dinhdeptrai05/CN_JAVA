package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.Course;
import vn.edu.donga.unischedule.model.CourseSection;
import vn.edu.donga.unischedule.model.Lecturer;
import vn.edu.donga.unischedule.model.Semester;
import vn.edu.donga.unischedule.model.Enums.CourseSectionStatus;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.util.Dialogs;

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

public class CourseSectionDialog extends JDialog {
    private final JTextField codeField = new JTextField();
    private final JComboBox<Course> courseBox = new JComboBox<>();
    private final JComboBox<Semester> semesterBox = new JComboBox<>();
    private final JComboBox<Lecturer> lecturerBox = new JComboBox<>();
    private final JTextField capacityField = new JTextField();
    private final JTextField studentCountField = new JTextField();
    private final JComboBox<CourseSectionStatus> statusBox = new JComboBox<>(CourseSectionStatus.values());
    private final CourseSection editing;
    private CourseSection result;

    public CourseSectionDialog(JFrame owner, AppControllers controllers, CourseSection editing) {
        super(owner, editing == null ? "Thêm lớp học phần" : "Sửa lớp học phần", true);
        this.editing = editing;
        studentCountField.setEditable(false);
        studentCountField.setToolTipText("Tự động tính từ danh sách sinh viên đăng ký");
        controllers.catalog().getCourses().forEach(courseBox::addItem);
        controllers.catalog().getSemesters().forEach(semesterBox::addItem);
        controllers.catalog().getLecturers().forEach(lecturerBox::addItem);
        buildUi();
        fill(editing);
        setSize(540, 460);
        DialogTheme.apply(this);
        setLocationRelativeTo(owner);
    }

    public static Optional<CourseSection> showDialog(Component parent, AppControllers controllers, CourseSection editing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        CourseSectionDialog dialog = new CourseSectionDialog(owner, controllers, editing);
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
        addRow(form, gbc, "Mã lớp học phần", codeField);
        addRow(form, gbc, "Môn học", courseBox);
        addRow(form, gbc, "Học kỳ", semesterBox);
        addRow(form, gbc, "Giảng viên", lecturerBox);
        addRow(form, gbc, "Sức chứa dự kiến", capacityField);
        addRow(form, gbc, "Sĩ số", studentCountField);
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

    private void fill(CourseSection section) {
        if (section == null) {
            capacityField.setText("50");
            studentCountField.setText("0");
            return;
        }
        codeField.setText(section.getCode());
        courseBox.setSelectedItem(section.getCourse());
        semesterBox.setSelectedItem(section.getSemester());
        lecturerBox.setSelectedItem(section.getLecturer());
        capacityField.setText(String.valueOf(section.getCapacity()));
        studentCountField.setText(String.valueOf(section.getStudentCount()));
        statusBox.setSelectedItem(section.getStatus());
    }

    private void save() {
        try {
            result = vn.edu.donga.unischedule.controller.FormController.section(editing, codeField.getText(),
                    (Course) courseBox.getSelectedItem(), (Semester) semesterBox.getSelectedItem(),
                    (Lecturer) lecturerBox.getSelectedItem(),
                    capacityField.getText(), studentCountField.getText(),
                    (CourseSectionStatus) statusBox.getSelectedItem());
            dispose();
        } catch (NumberFormatException ex) {
            Dialogs.error(this, "Sức chứa và sĩ số phải là số nguyên dương.");
        }
    }
}
