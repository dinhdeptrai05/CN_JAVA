package vn.edu.donga.unischedule.ui.panel;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.controller.AppControllers;
import vn.edu.donga.unischedule.controller.ReportController.ExportFormat;
import vn.edu.donga.unischedule.model.*;
import vn.edu.donga.unischedule.ui.component.*;
import vn.edu.donga.unischedule.util.Dialogs;
import vn.edu.donga.unischedule.util.TableUtils;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.nio.file.*;
import java.time.LocalDate;

public final class ReportPanel extends JPanel implements Refreshable {
    private final AppControllers controllers;
    private final User user;
    private final JTextField from=new JTextField(LocalDate.now().withDayOfMonth(1).toString());
    private final JTextField to=new JTextField(LocalDate.now().toString());
    private final JComboBox<Object> semester=new JComboBox<>(),department=new JComboBox<>();
    private final PrimaryButton load=new PrimaryButton("Tạo báo cáo");
    private final SecondaryButton pdf=new SecondaryButton("Xuất PDF"),word=new SecondaryButton("Xuất Word"),excel=new SecondaryButton("Xuất Excel");
    private final JLabel status=new JLabel("Chọn bộ lọc và tạo báo cáo.");
    private final JProgressBar progress=new JProgressBar();
    private final JPanel metrics=new JPanel(new GridLayout(1,4,12,0));
    private final JTabbedPane tabs=new JTabbedPane();
    private Report report;
    private boolean busy;

    public ReportPanel(AppControllers controllers,User user) {
        this.controllers=controllers;this.user=user;
        setLayout(new BorderLayout(0,14));setBackground(AppConfig.BACKGROUND);setBorder(BorderFactory.createEmptyBorder(20,22,22,22));
        semester.addItem("Tất cả học kỳ");department.addItem("Tất cả khoa");
        controllers.catalog().getSemesters().forEach(semester::addItem);controllers.catalog().getDepartments().forEach(department::addItem);
        JPanel filters=new JPanel(new GridLayout(1,4,12,0));filters.setOpaque(false);
        filters.add(field("Từ ngày (yyyy-MM-dd)",from));filters.add(field("Đến ngày (yyyy-MM-dd)",to));filters.add(field("Học kỳ",semester));filters.add(field("Khoa",department));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));actions.setOpaque(false);actions.add(load);actions.add(pdf);actions.add(word);actions.add(excel);
        JPanel top=new JPanel(new BorderLayout(0,14));top.setOpaque(false);top.add(filters,BorderLayout.NORTH);top.add(actions,BorderLayout.CENTER);
        JPanel summary=new JPanel(new BorderLayout(0,10));summary.setOpaque(false);metrics.setOpaque(false);summary.add(metrics,BorderLayout.CENTER);
        JPanel feedback=new JPanel(new BorderLayout(0,5));feedback.setOpaque(false);feedback.add(status,BorderLayout.NORTH);
        progress.setIndeterminate(true);progress.setVisible(false);feedback.add(progress,BorderLayout.SOUTH);
        summary.add(feedback,BorderLayout.SOUTH);top.add(summary,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);add(tabs,BorderLayout.CENTER);
        setExportEnabled(false);
        load.addActionListener(e->generate(true));pdf.addActionListener(e->export(ExportFormat.PDF));word.addActionListener(e->export(ExportFormat.WORD));excel.addActionListener(e->export(ExportFormat.EXCEL));
        var changes=new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){dirty();}public void removeUpdate(javax.swing.event.DocumentEvent e){dirty();}public void changedUpdate(javax.swing.event.DocumentEvent e){dirty();}};
        from.getDocument().addDocumentListener(changes);to.getDocument().addDocumentListener(changes);semester.addActionListener(e->dirty());department.addActionListener(e->dirty());
    }
    private JPanel field(String label,JComponent input) {var panel=new JPanel(new BorderLayout(0,6));panel.setOpaque(false);panel.add(new JLabel(label),BorderLayout.NORTH);panel.add(input,BorderLayout.CENTER);return panel;}
    private void dirty(){report=null;setExportEnabled(false);status.setText("Bộ lọc đã thay đổi. Nhấn Tạo báo cáo để cập nhật số liệu.");}
    @Override public void refresh() { generate(false); }
    private void generate(boolean notify) {
        if(busy)return;
        String start=from.getText(),end=to.getText();Long sid=semester.getSelectedItem() instanceof Semester s?s.getId():null,did=department.getSelectedItem() instanceof Department d?d.getId():null;
        setBusy(true);report=null;status.setText("Đang tổng hợp dữ liệu…");
        new SwingWorker<Report,Void>() {
            protected Report doInBackground(){return controllers.reports().generate(user,start,end,sid,did);}
            protected void done(){
                try{display(get());if(notify)Dialogs.success(ReportPanel.this,"Đã tạo báo cáo.");}catch(Exception ex){status.setText("Không tạo được báo cáo.");Dialogs.error(ReportPanel.this,ex.getCause()==null?ex.getMessage():ex.getCause().getMessage());}
                finally{setBusy(false);}
            }
        }.execute();
    }
    /** Render an already loaded snapshot; also used by UI verification. */
    public void display(Report value) {
        from.setText(value.filter().from().toString());to.setText(value.filter().to().toString());
        semester.setSelectedIndex(0);department.setSelectedIndex(0);
        for(int i=1;i<semester.getItemCount();i++)if(((Semester)semester.getItemAt(i)).getId().equals(value.filter().semesterId()))semester.setSelectedIndex(i);
        for(int i=1;i<department.getItemCount();i++)if(((Department)department.getItemAt(i)).getId().equals(value.filter().departmentId()))department.setSelectedIndex(i);
        report=value;metrics.removeAll();
        metrics.add(new StatCard("LH","Buổi học",String.valueOf(value.sessions()),"Lịch đã công bố",AppConfig.PRIMARY));
        metrics.add(new StatCard("PH","Phòng có lịch",String.valueOf(value.usedRooms()),"Trong khoảng ngày chọn",AppConfig.PRIMARY));
        metrics.add(new StatCard("ND","Lượt đăng ký",String.valueOf(value.registrations()),"Sĩ số hiện tại",AppConfig.PRIMARY));
        metrics.add(new StatCard("YC","Yêu cầu",String.valueOf(value.requests()),"Theo ngày tạo (UTC)",AppConfig.PRIMARY));
        tabs.removeAll();
        for(var data:value.tables()) {
            var model=new AbstractTableModel(){
                public int getRowCount(){return data.rows().size();}public int getColumnCount(){return data.columns().size();}
                public String getColumnName(int c){return data.columns().get(c);}public Object getValueAt(int r,int c){return data.rows().get(r).get(c);}
                public Class<?> getColumnClass(int c){return data.rows().isEmpty()?Object.class:data.rows().get(0).get(c).getClass();}
            };
            var table=new JTable(model);TableUtils.style(table);table.setAutoCreateRowSorter(true);table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            for(int i=0;i<table.getColumnCount();i++){table.getColumnModel().getColumn(i).setPreferredWidth(i==1?190:120);table.getColumnModel().getColumn(i).setMinWidth(80);}
            var panel=new JPanel(new BorderLayout(0,10));panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));panel.setBackground(Color.WHITE);
            var note=new JTextArea(data.note());note.setEditable(false);note.setLineWrap(true);note.setWrapStyleWord(true);note.setRows(2);note.setBackground(Color.WHITE);note.setForeground(AppConfig.MUTED);panel.add(note,BorderLayout.NORTH);panel.add(new JScrollPane(table),BorderLayout.CENTER);
            if(data.rows().isEmpty())panel.add(new JLabel("Không có dữ liệu phù hợp với bộ lọc."),BorderLayout.SOUTH);
            tabs.addTab(data.title(),panel);
        }
        status.setText("Dữ liệu: "+value.filter().from()+" → "+value.filter().to()+"  |  "+value.scope());
        setExportEnabled(true);revalidate();repaint();
    }
    private void setBusy(boolean value){busy=value;progress.setVisible(value);load.setEnabled(!value);from.setEnabled(!value);to.setEnabled(!value);semester.setEnabled(!value);department.setEnabled(!value);setExportEnabled(!value&&report!=null);}
    private void setExportEnabled(boolean value){pdf.setEnabled(value);word.setEnabled(value);excel.setEnabled(value);}
    private void export(ExportFormat format) {
        if(report==null)return;
        Report snapshot=report;
        String extension=switch(format){case PDF->".pdf";case WORD->".docx";case EXCEL->".xlsx";};
        String description=switch(format){case PDF->"Tài liệu PDF";case WORD->"Tài liệu Word";case EXCEL->"Bảng tính Excel";};
        var chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("UniSchedule-"+snapshot.filter().from()+extension));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(description,extension.substring(1)));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path chosen=chooser.getSelectedFile().toPath();if(!chosen.toString().toLowerCase(java.util.Locale.ROOT).endsWith(extension))chosen=Path.of(chosen+extension);
        if(Files.exists(chosen)&&!Dialogs.confirm(this,"Tệp đã tồn tại. Ghi đè tệp báo cáo này?"))return;
        final Path target=chosen;setBusy(true);
        new SwingWorker<Void,Void>() {
            protected Void doInBackground(){controllers.reports().export(snapshot,target,format);return null;}
            protected void done(){try{get();Dialogs.success(ReportPanel.this,"Đã xuất: "+target.toAbsolutePath());}catch(Exception ex){Dialogs.error(ReportPanel.this,ex.getCause()==null?ex.getMessage():ex.getCause().getMessage());}finally{setBusy(false);}}
        }.execute();
    }
}
