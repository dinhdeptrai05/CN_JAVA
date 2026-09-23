# Thời khóa biểu phòng học bốn ngày mỗi tuần

Đây là bước mở rộng sau [bộ lịch sử dụng phòng](ROOM_OCCUPANCY_SEED.md). Trên database dự án ngày 23/09/2026, **998 phòng có trạng thái `AVAILABLE` đều có lịch đã công bố vào đúng 4 ngày khác nhau trong tuần ở mỗi học kỳ của năm học 2021–2026**. Hai phòng `B204` đang bảo trì và `D306` ngừng sử dụng được giữ nguyên, không gán lịch trái với trạng thái phòng.

| Năm học | Học kỳ 1 | Học kỳ 2 |
| --- | --- | --- |
| 2021 | 998/998 phòng, 4 ngày/phòng | 998/998, 4 ngày/phòng |
| 2022 | 998/998, 4 ngày/phòng | 998/998, 4 ngày/phòng |
| 2023 | 998/998, 4 ngày/phòng | 998/998, 4 ngày/phòng |
| 2024 | 998/998, 4 ngày/phòng | 998/998, 4 ngày/phòng |
| 2025 | 998/998, 4 ngày/phòng | 998/998, 4 ngày/phòng |
| 2026 | 998/998, 4 ngày/phòng | 998/998, 4 ngày/phòng |

Học kỳ 1 năm 2026 có hai bản ghi kỳ chồng thời gian; lịch của cả hai được gộp khi tính số ngày của phòng. Học kỳ 2 năm học 2026 bắt đầu tháng 1/2027 và là lịch đã lên kế hoạch, không được tính là buổi học đã diễn ra trước ngày chốt.

## Dữ liệu và giả định

Bộ nạp thêm **23.855 lớp học phần, 40.601 lịch tuần, 225 giảng viên và 4.050 sinh viên**. Sau nạp, database có 48.159 bản ghi lịch, 31.358 lớp và 213.296 lượt đăng ký. Tài khoản Đào tạo `daotao` là người công bố lịch (`schedules.created_by`); nhật ký kỹ thuật ghi `details.seed=ROOM_FOUR_DAYS` và dấu `ROOM_FOUR_DAYS_V1`.

- Một lớp bổ sung có 6 sinh viên đăng ký còn hiệu lực và thường học 2 ngày/tuần, mỗi ngày 2 ca. Lớp cuối của một phòng có thể học 1 ngày nếu phòng đã có lịch trước đó. Sĩ số trong `course_sections` khớp số đăng ký `ACTIVE`.
- Giảng viên, sinh viên và phòng không bị xếp hai lớp cùng ngày/ca trong khoảng hiệu lực. Môn có loại phòng phù hợp; riêng ba phòng thí nghiệm được bổ sung môn thực hành phù hợp. Mỗi kỳ có lịch vào ít nhất 4 ngày/tuần cho từng phòng, không có nghĩa là phòng bận cả ngày trong 4 ngày đó.
- Bổ sung tài khoản sinh viên theo khóa học để đủ quy mô đăng ký; khóa 2021–2022 đã ngừng sử dụng ở mốc năm thứ tư nhưng lịch sử học vẫn lưu. Ngày tạo, đăng nhập cuối và trạng thái được đặt theo vòng đời tài khoản. Không dùng thông tin cá nhân thật.
- Dùng seed cố định `20260923`, chỉ `INSERT` trong một transaction; không thay đổi lịch, tài khoản hay trạng thái phòng đã có. Chạy lại trả `ALREADY_APPLIED`.

## Giao diện và quyền xem

Admin và Phòng đào tạo tiếp tục xem lịch trong cửa sổ **5 năm**; Giảng viên và Sinh viên chỉ chọn được các năm dương lịch **2024, 2025, 2026**. Phạm vi này được kiểm tra qua bốn tài khoản gốc. Lịch cá nhân của Giảng viên/Sinh viên vẫn dựa trên phân công hoặc đăng ký; lịch công bố toàn trường là chế độ chỉ đọc.

Với khoảng 4.000 lịch/tuần của toàn trường, truy vấn thời khóa biểu nay chỉ đọc lịch của tuần được chọn. Dạng lịch chia tối đa **48 thẻ/trang cho một ngày**; bộ lọc phòng/giảng viên/khoa và dạng bảng vẫn cho phép xem toàn bộ lịch phù hợp. Ô tìm kiếm đợi người dùng ngừng gõ trong 250 ms trước khi tải lại để tránh truy vấn liên tục.

## Chạy lại và kiểm chứng

```powershell
./scripts/seed-four-day-timetable.ps1 -Preview
./scripts/seed-four-day-timetable.ps1 -DryRun
./scripts/seed-four-day-timetable.ps1
./scripts/seed-four-day-timetable.ps1 -Verify
./scripts/test-timetable-ui.ps1
mvn test
```

`-Preview` chỉ đọc. `-DryRun` thực hiện toàn bộ INSERT và kiểm tra trong transaction rồi rollback. `-Verify` kiểm tra 12 kỳ, ngày học của từng phòng, sĩ số, người công bố và xung đột phòng/giảng viên/sinh viên. Bản chạy của dự án đã qua `-DryRun`, commit và `-Verify`; thử chạy lại không tạo bản ghi trùng. Cần chạy [bộ seed 1.000 dòng](THOUSAND_ROW_SEED.md) và [bộ lịch phòng 60%](ROOM_OCCUPANCY_SEED.md) trước trên database mới.
