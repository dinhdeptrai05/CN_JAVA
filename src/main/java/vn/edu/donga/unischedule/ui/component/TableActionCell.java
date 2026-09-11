package vn.edu.donga.unischedule.ui.component;

import javax.swing.JPanel;
import java.awt.FlowLayout;

public final class TableActionCell {
    private TableActionCell() {
    }

    public static JPanel create(SecondaryButton viewButton, SecondaryButton editButton, DangerButton deleteButton) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);
        if (viewButton != null) {
            panel.add(viewButton);
        }
        if (editButton != null) {
            panel.add(editButton);
        }
        if (deleteButton != null) {
            panel.add(deleteButton);
        }
        return panel;
    }
}
