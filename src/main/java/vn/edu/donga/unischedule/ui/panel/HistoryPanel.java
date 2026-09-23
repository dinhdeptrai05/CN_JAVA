package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.model.HistorySnapshot;
import vn.edu.donga.unischedule.model.User;
import vn.edu.donga.unischedule.model.Enums.Role;
import vn.edu.donga.unischedule.ui.component.SecondaryButton;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Role-scoped live history: five years for staff, three for lecturers and students. */
public final class HistoryPanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final ScreenNavigator navigator;
    private final int visibleYears;
    private final JLabel status=new JLabel("Đang chuẩn bị lịch sử hoạt động…");
    private final JLabel focus=new JLabel();
    private final JComboBox<Integer> yearBox=new JComboBox<>();
    private final JPanel chart=new JPanel();
    private final JTable table;
    private HistorySnapshot snapshot;
    private boolean busy;
    public HistoryPanel(AppControllers controllers,User user,ScreenNavigator navigator) {
        this.controllers=controllers;this.navigator=navigator;
        boolean staff=user.getRole()==Role.ADMIN||user.getRole()==Role.ACADEMIC;
        visibleYears=staff?5:3;
        setLayout(new BorderLayout(0,14));setBackground(AppConfig.BACKGROUND);setBorder(BorderFactory.createEmptyBorder(20,22,22,22));
        var top=new JPanel(new BorderLayout(0,12));top.setOpaque(false);
        var intro=new JLabel("Hoạt động theo năm · tính trực tiếp từ dữ liệu ứng dụng");intro.setFont(intro.getFont().deriveFont(Font.BOLD,16f));top.add(intro,BorderLayout.NORTH);
        var actions=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));actions.setOpaque(false);
        actions.add(new JLabel("Năm:"));actions.add(yearBox);
        if(staff) {
            var report=new SecondaryButton("Xem báo cáo năm đã chọn");report.addActionListener(e->{Integer year=(Integer)yearBox.getSelectedItem();if(year!=null)navigator.showReportYear(year);});actions.add(report);
        }
        var reload=new SecondaryButton("Cập nhật số liệu");reload.addActionListener(e->refresh());actions.add(reload);
        top.add(actions,BorderLayout.CENTER);top.add(status,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        var center=new JPanel(new BorderLayout(0,15));center.setOpaque(false);
        focus.setFont(focus.getFont().deriveFont(Font.BOLD,18f));center.add(focus,BorderLayout.NORTH);
        chart.setLayout(new BoxLayout(chart,BoxLayout.Y_AXIS));chart.setOpaque(false);
        center.add(chart,BorderLayout.CENTER);
        var scroll=new JScrollPane(center);scroll.getVerticalScrollBar().setUnitIncrement(20);add(scroll,BorderLayout.CENTER);
        table=new JTable(new AbstractTableModel(){
            final String[] columns={"Năm","Tài khoản mới","Lớp mở","Lượt đăng ký","Đăng ký hủy","Buổi học","Yêu cầu","Đã duyệt","Từ chối","Đang chờ","Bảo trì"};
            public int getRowCount(){return snapshot==null?0:snapshot.years().size();}
            public int getColumnCount(){return columns.length;}
            public String getColumnName(int column){return columns[column];}
            public Object getValueAt(int row,int col){var y=snapshot.years().get(row);return switch(col){case 0->y.year();case 1->y.newUsers();case 2->y.sections();case 3->y.enrollments();case 4->y.cancelledEnrollments();case 5->y.sessions();case 6->y.requests();case 7->y.approvedRequests();case 8->y.rejectedRequests();case 9->y.pendingRequests();default->y.maintenance();};}
        });
        TableUtils.style(table);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);for(int i=0;i<table.getColumnCount();i++)table.getColumnModel().getColumn(i).setPreferredWidth(i==0?65:120);
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&table.getSelectedRow()>=0&&snapshot!=null)yearBox.setSelectedItem(snapshot.years().get(table.convertRowIndexToModel(table.getSelectedRow())).year());});
        yearBox.addActionListener(e->showSelected());
        var bottom=new JPanel(new BorderLayout(0,8));bottom.setOpaque(false);
        var note=new JTextArea("Số buổi học chỉ tính lịch đã công bố và ngày đã diễn ra. Lượt đăng ký không phải số sinh viên duy nhất. Hai năm ở đầu và cuối khoảng "+visibleYears+" năm có thể chỉ gồm một phần năm lịch.");
        note.setEditable(false);note.setLineWrap(true);note.setWrapStyleWord(true);note.setBackground(AppConfig.BACKGROUND);note.setForeground(AppConfig.MUTED);note.setRows(2);bottom.add(note,BorderLayout.NORTH);
        var tableScroll=new JScrollPane(table);tableScroll.setPreferredSize(new Dimension(800,190));bottom.add(tableScroll,BorderLayout.CENTER);add(bottom,BorderLayout.SOUTH);
    }
    @Override public void refresh() {
        if(busy)return;busy=true;status.setText("Đang tổng hợp "+visibleYears+" năm từ MySQL…");
        new SwingWorker<HistorySnapshot,Void>() {
            @Override protected HistorySnapshot doInBackground(){return controllers.history().load();}
            @Override protected void done(){try{display(get());}catch(Exception ex){status.setText("Không tải được lịch sử.");Dialogs.error(HistoryPanel.this,ex.getCause()==null?ex.getMessage():ex.getCause().getMessage());}finally{busy=false;}}
        }.execute();
    }
    public void display(HistorySnapshot value) {
        snapshot=value;yearBox.removeAllItems();
        chart.removeAll();long max=value.years().stream().mapToLong(HistorySnapshot.Year::sessions).max().orElse(1);
        for(var row:value.years()) {
            yearBox.addItem(row.year());
            var bar=new JPanel(new BorderLayout(12,0));bar.setOpaque(false);bar.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
            var label=new JLabel(row.year()+"   "+row.sessions()+" buổi");label.setPreferredSize(new Dimension(120,26));bar.add(label,BorderLayout.WEST);
            var progress=new JProgressBar(0,100);progress.setValue((int)(row.sessions()*100/Math.max(1,max)));progress.setForeground(AppConfig.PRIMARY);progress.setPreferredSize(new Dimension(480,22));bar.add(progress,BorderLayout.CENTER);
            chart.add(bar);
        }
        if(yearBox.getItemCount()>0)yearBox.setSelectedIndex(yearBox.getItemCount()-1);
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
        long sessions=value.years().stream().mapToLong(HistorySnapshot.Year::sessions).sum();
        status.setText("Khoảng "+value.from()+" → "+value.to()+"  ·  "+value.activeUsers()+" tài khoản đang hoạt động  ·  "+sessions+" buổi đã diễn ra");
        showSelected();chart.revalidate();chart.repaint();
    }
    private void showSelected() {
        Integer year=(Integer)yearBox.getSelectedItem();
        if(snapshot==null||year==null)return;
        var row=snapshot.years().stream().filter(item->item.year()==year).findFirst().orElseThrow();
        focus.setText(year+"  ·  "+row.sections()+" lớp mở  ·  "+row.enrollments()+" lượt đăng ký  ·  "+row.sessions()+" buổi học  ·  "+row.requests()+" yêu cầu");
    }
}
