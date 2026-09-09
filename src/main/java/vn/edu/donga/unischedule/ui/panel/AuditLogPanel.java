package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
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
    private final GenericTableModel<AuditRow> tableModel;

    public AuditLogPanel() {
        tableModel = new GenericTableModel<>(
                new String[]{"Thời gian", "Người dùng", "Hành động", "Đối tượng", "Kết quả"},
                AuditRow::time,
                AuditRow::user,
                AuditRow::action,
                AuditRow::target,
                AuditRow::result);
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
        tableModel.setRows(List.of(
                row("admin", "Đăng nhập", "Hệ thống", "Thành công"),
                row("daotao", "Tạo lịch học", "IT101-01", "Mô phỏng"),
                row("daotao", "Kiểm tra xung đột", "Tuần hiện tại", "Phát hiện 3 loại"),
                row("admin", "Cập nhật phòng", "B204", "Chuyển bảo trì"),
                row("giangvien", "Gửi yêu cầu", "Mượn thiết bị", "Chờ duyệt"),
                row("sinhvien", "Tra cứu phòng", "C304", "Chỉ xem")
        ));
    }

    private AuditRow row(String user, String action, String target, String result) {
        return new AuditRow(LocalDateTime.now().minusMinutes((long) (Math.random() * 120)).toString(), user, action, target, result);
    }

    private record AuditRow(String time, String user, String action, String target, String result) {
    }
}
