package vn.edu.donga.unischedule.ui.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GenericTableModel<T> extends AbstractTableModel {
    private final String[] columns;
    private final Function<T, Object>[] accessors;
    private List<T> rows = new ArrayList<>();

    @SafeVarargs
    public GenericTableModel(String[] columns, Function<T, Object>... accessors) {
        this.columns = columns;
        this.accessors = accessors;
    }

    public void setRows(List<T> rows) {
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public T getRowAt(int rowIndex) {
        return rows.get(rowIndex);
    }

    public List<T> getRows() {
        return rows;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return accessors[columnIndex].apply(rows.get(rowIndex));
    }
}
