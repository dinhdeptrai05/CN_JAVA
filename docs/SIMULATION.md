# Bài tập mô phỏng UniSchedule trong 5 năm: 2027–2031

## 1. Kết quả và cách mở

Mở [báo cáo HTML đã chạy](../simulation/results/2027-2031-seed42-verified/report.html) bằng Chrome/Edge. Báo cáo có bảng từng năm, biểu đồ người dùng, bảng 60 tháng, tổng kết và giải thích công thức. Không cần mạng hoặc MySQL để xem; có thể in bằng chức năng Print của trình duyệt.

**Đây là kịch bản giả định phục vụ bài tập, không phải dữ liệu thật hoặc dự báo được hiệu chỉnh từ lịch sử.** Toàn bộ dữ liệu mô phỏng nằm riêng trong `simulation/`. Ứng dụng Swing vẫn sử dụng database hiện tại như trước.

| Năm | Đầu năm | Mới | Rời đi | Cuối năm | Lớp HP | Buổi học hợp lệ | Yêu cầu |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 2027 | 28 | 7 | 3 | 32 | 16 | 240 | 7 |
| 2028 | 32 | 8 | 4 | 36 | 24 | 369 | 16 |
| 2029 | 36 | 10 | 4 | 42 | 32 | 450 | 19 |
| 2030 | 42 | 12 | 5 | 49 | 32 | 497 | 23 |
| 2031 | 49 | 14 | 6 | 57 | 32 | 498 | 19 |

Tổng 5 năm, seed `42`:

- Người dùng hoạt động: **28 + 51 − 22 = 57**, gồm 49 sinh viên, 6 giảng viên, 1 Admin và 1 nhân viên đào tạo. Có **79 hồ sơ** được giữ lại, trong đó 22 `INACTIVE`; không nhầm số hồ sơ với số đang hoạt động.
- 136 lớp học phần; 2.584 lượt đăng ký, trong đó 228 lượt `CANCELLED`, còn 2.356 lượt `ACTIVE` theo hồ sơ từng học kỳ. Một sinh viên đăng ký nhiều môn nên lượt đăng ký không phải số người duy nhất.
- 142 bản ghi lịch gồm 136 lịch tuần và 6 lịch mượn phòng một ngày. 8 lịch tuần bị hủy; chỉ 134 lịch `PUBLISHED` sinh ra **2.054 buổi học**.
- 4.108 ca phòng được sử dụng, **438.250 phút dạy** (7.304 giờ 10 phút), 102.360 ca phòng khả dụng. Công suất toàn kỳ = **4,01%**.
- 84 yêu cầu: **57 duyệt + 18 từ chối + 9 chờ = 84**; 36 đợt bảo trì, 407 thông báo, 357 bản ghi nhật ký.

Số lớp tăng theo ngưỡng nhóm 25 sinh viên, nên không tăng đều theo phần trăm. Số buổi 2030–2031 gần như ngang nhau dù số sinh viên tăng, vì vẫn chỉ cần hai nhóm/môn; khác biệt đến từ hủy lịch và buổi bổ sung. Công suất thấp phù hợp với quy mô dữ liệu demo, không chứng minh toàn trường đang sử dụng phòng kém hiệu quả.

## 2. Ứng dụng hiện tại làm gì?

UniSchedule là ứng dụng desktop **Java 17, Swing, JDBC, MySQL**, tổ chức MVC. View gọi Controller, Controller sử dụng Service/Repository. Các nhóm chức năng đã khảo sát:

| Nhóm | Chức năng | Bảng chính |
| --- | --- | --- |
| Danh tính | Đăng nhập, phân quyền ADMIN/ACADEMIC/LECTURER/STUDENT, hồ sơ | `roles`, `users`, `user_roles`, `departments` |
| Đào tạo | Học kỳ, môn, lớp học phần, phân công, đăng ký/hủy học | `semesters`, `time_slots`, `courses`, `course_sections`, `lecturer_assignments`, `student_enrollments` |
| Tài nguyên | Phòng, sức chứa, loại phòng, thiết bị và tình trạng | `classrooms`, `equipment`, `classroom_equipment` |
| Lịch học | Lịch tuần, lịch bổ sung, tìm phòng trống, phát hiện trùng | `schedules` |
| Công việc | Đổi phòng/giờ, mượn phòng/thiết bị, báo hỏng, duyệt theo vai trò | `change_requests`, `maintenance_records` |
| Theo dõi | Thông báo, nhật ký và báo cáo thống kê | `notifications`, `audit_logs` |

Tổng **18 bảng**. Lịch tham chiếu lớp, phân công đúng lớp, phòng và ca; đăng ký tham chiếu sinh viên/lớp; thiết bị phân bổ theo cặp phòng/thiết bị. Bộ mô phỏng giữ tên cột, khóa và trạng thái của schema SQL; không thay đổi các repository hoặc đường ghi của ứng dụng.

Ứng dụng không có hóa đơn, học phí hoặc giao dịch thanh toán. Vì vậy **doanh thu không áp dụng**, không giả định thu tiền theo đăng ký.

## 3. Mốc dữ liệu đã kiểm tra

Ngày 22/09/2026, đã truy vấn chỉ đọc database `unischedule` hiện có và lưu [baseline](../simulation/baseline-2026-09-22.properties).

| Dữ liệu | Số lượng |
| --- | ---: |
| Người dùng/quan hệ vai trò | 28 / 28 |
| Admin / Đào tạo / Giảng viên / Sinh viên hoạt động | 1 / 1 / 6 / 20 |
| Vai trò / Khoa / Học kỳ / Ca học | 4 / 3 / 2 / 6 |
| Môn / Lớp HP / Phân công / Đăng ký | 8 / 10 / 10 / 68 |
| Phòng / Loại thiết bị / Phân bổ thiết bị | 12 / 15 / 15 |
| Lịch / Yêu cầu / Thông báo / Nhật ký / Bảo trì | 23 / 8 / 12 / 30 / 2 |

Mốc hiện tại khác seed ở lịch và bảo trì. Không suy diễn rằng mọi dữ liệu hiện có là dữ liệu sản xuất thực tế: đây là dữ liệu của database dự án tại thời điểm khảo sát.

Baseline chỉ chứa **số lượng tổng hợp và danh mục**, không chứa danh tính, email, mật khẩu băm, ảnh hay nội dung yêu cầu của người dùng hiện tại. 28 người ban đầu của mô phỏng là **28 hồ sơ tổng hợp mới**, không ánh xạ ngược tới 28 người thật. Danh mục phòng/môn/thiết bị giữ cấu trúc hiện tại. Dữ liệu giao dịch cũ không được chép sang tương lai; mỗi học kỳ mô phỏng tạo lớp/đăng ký/lịch mới.

Không giả định tăng trưởng từ 22/09 đến 31/12/2026; lấy lượng người dùng hoạt động đã quan sát làm đầu năm 2027. Đây là giả định cầu nối, vì chưa có dữ liệu cuối năm.

## 4. Chạy lại

Yêu cầu: Java 17+, Maven và các dependency của dự án. Mô phỏng thông thường **không cần MySQL, tài khoản database hoặc kết nối mạng khi dependency đã có trong cache**.

Từ thư mục gốc dự án:

```powershell
# Mỗi lần tự tạo một thư mục đầu ra có thời gian riêng.
./scripts/run-simulation.ps1

# Chọn seed/thư mục mới để so sánh kịch bản.
./scripts/run-simulation.ps1 -Seed 43 -Output simulation/results/seed43

# Tái tạo chính xác nội dung seed 42 vào một thư mục CHƯA tồn tại.
./scripts/run-simulation.ps1 -Seed 42 -StartYear 2027 -Output simulation/results/replay42

# Chỉ chạy 4 kiểm thử mô phỏng.
mvn '-Dtest=FiveYearSimulationTest' test

# Bộ kiểm thử toàn dự án, không đặt biến môi trường database test.
mvn test
```

Trên hệ điều hành khác, hoặc không dùng PowerShell:

```text
mvn compile exec:java -Dexec.mainClass=vn.edu.donga.unischedule.simulation.SimulationMain "-Dexec.args=--baseline simulation/baseline-2026-09-22.properties --start-year 2027 --seed 42 --out simulation/results/replay42"
```

Thư mục đích tồn tại thì **từ chối**, kể cả khi rỗng. Hãy đặt tên mới; không có cờ ghi đè/xóa. Wrapper đặt thời gian trong tên thư mục, nhưng **nội dung file không phụ thuộc thời điểm chạy**. Cùng baseline, seed, mã nguồn và schema cho cùng kết quả; `checksums.json` hỗ trợ so sánh SHA-256 từng file. Khi đã thay seed hoặc baseline thì kết quả khác là bình thường.

Nếu cần cập nhật mốc dữ liệu từ MySQL:

```powershell
# MySQL phải đang chạy. File đích phải chưa tồn tại.
./scripts/run-simulation.ps1 -CaptureBaseline simulation/baseline-moi.properties
./scripts/run-simulation.ps1 -Baseline simulation/baseline-moi.properties -Output simulation/results/new-baseline
```

Wrapper dùng cấu hình riêng `.local/mysql-test/connection.json` nếu có; nếu không, dùng cấu hình database thông thường của dự án. Mật khẩu không in ra hoặc ghi vào baseline. Thao tác capture dùng transaction chỉ đọc, isolation `REPEATABLE_READ`, rồi rollback. Nó không khởi tạo database hoặc chạy seed. Bản mặc định không tự capture để không làm mất khả năng tái tạo.

## 5. Các giả định

Các tỷ lệ là giả định sư phạm vì không có chuỗi quan sát lịch sử. Chúng được tập trung trong `FiveYearSimulation`, sử dụng `java.util.Random(seed)`; chọn người rời đi, hủy và yêu cầu đều dùng cùng bộ sinh số xác định.

| Tham số | Giả định và thời điểm |
| --- | --- |
| Phạm vi | Đúng 5 năm lịch, mặc định 2027–2031; dùng ngày/giờ mô phỏng xác định |
| Sinh viên mới | `max(4, round(0,35 × SV đầu năm))`; nhập ngày 20/08 |
| Sinh viên rời đi | `max(1, round(0,15 × SV đầu năm))`; chọn không lặp từ SV hiện hữu, ngày 01/07; đại diện tốt nghiệp/ngừng học/ngừng sử dụng |
| Giảng viên | Bổ sung trước học kỳ 2 khi thấp hơn `max(6, ceil(SV hiện tại/10))`; không mô phỏng giảng viên nghỉ việc |
| Nhân viên | Giữ nguyên Admin/Phòng đào tạo |
| Lịch học kỳ | HK1 bắt đầu thứ Hai từ 11–17/01; HK2 thứ Hai từ 04–10/09; mỗi kỳ 16 tuần |
| Đăng ký | Mỗi SV đăng ký đủ 8 môn danh mục (22 tín chỉ/kỳ); đơn giản hóa chương trình học chung, chưa mô phỏng tiên quyết hoặc học lại |
| Chia lớp | Nhóm tối đa 25 SV/môn/học kỳ, phù hợp phòng nhỏ nhất đang dùng; một GV/lớp |
| Hủy đăng ký | Xác suất 6%, có hiệu lực trước kỳ học; lưu `CANCELLED` |
| Hủy lớp/lịch | Xác suất 4% trên lớp đã bố trí được phòng/ca; hủy luôn các đăng ký, sĩ số về 0; lớp không còn SV cũng hủy |
| Lịch tuần | Một buổi gồm hai ca liên tiếp/tuần; thứ Hai–thứ Bảy; xét sức chứa, đúng loại phòng và tránh trùng người/phòng/lớp |
| Yêu cầu | 70% lớp có lịch công bố tạo một yêu cầu trước đầu kỳ; 5 loại phân bổ luân phiên theo môn |
| Trạng thái yêu cầu | Rút thăm 65% duyệt, 23% từ chối, 12% chờ; thiếu tài nguyên/không bố trí được thì chuyển từ dự kiến duyệt sang từ chối |
| Hiệu lực duyệt | Đổi phòng/giờ được áp dụng trước học kỳ; USE_ROOM thêm đúng một ngày học; mượn thiết bị không vượt tồn theo phòng/ngày/ca; báo hỏng duyệt tạo đợt sửa ngoài giờ học |
| Bảo trì | 30% phòng hoạt động có bảo trì 10–16/07; sửa tồn đầu kỳ 01–03/01/2027; sửa theo báo hỏng 15/05 hoặc 31/12; phòng INACTIVE không mở lại |
| Thông báo | Thông báo học kỳ cho SV và kết quả yêu cầu cho người gửi; 80% có xác nhận đọc ngày tiếp theo |
| Nhật ký | Ghi hồ sơ mới/rời đi, công bố/hủy lịch, trạng thái yêu cầu và hoàn tất bảo trì, liên kết entity thực sự tồn tại |
| Cơ sở vật chất | Giữ 12 phòng, trong đó 1 INACTIVE; không mua thêm phòng/thiết bị trong kịch bản này |

Ca 1–5 dài 100 phút; ca 6 dài 150 phút. Hai ca cuối ngày vì vậy dài 250 phút, không được mặc định mọi buổi đều 200 phút.

`PENDING` là nhánh tồn đọng chưa được giải quyết trong kịch bản; không tự động duyệt hoặc giả định SLA. Các yêu cầu chờ cũ được giữ lại, cuối giai đoạn còn 9 yêu cầu. Bảng theo năm phân loại **yêu cầu tạo trong năm**, không phải số tồn đọng đầu/cuối năm. Các yêu cầu bị từ chối/chờ không thay đổi lịch. Đây là mô hình quyết định đơn giản, chưa mô phỏng hàng đợi xử lý hằng ngày hoặc thời gian đáp ứng.

Các học kỳ đã qua vẫn giữ đăng ký `ACTIVE` nếu sinh viên đã học, kể cả khi người đó rời hệ thống ở năm sau. Người `INACTIVE` không được đăng ký hoặc xếp lịch sau ngày rời đi. Trạng thái năm cuối của hồ sơ không được dùng để viết lại lịch sử.

## 6. Công thức và kiểm chứng

- `Người dùng cuối năm = đầu năm + mới − rời đi`; đầu năm sau bằng cuối năm trước. Đối chiếu lại bằng `user_events` và trạng thái hồ sơ.
- `Sĩ số lớp = COUNT(student_enrollments WHERE status='ACTIVE')`; không vượt capacity lớp/phòng. Cặp lớp–SV không lặp.
- `Buổi học = tổng ngày thực xảy ra của lịch PUBLISHED`. Với lịch tuần, bắt đầu từ đúng thứ trong khoảng ngày, tăng mỗi 7 ngày. Lịch hủy/nháp không đóng góp.
- `Phút dạy = Σ số buổi × Σ thời lượng các ca`; không bao gồm khoảng nghỉ giữa ca.
- `Ca phòng dùng = số ô (phòng, ngày, ca) có học`; các ô không trùng. `Ca khả dụng` tính từng ngày thứ Hai–thứ Bảy, mỗi phòng hoạt động 6 ca, trừ ngày bảo trì theo hợp các khoảng để tránh trừ hai lần.
- `Công suất = ca dùng / ca khả dụng × 100`. Công suất toàn 5 năm tính từ tổng tử/mẫu, **không lấy trung bình đơn giản 5 tỷ lệ**. Mẫu số này khác báo cáo Swing hiện tại vốn gồm cả Chủ nhật và chưa trừ bảo trì.
- `Yêu cầu tạo trong năm = duyệt + từ chối + chờ`; `đăng ký hợp lệ = tổng đăng ký − đăng ký hủy`. Các nhóm trạng thái loại trừ nhau.
- Tổng từng năm bằng tổng 12 tháng; tổng 5 năm chỉ cộng các lượng phát sinh, không cộng số dư người dùng.

`SimulationValidator` chạy **trước khi xuất file**, kiểm tra PK/FK, vai trò, đăng ký duy nhất, phân công đúng lớp, ngày nằm trong học kỳ, sức chứa/loại phòng, trùng phòng/GV/lớp/SV, lịch gặp bảo trì, hồ sơ hoạt động tại ngày phát sinh, mượn thiết bị, yêu cầu duyệt đã áp dụng, thông báo/nhật ký tham chiếu hợp lệ và đối chiếu tháng/năm.

Kiểm thử tự động còn kiểm tra cùng seed cho cùng dữ liệu, seed khác làm thay đổi hoạt động, 5 seed gồm năm nhuận 2028, phát hiện dữ liệu cố ý làm sai và không ghi đè file đầu ra. Kết quả thực hiện: **29 bài đạt, 0 lỗi, 11 bài tích hợp cũ bỏ qua** (chúng chỉ bật khi cấu hình database `unischedule_test`). Không chạy script tạo lại database test cũ.

Đã nhập thử bản xuất cuối cùng vào database **mới, riêng biệt** `unischedule_sim_validation_20260922_02` (bản chạy thử trước nằm ở hậu tố `_01`). Schema MySQL chấp nhận toàn bộ 18 bảng, khóa và CHECK. [Truy vấn kiểm chứng chỉ đọc](../simulation/verify.sql) cho **0 cặp lịch xung đột**, không có sai lệch sĩ số/phân công và tái tính độc lập:

| Năm | Buổi học | Ca phòng dùng | Phút dạy |
| --- | ---: | ---: | ---: |
| 2027 | 240 | 480 | 51.200 |
| 2028 | 369 | 738 | 78.650 |
| 2029 | 450 | 900 | 95.650 |
| 2030 | 497 | 994 | 106.650 |
| 2031 | 498 | 996 | 106.100 |

Các database mô phỏng này giữ lại để kiểm tra, không thay đổi cấu hình của ứng dụng. Bản xuất `verified` đã được nhập và kiểm chứng trực tiếp; đã đối chiếu hash cả 7 file kết quả. Database gốc vẫn có 28 người dùng; mã chạy mô phỏng không có đường ghi vào MySQL.

Nội dung và số liệu HTML đã được đối chiếu; chưa kiểm tra ảnh render vì công cụ trình duyệt trong phiên làm việc chặn mở URL file cục bộ. Người dùng có thể mở trực tiếp file bằng trình duyệt máy mình.

## 7. Dữ liệu đầu ra và giới hạn

| File | Công dụng |
| --- | --- |
| `report.html` | Báo cáo tiếng Việt, biểu đồ, bảng năm/tháng và giả định |
| `summary.json` | `annual`, `monthly`, `total`, metadata; dễ lấy số liệu làm bài |
| `data.json` | Đủ 18 bảng theo schema + `user_events` phục vụ diễn tiến hoạt động; tất cả gắn nhãn ở metadata |
| `data.sql` | INSERT dữ liệu tổng hợp, giữ khóa liên quan; tài khoản `sim_*`, email `example.invalid`, mật khẩu cố ý không đăng nhập được |
| `schema.sql` | DDL gốc 01–05 của dự án; không đổi định nghĩa bảng |
| `baseline.properties` | Bản sao mốc đầu vào, giúp chạy lại độc lập |
| `manifest.json` | Nhãn SIMULATION_ONLY, seed, phiên bản, ngày và hash baseline/schema |
| `checksums.json` | Hash SHA-256 từng file kết quả |

SQL là snapshot cuối giai đoạn, không phải bản ghi theo thời gian của mọi thay đổi; lịch sử người dùng và bảng tổng hợp từng năm nằm trong JSON. Không tự nạp SQL vào database ứng dụng. Nếu cần kiểm tra SQL trên máy khác, tự tạo một database trống có tên `unischedule_sim_...`, chọn database đó, nạp `schema.sql`, rồi `data.sql`, rồi chạy `simulation/verify.sql`. Không dùng `--force` để bỏ qua lỗi schema. Không chạy lại seed của ứng dụng.

Mô phỏng là module CLI riêng trong package `simulation`, không can thiệp quyền truy cập, đăng nhập, repository hay màn hình Swing. Tài khoản mô phỏng không dùng để đăng nhập ứng dụng; báo cáo HTML là giao diện trình bày kết quả. Bản chụp baseline mới phải có đủ bốn vai trò hoạt động, mỗi người đúng một vai trò và 6 ca; cấu hình ngoài giả định hoặc hết chỗ xếp lịch sẽ báo lỗi thay vì tạo số liệu sai.

## 8. Gợi ý thuyết trình

1. Giới thiệu UniSchedule và quan hệ lớp–sinh viên–giảng viên–phòng; giải thích vì sao không tính doanh thu.
2. Trình bày mốc khảo sát 28 người dùng và 12 phòng; phân biệt dữ liệu đang có với 28 hồ sơ tổng hợp ban đầu.
3. Giải thích tuyển mới 35%, rời đi 15%, chia nhóm 25 người, hủy đăng ký/lịch và hai cao điểm học kỳ; nhấn mạnh đây là giả định để thử hoạt động.
4. Mở báo cáo, đọc bảng 2027–2031 và phương trình **28 + 51 − 22 = 57**. So sánh 142 bản ghi lịch với 2.054 buổi thực xảy ra để giải thích lịch lặp.
5. Chỉ ra tổng phút dạy dùng thời lượng thật của ca, công suất 4,01% phù hợp quy mô demo và 9 yêu cầu chờ là tồn đọng cần chú ý.
6. Chạy lại seed 42 vào thư mục mới để minh họa tính tái tạo; trình bày các kiểm tra và kết quả MySQL 0 xung đột. Nêu giới hạn: chưa hiệu chỉnh tỷ lệ từ lịch sử, chưa có chương trình học nhiều khóa và chưa mô phỏng hàng đợi/SLA.
