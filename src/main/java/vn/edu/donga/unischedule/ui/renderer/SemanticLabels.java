package vn.edu.donga.unischedule.ui.renderer;

import vn.edu.donga.unischedule.model.Enums.*;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import javax.swing.*;
import java.awt.*;

public final class SemanticLabels {
    private SemanticLabels() { }
    public static String text(Object value) {
        if (value == null) return "Chưa chọn";
        if (value instanceof Role v) return v.getDisplayName();
        if (value instanceof UserStatus v) return v.getDisplayName();
        if (value instanceof RoomStatus v) return v.getDisplayName();
        if (value instanceof RoomType v) return v.getDisplayName();
        if (value instanceof ResourceStatus v) return v.getDisplayName();
        if (value instanceof ScheduleStatus v) return v.getDisplayName();
        if (value instanceof CourseSectionStatus v) return v.getDisplayName();
        if (value instanceof RequestType v) return v.getDisplayName();
        if (value instanceof RequestStatus v) return v.getDisplayName();
        if (value instanceof Priority v) return v.getDisplayName();
        if (value instanceof ConflictType v) return v.getDisplayName();
        if (value instanceof ConflictStatus v) return v.getDisplayName();
        if (value instanceof NotificationType v) return v.getDisplayName();
        if (value instanceof ScheduleEntry v) return v.getCourseSection().getCode() + " - "
                + v.getCourseSection().getCourse().getName() + " / " + v.getRoom().getCode();
        return value.toString();
    }
    public static void install(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JComboBox<?> combo) combo.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, text(value), index, selected, focus);
                    setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                    setToolTipText(text(value)); return this;
                }
            });
            else if (child instanceof Container nested) install(nested);
        }
    }
}
