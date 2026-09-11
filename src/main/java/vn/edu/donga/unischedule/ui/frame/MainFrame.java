package vn.edu.donga.unischedule.ui.frame;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.HeroIcons;
import vn.edu.donga.unischedule.ui.component.Avatar;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.panel.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame implements ScreenNavigator {
    private final AppControllers controllers;
    private final User currentUser;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JLabel titleLabel = new JLabel("Dashboard");
    private final JButton notificationButton = new JButton();
    private final Map<String, JButton> menuButtons = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();
    private TimetablePanel timetableScreen;

    public MainFrame(AppControllers controllers, User currentUser) {
        super(AppConfig.APP_NAME);
        this.controllers = controllers;
        this.currentUser = currentUser;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        buildUi();
        showScreen("dashboard");
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppConfig.BACKGROUND);
        setContentPane(root);
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.add(buildTopbar(), BorderLayout.NORTH);
        workspace.add(contentPanel, BorderLayout.CENTER);
        workspace.add(buildStatusBar(), BorderLayout.SOUTH);
        root.add(workspace, BorderLayout.CENTER);
        registerScreens();
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(AppConfig.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(256, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.decode("#F1F5F9")), BorderFactory.createEmptyBorder(24, 20, 18, 20)));

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        JLabel logo = new JLabel("<html><b style='font-size:17px;color:#0F172A'>UniSchedule</b><br>"
                + "<span style='color:#94A3B8;font-size:10px'>" + currentUser.getRole().getDisplayName() + "</span></html>");
        logo.setIcon(HeroIcons.of("academic-cap", 32, AppConfig.PRIMARY));
        logo.setIconTextGap(10);
        top.add(logo, BorderLayout.NORTH);
        PrimaryButton create = new PrimaryButton(currentUser.getRole() == Role.ACADEMIC ? "Tạo lịch học mới" : "Tra cứu phòng trống");
        create.setPreferredSize(new Dimension(210, 44));
        create.addActionListener(e -> {
            if (currentUser.getRole() == Role.ACADEMIC) {
                vn.edu.donga.unischedule.ui.dialog.ScheduleFormDialog.showDialog(this, controllers, null).ifPresent(entry -> {
                    try { controllers.schedules().save(entry); showScreen("timetable"); }
                    catch (vn.edu.donga.unischedule.validation.ValidationException ex) { vn.edu.donga.unischedule.util.Dialogs.error(this, ex.getMessage()); }
                });
            } else showScreen("roomSearch");
        });
        top.add(create, BorderLayout.SOUTH);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        sidebar.add(top, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 4));
        menu.setOpaque(false);
        addMenu(menu, "dashboard", "Dashboard", true);
        addMenu(menu, "timetable", "Thời khóa biểu", true);
        addMenu(menu, "conflicts", "Xung đột", currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.ACADEMIC);
        addMenu(menu, "courseSections", "Lớp học phần", true);
        addMenu(menu, "rooms", "Phòng và thiết bị", true);
        addMenu(menu, "requests", "Yêu cầu", currentUser.getRole() != Role.STUDENT);
        addMenu(menu, "roomSearch", "Tra cứu phòng", true);
        addMenu(menu, "users", "Người dùng", currentUser.getRole() == Role.ADMIN);
        addMenu(menu, "audit", "Nhật ký", currentUser.getRole() == Role.ADMIN);
        addMenu(menu, "notifications", "Thông báo", true);
        addMenu(menu, "profile", "Hồ sơ", true);
        JPanel menuHost = new JPanel(new BorderLayout());
        menuHost.setBackground(Color.WHITE);
        menuHost.add(menu, BorderLayout.NORTH);
        sidebar.add(new JScrollPane(menuHost, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

        JButton logout = sidebarButton("Đăng xuất");
        logout.setIcon(HeroIcons.of("arrow-right-on-rectangle", 18, AppConfig.MUTED));
        logout.addActionListener(event -> {
            new LoginFrame(controllers).setVisible(true);
            dispose();
        });
        sidebar.add(logout, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildTopbar() {
        JPanel topbar = new JPanel(new BorderLayout(18, 0));
        topbar.setBackground(Color.WHITE);
        topbar.setPreferredSize(new Dimension(0, 64));
        topbar.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        titleLabel.setForeground(AppConfig.TEXT);
        topbar.add(titleLabel, BorderLayout.WEST);

        SearchField searchField = new SearchField("Tìm phòng, giảng viên, lớp...");
        searchField.getTextField().addActionListener(event -> {
            showScreen("timetable");
            timetableScreen.search(searchField.getText());
        });
        topbar.add(searchField, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        notificationButton.setFocusPainted(false);
        notificationButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notificationButton.addActionListener(event -> showScreen("notifications"));
        right.add(notificationButton);
        Avatar avatar = new Avatar(currentUser.getId(), currentUser.getFullName(), 34);
        right.add(avatar);
        topbar.add(right, BorderLayout.EAST);
        return topbar;
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(Color.WHITE);
        status.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        JLabel label = new JLabel(AppConfig.STATUS_TEXT);
        label.setForeground(AppConfig.MUTED);
        status.add(label, BorderLayout.WEST);
        return status;
    }

    private void registerScreens() {
        addScreen("dashboard", "Tổng quan điều hành", new DashboardPanel(controllers, currentUser, this));
        timetableScreen = new TimetablePanel(controllers, currentUser);
        addScreen("timetable", "Thời khóa biểu", timetableScreen);
        addScreen("conflicts", "Xung đột thời khóa biểu", new ConflictPanel(controllers));
        addScreen("courseSections", "Lớp học phần", new CourseSectionPanel(controllers, currentUser));
        addScreen("rooms", "Phòng và thiết bị", new RoomManagementPanel(controllers, currentUser));
        addScreen("requests", "Quản lý yêu cầu", new RequestManagementPanel(controllers, currentUser));
        addScreen("roomSearch", "Tra cứu phòng trống", new RoomSearchPanel(controllers, currentUser));
        addScreen("users", "Quản lý người dùng", new UserManagementPanel(controllers));
        addScreen("audit", "Nhật ký hoạt động", new AuditLogPanel(controllers.audit()));
        addScreen("notifications", "Thông báo", new NotificationPanel(controllers, currentUser, this));
        addScreen("profile", "Hồ sơ và cài đặt", new ProfilePanel(controllers, currentUser));
    }

    private void addScreen(String key, String title, Component component) {
        if (component instanceof java.awt.Container container) vn.edu.donga.unischedule.ui.renderer.SemanticLabels.install(container);
        titles.put(key, title);
        if (!key.equals("dashboard")) {
            JPanel wrapper = new JPanel(new BorderLayout());
            JLabel heading = new JLabel(title);
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 26f));
            heading.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
            wrapper.add(heading, BorderLayout.NORTH);
            wrapper.add(component, BorderLayout.CENTER);
            contentPanel.add(wrapper, key);
        } else contentPanel.add(component, key);
    }

    private void addMenu(JPanel menu, String key, String label, boolean allowed) {
        if (!allowed) {
            return;
        }
        JButton button = sidebarButton(label);
        String icon = switch (key) {
            case "dashboard" -> "squares-2x2"; case "timetable" -> "calendar-days";
            case "conflicts" -> "exclamation-triangle"; case "courseSections" -> "document-text";
            case "rooms" -> "building-office-2"; case "requests" -> "check-circle";
            case "roomSearch" -> "magnifying-glass"; case "users" -> "users";
            case "audit" -> "clipboard-document-list"; case "notifications" -> "bell";
            default -> "cog-6-tooth";
        };
        button.setIcon(HeroIcons.of(icon, 18, AppConfig.MUTED));
        button.setIconTextGap(12);
        button.addActionListener(event -> showScreen(key));
        menuButtons.put(key, button);
        menu.add(button);
    }

    private JButton sidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        button.setHorizontalAlignment(JButton.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(AppConfig.SIDEBAR);
        button.setForeground(AppConfig.MUTED);
        button.setOpaque(false);
        button.setFont(button.getFont().deriveFont(12f));
        button.setPreferredSize(new Dimension(210, 42));
        button.setBorderPainted(false);
        return button;
    }

    @Override
    public void showScreen(String screenKey) {
        if (!titles.containsKey(screenKey)) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Màn hình này chưa khả dụng cho vai trò hiện tại.",
                    "Thông báo", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        cardLayout.show(contentPanel, screenKey);
        titleLabel.setText("UniSchedule   /   " + titles.get(screenKey));
        menuButtons.forEach((key, button) -> {
            boolean active = key.equals(screenKey);
            button.setBackground(active ? Color.decode("#EEF2FF") : AppConfig.SIDEBAR);
            button.setForeground(active ? AppConfig.PRIMARY : AppConfig.MUTED);
        });
        Component current = findVisibleComponent();
        if (current instanceof Refreshable refreshable) {
            refreshable.refresh();
        } else if (current instanceof JPanel wrapper) {
            for (Component child : wrapper.getComponents()) if (child instanceof Refreshable refreshable) refreshable.refresh();
        }
        refreshNotificationButton();
    }

    private Component findVisibleComponent() {
        for (Component component : contentPanel.getComponents()) {
            if (component.isVisible()) {
                return component;
            }
        }
        return null;
    }

    private void refreshNotificationButton() {
        long unread = controllers.notifications().unreadCount(currentUser);
        notificationButton.setText(String.valueOf(unread));
        notificationButton.setIcon(HeroIcons.of("bell", 18, AppConfig.PRIMARY));
        notificationButton.setToolTipText("Thông báo chưa đọc");
    }

    private String initials(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
