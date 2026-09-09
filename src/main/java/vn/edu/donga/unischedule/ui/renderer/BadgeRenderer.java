package vn.edu.donga.unischedule.ui.renderer;

import vn.edu.donga.unischedule.ui.component.StatusBadge;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

public class BadgeRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        StatusBadge badge = new StatusBadge(value);
        if (isSelected) {
            badge.setBackground(table.getSelectionBackground());
            badge.setForeground(table.getSelectionForeground());
        }
        javax.swing.JPanel cell = new javax.swing.JPanel(new java.awt.GridBagLayout());
        cell.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        badge.setFont(badge.getFont().deriveFont(10f));
        cell.add(badge);
        return cell;
    }
}
