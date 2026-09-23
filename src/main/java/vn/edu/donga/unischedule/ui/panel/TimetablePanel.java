package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.Department;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.Semester;
import vn.edu.donga.unischedule.model.TimetablePeriod;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.ScheduleStatus;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.DangerButton;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.RoundedPanel;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.component.UiTasks;
import vn.edu.donga.unischedule.ui.dialog.ScheduleFormDialog;
import vn.edu.donga.unischedule.ui.dialog.MakeupClassDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimetablePanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final User user;
    private final SearchField searchField = new SearchField("Tìm môn, lớp, giảng viên, phòng");
    private final Timer searchDebounce = new Timer(250, event -> refresh());
    private final JComboBox<Integer> yearBox = new JComboBox<>();
    private final JComboBox<TimetablePeriod> periodBox = new JComboBox<>();
    private final JComboBox<String> scopeBox = new JComboBox<>(new String[]{"Lịch của tôi", "Lịch đã công bố"});
    private final JComboBox<String> departmentBox = new JComboBox<>();
    private final JComboBox<String> lecturerBox = new JComboBox<>();
    private final JComboBox<String> roomBox = new JComboBox<>();
    private final JComboBox<String> viewModeBox = new JComboBox<>(new String[]{"Dạng lịch", "Dạng bảng"});
    private final GenericTableModel<ScheduleEntry> tableModel;
    private final JTable table;
    private final JPanel calendarPanel = new JPanel(new BorderLayout());
    private final JPanel dayStrip = new JPanel(new GridLayout(1, 7, 8, 0));
    private final JScrollPane calendarScroll = new JScrollPane(calendarPanel);
    private final JPanel viewPanel = new JPanel(new CardLayout());
    private LocalDate weekStart = DateUtils.currentWeekMonday();
    private final JLabel weekLabel = new JLabel();
    private final JLabel resultLabel = new JLabel();
    private final SecondaryButton previous = new SecondaryButton("Tuần trước");
    private final SecondaryButton today = new SecondaryButton("Tuần hiện tại");
    private final SecondaryButton next = new SecondaryButton("Tuần sau");
    private final List<Semester> visibleSemesters = new ArrayList<>();
    private boolean updatingPeriod;
    private int selectedDay = -1;
    private static final int CALENDAR_PAGE_SIZE = 48;
    private int calendarPage;
    private List<ScheduleEntry> displayedRows = List.of();
    private ScheduleEntry selectedCard;

    public void search(String keyword) {
        searchField.setText(keyword);
        searchDebounce.stop();
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
        searchDebounce.setRepeats(false);
        LocalDate now = LocalDate.now();
        controllers.catalog().getSemesters().stream()
                .filter(semester -> TimetablePeriod.visibleTo(semester, user.getRole(), now))
                .forEach(visibleSemesters::add);
        visibleSemesters.stream().map(semester -> semester.getStartDate().getYear()).distinct()
                .sorted(Comparator.reverseOrder()).forEach(yearBox::addItem);
        if (visibleSemesters.stream().anyMatch(semester -> semester.getStartDate().getYear() == now.getYear()))
            yearBox.setSelectedItem(now.getYear());
        populatePeriods(TimetablePeriod.of(now));
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ACADEMIC) {
            scopeBox.removeAllItems();
            scopeBox.addItem("Lịch toàn trường");
            scopeBox.setEnabled(false);
        }
        departmentBox.addItem("Tất cả khoa");
        for (Department department : controllers.catalog().getDepartments()) {
            departmentBox.addItem(department.getName());
        }
        lecturerBox.addItem("Tất cả giảng viên");
        controllers.catalog().getLecturers().forEach(lecturer -> lecturerBox.addItem(lecturer.getFullName()));
        roomBox.addItem("Tất cả phòng");
        controllers.rooms().findAll().forEach(room -> roomBox.addItem(room.getCode()));
        yearBox.addActionListener(event -> {
            if (!updatingPeriod) {
                populatePeriods(null);
                selectPeriod();
            }
        });
        periodBox.addActionListener(event -> {
            if (!updatingPeriod) selectPeriod();
        });
        scopeBox.addActionListener(event -> refresh());
        departmentBox.addActionListener(event -> refresh());
        lecturerBox.addActionListener(event -> refresh());
        roomBox.addActionListener(event -> refresh());
        viewModeBox.addActionListener(event -> switchView());
        searchField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchDebounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchDebounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchDebounce.restart();
            }
        });
    }

    private void populatePeriods(TimetablePeriod preferred) {
        updatingPeriod = true;
        periodBox.removeAllItems();
        Integer year = (Integer) yearBox.getSelectedItem();
        if (year != null) {
            visibleSemesters.stream().map(TimetablePeriod::of).distinct()
                    .filter(period -> period.year() == year)
                    .sorted(Comparator.comparingInt(TimetablePeriod::half).reversed())
                    .forEach(periodBox::addItem);
            if (preferred != null) {
                for (int index = 0; index < periodBox.getItemCount(); index++) {
                    if (preferred.equals(periodBox.getItemAt(index))) {
                        periodBox.setSelectedIndex(index);
                        break;
                    }
                }
            }
        }
        updatingPeriod = false;
    }

    private void selectPeriod() {
        TimetablePeriod period = (TimetablePeriod) periodBox.getSelectedItem();
        if (period == null) return;
        LocalDate firstWeek = monday(period.firstDay(visibleSemesters));
        LocalDate currentWeek = DateUtils.currentWeekMonday();
        weekStart = !currentWeek.isBefore(firstWeek) && !currentWeek.isAfter(period.lastDay(visibleSemesters))
                ? currentWeek : firstWeek;
        selectedDay = -1;
        if (user.getRole() == Role.LECTURER || user.getRole() == Role.STUDENT)
            scopeBox.setSelectedIndex(period.equals(TimetablePeriod.of(LocalDate.now())) ? 0 : 1);
        refresh();
    }

    private static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void buildUi() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 12));
        toolbar.setOpaque(false);
        JPanel weekControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        weekControls.setOpaque(false);
        previous.addActionListener(event -> {
            weekStart = weekStart.minusWeeks(1);
            selectedDay = -1;
            refresh();
        });
        today.addActionListener(event -> {
            updatingPeriod = true;
            yearBox.setSelectedItem(LocalDate.now().getYear());
            updatingPeriod = false;
            populatePeriods(TimetablePeriod.of(LocalDate.now()));
            selectPeriod();
        });
        next.addActionListener(event -> {
            weekStart = weekStart.plusWeeks(1);
            selectedDay = -1;
            refresh();
        });
        weekControls.add(previous);
        weekControls.add(today);
        weekControls.add(next);
        weekLabel.setFont(weekLabel.getFont().deriveFont(Font.BOLD, 14f));
        weekLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        weekControls.add(weekLabel);
        toolbar.add(weekControls, BorderLayout.NORTH);

        JPanel filters = new JPanel(new GridLayout(2, 4, 10, 10));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(yearBox);
        filters.add(periodBox);
        filters.add(scopeBox);
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
        if (user.getRole() == Role.LECTURER) {
            PrimaryButton makeup = new PrimaryButton("Đăng ký học bù");
            makeup.addActionListener(event -> {
                if (scopeBox.getSelectedIndex() == 1) {
                    Dialogs.warning(this, "Chuyển sang Lịch của tôi để đăng ký học bù.");
                    return;
                }
                int row = table.getSelectedRow();
                ScheduleEntry selected = viewModeBox.getSelectedIndex() == 0 ? selectedCard
                        : row < 0 ? null : tableModel.getRowAt(table.convertRowIndexToModel(row));
                if (MakeupClassDialog.showDialog(this, controllers, user, selected, null, null, null))
                    Dialogs.success(this, "Yêu cầu học bù đã gửi và đang chờ phòng đào tạo duyệt.");
            });
            actions.add(makeup);
        }
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
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);
        resultLabel.setForeground(AppConfig.MUTED);
        resultLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        actionBar.add(resultLabel, BorderLayout.WEST);
        actionBar.add(actions, BorderLayout.EAST);
        toolbar.add(actionBar, BorderLayout.SOUTH);
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
        calendarScroll.getVerticalScrollBar().setUnitIncrement(20);
        calendarScroll.getHorizontalScrollBar().setUnitIncrement(20);
        calendarScroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel calendarView = new JPanel(new BorderLayout(0, 10));
        calendarView.setBackground(AppConfig.BACKGROUND);
        dayStrip.setBackground(AppConfig.BACKGROUND);
        calendarView.add(dayStrip, BorderLayout.NORTH);
        calendarView.add(calendarScroll, BorderLayout.CENTER);
        viewPanel.add(calendarView, "calendar");
        add(viewPanel, BorderLayout.CENTER);
    }

    private void switchView() {
        CardLayout layout = (CardLayout) viewPanel.getLayout();
        layout.show(viewPanel, viewModeBox.getSelectedIndex() == 0 ? "calendar" : "table");
    }

    @Override
    public void refresh() {
        TimetablePeriod period = (TimetablePeriod) periodBox.getSelectedItem();
        weekLabel.setText(DateUtils.format(weekStart) + " – " + DateUtils.format(weekStart.plusDays(6)));
        if (period != null) {
            previous.setEnabled(!weekStart.minusWeeks(1).plusDays(6).isBefore(period.firstDay(visibleSemesters)));
            next.setEnabled(!weekStart.plusWeeks(1).isAfter(period.lastDay(visibleSemesters)));
        }
        List<ScheduleEntry> rows = filteredRows();
        selectedCard = null;
        calendarPage = 0;
        tableModel.setRows(rows);
        resultLabel.setText(rows.size() + " lịch trong tuần · "
                + ((user.getRole() == Role.STUDENT || user.getRole() == Role.LECTURER) && scopeBox.getSelectedIndex() == 0
                ? "Lịch cá nhân" : "Lịch toàn trường") + " · Chọn một lịch để xem chi tiết");
        rebuildCalendar(rows);
        switchView();
    }

    private List<ScheduleEntry> filteredRows() {
        String keyword = searchField.getText();
        String department = (String) departmentBox.getSelectedItem();
        String lecturer = (String) lecturerBox.getSelectedItem();
        String room = (String) roomBox.getSelectedItem();
        TimetablePeriod period = (TimetablePeriod) periodBox.getSelectedItem();
        if (period == null) return List.of();
        boolean publishedCalendar = (user.getRole() == Role.STUDENT || user.getRole() == Role.LECTURER)
                && scopeBox.getSelectedIndex() == 1;
        return controllers.schedules().search(weekStart, user, keyword, null, department, lecturer, room, publishedCalendar)
                .stream().filter(entry -> period.includes(entry.getCourseSection().getSemester()))
                .sorted(Comparator.comparingInt(ScheduleEntry::getDayOfWeek)
                        .thenComparingInt(entry -> entry.getStartSlot().getOrder())
                        .thenComparing(entry -> entry.getCourseSection().getCode()))
                .toList();
    }

    private void rebuildCalendar(List<ScheduleEntry> rows) {
        displayedRows = rows;
        if (selectedDay < 2 || selectedDay > 8) {
            int todayDay = DateUtils.currentWeekMonday().equals(weekStart) ? DateUtils.toSchoolDay(LocalDate.now()) : -1;
            selectedDay = todayDay >= 2 && todayDay <= 8 && rows.stream().anyMatch(entry -> entry.getDayOfWeek() == todayDay)
                    ? todayDay : rows.stream().mapToInt(ScheduleEntry::getDayOfWeek).findFirst().orElse(2);
        }
        dayStrip.removeAll();
        for (int day = 2; day <= 8; day++) {
            int schoolDay = day;
            long count = rows.stream().filter(entry -> entry.getDayOfWeek() == schoolDay).count();
            LocalDate date = weekStart.plusDays(day - 2L);
            JButton button = new JButton("<html><center><b>" + DateUtils.dayName(day) + " · "
                    + DateUtils.format(date).substring(0, 5) + "</b><br>" + count + " lịch</center></html>");
            boolean selected = day == selectedDay;
            button.setOpaque(true);
            button.setBackground(selected ? Color.decode("#EDEBFF") : Color.WHITE);
            button.setForeground(selected ? AppConfig.PRIMARY : AppConfig.TEXT);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? AppConfig.PRIMARY : Color.decode("#E2E8F0")),
                    BorderFactory.createEmptyBorder(9, 4, 9, 4)));
            button.setFocusPainted(false);
            button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            button.addActionListener(event -> {
                selectedDay = schoolDay;
                calendarPage = 0;
                rebuildCalendar(displayedRows);
            });
            dayStrip.add(button);
        }
        calendarPanel.removeAll();
        calendarPanel.setBackground(AppConfig.BACKGROUND);
        List<ScheduleEntry> daily = rows.stream().filter(entry -> entry.getDayOfWeek() == selectedDay).toList();
        LocalDate date = weekStart.plusDays(selectedDay - 2L);
        JLabel heading = new JLabel(DateUtils.dayName(selectedDay) + " · " + DateUtils.format(date)
                + "     " + daily.size() + " lịch học");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        heading.setForeground(AppConfig.TEXT);
        heading.setBorder(BorderFactory.createEmptyBorder(13, 3, 13, 0));
        calendarPanel.add(heading, BorderLayout.NORTH);
        if (daily.isEmpty()) {
            JLabel empty = new JLabel("Ngày này chưa có lịch. Chọn ngày khác hoặc chuyển tuần để xem tiếp.", JLabel.CENTER);
            empty.setForeground(AppConfig.MUTED);
            empty.setOpaque(true);
            empty.setBackground(Color.WHITE);
            calendarPanel.add(empty, BorderLayout.CENTER);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 3, 10, 10));
            grid.setBackground(AppConfig.BACKGROUND);
            grid.setBorder(BorderFactory.createEmptyBorder(0, 2, 14, 2));
            int from = Math.min(calendarPage * CALENDAR_PAGE_SIZE, daily.size());
            int to = Math.min(from + CALENDAR_PAGE_SIZE, daily.size());
            grid.setPreferredSize(new Dimension(900, (int) Math.ceil((to - from) / 3.0) * 135));
            daily.subList(from, to).forEach(entry -> grid.add(scheduleChip(entry)));
            JPanel agenda = new JPanel(new BorderLayout());
            agenda.setBackground(AppConfig.BACKGROUND);
            if (daily.size() > CALENDAR_PAGE_SIZE) {
                JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                pager.setBackground(AppConfig.BACKGROUND);
                JLabel pageLabel = new JLabel("Hiển thị " + (from + 1) + "–" + to + " / " + daily.size() + " lịch");
                pageLabel.setForeground(AppConfig.MUTED);
                SecondaryButton previousPage = new SecondaryButton("Trang trước");
                previousPage.setEnabled(calendarPage > 0);
                previousPage.addActionListener(event -> { calendarPage--; rebuildCalendar(displayedRows); });
                SecondaryButton nextPage = new SecondaryButton("Trang sau");
                nextPage.setEnabled(to < daily.size());
                nextPage.addActionListener(event -> { calendarPage++; rebuildCalendar(displayedRows); });
                pager.add(pageLabel);
                pager.add(previousPage);
                pager.add(nextPage);
                agenda.add(pager, BorderLayout.NORTH);
            }
            JPanel gridHost = new JPanel(new BorderLayout());
            gridHost.setBackground(AppConfig.BACKGROUND);
            gridHost.add(grid, BorderLayout.NORTH);
            agenda.add(gridHost, BorderLayout.CENTER);
            calendarPanel.add(agenda, BorderLayout.CENTER);
        }
        dayStrip.revalidate();
        dayStrip.repaint();
        calendarPanel.revalidate();
        calendarPanel.repaint();
        calendarScroll.getVerticalScrollBar().setValue(0);
    }

    private Component scheduleChip(ScheduleEntry entry) {
        String[] colors = {"#EEF2FF", "#ECFDF5", "#FFF7ED", "#FDF2F8"};
        Color chipColor = entry.getStatus() == ScheduleStatus.DRAFT
                ? Color.decode("#FFF7ED")
                : Color.decode(colors[Math.floorMod(entry.getCourseSection().getId().intValue(), colors.length)]);
        RoundedPanel chip = new RoundedPanel(8, chipColor);
        chip.setLayout(new BorderLayout());
        chip.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 9));
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        String time = entry.getStartSlot().getStartTime() + "–" + entry.getEndSlot().getEndTime();
        JLabel label = new JLabel("<html><div style='width:250px'><b>" + time + "</b><br>"
                + html(entry.getCourseSection().getCourse().getName()) + "<br><small>"
                + html(entry.getCourseSection().getCode()) + " · " + html(entry.getRoom().getCode())
                + "</small><br><small>" + html(entry.getCourseSection().getLecturer().getFullName())
                + "</small>" + (entry.getStatus() == ScheduleStatus.DRAFT
                ? "<br><small><b>Chưa công bố</b></small>" : "") + "</div></html>");
        label.setForeground(AppConfig.TEXT);
        chip.add(label, BorderLayout.CENTER);
        chip.setToolTipText(entry.getCourseSection().getCourse().getName() + " · " + entry.getRoom().getCode());
        chip.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        label.setCursor(chip.getCursor());
        java.awt.event.MouseAdapter openDetail = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedCard = entry;
                showDetail(entry);
            }
        };
        chip.addMouseListener(openDetail);
        label.addMouseListener(openDetail);
        return chip;
    }

    private static String html(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private ScheduleEntry selectedEntry() {
        if (viewModeBox.getSelectedIndex() == 0 && selectedCard != null) return selectedCard;
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một lịch trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void addSchedule() {
        ScheduleFormDialog.showDialog(this, controllers, null).ifPresent(entry -> {
            UiTasks.run(this, "Đang lưu lịch học…", () -> controllers.schedules().save(entry), () -> {
                refresh();
                Dialogs.success(this, "Đã thêm lịch học vào cơ sở dữ liệu.");
            });
        });
    }

    private void editSchedule() {
        try {
            ScheduleEntry entry = selectedEntry();
            ScheduleFormDialog.showDialog(this, controllers, entry).ifPresent(updated -> {
                UiTasks.run(this, "Đang cập nhật lịch học…", () -> controllers.schedules().save(updated), () -> {
                    refresh();
                    Dialogs.success(this, "Đã cập nhật lịch học.");
                });
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void deleteSchedule() {
        try {
            ScheduleEntry entry = selectedEntry();
            if (Dialogs.confirm(this, "Xóa lịch " + entry.getCourseSection().getCode() + " (hủy lịch)?")) {
                UiTasks.run(this, "Đang hủy lịch học…", () -> controllers.schedules().delete(entry.getId()), cancelled -> {
                    refresh();
                    if(cancelled) Dialogs.success(this, "Đã hủy lịch học.");
                    else Dialogs.warning(this, "Lịch học đã được hủy trước đó.");
                }, error -> Dialogs.error(this, UiTasks.message(error)));
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
