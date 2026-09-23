package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.controller.AuditController;
import vn.edu.donga.unischedule.model.AuditEntry;
import vn.edu.donga.unischedule.ui.component.PrimaryButton;
import vn.edu.donga.unischedule.ui.component.UiTasks;
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
    private static final int PAGE_SIZE=100;
    private final GenericTableModel<AuditEntry> tableModel;
    private final AuditController controller;
    private final javax.swing.JLabel pageLabel=new javax.swing.JLabel();
    private final javax.swing.JButton previous=new javax.swing.JButton("Trước"),next=new javax.swing.JButton("Sau");
    private int page;

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
        refresh.addActionListener(event -> {page=0;refresh();});
        actions.add(refresh);
        previous.addActionListener(e->{if(page>0){page--;refresh();}});
        next.addActionListener(e->{page++;refresh();});
        actions.add(previous);actions.add(pageLabel);actions.add(next);
        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        long count=controller.count();int maxPage=(int)Math.max(0,(count-1)/PAGE_SIZE);page=Math.min(page,maxPage);
        tableModel.setRows(controller.findPage(page,PAGE_SIZE));
        pageLabel.setText("Trang "+(page+1)+" / "+(maxPage+1)+"  ·  "+count+" sự kiện");
        previous.setEnabled(page>0);next.setEnabled(page<maxPage);
    }
}
