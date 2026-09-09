package vn.edu.donga.unischedule.ui.component;

import java.awt.*;

public class ResponsiveCardGrid extends ScrollPage {
    public ResponsiveCardGrid() { setLayout(new GridLayout(0, 3, 16, 16)); }
    private int columns() { return Math.max(1, Math.min(3, Math.max(1, getWidth()) / 300)); }
    @Override public void doLayout() {
        ((GridLayout) getLayout()).setColumns(columns());
        super.doLayout();
    }
    @Override public Dimension getPreferredSize() {
        int rows = (getComponentCount() + columns() - 1) / columns();
        return new Dimension(300, Math.max(220, rows * 236 - 16));
    }
}
