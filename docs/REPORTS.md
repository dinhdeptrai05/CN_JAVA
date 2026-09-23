# Báo cáo thống kê

Đăng nhập bằng tài khoản **Admin** hoặc **Phòng đào tạo**, chọn **Báo cáo thống kê** ở thanh bên. Chọn ngày bắt đầu/kết thúc (`yyyy-MM-dd`), học kỳ và khoa, rồi nhấn **Tạo báo cáo**. Khoảng ngày bao gồm cả hai đầu, tối đa 366 ngày. Dữ liệu được đọc từ MySQL trong một transaction có snapshot nhất quán; không cần tạo thêm bảng hoặc chạy migration.

| Báo cáo | Nội dung và cách tính |
| --- | --- |
| Sử dụng phòng | Mỗi phòng có số buổi học, ca đã dùng và tỷ lệ sử dụng. Lịch tuần được triển khai thành từng ngày thực tế trong khoảng chọn. Ca đã dùng không đếm lặp cùng phòng/ngày/ca nếu có lịch trùng. |
| Khối lượng giảng dạy | Số lớp có buổi dạy, tổng buổi, tổng ca và giờ dạy của từng giảng viên. Giờ dạy cộng thời lượng từng ca, không cộng khoảng nghỉ. Giảng viên được phân công lớp trong phạm vi lọc nhưng chưa có buổi dạy vẫn xuất hiện với giá trị 0. |
| Lớp học phần | Sĩ số, sức chứa, tỷ lệ lấp đầy, trạng thái hiện tại và số buổi trong khoảng chọn. Bao gồm lớp thuộc học kỳ giao với khoảng ngày dù chưa có lịch. Tổng lượt đăng ký không phải số sinh viên duy nhất. |
| Yêu cầu | Số yêu cầu tạo trong khoảng ngày (UTC), phân theo loại và trạng thái hiện tại; tỷ lệ xử lý = (đã duyệt + từ chối) / tổng số. Khi lọc khoa/học kỳ, chỉ tính yêu cầu liên kết với lịch của lớp tương ứng. |

Chỉ lịch **PUBLISHED** được tính vào số buổi, ca và giờ. Lịch nháp hoặc đã hủy không được tính. Tỷ lệ sử dụng phòng lấy **công suất lý thuyết = số ngày × số ca cấu hình**, gồm cả cuối tuần, chưa trừ ngày bảo trì hoặc trạng thái phòng. Khi lọc khoa/học kỳ, số ca đã dùng là phần sử dụng của phạm vi đó trên công suất phòng toàn khoảng ngày. Trạng thái phòng và sĩ số là dữ liệu hiện tại, không phải ảnh chụp lịch sử tại ngày kết thúc báo cáo.

## Xuất và in

- **Xuất PDF**: tạo tài liệu PDF hoàn chỉnh, dùng font Unicode để hiển thị đúng tiếng Việt và tự ngắt trang khi bảng dài.
- **Xuất Word**: tạo tệp `.docx` gồm số liệu tổng quan và cả bốn bảng báo cáo.
- **Xuất Excel**: tạo tệp `.xlsx` với một sheet tổng quan và một sheet riêng cho mỗi bảng báo cáo.
- Sau khi thay đổi bộ lọc, phải tạo lại báo cáo trước khi xuất. Bản xuất lấy đúng snapshot đã tải trên màn hình.
- File có sẵn chỉ được ghi đè sau khi xác nhận trong hộp thoại.

## Kiểm thử

`ReportServiceTest` kiểm tra số buổi theo ngày, loại lịch hủy, chống đếm lặp ca phòng, thời lượng ca, ngày biên, phạm vi rỗng, phân quyền và nội dung xuất an toàn. `ReportIntegrationTest` đối chiếu kết quả với truy vấn lịch ngày độc lập trên MySQL, kiểm tra quyền thực tế trong database và render màn hình.

Kiểm thử tích hợp dùng cấu hình `UNISCHEDULE_DB_URL` trỏ tới `unischedule_test`; không cần xóa hoặc seed lại database chỉ để chạy hai test báo cáo. Ảnh và PDF kiểm chứng nằm trong `target/jdbc-previews/reports.png` và `target/jdbc-previews/report.pdf`.
