# VINABACUS CẦN THƠ — Khung Java Swing MVC đơn giản (2 cơ sở)

Phiên bản được chỉnh trên **`VINABACUS_MVC_KHUNG_TRONG(1).zip`** đã cung cấp.

## Đã có

- Màu xanh lá – vàng – trắng theo biển hiệu VINABACUS Cần Thơ; logo theo mẫu giao diện đã duyệt.
- Header chỉ có logo + hộp chọn **Cơ sở**; đã bỏ dòng `VINABACUS / Trung tâm / Mầm non` giữa header.
- Sidebar mở toàn bộ (không thu gọn), cuộn dọc, tô vàng dòng được chọn, chữ sáng dễ đọc.
- **Trung tâm: 15 mục**; **Mầm non: 13 mục**; mã menu khác nhau, không xuất hiện chéo.
- Vùng nội dung để trống cho thành viên tự lập trình: **không có sẵn form, bảng, nút CRUD, dữ liệu mẫu**.
- `KhungNoiDungPanel` nạp `JPanel` qua `DangKyManHinh` khi thành viên đăng ký module.
- `Database.java` là tiện ích cấu hình JDBC độc lập; **không tự kết nối database lúc khởi động**.

## Chạy trên Windows/NetBeans 25

1. Giải nén vào ví dụ `C:\TaiLieu\VINABACUS_MVC_2_CO_SO`.
2. Mở NetBeans 25 > **File > Open Project**, chọn đúng thư mục có `pom.xml`.
3. Dùng **Clean and Build** rồi chạy lớp chính `vn.vinabacus.Main`.
4. Hoặc chạy `CHAY_TRUC_TIEP.cmd` từ thư mục dự án (máy cần Java 17 trở lên).
5. Chọn **Trung tâm / Mầm non** trong góc trên bên phải; menu sẽ tự đổi.

> File ZIP đã có `dist/VINABACUS_2_CO_SO.jar` để chạy giao diện, **không cần SQL Server** để xem khung.

## Cấu trúc MVC

```text
src/main/java/vn/vinabacus/
├── Main.java                   // Khởi động
├── config/
│   ├── MenuConfig.java         // Menu từng cơ sở
│   └── Database.java           // JDBC (tùy chọn, sau khi cài driver)
├── model/
│   ├── CoSo.java               // TRUNG_TAM, MAM_NON (loại giao diện)
│   └── MucMenu.java            // Mã, tên, nhóm, biểu tượng menu
├── controller/MenuController.java
├── service/MenuService.java
├── repository/README.md       // Thành viên viết Repository vào đây
└── view/
    ├── MainFrame.java          // Cửa sổ chung, chọn cơ sở
    ├── MenuTrai.java           // Menu dọc, cuộn
    ├── KhungNoiDungPanel.java  // Hiển thị placeholder hoặc module
    ├── DangKyManHinh.java      // Map mã menu -> Supplier<JComponent>
    ├── GiaoDienMau.java        // Bảng màu
    ├── BieuTuong.java          // Icon Java2D
    └── NutRoChu.java           // Nút menu tương phản
```

## Cách ghép một module thành viên

Thành viên viết lớp ví dụ `vn.vinabacus.view.trungtam.MonHocPanel extends JPanel`. Sau khi nhận code và ghép vào cùng project, trưởng nhóm thêm vào **`Main.main()`**, trước khi tạo `MainFrame`:

```java
import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.trungtam.MonHocPanel;

// Trong SwingUtilities.invokeLater, trước new MainFrame(...):
DangKyManHinh.dangKy("tt-mon-hoc", MonHocPanel::new);
```

Sau đó chọn Trung tâm → **Môn học** sẽ tự hiển thị panel mới. Không sửa `MainFrame` và không tạo nút công cụ chung.

Đăng ký module Mầm non tương tự: `DangKyManHinh.dangKy("mn-lop-hoc", LopMamNonPanel::new);`

**Quan trọng:** `CoSo` hiện là enum chọn loại giao diện, **không phải MaCoSo trong database**. Khi kết nối SQL phải map sang `MaCoSo` thật dựa trên dữ liệu, kiểm tra quan hệ bảng và phân quyền. Không tự giả định Trung tâm = 1, Mầm non = 2.

## SQL Server

- File `db.local.properties.example` chỉ là mẫu, không có mật khẩu.
- Khi cần truy vấn thật: sao chép mẫu thành `db.local.properties`, cấu hình URL/user/password trên máy riêng.
- Thêm thư viện **Microsoft JDBC Driver for SQL Server** vào Maven (hoặc classpath). JAR giao diện thuần hiện tại **không đóng gói driver SQL**.
- Repository gọi `Database.moKetNoi()` và dùng `PreparedStatement` với tham số; đóng Connection bằng try-with-resources.
- `db.local.properties` nằm trong `.gitignore`, không chia sẻ mật khẩu lên GitHub.
- SQL nguồn 57 bảng không được tự động nhập hay sửa. Chức năng nghiệp vụ thuộc phần việc của nhóm.

## Kiểm thử

- Biên dịch Java với `javac --release 17`.
- Chạy `vn.vinabacus.KiemThuMenu`: xác minh đúng danh sách menu và placeholder chưa gắn module.
- Chạy `vn.vinabacus.KiemThuGhepModule`: xác minh map module vào đúng vị trí.
- `XEM_TRUOC_TRUNG_TAM.png`, `XEM_TRUOC_MAM_NON.png`: ảnh chụp **chương trình Java Swing thực tế**.

## Ghi chú logo

`src/main/resources/vn/vinabacus/assets/logo_vinabacus_can_tho.png` là hình logo lấy từ **mẫu giao diện bạn đã duyệt**, dựa trên biển hiệu bạn cung cấp. Ảnh cắt trực tiếp từ biển hiệu gốc lưu tại `database/LOGO_GOC_THAM_KHAO.png` để đối chiếu. Với ứng dụng chính thức, nên thay bằng file logo gốc rõ nét của trung tâm nếu có.
