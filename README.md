# UniSchedule

Ứng dụng desktop Java Swing, JDBC và MySQL cho đề tài quản lý thời khóa biểu và tài nguyên phòng học.

## Trạng thái triển khai

Ứng dụng đọc/ghi MySQL bằng JDBC, có đăng nhập bằng mật khẩu băm, transaction, nhật ký, thông báo và cập nhật ảnh hồ sơ. Xem [hướng dẫn database và chạy ứng dụng](README_DATABASE.md). Mock chỉ còn trong thư mục test.

## Công nghệ

Dự án được tổ chức theo MVC: `ui` (View) gọi `controller`; Controller sử dụng `service`, `model` và `repository`. Xem [tài liệu kiến trúc](docs/ARCHITECTURE.md) để biết trách nhiệm từng tầng, luồng xử lý và các quy tắc kiểm thử.

- Java 17+
- Maven
- Java Swing
- FlatLaf
- Heroicons outline (SVG đóng gói trong ứng dụng)
- Poppins Regular/Medium/SemiBold/Bold, có font dự phòng cho ký tự tiếng Việt mà Poppins không hỗ trợ
- MySQL 8.4 và MySQL Connector/J

## Chạy ứng dụng

```bash
./scripts/run-local.ps1 # bản MySQL riêng đã cấu hình trên máy này
mvn exec:java # sau khi cấu hình MySQL theo README_DATABASE.md
```

## Màn hình đã có

- Đăng nhập từ tài khoản lưu trong MySQL.
- Dashboard theo vai trò.
- Báo cáo thống kê cho Admin/Phòng đào tạo: sử dụng phòng, khối lượng giảng dạy, sĩ số lớp, yêu cầu; lọc ngày/học kỳ/khoa, xuất PDF, Word và Excel. Xem [hướng dẫn báo cáo](docs/REPORTS.md).
- Thời khóa biểu tuần dạng lịch và dạng bảng.
- Dialog thêm/sửa lịch có validation và kiểm tra xung đột.
- Danh sách xung đột và dialog so sánh hai lịch.
- Quản lý lớp học phần.
- Quản lý phòng học và thiết bị.
- Chi tiết phòng, danh sách thiết bị và lịch sử dụng phòng.
- Quản lý yêu cầu và form gửi yêu cầu giảng viên.
- Quản lý người dùng cho quản trị viên.
- Tra cứu phòng trống dạng card hoặc bảng.
- Thông báo.
- Hồ sơ và cài đặt.
- Nhật ký hoạt động lưu trong MySQL.

## Mô phỏng 5 năm (2027–2031)

Chạy `./scripts/run-simulation.ps1` để tạo dữ liệu riêng cho 18 bảng với seed, kiểm tra nhất quán và báo cáo HTML theo năm/tháng. Không ghi vào database ứng dụng. Xem [hướng dẫn, giả định và kết quả](docs/SIMULATION.md) hoặc [báo cáo đã chạy với seed 42](simulation/results/2027-2031-seed42-verified/report.html).

## Seed dữ liệu như ứng dụng đã sử dụng 5 năm

Chạy `./scripts/seed-history.ps1` để **thêm dữ liệu lịch sử trực tiếp vào database hiện tại**: hàng trăm người dùng, 11 học kỳ, lớp học phần, hàng nghìn đăng ký/thông báo/nhật ký, lịch và yêu cầu. Giữ nguyên dữ liệu cũ; chạy lại không chèn trùng. Dùng `-Preview` để chỉ xem trước hoặc `-Verify` để kiểm chứng.

Sau bước này, chạy `./scripts/seed-thousand-history.ps1` để mở rộng dữ liệu: 14 bảng nghiệp vụ có **ít nhất 1.000 dòng mỗi bảng** trên database dự án, với lớp và lịch phân bố qua 11 học kỳ trong 5 năm. Bốn bảng danh mục cố định (`roles`, `departments`, `semesters`, `time_slots`) giữ đúng quy mô nghiệp vụ. Xem [số lượng từng bảng, giả định và lệnh kiểm chứng](docs/THOUSAND_ROW_SEED.md).

Chạy tiếp `./scripts/seed-room-occupancy.ps1` để Đào tạo công bố thêm lịch: **600/1.000 phòng có lịch, 400 phòng chưa có lịch trong từng học kỳ** từ năm học 2021 đến 2026. Xem [quy tắc tính và hướng dẫn kiểm tra](docs/ROOM_OCCUPANCY_SEED.md).

Bước mới nhất `./scripts/seed-four-day-timetable.ps1` lấp lịch học cho **998 phòng đang sử dụng được**, mỗi phòng đúng 4 ngày/tuần trong từng học kỳ từ năm học 2021 đến 2026; hai phòng bảo trì/ngừng dùng được giữ nguyên. Màn hình lịch dạng thẻ đã phân trang để xem được khối lượng lịch lớn. Xem [số liệu, giả định và kiểm chứng](docs/FOUR_DAY_TIMETABLE.md).

Đăng nhập bằng tài khoản gốc hoặc tài khoản bổ sung. Admin/Đào tạo mở **Hoạt động 5 năm** và có thể mở báo cáo của năm đã chọn; Giảng viên/Sinh viên mở **Hoạt động 3 năm** trên thanh bên. Đây là số liệu tổng hợp toàn hệ thống trong khoảng thời gian theo vai trò. Tên, mã và nội dung hiển thị trong bộ dữ liệu lịch sử có dạng vận hành thông thường; dấu nguồn gốc mô phỏng nằm trong nhật ký kỹ thuật và biên nhận. Các màn hình người dùng, lớp, yêu cầu mặc định tập trung vào dữ liệu đang hoạt động; nhật ký phân trang để xem được lịch sử lớn. Có thể chạy `./scripts/test-history-ui.ps1` để kiểm tra giao diện trên database đã seed và tạo ảnh xem trước. Xem [tài khoản, giả định và hướng dẫn](docs/HISTORY_SEED.md).

Màn hình **Người dùng** có bộ lọc năm đăng nhập cuối và tổng hợp số tài khoản theo năm. Dữ liệu đăng nhập mô phỏng hiện phân bố từ 2022 đến 2026; tài khoản `ACTIVE` là tài khoản được phép đăng nhập, còn `INACTIVE` là đã ngừng sử dụng. Xem [cách tái tạo và kiểm chứng các mốc đăng nhập](docs/HISTORY_SEED.md).

Màn hình **Thời khóa biểu** chọn năm và **Học kỳ 1 / Học kỳ 2** theo ngày học thực tế. Admin/Đào tạo xem cửa sổ 5 năm; Giảng viên/Sinh viên chọn được các năm 2024–2026. Lịch cũ tự mở về tuần đầu kỳ; Giảng viên/Sinh viên có thể xem lịch cá nhân hoặc lịch đã công bố toàn trường. Lệnh `./scripts/test-timetable-ui.ps1` kiểm tra các kỳ và xuất ảnh tại `target/timetable-ui-previews`. Xem [cách dùng và số liệu theo kỳ](docs/HISTORY_SEED.md#thời-khóa-biểu-theo-năm-và-kỳ).

## Kiểm chứng

```bash
mvn test
```

Bộ smoke test kiểm tra đăng nhập đủ 4 tài khoản, dữ liệu mock tối thiểu, đủ 3 loại xung đột, luồng gửi yêu cầu giảng viên và khởi tạo frame Swing cho 4 role.

Kiểm thử render tạo ảnh trong `target/ui-previews`: màn hình chính ở chiều rộng 1180/1440, dashboard theo 4 vai trò và các form nhập liệu. Kiểm tra font, SVG và ảnh không rỗng được chạy cùng `mvn test`.

## Giao diện tham chiếu

Giao diện được điều chỉnh theo `screen.png` và `code.html` trong `stitch_prompt_ui_generator.zip`: sidebar trắng 256px, nền `#F8FAFC`, màu chính `#5850EC`, trạng thái pastel, dashboard hai cột, khoảng cách 16/24/32px và biểu mẫu sáng. Các màn hình quản lý dùng chung hệ màu, typography và thành phần Swing.

Số liệu lấy từ MySQL. Ảnh hồ sơ được người dùng cập nhật và lưu trong database. Font và icon được lưu cục bộ trong `src/main/resources`, kèm giấy phép OFL/MIT, không cần tải tài nguyên giao diện khi chạy.

## Tài khoản demo

Mật khẩu chung: `123456`

| Username | Vai trò | Tên hiển thị |
| --- | --- | --- |
| admin | ADMIN | Nguyễn Quản Trị |
| daotao | ACADEMIC | Trần Thu Hà |
| giangvien | LECTURER | Phạm Anh Tuấn |
| sinhvien | STUDENT | Nguyễn Hoàng Nam |

## Cơ sở dữ liệu

Xem [README_DATABASE.md](README_DATABASE.md) để tạo schema, cấu hình kết nối, quản lý dữ liệu seed và chạy kiểm thử JDBC.
