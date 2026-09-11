package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.controller.AuditController;
import vn.edu.donga.unischedule.model.AuditEntry;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.model.GenericTableModel;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.util.List;

public class AuditLogPanel extends JPanel implements Refreshable {
    private final GenericTableModel<AuditEntry> tableModel;
    private final AuditController controller;

    public AuditLogPanel(AuditController controller) {
        this.controller = controller;
        tableModel = new GenericTableModel<>(
                new String[]{"Thời gian", "Người dùng", "Hành động", "Đối tượng", "Kết quả"},
                AuditEntry::time,
                AuditEntry::user,
                AuditEntry::action,
                AuditEntry::target,
                AuditEntry::result);
        JTable table = new JTable(tableModel);
        TableUtils.style(table);
        setLayout(new BorderLayout(0, 14));
        setBackground(AppConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        PrimaryButton refresh = new PrimaryButton("Làm mới");
        refresh.addActionListener(event -> {
            refresh();
            Dialogs.success(this, "Nhật ký demo đã được tải lại.");
        });
        actions.add(refresh);
        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        tableModel.setRows(controller.findAll());
    }
}
