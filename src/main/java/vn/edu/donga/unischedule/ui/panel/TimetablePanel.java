package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.Department;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.Semester;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.DangerButton;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.RoundedPanel;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.dialog.ScheduleFormDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.util.TextUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

public class TimetablePanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final User user;
    private final SearchField searchField = new SearchField("Tìm môn, lớp, giảng viên, phòng");
    private final JComboBox<String> semesterBox = new JComboBox<>();
    private final JComboBox<String> departmentBox = new JComboBox<>();
    private final JComboBox<String> lecturerBox = new JComboBox<>();
    private final JComboBox<String> roomBox = new JComboBox<>();
    private final JComboBox<String> viewModeBox = new JComboBox<>(new String[]{"Dạng lịch", "Dạng bảng"});
    private final GenericTableModel<ScheduleEntry> tableModel;
    private final JTable table;
    private final JPanel calendarPanel = new JPanel(new BorderLayout());
    private final JPanel viewPanel = new JPanel(new CardLayout());
    private LocalDate weekStart = DateUtils.currentWeekMonday();
    private final JLabel weekLabel = new JLabel();

    public void search(String keyword) {
        searchField.setText(keyword);
        viewModeBox.setSelectedIndex(1);
        refresh();
    }

    public TimetablePanel(AppControllers controllers, User user) {
        this.controllers = controllers;
        this.user = user;
        tableModel = new GenericTableModel<>(
                new String[]{"Thứ", "Ca", "Mã lớp", "Môn học", "Phòng", "Giảng viên", "Trạng thái"},
                entry -> DateUtils.dayName(entry.getDayOfWeek()),
                entry -> entry.getStartSlot().getName() + " - " + entry.getEndSlot().getName(),
                entry -> entry.getCourseSection().getCode(),
                entry -> entry.getCourseSection().getCourse().getName(),
                entry -> entry.getRoom().getCode(),
                entry -> entry.getCourseSection().getLecturer().getFullName(),
                entry -> entry.getStatus().getDisplayName());
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
        semesterBox.addItem("Tất cả học kỳ");
        for (Semester semester : controllers.catalog().getSemesters()) {
            semesterBox.addItem(semester.getName());
        }
        departmentBox.addItem("Tất cả khoa");
        for (Department department : controllers.catalog().getDepartments()) {
            departmentBox.addItem(department.getName());
        }
        lecturerBox.addItem("Tất cả giảng viên");
        controllers.catalog().getLecturers().forEach(lecturer -> lecturerBox.addItem(lecturer.getFullName()));
        roomBox.addItem("Tất cả phòng");
        controllers.rooms().findAll().forEach(room -> roomBox.addItem(room.getCode()));
        semesterBox.addActionListener(event -> refresh());
        departmentBox.addActionListener(event -> refresh());
        lecturerBox.addActionListener(event -> refresh());
        roomBox.addActionListener(event -> refresh());
        viewModeBox.addActionListener(event -> switchView());
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
        JPanel toolbar = new JPanel(new BorderLayout(12, 12));
        toolbar.setOpaque(false);
        JPanel weekControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        weekControls.setOpaque(false);
        SecondaryButton previous = new SecondaryButton("Tuần trước");
        SecondaryButton today = new SecondaryButton("Tuần hiện tại");
        SecondaryButton next = new SecondaryButton("Tuần sau");
        previous.addActionListener(event -> {
            weekStart = weekStart.minusWeeks(1);
            refresh();
        });
        today.addActionListener(event -> {
            weekStart = DateUtils.currentWeekMonday();
            refresh();
        });
        next.addActionListener(event -> {
            weekStart = weekStart.plusWeeks(1);
            refresh();
        });
        weekControls.add(previous);
        weekControls.add(today);
        weekControls.add(next);
        weekControls.add(weekLabel);
        toolbar.add(weekControls, BorderLayout.NORTH);

        JPanel filters = new JPanel(new GridLayout(2, 3, 10, 10));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(semesterBox);
        filters.add(departmentBox);
        filters.add(lecturerBox);
        filters.add(roomBox);
        filters.add(viewModeBox);
        toolbar.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        SecondaryButton detail = new SecondaryButton("Xem chi tiết");
        detail.addActionListener(event -> showSelectedDetail());
        actions.add(detail);
        if (user.getRole() == Role.ACADEMIC) {
            PrimaryButton add = new PrimaryButton("Thêm lịch");
            SecondaryButton edit = new SecondaryButton("Sửa");
            DangerButton delete = new DangerButton("Xóa");
            add.addActionListener(event -> addSchedule());
            edit.addActionListener(event -> editSchedule());
            delete.addActionListener(event -> deleteSchedule());
            actions.add(add);
            actions.add(edit);
            actions.add(delete);
        }
        toolbar.add(actions, BorderLayout.SOUTH);
        add(toolbar, BorderLayout.NORTH);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedDetail();
                }
            }
        });
        viewPanel.add(new JScrollPane(table), "table");
        viewPanel.add(new JScrollPane(calendarPanel), "calendar");
        add(viewPanel, BorderLayout.CENTER);
    }

    private void switchView() {
        CardLayout layout = (CardLayout) viewPanel.getLayout();
        layout.show(viewPanel, viewModeBox.getSelectedIndex() == 0 ? "calendar" : "table");
    }

    @Override
    public void refresh() {
        weekLabel.setText("Tuần: " + DateUtils.format(weekStart) + " - " + DateUtils.format(weekStart.plusDays(6)));
        List<ScheduleEntry> rows = filteredRows();
        tableModel.setRows(rows);
        rebuildCalendar(rows);
        switchView();
    }

    private List<ScheduleEntry> filteredRows() {
        String keyword = searchField.getText();
        String semester = (String) semesterBox.getSelectedItem();
        String department = (String) departmentBox.getSelectedItem();
        String lecturer = (String) lecturerBox.getSelectedItem();
        String room = (String) roomBox.getSelectedItem();
        return controllers.schedules().search(weekStart, user, keyword, semester, department, lecturer, room);
    }

    private void rebuildCalendar(List<ScheduleEntry> rows) {
        calendarPanel.removeAll();
        JPanel grid = new JPanel(new GridLayout(controllers.catalog().getTimeSlots().size() + 1, 8, 1, 1));
        grid.setBackground(Color.decode("#CBD5E1"));
        grid.add(headerCell(""));
        for (int day = 2; day <= 8; day++) {
            grid.add(headerCell(DateUtils.dayName(day)));
        }
        controllers.catalog().getTimeSlots().forEach(slot -> {
            grid.add(headerCell(slot.getName()));
            for (int day = 2; day <= 8; day++) {
                JPanel cell = new JPanel(new GridLayout(0, 1, 4, 4));
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                int schoolDay = day;
                rows.stream()
                        .filter(entry -> entry.getDayOfWeek() == schoolDay
                                && entry.getStartSlot().getOrder() <= slot.getOrder()
                                && slot.getOrder() <= entry.getEndSlot().getOrder())
                        .forEach(entry -> cell.add(scheduleChip(entry)));
                grid.add(cell);
            }
        });
        calendarPanel.add(grid, BorderLayout.CENTER);
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private Component headerCell(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setOpaque(true);
        label.setBackground(Color.decode("#F8FAFC"));
        label.setForeground(AppConfig.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        return label;
    }

    private Component scheduleChip(ScheduleEntry entry) {
        String[] colors = {"#EEF2FF", "#ECFDF5", "#FFF7ED", "#FDF2F8"};
        RoundedPanel chip = new RoundedPanel(8, Color.decode(colors[Math.floorMod(entry.getCourseSection().getId().intValue(), colors.length)]));
        chip.setLayout(new BorderLayout());
        chip.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
        JLabel label = new JLabel("<html><b>" + entry.getCourseSection().getCode() + "</b><br>"
                + entry.getRoom().getCode() + " - " + entry.getCourseSection().getLecturer().getFullName() + "</html>");
        label.setForeground(AppConfig.TEXT);
        chip.add(label, BorderLayout.CENTER);
        chip.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showDetail(entry);
            }
        });
        return chip;
    }

    private ScheduleEntry selectedEntry() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một lịch trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void addSchedule() {
        ScheduleFormDialog.showDialog(this, controllers, null).ifPresent(entry -> {
            try {
                controllers.schedules().save(entry);
                refresh();
                Dialogs.success(this, "Đã thêm lịch học vào dữ liệu giả.");
            } catch (ValidationException ex) {
                Dialogs.error(this, ex.getMessage());
            }
        });
    }

    private void editSchedule() {
        try {
            ScheduleEntry entry = selectedEntry();
            ScheduleFormDialog.showDialog(this, controllers, entry).ifPresent(updated -> {
                try {
                    controllers.schedules().save(updated);
                    refresh();
                    Dialogs.success(this, "Đã cập nhật lịch học.");
                } catch (ValidationException ex) {
                    Dialogs.error(this, ex.getMessage());
                }
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void deleteSchedule() {
        try {
            ScheduleEntry entry = selectedEntry();
            if (Dialogs.confirm(this, "Xóa lịch " + entry.getCourseSection().getCode() + " khỏi dữ liệu giả?")) {
                controllers.schedules().delete(entry.getId());
                refresh();
                Dialogs.success(this, "Đã xóa lịch học.");
            }
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void showSelectedDetail() {
        try {
            showDetail(selectedEntry());
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void showDetail(ScheduleEntry entry) {
        JTextArea area = new JTextArea("""
                Lớp học phần: %s
                Môn học: %s
                Giảng viên: %s
                Phòng học: %s
                Thời gian: %s, %s đến %s
                Hiệu lực: %s - %s
                Trạng thái: %s
                Ghi chú: %s
                """.formatted(entry.getCourseSection().getCode(),
                entry.getCourseSection().getCourse().getName(),
                entry.getCourseSection().getLecturer().getFullName(),
                entry.getRoom().getCode(),
                DateUtils.dayName(entry.getDayOfWeek()),
                entry.getStartSlot().getName(),
                entry.getEndSlot().getName(),
                DateUtils.format(entry.getStartDate()),
                DateUtils.format(entry.getEndDate()),
                entry.getStatus().getDisplayName(),
                entry.getNote()));
        area.setEditable(false);
        javax.swing.JOptionPane.showMessageDialog(this, new JScrollPane(area), "Chi tiết lịch học", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
}
