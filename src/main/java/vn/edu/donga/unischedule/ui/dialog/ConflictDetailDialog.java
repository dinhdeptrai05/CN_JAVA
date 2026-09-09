package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.Conflict;
import vn.edu.donga.unischedule.model.ScheduleEntry;
import vn.edu.donga.unischedule.util.DateUtils;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;

public final class ConflictDetailDialog {
    private ConflictDetailDialog() {
    }

    public static void showDialog(Component parent, Conflict conflict) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Chi tiết xung đột " + conflict.getId(), true);
        JTextArea area = new JTextArea(detail(conflict), 14, 64);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        dialog.add(area, BorderLayout.CENTER);
        dialog.pack();
        DialogTheme.apply(dialog);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static String detail(Conflict conflict) {
        return """
                Mã xung đột: %s
                Loại: %s
                Trạng thái: %s
                Mô tả: %s

                Lịch thứ nhất:
                %s

                Lịch thứ hai:
                %s
                """.formatted(conflict.getId(), conflict.getType().getDisplayName(),
                conflict.getStatus().getDisplayName(), conflict.getMessage(),
                schedule(conflict.getFirstSchedule()), schedule(conflict.getSecondSchedule()));
    }

    private static String schedule(ScheduleEntry entry) {
        if (entry == null) {
            return "Không có";
        }
        return "%s | %s | %s | %s | %s-%s | %s đến %s".formatted(
                entry.getCourseSection().getCode(),
                entry.getCourseSection().getCourse().getName(),
                entry.getCourseSection().getLecturer().getFullName(),
                entry.getRoom().getCode(),
                entry.getStartSlot().getName(),
                entry.getEndSlot().getName(),
                DateUtils.format(entry.getStartDate()),
                DateUtils.format(entry.getEndDate()));
    }
}
