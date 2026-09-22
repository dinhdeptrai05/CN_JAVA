package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.RequestStatus;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.model.Enums.RoomStatus;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.RoundedPanel;
import vn.edu.donga.unischedule.ui.component.StatCard;
import vn.edu.donga.unischedule.ui.component.UiTasks;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.TableUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

public class DashboardPanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final User user;
    private final ScreenNavigator navigator;
    private String department = "Tất cả khoa";

    public DashboardPanel(AppControllers controllers, User user) {
        this(controllers, user, key -> { });
    }

    public DashboardPanel(AppControllers controllers, User user, ScreenNavigator navigator) {
        this.controllers = controllers;
        this.user = user;
        this.navigator = navigator;
        setLayout(new BorderLayout());
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        refresh();
    }

    @Override
    public void refresh() {
        removeAll();
        JPanel page = new vn.edu.donga.unischedule.ui.component.ScrollPage();
        page.setLayout(new javax.swing.BoxLayout(page, javax.swing.BoxLayout.Y_AXIS));
        page.setBorder(BorderFactory.createEmptyBorder(28, 30, 30, 30));
        page.setBackground(AppConfig.BACKGROUND);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Tổng quan điều hành");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 28f));
        heading.add(title, BorderLayout.NORTH);
        JLabel date = new JLabel("Tuần " + DateUtils.format(DateUtils.currentWeekMonday()) + " - " + DateUtils.format(DateUtils.currentWeekMonday().plusDays(6)));
        date.setForeground(Color.decode("#94A3B8"));
        date.setBorder(BorderFactory.createEmptyBorder(4, 0, 18, 0));
        heading.add(date, BorderLayout.SOUTH);
        page.add(heading);
        JPanel filters = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
        java.util.List<String> departments = new java.util.ArrayList<>();
        departments.add("Tất cả khoa");
        controllers.catalog().getDepartments().forEach(d -> departments.add(d.getName()));
        for (String name : departments) {
            javax.swing.JToggleButton tab = new javax.swing.JToggleButton(name);
            tab.setSelected(name.equals(department));
            tab.setFont(tab.getFont().deriveFont(11f));
            tab.setForeground(AppConfig.PRIMARY);
            tab.addActionListener(e -> { department = name; refresh(); });
            group.add(tab); filters.add(tab);
        }
        page.add(filters);
        page.add(javax.swing.Box.createVerticalStrut(20));
        JLabel notice = new JLabel("<html><b>Lưu ý điều hành:</b>&nbsp; Kiểm tra xung đột và xác nhận các yêu cầu thay đổi trước khi công bố lịch học.</html>");
        notice.setIcon(vn.edu.donga.unischedule.ui.component.HeroIcons.of("exclamation-triangle", 20, AppConfig.DANGER));
        notice.setIconTextGap(14);
        notice.setOpaque(true);
        notice.setBackground(Color.decode("#FFF1F2"));
        notice.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        page.add(notice);
        page.add(javax.swing.Box.createVerticalStrut(24));
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        buildStats(stats);
        page.add(stats);
        page.add(javax.swing.Box.createVerticalStrut(24));

        JPanel center = new JPanel(new java.awt.GridBagLayout());
        center.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(0, 16)); left.setOpaque(false);
        JTable today = buildTodayTable();
        today.setRowHeight(76);
        today.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                int width = Math.max(24, table.getColumnModel().getColumn(column).getWidth() - 28);
                String text = String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;");
                setText("<html><div style='width:" + width + "px'>" + text + "</div></html>");
                setFont(getFont().deriveFont(column == 0 ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 11f));
                setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); return this;
            }
        });
        today.getColumnModel().getColumn(0).setPreferredWidth(180);
        today.getColumnModel().getColumn(1).setPreferredWidth(75);
        today.getColumnModel().getColumn(2).setPreferredWidth(65);
        today.getColumnModel().getColumn(3).setPreferredWidth(135);
        today.getColumnModel().getColumn(4).setPreferredWidth(75);
        today.setPreferredScrollableViewportSize(new java.awt.Dimension(550, Math.max(240, today.getRowCount() * 76)));
        RoundedPanel schedule = card("Lịch học trọng điểm hôm nay", today);
        javax.swing.JButton all = new vn.edu.donga.unischedule.ui.component.SecondaryButton("Xem toàn bộ lịch học");
        all.addActionListener(e -> navigator.showScreen("timetable"));
        schedule.add(all, BorderLayout.SOUTH);
        left.add(schedule, BorderLayout.NORTH);
        JPanel quick = new JPanel(new GridLayout(1, 2, 16, 0)); quick.setOpaque(false);
        javax.swing.JButton rooms = new vn.edu.donga.unischedule.ui.component.SecondaryButton("Tra cứu phòng trống");
        rooms.addActionListener(e -> navigator.showScreen("roomSearch")); quick.add(rooms);
        javax.swing.JButton alerts = new vn.edu.donga.unischedule.ui.component.SecondaryButton("Thông báo mới");
        alerts.addActionListener(e -> navigator.showScreen("notifications")); quick.add(alerts);
        left.add(quick, BorderLayout.CENTER);
        JPanel right = new JPanel(); right.setOpaque(false);
        right.setLayout(new javax.swing.BoxLayout(right, javax.swing.BoxLayout.Y_AXIS));
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ACADEMIC) {
            right.add(card("Xung đột cần xử lý", buildConflicts()));
            right.add(javax.swing.Box.createVerticalStrut(16));
        }
        right.add(card("Sử dụng giảng đường", buildUsagePanel()));
        right.add(javax.swing.Box.createVerticalStrut(16));
        right.add(card(user.getRole() == Role.STUDENT ? "Thông báo mới nhất" : "Yêu cầu mới nhất",
                user.getRole() == Role.STUDENT ? buildNotificationPanel() : buildRequests()));
        java.awt.GridBagConstraints constraints = new java.awt.GridBagConstraints();
        constraints.gridx = 0; constraints.weightx = 0.66; constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        constraints.anchor = java.awt.GridBagConstraints.NORTH; constraints.insets = new java.awt.Insets(0, 0, 0, 20);
        left.setMinimumSize(new java.awt.Dimension(420, left.getPreferredSize().height));
        center.add(left, constraints);
        constraints.gridx = 1; constraints.weightx = 0.34; constraints.insets = new java.awt.Insets(0, 0, 0, 0);
        right.setMinimumSize(new java.awt.Dimension(280, right.getPreferredSize().height));
        center.add(right, constraints);
        page.add(center);
        for (java.awt.Component child : page.getComponents()) {
            if (child instanceof javax.swing.JComponent component) {
                component.setAlignmentX(0f);
                component.setMinimumSize(new java.awt.Dimension(0, component.getPreferredSize().height));
                component.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
            }
        }
        JScrollPane scroll = new JScrollPane(page, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void buildStats(JPanel stats) {
        for (var metric : controllers.dashboard().metrics(user, department)) {
            Color accent = switch (metric.tone()) {
                case PRIMARY -> AppConfig.PRIMARY; case SUCCESS -> AppConfig.SUCCESS;
                case WARNING -> AppConfig.WARNING; case DANGER -> AppConfig.DANGER;
                case INFO -> Color.decode("#0EA5E9");
            };
            stats.add(new StatCard(metric.icon(), metric.title(), metric.value(), metric.description(), accent));
        }
    }

    private JTable buildTodayTable() {
        GenericTableModel<ScheduleEntry> model = new GenericTableModel<>(
                new String[]{"Môn học", "Lớp", "Phòng", "Giảng viên", "Ca"},
                entry -> entry.getCourseSection().getCourse().getName(),
                entry -> entry.getCourseSection().getCode(),
                entry -> entry.getRoom().getCode(),
                entry -> entry.getCourseSection().getLecturer().getFullName(),
                entry -> entry.getStartSlot().getName() + " - " + entry.getEndSlot().getName());
        int today = DateUtils.toSchoolDay(LocalDate.now());
        model.setRows(controllers.dashboard().today(user, department));
        JTable table = new JTable(model);
        TableUtils.style(table);
        return table;
    }


    private JPanel buildUsagePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 12));
        panel.setOpaque(false);
        controllers.dashboard().usage().forEach(usage -> {
            String building = usage.building();
            JPanel row = new JPanel(new BorderLayout(0, 6)); row.setOpaque(false);
            row.add(new JLabel(building + "  ·  " + usage.used() + "/" + usage.total() + " phòng"), BorderLayout.NORTH);
            JProgressBar progress = new JProgressBar(0, 100); progress.setValue(usage.percent());
            progress.setForeground(building.contains("B") ? AppConfig.SUCCESS : AppConfig.PRIMARY);
            progress.setPreferredSize(new java.awt.Dimension(120, 7));
            row.add(progress, BorderLayout.CENTER); panel.add(row);
        });
        return panel;
    }

    private JPanel buildConflicts() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 14)); panel.setOpaque(false);
        controllers.dashboard().urgentConflicts().forEach(conflict -> {
                    JPanel row = new JPanel(new BorderLayout(0, 10)); row.setOpaque(false);
                    JLabel text = new JLabel("<html><div style='width:220px'><b style='color:#E11D48'>" + conflict.getType().getDisplayName()
                            + "</b><br>" + conflict.getMessage() + "</div></html>");
                    text.setFont(text.getFont().deriveFont(11f)); row.add(text, BorderLayout.CENTER);
        javax.swing.JButton resolve = new vn.edu.donga.unischedule.ui.component.DangerButton("Giải quyết ngay");
                    resolve.addActionListener(e -> navigator.showScreen("conflicts")); row.add(resolve, BorderLayout.SOUTH); panel.add(row);
                });
        if (panel.getComponentCount() == 0) panel.add(new JLabel("Không có xung đột đang mở."));
        return panel;
    }

    private JPanel buildRequests() {
        JPanel panel = new JPanel(); panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        controllers.dashboard().recentRequests(user).forEach(request -> {
            JPanel row = new JPanel(new BorderLayout(0, 12)); row.setOpaque(false);
            JLabel name = new JLabel("<html><b>" + request.getRequester().getFullName() + "</b><br>" + request.getType().getDisplayName() + "</html>");
            name.setFont(name.getFont().deriveFont(11f));
            JPanel person = new JPanel(new BorderLayout(8, 0)); person.setOpaque(false);
            person.add(new vn.edu.donga.unischedule.ui.component.Avatar(request.getRequester(), 30), BorderLayout.WEST);
            person.add(name, BorderLayout.CENTER); row.add(person, BorderLayout.NORTH);
            JLabel reason = new JLabel("<html><div style='width:220px'><b>Lý do:</b> " + request.getReason() + "</div></html>");
            reason.setFont(reason.getFont().deriveFont(11f)); reason.setForeground(AppConfig.MUTED); row.add(reason, BorderLayout.CENTER);
            if (controllers.requests().canProcess(request, user)) {
                javax.swing.JButton approve = new vn.edu.donga.unischedule.ui.component.PrimaryButton("Duyệt yêu cầu");
                approve.addActionListener(e -> UiTasks.run(this, "Đang duyệt yêu cầu…",
                        () -> controllers.requests().approve(request, user),
                        () -> { refresh(); Dialogs.success(this, "Đã duyệt yêu cầu."); }));
                row.add(approve, BorderLayout.SOUTH);
            } else row.add(new vn.edu.donga.unischedule.ui.component.StatusBadge(request.getStatus()), BorderLayout.SOUTH);
            row.setBorder(BorderFactory.createEmptyBorder(6, 0, 16, 0)); panel.add(row);
        });
        if (panel.getComponentCount() == 0) panel.add(new JLabel("Chưa có yêu cầu."));
        return panel;
    }

    private JPanel buildNotificationPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.setOpaque(false);
        controllers.dashboard().recentNotifications(user).forEach(notification -> {
            JLabel label = new JLabel("<html><b>" + notification.getTitle() + "</b><br><span style='color:#64748B'>"
                    + notification.getContent() + "</span></html>");
            panel.add(label);
        });
        return panel;
    }

    private RoundedPanel card(String title, java.awt.Component content) {
        RoundedPanel card = new RoundedPanel(8, Color.WHITE);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel label = new JLabel(title);
        label.setForeground(AppConfig.TEXT);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        card.add(label, BorderLayout.NORTH);
        if (content instanceof JTable table) {
            card.add(new JScrollPane(table), BorderLayout.CENTER);
        } else {
            card.add(content, BorderLayout.CENTER);
        }
        return card;
    }
}
