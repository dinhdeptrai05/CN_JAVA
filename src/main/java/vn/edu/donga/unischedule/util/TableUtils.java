package vn.edu.donga.unischedule.util;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Font;

public final class TableUtils {
    private TableUtils() {
    }

    public static void style(JTable table) {
        table.setRowHeight(52);
        table.setBackground(Color.WHITE);
        table.setShowHorizontalLines(true);
        table.setGridColor(Color.decode("#F1F5F9"));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable owner, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(owner, value, selected, focus, row, column);
                setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        });
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setBackground(Color.decode("#F8FAFC"));
        header.setForeground(Color.decode("#94A3B8"));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setPreferredSize(new java.awt.Dimension(0, 44));
    }
}
