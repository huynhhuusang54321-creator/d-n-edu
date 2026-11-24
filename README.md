# VINABACUS — Bản tích hợp 6 TV

Mở thư mục `Vinabacus` chứa `pom.xml` bằng NetBeans (File > Open Project), JDK 17+. Kiểm tra cấu hình SQL Server trong `src/main/java/Data/DataConnection.java`, Clean and Build, Run Project (`Main.Main`). Dùng cơ sở dữ liệu 12 bảng hiện có; không chạy lại script tạo database để tránh mất dữ liệu. MainPage đọc danh sách cơ sở hoạt động qua CoSoService. Không cần sửa Key.txt.

## Chức năng

| Phụ trách | Chức năng đã xây dựng |
|---|---|
| Admin | Trang chủ tổng hợp số học viên, hồ sơ chờ, lớp, buổi hôm nay; giữ MainPage và Badge hiện hành. Workspace là các component Swing dùng chung; PageTasks là tiện ích điều phối nền. |
| TV1 | Hệ thống giữ nguyên bản 12 bảng; Nhân sự là giao diện mẫu chưa kết nối chức năng quản trị tài khoản. |
| TV2 | Học viên: lọc trạng thái, hồ sơ lỗi, xếp lớp, menu chuột phải, sao chép thông tin, tab hồ sơ, tiến độ theo TinhBuoi, sơ đồ buổi học. Tuyển sinh: tiếp nhận, học thử bằng lịch thực tế hoặc dự kiến 6 tuần, chuyển chính thức, xếp lớp. |
| TV3 | Khóa học: thêm/sửa/ngừng kích hoạt, số buổi, học phí, phúc lợi, mô tả trong DanhMuc.CauHinh. Lớp: thêm/sửa/xóa nếu SQL cho phép, chọn giáo viên, sĩ số, xem danh sách và mở hồ sơ học viên. |
| TV4 | TKB: lưới tuần thứ 2–7, kéo thả lớp, thêm lớp, xác nhận lịch lặp, tạo/kích hoạt/hủy buổi, ngừng lịch. Điểm danh: chọn lớp/buổi, sửa các ô chọn, cờ tính buổi, ghi chú, xác nhận và xem lịch sử. |
| TV5 | Chi thu: danh sách giao dịch thật, lọc tháng/quý/năm, tổng dòng tiền đã xác nhận, badge giao dịch nháp. Kho: nhật ký nhập/xuất/kiểm kê chỉ đọc. |
| TV6 | Chương trình: thêm/sửa, mô tả, thời gian và lọc đang kích hoạt. Báo cáo: thống kê số lượng theo trạng thái học viên, lớp, điểm danh. |

## Kiến trúc và phạm vi

Mỗi TV quản lý trực tiếp `TvX/View` và `TvX/Controller`. View chỉ dựng và hiển thị component, phát sự kiện. Controller quyết định dữ liệu, điều hướng, gọi Service, kiểm tra và ghi. View con không chứa SQL hay gọi Service. Khi mở chức năng của TV khác, gọi entry point công khai của Controller chịu trách nhiệm; không nhân bản giao diện/nghiệp vụ. Tầng Model, Repository, Service và Data giữ nguyên bản mới nhất Cache First. MainPageController giữ bản cập nhật TV2, Key.txt giữ 13 tuyến và Badge.

## WARNING — giới hạn trước khi đưa vào vận hành

- Chưa có phiên đăng nhập/phân quyền thực tế. Các thao tác cần MaNhanSu yêu cầu chọn nhân sự đang hoạt động trong cơ sở từ DB ở chế độ phát triển. Đây không phải xác thực danh tính; không dùng bản này cho dữ liệu sản xuất hoặc cho nhân viên không được phép. Nhân sự chỉ là View mẫu.
- API Service hiện tại chỉ ghi từng bản ghi, chưa có transaction nhiều bảng cho tiếp nhận, xếp học thử, chuyển trạng thái, xếp lớp và xác nhận điểm danh hàng loạt. Các thao tác kiểm tra kết quả, báo số dòng đã ghi và lỗi; có thể ghi một phần nếu thao tác sau thất bại. Admin cần bổ sung transaction và phân quyền ở Service trước vận hành thực tế. Không tự tạo dữ liệu giả hoặc tự gán mã nhân sự.
- Tình trạng `CHO_XAC_NHAN` không có trong schema; tab giữ nguyên nhưng không gán dữ liệu. Sơ đồ toàn khóa chỉ mở khi đã có lớp và tổng số buổi; khóa bán trú chưa có tổng buổi cần quy tắc riêng. Dự kiến ngày kết thúc dựa trên lịch lặp hiện có; khi thiếu lịch, không đoán ngày. Chưa tự động phát sinh công nợ/học phí khi đăng ký hoặc đổi lịch; chưa có nghiệp vụ chuyển lớp có transaction. Badge Chi thu/Tuyển sinh cập nhật khi trang tải, không phải thông báo realtime đa máy.
- Trang Hệ thống giữ nguyên theo yêu cầu, nút tải lại của trang này có thể vẫn đọc RAM. Các trang mới dùng reload khi yêu cầu tải lại SQL. Cache chỉ đồng bộ trong một JVM, không tự phát hiện chỉnh sửa từ máy khác.
- Đã biên dịch toàn bộ source bằng javac release 17 và smoke test khởi tạo 18 View Swing, form, điểm danh, biểu đồ và JSON. Chưa có kết nối SQL Server thực tế ở môi trường kiểm tra; chưa kiểm thử end-to-end CRUD với database của bạn. Kiểm tra bằng DB thử nghiệm trước khi đưa vào sử dụng.
- DataConnection được giữ nguyên cấu hình từ dự án gốc. Kiểm tra và bảo vệ thông tin kết nối trước khi chia sẻ dự án.
