package vn.edu.donga.unischedule.service;

import vn.edu.donga.unischedule.model.AuditEntry;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only demo history. Replace with a persisted audit trail in the JDBC phase. */
public final class AuditService {
    private final List<AuditEntry> entries;
    public AuditService() {
        LocalDateTime now = LocalDateTime.now();
        entries = List.of(
                new AuditEntry(now.minusMinutes(10), "admin", "Đăng nhập", "Hệ thống", "Thành công"),
                new AuditEntry(now.minusMinutes(20), "daotao", "Tạo lịch học", "IT101-01", "Mô phỏng"),
                new AuditEntry(now.minusMinutes(30), "daotao", "Kiểm tra xung đột", "Tuần hiện tại", "Phát hiện 3 loại"),
                new AuditEntry(now.minusMinutes(40), "admin", "Cập nhật phòng", "B204", "Chuyển bảo trì"),
                new AuditEntry(now.minusMinutes(50), "giangvien", "Gửi yêu cầu", "Mượn thiết bị", "Chờ duyệt"),
                new AuditEntry(now.minusMinutes(60), "sinhvien", "Tra cứu phòng", "C304", "Chỉ xem"));
    }
    public List<AuditEntry> findAll() { return entries; }
}
