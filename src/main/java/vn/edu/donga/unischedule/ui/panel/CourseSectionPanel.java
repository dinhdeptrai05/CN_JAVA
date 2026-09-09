package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.CourseSection;
import vn.edu.donga.unischedule.model.Lecturer;
import vn.edu.donga.unischedule.model.Student;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.CourseSectionStatus;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.service.AppServices;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.dialog.CourseSectionDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.ui.renderer.BadgeRenderer;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.util.TextUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class CourseSectionPanel extends JPanel implements Refreshable {
    private final AppServices services;
    private final User user;
    private final SearchField searchField = new SearchField("Tìm mã lớp hoặc tên môn");
    private final JComboBox<String> semesterBox = new JComboBox<>();
    private final JComboBox<String> departmentBox = new JComboBox<>();
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final GenericTableModel<CourseSection> tableModel;
    private final JTable table;

    public CourseSectionPanel(AppServices services, User user) {
        this.services = services;
        this.user = user;
        tableModel = new GenericTableModel<>(
                new String[]{"Mã lớp", "Môn học", "Tín chỉ", "Khoa", "Sĩ số", "Giảng viên", "Trạng thái"},
                CourseSection::getCode,
                section -> section.getCourse().getName(),
                section -> section.getCourse().getCredits(),
                section -> section.getCourse().getDepartment().getName(),
                section -> section.getStudentCount() + "/" + section.getCapacity(),
                section -> section.getLecturer().getFullName(),
                CourseSection::getStatus);
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
        semesterBox.addItem("Tất cả học kỳ");
        services.catalog().getSemesters().forEach(semester -> semesterBox.addItem(semester.getName()));
        departmentBox.addItem("Tất cả khoa");
        services.catalog().getDepartments().forEach(department -> departmentBox.addItem(department.getName()));
        statusBox.addItem("Tất cả trạng thái");
        for (CourseSectionStatus status : CourseSectionStatus.values()) {
            statusBox.addItem(status.getDisplayName());
        }
        semesterBox.addActionListener(event -> refresh());
        departmentBox.addActionListener(event -> refresh());
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
        JPanel filters = new JPanel(new GridLayout(1, 4, 10, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(semesterBox);
        filters.add(departmentBox);
        filters.add(statusBox);
        top.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        SecondaryButton detail = new SecondaryButton("Xem chi tiết");
        detail.addActionListener(event -> showDetail());
        actions.add(detail);
        if (user.getRole() == Role.ACADEMIC) {
            PrimaryButton add = new PrimaryButton("Thêm lớp");
            SecondaryButton edit = new SecondaryButton("Sửa");
            SecondaryButton assign = new SecondaryButton("Phân công giảng viên");
            add.addActionListener(event -> addSection());
            edit.addActionListener(event -> editSection());
            assign.addActionListener(event -> assignLecturer());
            actions.add(add);
            actions.add(edit);
            actions.add(assign);
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
        String semester = (String) semesterBox.getSelectedItem();
        String department = (String) departmentBox.getSelectedItem();
        String status = (String) statusBox.getSelectedItem();
        List<CourseSection> rows = services.courseSections().findAll().stream()
                .filter(this::allowedForRole)
                .filter(section -> semester == null || semester.startsWith("Tất cả") || section.getSemester().getName().equals(semester))
                .filter(section -> department == null || department.startsWith("Tất cả") || section.getCourse().getDepartment().getName().equals(department))
                .filter(section -> status == null || status.startsWith("Tất cả") || section.getStatus().getDisplayName().equals(status))
                .filter(section -> keyword == null || keyword.isBlank()
                        || TextUtils.containsIgnoreAccent(section.getCode(), keyword)
                        || TextUtils.containsIgnoreAccent(section.getCourse().getName(), keyword))
                .toList();
        tableModel.setRows(rows);
    }

    private boolean allowedForRole(CourseSection section) {
        if (user.getRole() == Role.LECTURER) {
            return section.getLecturer().getId().equals(user.getId());
        }
        if (user instanceof Student student) {
            return section.getCourse().getDepartment().getCode().equalsIgnoreCase(student.getClassCode().substring(0, 4));
        }
        return true;
    }

    private CourseSection selectedSection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một lớp học phần trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void addSection() {
        CourseSectionDialog.showDialog(this, services, null).ifPresent(section -> {
            try {
                services.courseSections().save(section);
                refresh();
                Dialogs.success(this, "Đã thêm lớp học phần.");
            } catch (ValidationException ex) {
                Dialogs.error(this, ex.getMessage());
            }
        });
    }

    private void editSection() {
        try {
            CourseSectionDialog.showDialog(this, services, selectedSection()).ifPresent(section -> {
                try {
                    services.courseSections().save(section);
                    refresh();
                    Dialogs.success(this, "Đã cập nhật lớp học phần.");
                } catch (ValidationException ex) {
                    Dialogs.error(this, ex.getMessage());
                }
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void assignLecturer() {
        try {
            CourseSection section = selectedSection();
            JComboBox<Lecturer> lecturerBox = new JComboBox<>();
            services.catalog().getLecturers().forEach(lecturerBox::addItem);
            lecturerBox.setSelectedItem(section.getLecturer());
            int result = JOptionPane.showConfirmDialog(this, lecturerBox, "Chọn giảng viên",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                section.setLecturer((Lecturer) lecturerBox.getSelectedItem());
                services.courseSections().save(section);
                refresh();
                Dialogs.success(this, "Đã phân công giảng viên cho lớp học phần.");
            }
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void showDetail() {
        try {
            CourseSection section = selectedSection();
            JTextArea area = new JTextArea("""
                    Mã lớp: %s
                    Môn học: %s
                    Học kỳ: %s
                    Khoa: %s
                    Tín chỉ: %d
                    Giảng viên: %s
                    Sĩ số: %d/%d
                    Trạng thái: %s
                    """.formatted(section.getCode(), section.getCourse().getName(),
                    section.getSemester().getName(), section.getCourse().getDepartment().getName(),
                    section.getCourse().getCredits(), section.getLecturer().getFullName(),
                    section.getStudentCount(), section.getCapacity(), section.getStatus().getDisplayName()));
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, area, "Chi tiết lớp học phần", JOptionPane.INFORMATION_MESSAGE);
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
