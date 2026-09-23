package vn.edu.donga.unischedule.ui.component;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.model.Enums.*;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;

public class StatusBadge extends JLabel {
    public StatusBadge(Object status) {
        setOpaque(false);
        setFont(getFont().deriveFont(Font.BOLD, 12f));
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setText(display(status));
        setForeground(color(status));
        setBackground(background(color(status)));
    }

    private String display(Object status) {
        if (status instanceof UserStatus value) return value.getDisplayName();
        if (status instanceof RoomStatus value) return value.getDisplayName();
        if (status instanceof ResourceStatus value) return value.getDisplayName();
        if (status instanceof ScheduleStatus value) return value.getDisplayName();
        if (status instanceof CourseSectionStatus value) return value.getDisplayName();
        if (status instanceof RequestStatus value) return value.getDisplayName();
        if (status instanceof ConflictStatus value) return value.getDisplayName();
        if (status instanceof Priority value) return value.getDisplayName();
        return String.valueOf(status);
    }

    @Override protected void paintComponent(java.awt.Graphics graphics) {
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground()); g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.dispose(); super.paintComponent(graphics);
    }

    private Color color(Object status) {
        if (status == UserStatus.INACTIVE) return AppConfig.MUTED;
        if ("Chờ đăng nhập".equals(status)) return AppConfig.WARNING;
        if (status == UserStatus.LOCKED || status == RoomStatus.MAINTENANCE || status == RoomStatus.INACTIVE
                || status == ResourceStatus.BROKEN || status == CourseSectionStatus.CONFLICTED
                || status == RequestStatus.REJECTED) {
            return AppConfig.DANGER;
        }
        if (status == RequestStatus.PENDING || status == ConflictStatus.OPEN || status == ConflictStatus.PROCESSING
                || status == Priority.HIGH || status == Priority.URGENT || status == ResourceStatus.MAINTENANCE
                || status == ScheduleStatus.DRAFT) {
            return AppConfig.WARNING;
        }
        return AppConfig.SUCCESS;
    }

    private Color background(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 28);
    }
}
