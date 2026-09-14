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
- Báo cáo thống kê cho Admin/Phòng đào tạo: sử dụng phòng, khối lượng giảng dạy, sĩ số lớp, yêu cầu; lọc ngày/học kỳ/khoa, xuất CSV và bản in HTML. Xem [hướng dẫn báo cáo](docs/REPORTS.md).
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
