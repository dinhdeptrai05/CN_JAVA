# Mở rộng dữ liệu lịch sử lên ít nhất 1.000 dòng mỗi bảng nghiệp vụ

Bộ nạp này bổ sung dữ liệu **mô phỏng** trực tiếp vào MySQL `unischedule` sau khi đã chạy [bộ seed lịch sử cơ bản](HISTORY_SEED.md). Nó làm cho ứng dụng có người dùng, môn, lớp, phòng, lịch, yêu cầu và nhật ký trải qua năm học 2021–2026. Dữ liệu cũ được giữ nguyên. Tên và nội dung hiển thị như hoạt động thông thường; nguồn mô phỏng được ghi trong `audit_logs.details.seed = SCALE_1K_5Y` và dấu `SCALE_HISTORY_1000_V1`.

Database dự án đã được [bổ sung lịch sử dụng phòng theo từng kỳ](ROOM_OCCUPANCY_SEED.md) sau bước này. Số lượng trong bảng dưới là mốc **trước khi thêm lịch phòng**, còn tỷ lệ phòng 600/400 là kết quả hiện tại.

## Kết quả trên database của dự án

Đã chạy ngày 23/09/2026 với seed ngẫu nhiên cố định `20260923`. Số dưới đây gồm dữ liệu vốn có, bộ seed lịch sử cơ bản và bộ mở rộng:

| Bảng | Trước | Sau | Lý do / chức năng |
| --- | ---: | ---: | --- |
| `users` | 564 | 1.824 | Tài khoản giảng viên và sinh viên |
| `user_roles` | 564 | 1.824 | Quyền đăng nhập tương ứng |
| `courses` | 32 | 1.000 | Danh mục môn giảng dạy |
| `course_sections` | 430 | 1.398 | Lớp mở theo học kỳ |
| `lecturer_assignments` | 430 | 1.398 | Phân công giảng dạy |
| `student_enrollments` | 9.516 | 21.240 | Đăng ký học, gồm các lượt hủy |
| `classrooms` | 32 | 1.000 | Phòng ở nhiều cơ sở |
| `equipment` | 75 | 1.043 | Thiết bị trong phòng |
| `classroom_equipment` | 75 | 1.043 | Quan hệ phòng–thiết bị |
| `schedules` | 485 | 1.453 | Lịch tuần đã công bố |
| `change_requests` | 309 | 1.000 | Yêu cầu mượn thiết bị, báo hỏng, dùng phòng |
| `notifications` | 9.783 | 12.702 | Thông báo cho người dùng |
| `audit_logs` | 11.609 | 15.497 | Sự kiện nghiệp vụ và dấu nguồn gốc |
| `maintenance_records` | 99 | 1.067 | Lịch sử kiểm tra phòng/thiết bị |
| `roles` | 4 | 4 | Bốn vai trò cố định của ứng dụng |
| `departments` | 3 | 3 | Ba khoa hiện có |
| `semesters` | 13 | 13 | Hai học kỳ mỗi năm, cộng dữ liệu gốc |
| `time_slots` | 6 | 6 | Sáu ca học cố định trong ngày |

**14/18 bảng trong schema đạt ít nhất 1.000 dòng.** Bốn bảng còn lại là danh mục hữu hạn. Thêm hàng nghìn vai trò, khoa, học kỳ hoặc ca học giả sẽ làm sai chức năng phân quyền, lọc học kỳ và xếp lịch; vì vậy công cụ cố ý không nhân chúng lên 1.000.

## Phân bố và giả định

| Năm bắt đầu học kỳ | Lớp/lịch mới | Học kỳ có lịch mới |
| --- | ---: | --- |
| 2021 | 88 | Học kỳ 1 |
| 2022 | 176 | Học kỳ 2, Học kỳ 1 |
| 2023 | 176 | Học kỳ 2, Học kỳ 1 |
| 2024 | 176 | Học kỳ 2, Học kỳ 1 |
| 2025 | 176 | Học kỳ 2, Học kỳ 1 |
| 2026 | 176 | Học kỳ 2, Học kỳ 1 |
| **Tổng** | **968** | **11 học kỳ** |

- Thêm 60 giảng viên và sáu khóa sinh viên, mỗi khóa 200 người từ 2021 đến 2026. Mỗi người có một vai trò tương ứng. Khóa 2021 và 2022 đã ngừng học ở năm thứ tư; tài khoản còn lưu để xem lịch sử. Tài khoản mới có ngày đăng nhập sau ngày tạo và không muộn hơn ngày chốt; tài khoản ngừng học có ngày đăng nhập cuối trước ngày ngừng.
- Mỗi lớp mới có một giảng viên, một lịch tuần hai ca, phòng phù hợp loại môn và 12 sinh viên đăng ký còn hiệu lực; một số lớp có thêm lượt đăng ký `CANCELLED`. `student_count` chỉ tính lượt `ACTIVE`. Mỗi lịch mới dùng một phòng riêng để không tạo trùng phòng; bộ phân ca tránh trùng giảng viên và sinh viên trong cùng kỳ.
- Các yêu cầu được duyệt hoặc từ chối trước ngày bắt đầu học kỳ. Chỉ yêu cầu mượn thiết bị đáp ứng quy tắc của bộ seed mới được duyệt; yêu cầu không thể đáp ứng có lý do từ chối. Bảo trì mới hoàn tất trước khi lịch dạy bắt đầu. Thông báo cũ đã đọc; thông báo gần ngày chốt có thể chưa đọc.
- Trong 11.724 lượt đăng ký mới, 11.616 còn hiệu lực và 108 đã hủy. Có 691 yêu cầu mới: 355 được duyệt và 336 bị từ chối. Các số này đối chiếu bằng quan hệ giữa bảng nghiệp vụ và nhật ký mang dấu nguồn gốc.
- Đây là bộ dữ liệu phục vụ trình bày quy mô lớn theo yêu cầu bài tập. Giả định nhiều cơ sở/phòng và nhiều môn tự chọn trong 5 năm giải thích số phòng, môn cao; không hàm ý trường thực tế có đúng quy mô này. Không có nghiệp vụ thanh toán trong schema nên không tạo doanh thu giả.
- Lịch của Học kỳ 1 năm 2026 có thể tiếp diễn sau 23/09/2026, nhưng các buổi chưa diễn ra không được tính là lịch sử đã xảy ra. Bộ nạp không thay đổi giới hạn xem 5 năm của Admin/Đào tạo và 3 năm của Giảng viên/Sinh viên.

## Chạy và kiểm tra

```powershell
# Xem số lượng hiện tại, không ghi database
./scripts/seed-thousand-history.ps1 -Preview

# Thử toàn bộ INSERT và phép kiểm tra trong transaction rồi rollback
./scripts/seed-thousand-history.ps1 -DryRun

# Chèn vào MySQL của dự án; chạy lần thứ hai trả ALREADY_APPLIED
./scripts/seed-thousand-history.ps1

# Kiểm chứng lại số dòng, sĩ số, đăng nhập, lịch trùng và độ phủ học kỳ
./scripts/seed-thousand-history.ps1 -Verify

# Kiểm tra ứng dụng và giao diện thời khóa biểu trên database đã nạp
mvn test
./scripts/test-timetable-ui.ps1
```

MySQL cần chạy theo cấu hình `.local/mysql-test/connection.json`. Wrapper dùng cấu hình này mà không ghi mật khẩu ra log. `HistoricalThousandSeed` mặc định chỉ xem trước; `--apply` mới ghi. Các thao tác thêm dùng một transaction, khóa MySQL để tránh chạy đồng thời và chỉ thực hiện `INSERT`. Dấu hoàn tất nằm trong cùng transaction, nên chạy lại không tăng số dòng. Seed cố định cho cùng chuỗi dữ liệu; khóa tự tăng có thể khác trên database khác hoặc sau khi thử rollback.

`-DryRun`, `-Verify` và chạy lại sau commit đã thành công. Kiểm tra riêng dữ liệu mới: 11/11 học kỳ có lịch, **0 nhóm trùng lịch giảng viên, 0 nhóm trùng lịch sinh viên**, 0 ngày đăng nhập không hợp lệ; sĩ số lớp khớp số đăng ký `ACTIVE`. Trong dữ liệu demo **có trước** bộ mở rộng có 7 nhóm trùng lịch sinh viên, không thuộc các tài khoản/lớp mới; công cụ không sửa hoặc xóa dữ liệu gốc. UI thời khóa biểu và bộ kiểm thử Maven đều chạy thành công trên database mở rộng.
