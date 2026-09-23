# Seed lịch sử 5 năm trực tiếp vào UniSchedule

Đây là bộ **dữ liệu mẫu được thêm vào database `unischedule` hiện tại**, để sử dụng ngay trong ứng dụng Swing như hệ thống đã vận hành nhiều năm. Khác với bộ mô phỏng tương lai trong `SIMULATION.md`, công cụ này thật sự nạp người dùng, lớp, lịch, yêu cầu và nhật ký vào ứng dụng.

Database của dự án đã được [mở rộng tiếp lên ít nhất 1.000 dòng cho 14 bảng nghiệp vụ](THOUSAND_ROW_SEED.md). Các con số bên dưới mô tả **bước seed lịch sử cơ bản trước khi mở rộng**, không phải số lượng hiện tại của database.

## Phạm vi và dữ liệu

Bản chạy ngày **23/09/2026**, seed **20260923**, tạo lịch sử trong cửa sổ **23/09/2021–23/09/2026**, kèm lịch đã công bố của học kỳ đang học. Ngày tạo/duyệt/đăng ký/bảo trì đều trong quá khứ hoặc ngày chốt. Lịch học hiện hành được phép kéo dài đến cuối học kỳ, nhưng những buổi sau ngày chốt không được tính là đã diễn ra.

| Nhóm dữ liệu thêm | Số lượng |
| --- | ---: |
| Người dùng | 536: 510 sinh viên, 24 giảng viên, 1 Admin, 1 đào tạo |
| Quan hệ vai trò | 536 |
| Học kỳ | 11: các kỳ lịch sử và kỳ đang hoạt động |
| Môn học | 24, chia cho 3 khoa hiện có |
| Lớp học phần / phân công | 420 / 420 |
| Đăng ký học | 9.448 |
| Phòng học | 20 |
| Thiết bị / phân bổ thiết bị | 60 / 60 |
| Lịch tuần và lịch bổ sung | 462 |
| Yêu cầu | 301, đủ 5 loại |
| Thông báo | 9.771 |
| Nhật ký | 10.997, gồm 1 dấu xác nhận hoàn tất seed |
| Bảo trì | 97 |

Không tạo lại vai trò, khoa hoặc ca học: dùng danh mục đang có. Mọi người dùng, phòng, môn và lớp bổ sung đều là dữ liệu mẫu mới. Cuối kỳ có 358 sinh viên mẫu còn hoạt động, 152 ngừng hoạt động; không xóa hồ sơ cũ hoặc đăng ký lịch sử.

| Năm | Lớp mở | Lượt đăng ký (cả hủy) | Buổi đã diễn ra, không tính lịch hủy | Yêu cầu |
| --- | ---: | ---: | ---: | ---: |
| 2021, phần cuối năm | 12 | 240 | 158 | 11 |
| 2022 | 36 | 752 | 563 | 25 |
| 2023 | 60 | 1.332 | 921 | 40 |
| 2024 | 84 | 1.968 | 1.302 | 56 |
| 2025 | 108 | 2.436 | 1.719 | 75 |
| 2026, đến 23/09 | 120 | 2.720 | 1.111 | 94 |

Cửa sổ 5 năm cắt qua 6 năm lịch, vì bắt đầu và kết thúc trong tháng 9. Kỳ đầu tiên rút ngắn ở ranh giới dữ liệu; các kỳ thông thường có 16 tuần. Không phải sáu năm đầy đủ.

## Xem ngay trong ứng dụng

Khởi động lại ứng dụng hoặc đăng xuất/đăng nhập lại để các màn hình tải dữ liệu mới:

```powershell
./scripts/run-local.ps1
```

Các tài khoản có sẵn trước đây vẫn giữ nguyên. Có thể đăng nhập `admin` hoặc `daotao` để xem dữ liệu mới trong các màn hình quản lý. Để xem lịch sử và lịch cá nhân của người dùng mẫu:

| Username | Vai trò |
| --- | --- |
| `qt_hethong` | Quản trị viên có dữ liệu lịch sử |
| `pdt_01` | Nhân viên đào tạo |
| `gv0001` | Giảng viên có lịch tuần hiện tại |
| `sv20260510` | Sinh viên đang học, có lịch tuần hiện tại |

**Mật khẩu demo: `123456`.** Các tài khoản này phục vụ đồ án cục bộ, dùng mật khẩu băm PBKDF2 cùng định dạng của ứng dụng. Không có mật khẩu hoặc thông tin cá nhân của người thật được sao chép. Tên, mã, email và nội dung trên giao diện được đặt như dữ liệu vận hành bình thường; dấu nguồn gốc mô phỏng chỉ nằm trong biên nhận và metadata nhật ký kỹ thuật.

- **Người dùng:** tìm `sv2026`, `gv0001`; xem sinh viên đang hoạt động và ngừng hoạt động.
- **Lần đăng nhập cuối:** màn hình người dùng có bộ lọc năm và dòng tổng hợp số tài khoản theo năm. `Hoạt động` nghĩa là tài khoản vẫn được phép đăng nhập, không có nghĩa vừa đăng nhập; tài khoản mới chưa từng đăng nhập hiện `Chờ đăng nhập` thay vì `Hoạt động`. `Ngừng hoạt động` được hiển thị bằng nhãn xám. Các ngày đăng nhập mô phỏng trải từ 2022–2026 và không vượt ngày tạo/ngày ngừng sử dụng.
- **Hoạt động theo vai trò:** tài khoản `admin`/`daotao` và các tài khoản Admin/Đào tạo khác thấy **Hoạt động 5 năm**. `giangvien`/`sinhvien` cùng các tài khoản Giảng viên/Sinh viên khác thấy **Hoạt động 3 năm**. Màn hình đọc trực tiếp MySQL, vẽ số buổi đã diễn ra theo từng năm và hiển thị bảng tài khoản mới, lớp mở, đăng ký/hủy, yêu cầu và bảo trì. Chỉ Admin/Đào tạo có nút **Xem báo cáo năm đã chọn** để mở báo cáo chi tiết và xuất file. Khoảng thời gian lùi đúng 5 hoặc 3 năm tính đến ngày máy đang chạy ứng dụng; hai năm ở biên chỉ là một phần năm. Số liệu ở đây là tổng hợp toàn hệ thống, còn thời khóa biểu cá nhân vẫn theo tài khoản đăng nhập.
- **Lớp học phần / lịch / phòng:** chọn các Học kỳ 1 và Học kỳ 2 từ năm học 2021 đến 2026, mã môn như `CNTT201`, mã lớp như `CNTT201-2026T-001`, phòng như `E101`.
- **Thời khóa biểu:** tài khoản giảng viên/sinh viên mẫu xem lịch riêng, hoặc chuyển sang lịch đã công bố để xem các kỳ cũ trong phạm vi ba năm; Admin/Đào tạo xem toàn bộ lịch trong phạm vi năm năm.
- **Yêu cầu:** có lịch sử duyệt/từ chối và 5 yêu cầu mới đang chờ xử lý. Màn hình mặc định lọc **Đang chờ**; có thể chọn **Tất cả** để xem lịch sử. Yêu cầu cũ không bị để chờ suốt nhiều năm.
- **Báo cáo:** có nút chọn nhanh năm, giới hạn tối đa 366 ngày/lần. Báo cáo mặc định gồm cả dữ liệu vốn có; lọc theo từng học kỳ lịch sử để xem phần được seed. Vì vậy số liệu năm 2026 trên giao diện có thể cao hơn bảng chỉ tính bộ seed ở trên.
- **Nhật ký / thông báo:** các sự kiện có ngày lịch sử; thông báo hiển thị như thông báo hoạt động thường ngày. Nhật ký tải 100 dòng/trang để không làm treo giao diện khi có hơn 11.000 sự kiện; các mốc kỹ thuật của công cụ seed không hiện trong danh sách nghiệp vụ.

Dashboard ưu tiên hiển thị tài khoản **đang hoạt động**; mục quản lý người dùng mặc định lọc trạng thái này. Màn hình lớp học phần mặc định hiển thị học kỳ hiện tại. Các bộ lọc đều đổi được để xem toàn bộ dữ liệu lịch sử.

## Thời khóa biểu theo năm và kỳ

Mở **Thời khóa biểu**, chọn **năm** rồi chọn **Học kỳ 1** hoặc **Học kỳ 2**. Năm ở đây là **năm dương lịch của ngày bắt đầu dạy**. Theo quy ước năm học đã có trong dữ liệu gốc, Học kỳ 1 bắt đầu khoảng tháng 8–9; Học kỳ 2 bắt đầu khoảng tháng 1 và thuộc **năm học trước**. Vì vậy năm dương lịch 2026 hiển thị Học kỳ 2 của năm học 2025 (tháng 1) và Học kỳ 1 của năm học 2026 (tháng 8–9). Năm 2021 chỉ có Học kỳ 1 vì dữ liệu 5 năm bắt đầu từ tháng 9.

Năm 2026 có hai bản ghi Học kỳ 1 - 2026: dữ liệu gốc bắt đầu 28/08 và dữ liệu bổ sung bắt đầu 07/09. Thời khóa biểu gộp chúng thành **một** lựa chọn cùng thời gian; các danh sách cần chọn một bản ghi cụ thể (báo cáo, lớp, biểu mẫu) kèm **ngày bắt đầu** để phân biệt. Học kỳ 2 - 2026 trong dữ liệu gốc bắt đầu tháng 01/2027, còn đang ở trạng thái kế hoạch và chưa có lịch, nên không nằm trong phạm vi lịch sử đến 2026.

| Năm dương lịch | Học kỳ 2 đầu năm: lịch đã công bố | Học kỳ 1 cuối năm: lịch đã công bố |
| --- | ---: | ---: |
| 2021, phần cuối năm | — | 14 |
| 2022 | 14 | 24 |
| 2023 | 26 | 40 |
| 2024 | 34 | 53 |
| 2025 | 51 | 63 |
| 2026 | 64 | 87 |

Các số trên là **bản ghi lịch tuần/lịch bổ sung**, không phải tổng số buổi đã diễn ra; một bản ghi lịch tuần có thể tạo nhiều buổi theo ngày hiệu lực. Học kỳ 1 cuối năm 2026 gồm **hai** bản ghi học kỳ trong cơ sở dữ liệu và 87 lịch đã công bố của cả dữ liệu gốc lẫn dữ liệu mô phỏng. Lịch đã hủy không xuất hiện trong thời khóa biểu; nhân viên Admin/Đào tạo còn có thể thấy lịch nháp.

- **Admin/Đào tạo:** xem mọi kỳ có dữ liệu trong cửa sổ 5 năm 23/09/2021–23/09/2026, tức Học kỳ 1 cuối năm 2021 và hai kỳ theo ngày bắt đầu/năm từ 2022–2026. Có thể dùng tài khoản gốc `admin`/`daotao`.
- **Giảng viên/Sinh viên:** chỉ chọn được năm **2024, 2025, 2026**, mỗi năm hai kỳ. **Lịch của tôi** chỉ gồm lớp được phân công hoặc đã đăng ký của tài khoản đó. Khi chọn kỳ cũ, màn hình tự chuyển sang **Lịch đã công bố** để tài khoản gốc cũng xem được lịch công khai của trường; có thể đổi lại lịch cá nhân. Lịch công bố chỉ đọc, không cấp quyền sửa lịch hay đăng ký học bù cho lớp của người khác.
- Chọn kỳ cũ sẽ mở **tuần đầu có hiệu lực của kỳ** thay vì giữ tuần hiện tại. Nút tuần trước/sau dừng tại ranh giới kỳ; nút **Tuần hiện tại** đưa về kỳ đang học. Dải Thứ 2–Chủ nhật cho biết số lịch mỗi ngày; chọn ngày để xem thẻ lịch rộng có giờ, môn, mã lớp, phòng và giảng viên. Bộ lọc khoa, giảng viên, phòng, tìm kiếm và dạng bảng dùng cùng tập dữ liệu.

Chạy kiểm tra đọc dữ liệu thật và xuất ảnh bốn vai trò tại `target/timetable-ui-previews`:

```powershell
./scripts/test-timetable-ui.ps1
```

Database đã seed trước thay đổi tên học kỳ có thể cập nhật bằng `./scripts/rename-history-semesters.ps1`. Lệnh này chỉ đổi trường **name** của đúng 11 học kỳ thuộc bộ seed, kiểm tra dấu nguồn gốc trước khi ghi, chạy trong một transaction và chạy lại không tạo thay đổi. Database của dự án này **đã cập nhật**. Bộ tạo seed mới sinh tên Học kỳ 1/2 ngay từ đầu. Mã kỳ kỹ thuật được giữ nguyên để không phá quan hệ với lớp, lịch và các báo cáo cũ.

Kiểm tra này xác nhận cả **11 kỳ lịch** trong phạm vi Admin/Đào tạo và **6 kỳ lịch** trong phạm vi Giảng viên/Sinh viên đều có lịch, đồng thời kiểm tra tài khoản ba năm không đọc được lịch 2022. Nó chỉ đọc MySQL; không sửa dữ liệu gốc hoặc dữ liệu đã seed. Ảnh hiển thị là ảnh kiểm tra trong thư mục `target`, không phải dữ liệu thật.

## Chạy lại hoặc kiểm tra

```powershell
# Nạp trực tiếp vào database đã cấu hình. Nếu đã nạp rồi: không thêm trùng.
./scripts/seed-history.ps1 -AsOf 2026-09-23

# Chỉ tạo dữ liệu và kiểm tra trong bộ nhớ, KHÔNG ghi database.
./scripts/seed-history.ps1 -Preview -AsOf 2026-09-23

# Đọc kiểm chứng bằng repository, lịch tuần và ReportService của ứng dụng.
./scripts/seed-history.ps1 -Verify -AsOf 2026-09-23

# Dùng database theo biến môi trường thay vì cấu hình local của máy.
./scripts/seed-history.ps1 -UseEnvironment -AsOf 2026-09-23

# Kiểm thử tự động của bộ lịch sử và bộ mô phỏng trước đó.
mvn '-Dtest=HistoricalSeedTest,FiveYearSimulationTest' test

# Kiểm tra màn hình 5 năm bằng database đã seed, xuất ảnh tại target/history-ui-previews.
./scripts/test-history-ui.ps1
```

MySQL phải đang chạy; công cụ không tự tạo lại database. `-UseEnvironment` dùng `UNISCHEDULE_DB_URL`, `UNISCHEDULE_DB_USER`, `UNISCHEDULE_DB_PASSWORD`; nếu không có cờ này, wrapper ưu tiên `.local/mysql-test/connection.json`. Mật khẩu kết nối không được ghi ra log hoặc báo cáo.

`HistoricalSeedMain` chạy trực tiếp qua Maven mặc định chỉ xem trước; phải có `--apply` mới ghi. Wrapper PowerShell ở trên mặc định ghi vì nó là lệnh seed trực tiếp. Cờ `-Preview` dành cho xem trước, `-Verify` chỉ đọc. Giữ `-AsOf` và `-Seed` giống nhau để tạo lại cùng nội dung; ID MySQL có thể khác vì lấy từ AUTO_INCREMENT của database đích.

Nếu database đã nạp bằng phiên bản đầu tiên ngày 23/09/2026, có thể chạy `./scripts/repair-history-requests.ps1` một lần để chọn phòng trống cho 3 yêu cầu đổi phòng mẫu đang chờ. Công cụ chỉ cập nhật yêu cầu có dấu nguồn gốc trong nhật ký kỹ thuật, không sửa lịch học hay yêu cầu thật; chạy lại cho kết quả 0 thay đổi. Bản seed tạo mới đã chọn phòng trống ngay từ đầu.

Để đổi dữ liệu đã nạp bằng phiên bản đầu tiên sang tên/mã hiển thị tự nhiên, chạy `./scripts/normalize-history-display.ps1`. Công cụ yêu cầu dấu seed hoàn tất, kiểm tra đúng số bản ghi, chạy trong một transaction và chỉ sửa trường hiển thị của bộ seed cũ; chạy lại trả về `ALREADY_NORMALIZED`. Database của dự án này **đã áp dụng bước đổi tên**. Bộ sinh dữ liệu hiện tại tạo tên tự nhiên ngay từ đầu nên database mới không cần bước này.

Để sửa dữ liệu đăng nhập của bản seed đầu tiên, chạy `./scripts/repair-history-logins.ps1`. Lệnh tạo các mốc đăng nhập mô phỏng có nhật ký `LOGIN` tương ứng, giữ nguyên các lần đăng nhập thật và các giá trị không trống của tài khoản gốc; riêng 24 tài khoản demo gốc chưa từng có mốc đăng nhập được điền ngày sau khi tạo tài khoản. Công cụ chạy trong một transaction và chạy lại trả về `ALREADY_REPAIRED`. Database hiện tại **đã áp dụng bước này**. Bộ sinh dữ liệu mới tự tạo lịch sử đăng nhập theo vòng đời nên không cần sửa trên database mới.

Biên nhận bản nạp nằm tại [simulation/history/2026-09-23-seed20260923](../simulation/history/2026-09-23-seed20260923/):

- `plan.json`: ngày chốt, seed, số lượng dự kiến và thống kê từng năm.
- `receipt.json`: trạng thái commit, số lượng đã thêm và SHA-256 của toàn bộ bản ghi có trước khi nạp, theo từng bảng.

## Bảo toàn dữ liệu và chống chèn trùng

1. Sinh và kiểm tra dữ liệu trong bộ nhớ trước khi mở transaction ghi.
2. Dùng biên nhận và trường `details.seed=HISTORY_5Y` của nhật ký kỹ thuật để nhận biết nguồn gốc dữ liệu; mã và tên trên giao diện không mang nhãn mô phỏng.
3. Dùng khóa AUTO_INCREMENT do MySQL cấp, ánh xạ lại toàn bộ FK và tham chiếu thông báo/nhật ký. Không giả định ID đích bắt đầu từ 1.
4. Chỉ dùng `INSERT`, không `UPDATE`, `DELETE`, `TRUNCATE`, `DROP`, `REPLACE` hoặc reset AUTO_INCREMENT trên dữ liệu hiện có.
5. Giữ khóa đồng bộ lịch của ứng dụng và khóa riêng cho seed. Toàn bộ INSERT cùng một transaction; lỗi thì rollback.
6. Tính lại sĩ số từ đăng ký. So sánh SHA-256 của **mọi bản ghi đã tồn tại ở cả 18 bảng** trước khi commit; nếu phát hiện thay đổi thì rollback.
7. Ghi dấu `SEED_HISTORY_5Y_V1` vào `audit_logs` trong cùng transaction. Chạy lại thấy dấu này sẽ dừng với `ALREADY_APPLIED`, không nạp trùng kể cả đổi seed/ngày. Nếu tài khoản/cơ sở vật chất của bộ seed cũ tồn tại mà không có dấu hoàn tất, từ chối nạp để người dùng kiểm tra.

Không có chức năng xóa dữ liệu cũ hay tự động dọn bộ seed. Sau khi bắt đầu sử dụng/chỉnh sửa dữ liệu mẫu trong ứng dụng, dấu hoàn tất vẫn giữ nguyên và lệnh seed không ghi đè những sửa đổi đó.

## Giả định nghiệp vụ

- Tuyển 60 sinh viên ban đầu, tăng 10 mỗi đợt nhập học năm sau, chia đều 3 khoa. Năm 2026 tuyển 110; tổng cộng 510 hồ sơ sinh viên.
- Mỗi tháng 7, khóa đủ 4 năm tốt nghiệp/ngừng hoạt động; sinh viên chưa đủ 4 năm có xác suất ngừng học 3,5%. Lịch sử đã học vẫn được giữ. Thời điểm rời đi được dùng để kiểm tra lịch, không lấy trạng thái hiện tại để xóa quá khứ.
- Mỗi tài khoản được gán mốc đăng nhập cuối bằng hàm xác định từ username và ngày chốt; ngày nằm sau ngày tạo và trước ngày ngừng sử dụng nếu đã rời đi. Tài khoản còn hoạt động có thể đăng nhập gần đây hoặc lâu chưa sử dụng, vì trạng thái `ACTIVE` là quyền truy cập. Những lần đăng nhập thật sau seed luôn được giữ nguyên; sự kiện mô phỏng được đánh dấu trong metadata nhật ký.
- Mỗi khoa có 8 môn mẫu, mỗi sinh viên đăng ký 4 môn/học kỳ, chia lớp tối đa 25 người. Đây là phần chương trình chọn để minh họa quản lý lịch, không phải chương trình đào tạo đầy đủ có môn tiên quyết hoặc điểm số.
- Hủy 4,5% lượt đăng ký; khoảng 2,5% lớp hủy trước kỳ học và toàn bộ đăng ký lớp đó chuyển `CANCELLED`.
- Xếp một buổi/tuần gồm 2 ca; đúng loại phòng, tránh trùng phòng, giảng viên, lớp và sinh viên. Đổi phòng chỉ đổi phòng; đổi giờ áp dụng trước kỳ. Mượn phòng được duyệt thêm lịch một ngày thực sự.
- Khoảng 75% lịch công bố có yêu cầu; dự kiến 80% được duyệt, còn lại từ chối. Không đủ tài nguyên thì từ chối thay vì tạo xung đột. Có 5 yêu cầu mới `PENDING` sát ngày chốt để thao tác thử.
- Bảo trì vào kỳ nghỉ hoặc trước ngày bắt đầu dạy; thiết bị được sửa xong. Có lịch sử 97 đợt, không gắn nhãn bảo trì hiện tại cho phòng đang có lịch.
- Tên/mã của mọi dữ liệu bổ sung là mẫu. Không seed tài chính vì schema không có nghiệp vụ thanh toán.

## Kiểm chứng

Đã thử nạp trước trên database mô phỏng riêng, sau đó **commit vào database `unischedule` chính của dự án**. Kết quả sau nạp: **564 người dùng, 430 lớp học phần, 9.516 đăng ký, 485 lịch, 309 yêu cầu, 9.783 thông báo, 11.028 nhật ký**. Số lượng tổng gồm cả dữ liệu cũ.

Đã đọc bằng `JdbcUserRepository`, `JdbcCourseSectionRepository`, `JdbcScheduleRepository`, `JdbcRequestRepository`, `JdbcRoomRepository` và `ReportService`. Kiểm tra mật khẩu, lịch cá nhân trong tuần hiện tại và xung đột bằng `ConflictService` của ứng dụng: **0 xung đột liên quan bộ seed**, giảng viên `gv0001` có 3 lịch và sinh viên `sv20260510` có 5 lịch trong tuần hiện tại.

Đã chạy lại lệnh seed trên database chính: trả về `ALREADY_APPLIED`; đối chiếu số lượng người dùng/lịch/đăng ký/thông báo/nhật ký trước và sau xác nhận **không thêm trùng**. Hash mọi bản ghi cũ ở 18 bảng khớp trước khi commit.

`mvn test` đạt **31 kiểm thử chạy, 0 lỗi; 13 kiểm thử tích hợp tùy chọn bỏ qua**. Các kiểm thử cũ dùng database `unischedule_test` chỉ chạy khi bật cấu hình đó. Kiểm chứng đọc dữ liệu thật sau seed được chạy riêng qua lệnh `-Verify`, không cần xóa/tạo lại database test.

`./scripts/test-history-ui.ps1` chạy thành công với MySQL đã seed, kiểm tra phạm vi 5/3 năm của cả bốn tài khoản gốc bằng kết nối chỉ đọc, phép tổng hợp 5 năm, báo cáo năm 2026 và ảnh render [màn hình hoạt động](../target/history-ui-previews/history-five-years.png) cùng [danh sách người dùng với tên tự nhiên](../target/history-ui-previews/users-natural-history.png). Tại ngày 23/09/2026, màn hình 5 năm đọc được **5.851 buổi đã diễn ra, 412 tài khoản đang hoạt động** trên toàn database; năm 2026 có **1.188 buổi**, gồm dữ liệu mẫu và dữ liệu vốn có. Ba đề xuất đổi phòng đang chờ đã được kiểm tra, đều có **0 lịch công bố trùng phòng**; chạy lại công cụ sửa cho kết quả **0 thay đổi**.

Sau khi sửa đăng nhập, [màn hình mặc định](../target/history-ui-previews/users-all-years.png) hiện tổng hợp năm và [bộ lọc năm 2024](../target/history-ui-previews/users-login-2024.png) hiển thị **39 tài khoản**. Phân bố năm của lần đăng nhập cuối trên toàn database: **2022: 2, 2023: 11, 2024: 39, 2025: 150, 2026: 362**. Không còn tài khoản `ACTIVE` có ngày đăng nhập trống; không có ngày đăng nhập trước ngày tạo hoặc sau ngày ngừng sử dụng. Các sự kiện `LOGIN` mô phỏng được đối chiếu khớp với ngày đăng nhập cuối, và chạy lại lệnh sửa không thêm trùng.

`SimulationValidator` còn kiểm tra PK/FK, vai trò, sĩ số, sinh viên còn học tại ngày xếp lịch, trùng sinh viên, sức chứa, loại phòng, lịch gặp bảo trì, thiết bị mượn, trạng thái yêu cầu và các tham chiếu thông báo/nhật ký. Kiểm thử bổ sung kiểm tra tính tái tạo, seed khác vẫn hợp lệ, dữ liệu đủ lớn, trạng thái tương thích UI và không sinh sự kiện sau ngày chốt.
