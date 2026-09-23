# Bổ sung lịch học cho phòng qua tài khoản Đào tạo

Database hiện có 1.000 phòng. Trước bước này, mỗi học kỳ chỉ khoảng 93–111 phòng có lịch; riêng Học kỳ 2 năm học 2026 chưa có lịch. Bộ nạp bổ sung **lớp học phần, phân công giảng viên, đăng ký sinh viên và lịch đã công bố** với `created_by` là tài khoản `daotao`. Dữ liệu cũ không bị sửa hoặc xóa.

Database đã được [mở rộng tiếp để mọi phòng sử dụng được có lịch ít nhất bốn ngày/tuần](FOUR_DAY_TIMETABLE.md). Tỷ lệ 600/400 bên dưới là **kết quả của bước trung gian**, trước lần bổ sung mới nhất; hiện tại 998 phòng sử dụng được đều có lịch trong từng kỳ.

## Cách tính 40% phòng trống

Trong **mỗi học kỳ**, phòng được tính là *có sử dụng* nếu có ít nhất một lịch học đã công bố thuộc kỳ đó. Đích là **600/1.000 phòng có lịch, 400/1.000 phòng chưa có lịch**. Hai bản ghi Học kỳ 1 năm 2026 có thời gian chồng nhau được tính chung thành một kỳ để tránh nhân đôi số phòng. Đây là tỷ lệ **phòng có lịch trong cả kỳ**, không phải tỷ lệ phòng đang bận tại mọi ngày và ca; màn hình tìm phòng trống theo ngày/ca vẫn tính đúng thời điểm mà người dùng chọn.

| Năm học | Học kỳ 1 | Học kỳ 2 |
| --- | --- | --- |
| 2021 | 600 có lịch / 400 trống | 600 / 400 |
| 2022 | 600 / 400 | 600 / 400 |
| 2023 | 600 / 400 | 600 / 400 |
| 2024 | 600 / 400 | 600 / 400 |
| 2025 | 600 / 400 | 600 / 400 |
| 2026 | 600 / 400 | 600 / 400 |

Học kỳ 2 năm học 2026 bắt đầu tháng 1/2027, nên lịch của kỳ này là **lịch đã lên kế hoạch**, không phải buổi đã diễn ra trong giai đoạn lịch sử đến 23/09/2026. Màn hình lịch sử theo vai trò vẫn giữ phạm vi cũ: Admin/Đào tạo xem 5 năm, Sinh viên/Giảng viên xem 2024–2026 theo năm bắt đầu dạy.

## Giả định và tính nhất quán

- Chọn phòng còn sẵn sàng, môn phù hợp loại phòng, rồi tạo lớp và phân công giảng viên thuộc khoa của môn. Mỗi phòng được thêm một lịch tuần hai ca trong kỳ; ngày và ca được dàn đều trong tuần. Số sinh viên/lớp bổ sung tăng theo quy mô các khóa đang học: 4 ở kỳ đầu 2021, 6 ở 2022, 8 ở 2023 và 10 từ 2024. Sĩ số khớp số đăng ký `ACTIVE`.
- Công cụ đọc lịch và đăng ký đã có trước khi xếp. Mỗi lịch mới dùng phòng chưa có lịch trong kỳ; giảng viên và sinh viên không bị xếp hai lớp cùng ca. Các lịch gốc, kể cả lịch của bản ghi Học kỳ 1 năm 2026 còn lại, đều được xét khi kiểm tra xung đột.
- Lịch do `daotao` công bố và có nhật ký `CREATE_SECTION`, `PUBLISH_SCHEDULE` với `details.seed=ROOM_OCCUPANCY_60`. Dấu `ROOM_OCCUPANCY_60_V1` cho phép chạy lại mà không tạo bản ghi trùng. Tên lớp và nội dung trên giao diện không có nhãn “5N”.
- Bộ nạp cố định seed `20260923`, chỉ dùng `INSERT` trong một transaction. Ngày tạo của lịch Học kỳ 2 năm học 2026 được chặn ở ngày 23/09/2026 để không giả vờ rằng thao tác hành chính đã xảy ra trong tương lai.

## Chạy lại và kiểm chứng

```powershell
./scripts/seed-room-occupancy.ps1 -Preview  # chỉ đọc số liệu hiện tại
./scripts/seed-room-occupancy.ps1 -DryRun   # tạo và kiểm tra, rồi rollback
./scripts/seed-room-occupancy.ps1           # ghi thật; chạy lại trả ALREADY_APPLIED
./scripts/seed-room-occupancy.ps1 -Verify   # đọc kiểm tra đủ 600/400 ở 12 kỳ
./scripts/test-timetable-ui.ps1            # kiểm tra giao diện lịch và bảng phòng
mvn test
```

Cần chạy [bộ seed 1.000 dòng](THOUSAND_ROW_SEED.md) trước. Bản thử `-DryRun` đã qua kiểm tra, sau đó đã ghi thành công và `-Verify` xác nhận 12/12 kỳ đạt 600/400, không phát sinh xung đột phòng, giảng viên hoặc sinh viên từ dữ liệu mới. Lần kiểm tra giao diện đã mở được bảng 1.000 phòng. Màn hình **Phòng và thiết bị** hiện có hai chỉ số **Có lịch kỳ này** và **Chưa có lịch**; trạng thái `AVAILABLE` của phòng vẫn chỉ khả năng sử dụng, không có nghĩa là phòng chưa từng được xếp lịch.

Tab **Tra cứu phòng trống** tải kết quả ở luồng nền khi mở hoặc đổi tab, lấy thiết bị theo lô và chỉ dựng 48 thẻ mỗi trang; dạng bảng vẫn liệt kê toàn bộ kết quả. Khi quay lại tab trong lúc đang tải, ứng dụng giữ công việc hiện tại thay vì khởi tạo thêm truy vấn; kết quả mới được giữ trong 15 giây, nút **Tìm kiếm** luôn cho phép tải lại ngay. Điều này tránh treo giao diện khi cơ sở dữ liệu có hàng nghìn phòng và lịch.
