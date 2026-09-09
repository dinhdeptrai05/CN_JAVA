package vn.edu.donga.unischedule.ui.dialog;

import vn.edu.donga.unischedule.model.ChangeRequest;
import vn.edu.donga.unischedule.util.DateUtils;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;

public final class RequestDetailDialog {
    private RequestDetailDialog() {
    }

    public static void showDialog(Component parent, ChangeRequest request) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Chi tiết yêu cầu YC" + request.getId(), true);
        JTextArea area = new JTextArea(detail(request), 14, 58);
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

    private static String detail(ChangeRequest request) {
        String schedule = request.getScheduleEntry() == null ? "Không có"
                : request.getScheduleEntry().getCourseSection().getCode() + " - " + request.getScheduleEntry().getRoom().getCode();
        String room = request.getDesiredRoom() == null ? "Không chọn" : request.getDesiredRoom().getCode();
        String date = request.getDesiredDate() == null ? "Không chọn" : DateUtils.format(request.getDesiredDate());
        String slot = request.getDesiredSlot() == null ? "Không chọn" : request.getDesiredSlot().getName();
        return """
                Người gửi: %s
                Loại yêu cầu: %s
                Lịch liên quan: %s
                Phòng mong muốn: %s
                Ngày mong muốn: %s
                Ca mong muốn: %s
                Thiết bị: %s
                Số lượng: %d
                Mức ưu tiên: %s
                Trạng thái: %s

                Lý do:
                %s

                Phản hồi:
                %s
                """.formatted(request.getRequester().getFullName(),
                request.getType().getDisplayName(), schedule, room, date, slot,
                request.getEquipmentName().isBlank() ? "Không có" : request.getEquipmentName(),
                request.getEquipmentQuantity(), request.getPriority().getDisplayName(),
                request.getStatus().getDisplayName(), request.getReason(),
                request.getResponseReason() == null ? "Chưa có" : request.getResponseReason());
    }
}
