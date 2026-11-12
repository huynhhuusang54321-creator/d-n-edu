# VINABACUS – Java Swing MVC

Project Maven, Java 17 trở lên. Toàn bộ mã nguồn được tổ chức theo 7 package chính:
`view`, `controller`, `model`, `service`, `data`, `config`, `repository`.

## Cây package

```text
src/main/java/vn/vinabacus/
├── UngDungVINABACUS.java           # Điểm khởi động
├── view/                         # Giao diện Swing, bảng, biểu mẫu, hộp thoại
│   ├── GiaoDienChinh.java
│   ├── GiaoDienTrangChu.java
│   ├── GiaoDienDanhSach.java
│   ├── common/                   # Màu, biểu tượng, bộ chọn ngày, thành phần chung
│   ├── phanhe/                   # Màn hình phân hệ và sơ đồ môn – khóa – lớp
│   └── <module>/GiaoDien*.java
├── controller/                   # Tiếp nhận yêu cầu từ view, gọi service
│   ├── DieuKhienGiaoDienChinh.java
│   ├── DieuKhienTrangChu.java
│   ├── phanhe/
│   └── <module>/DieuKhien*.java
├── model/                        # Đối tượng dữ liệu, enum và record
│   ├── CoSo.java
│   ├── VaiTroNhanVien.java
│   ├── DuLieuMau.java             # Các kiểu dữ liệu thống kê/lịch/bảng
│   ├── phanhe/
│   └── <module>/ThongTin*.java
├── service/                      # Kiểm tra dữ liệu và xử lý nghiệp vụ
│   ├── XuLyDuLieuMau.java
│   ├── phanhe/
│   └── <module>/XuLy*.java
├── repository/                   # Hợp đồng và điểm truy cập kho dữ liệu
│   ├── DuLieuTrangChuRepository.java
│   ├── phanhe/KhoDuLieuManHinh.java
│   └── <module>/*Repository.java
├── data/                         # Nguồn dữ liệu mẫu, bộ nhớ và DAO
│   ├── DuLieuMauTrangChu.java
│   ├── phanhe/
│   └── <module>/DuLieu*.java
└── config/                       # Cấu hình ứng dụng, phân công module
    ├── CauHinhUngDung.java
    └── PhanCongModule.java
```

12 module dùng chung cách chia tầng: `hocvien`, `tuyensinh`, `khoahoc`, `lophoc`,
`lichhoc`, `diemdanh`, `hocvu`, `hocphi`, `tienan`, `kho`, `baocao`, `hethong`.

Ứng dụng dùng Java Swing, vì vậy các giao diện trong `view` là file `.java`.
Các lớp xử lý tương ứng nằm trong `controller`; không chuyển lớp giao diện sang
controller. Mỗi tầng tiếp tục chia theo module để dễ tìm và phân công phát triển.

## Luồng xử lý

```text
View → Controller → Service → Repository → Data
                       ↕            ↕
                              Model
```

- View dựng giao diện, đọc giá trị từ các ô nhập và hiển thị kết quả/lỗi.
- Controller nhận thao tác của người dùng và gọi service; controller điều hướng cập nhật view.
- Service kiểm tra biểu mẫu, xử lý điểm danh, gói năm, phiếu thu, tồn kho, học bù và chốt tháng.
- Repository cung cấp API đọc/ghi; các interface theo module cho phép thay nguồn dữ liệu.
- Data giữ dữ liệu mẫu/bộ nhớ, hoặc hiện thực DAO thông qua interface repository.
- Model không phụ thuộc Swing, controller hay kho dữ liệu.
- Config giữ thông tin cấu hình chung.

`KhoDuLieuManHinh.lay(...)` trả bản sao của danh sách và từng dòng. Mọi cập nhật
phải đi qua API ghi; sửa trực tiếp kết quả đọc không thay đổi kho dữ liệu.

## Trạng thái chức năng

Menu đang hoạt động sử dụng `GiaoDienPhanHe` → `DieuKhienPhanHe` → `XuLyPhanHe`
→ `KhoDuLieuManHinh` → nguồn dữ liệu trong `data`. Dashboard và sơ đồ
môn – khóa – lớp cũng đi qua controller, service và repository.

Các lớp riêng theo 12 module là khung mở rộng để phát triển từng phân hệ.
Các DAO SQL theo module vẫn báo chưa kết nối; chúng chưa được menu sử dụng.
Bản này tiếp tục chạy với dữ liệu demo trong RAM; dữ liệu thay đổi mất khi thoát.
File SQL trong `database/` được giữ để phát triển kết nối về sau. Chưa triển khai
JDBC, xác thực hoặc phân quyền thật.

## Mở và chạy

Mở thư mục chứa `pom.xml` bằng NetBeans, IntelliJ IDEA hoặc Eclipse với JDK 17+.

```sh
mvn clean package
java -jar target/VINABACUS-0.4.0-SNAPSHOT.jar
# Mở trực tiếp cơ sở Trung tâm:
java -jar target/VINABACUS-0.4.0-SNAPSHOT.jar --trungtam
```

Windows có thể chạy `chay_VINABACUS.bat` sau khi cài JDK/Maven hoặc mở bằng NetBeans.

## Kiểm tra

```sh
mvn clean verify
```

Maven biên dịch main/test và chạy các kiểm tra bằng `java -ea`: dữ liệu mẫu,
cấu trúc 12 module, toàn bộ tab/CRUD/điểm danh, phụ thuộc giữa các tầng và
nghiệp vụ qua controller. Không cần SQL Server hay màn hình cho nhóm kiểm tra này.

Kiểm tra cửa sổ và điều hướng trên máy có desktop:

```sh
java -ea -cp target/test-classes:target/classes vn.vinabacus.KiemThuDieuHuong
```

Trên Windows, thay dấu `:` trong classpath bằng `;`. Trên Linux không có desktop,
có thể chạy lệnh trên với tiền tố `xvfb-run -a` nếu đã cài Xvfb.
