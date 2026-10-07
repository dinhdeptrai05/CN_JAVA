# Dữ liệu vận hành 5 năm từ 2026 đến 2030

Ứng dụng hiện dùng giai đoạn mô phỏng **2026–2030**. Dữ liệu 2021–2025 vẫn được giữ trong MySQL để không làm mất dữ liệu, nhưng không còn nằm trong phạm vi hiển thị của Thời khóa biểu và Hoạt động theo năm.

## Phạm vi theo vai trò

| Vai trò | Năm được xem |
|---|---|
| Admin, Phòng đào tạo | 2026, 2027, 2028, 2029, 2030 |
| Giảng viên, Sinh viên | 2026, 2027, 2028 |

Mỗi năm có Học kỳ 1 và Học kỳ 2. Lịch tương lai là lịch đã lên kế hoạch và được hiển thị như dữ liệu vận hành thông thường. Phân quyền chỉ giới hạn phạm vi đọc; không cấp thêm quyền sửa lịch cho Giảng viên hoặc Sinh viên.

## Dữ liệu đã nạp

Seed cố định `20260923` đã bổ sung các học kỳ còn thiếu và lịch cho giai đoạn 2027–2030. Kết quả trên database dự án:

- 10 kỳ từ năm 2026 đến 2030;
- 998/998 phòng đang sử dụng được có lịch vào ít nhất 4 ngày khác nhau mỗi tuần trong từng kỳ;
- thêm 13.972 lớp học phần và 27.944 lịch học;
- thêm 240 tài khoản, 192 yêu cầu sử dụng phòng, 144 đợt bảo trì hoàn tất và 432 thông báo, phân đều trong 2027–2030;
- không tạo lịch cho hai phòng đang bảo trì hoặc ngừng sử dụng;
- không phát hiện xung đột mới về phòng, giảng viên hoặc sinh viên.

Nguồn mô phỏng được đánh dấu trong `audit_logs` bằng action `FORWARD_FIVE_YEARS_V1`, `FORWARD_ACTIVITY_2026_2030_V1` và marker chuẩn hóa ngày. Hai bước đầu chỉ thêm dữ liệu. Bước cuối chỉ điều chỉnh ngày của lớp, phân công, đăng ký và lịch có mã `LHD20...` do chính bộ mô phỏng tạo, để số liệu được tính vào đúng năm học; dữ liệu thật và dữ liệu cũ khác không bị cập nhật hoặc xóa.

## Chạy lại và kiểm tra

```powershell
# Xem phạm vi hiện có, không ghi dữ liệu
.\scripts\seed-forward-five-years.ps1 -Preview

# Chạy toàn bộ trong transaction rồi rollback
.\scripts\seed-forward-five-years.ps1 -DryRun

# Ghi dữ liệu; chạy lần hai sẽ trả về ALREADY_APPLIED
.\scripts\seed-forward-five-years.ps1

# Chỉ kiểm tra dữ liệu đã nạp
.\scripts\seed-forward-five-years.ps1 -Verify
```

MySQL cục bộ cần chạy trước. Có thể dùng `powershell -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 -Smoke` để khởi động database và kiểm tra kết nối.
