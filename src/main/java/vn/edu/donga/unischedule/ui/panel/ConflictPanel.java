package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.Conflict;
import vn.edu.donga.unischedule.model.Enums.ConflictStatus;
import vn.edu.donga.unischedule.model.Enums.ConflictType;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.SearchField;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.ui.component.UiTasks;
import vn.edu.donga.unischedule.ui.dialog.ConflictDetailDialog;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.ui.renderer.BadgeRenderer;
import vn.edu.donga.unischedule.util.DateUtils;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import vn.edu.donga.unischedule.util.TextUtils;
import vn.edu.donga.unischedule.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class ConflictPanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final SearchField searchField = new SearchField("Tìm mã lớp, phòng hoặc giảng viên");
    private final JComboBox<String> typeBox = new JComboBox<>();
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final GenericTableModel<Conflict> tableModel;
    private final JTable table;
    private final JLabel loadStatus = new JLabel(" ");
    private final javax.swing.Timer searchDelay = new javax.swing.Timer(250, event -> refresh());
    private int refreshVersion;

    public ConflictPanel(AppControllers controllers) {
        this.controllers = controllers;
        tableModel = new GenericTableModel<>(
                new String[]{"Mã", "Loại", "Lịch thứ nhất", "Lịch thứ hai", "Thời gian", "Trạng thái"},
                Conflict::getId,
                conflict -> conflict.getType().getDisplayName(),
                conflict -> scheduleName(conflict.getFirstSchedule()),
                conflict -> scheduleName(conflict.getSecondSchedule()),
                conflict -> DateUtils.dayName(conflict.getFirstSchedule().getDayOfWeek()) + ", "
                        + conflict.getFirstSchedule().getStartSlot().getName() + "-"
                        + conflict.getFirstSchedule().getEndSlot().getName(),
                Conflict::getStatus);
        table = new JTable(tableModel);
        TableUtils.style(table);
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer());
        setLayout(new BorderLayout(0, 14));
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        buildUi();
        searchDelay.setRepeats(false);
        refresh();
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);
        JPanel filters = new JPanel(new GridLayout(1, 3, 10, 0));
        filters.setOpaque(false);
        typeBox.addItem("Tất cả loại");
        for (ConflictType type : ConflictType.values()) {
            if (type != ConflictType.CAPACITY) {
                typeBox.addItem(type.getDisplayName());
            }
        }
        statusBox.addItem("Tất cả trạng thái");
        for (ConflictStatus status : ConflictStatus.values()) {
            statusBox.addItem(status.getDisplayName());
        }
        filters.add(searchField);
        filters.add(typeBox);
        filters.add(statusBox);
        top.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        SecondaryButton detail = new SecondaryButton("Xem chi tiết");
        PrimaryButton resolve = new PrimaryButton("Đánh dấu đã xử lý");
        detail.addActionListener(event -> showDetail());
        resolve.addActionListener(event -> markResolved());
        actions.add(detail);
        actions.add(resolve);
        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(loadStatus, BorderLayout.SOUTH);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetail();
                }
            }
        });
        typeBox.addActionListener(event -> refresh());
        statusBox.addActionListener(event -> refresh());
        searchField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchDelay.restart();
            }
        });
    }

    @Override
    public void refresh() {
        String keyword = searchField.getText();
        String type = (String) typeBox.getSelectedItem();
        String status = (String) statusBox.getSelectedItem();
        int version = ++refreshVersion;
        loadStatus.setText("Đang tải xung đột…");
        new SwingWorker<List<Conflict>, Void>() {
            @Override protected List<Conflict> doInBackground() {
                return controllers.conflicts().search(keyword, type, status);
            }
            @Override protected void done() {
                if (version != refreshVersion) return;
                try {
                    List<Conflict> rows = get();
                    tableModel.setRows(rows);
                    loadStatus.setText(rows.size() + " xung đột");
                } catch (Exception error) {
                    loadStatus.setText("Không thể tải xung đột: " + UiTasks.message(error));
                }
            }
        }.execute();
    }

    private String scheduleName(vn.edu.donga.unischedule.model.ScheduleEntry entry) {
        if (entry == null) {
            return "";
        }
        return entry.getCourseSection().getCode() + " - " + entry.getRoom().getCode();
    }

    private Conflict selectedConflict() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new ValidationException("Vui lòng chọn một xung đột trong bảng.");
        }
        return tableModel.getRowAt(table.convertRowIndexToModel(row));
    }

    private void showDetail() {
        try {
            ConflictDetailDialog.showDialog(this, selectedConflict());
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void markResolved() {
        try {
            Conflict conflict = selectedConflict();
            UiTasks.run(this, "Đang kiểm tra xung đột…", () -> controllers.conflicts().markResolved(conflict.getId()), () -> {
                refresh();
                Dialogs.success(this, "Đã đánh dấu xung đột là đã xử lý.");
            });
        } catch (ValidationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
