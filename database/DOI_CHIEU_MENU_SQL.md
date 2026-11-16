# Danh mục menu và nhóm bảng SQL VINABACUS

Tài liệu định hướng phân công, **chưa phải mã truy vấn hay quan hệ chính thức**. Trước khi viết Repository phải đọc SQL thực tế, xem khóa chính/khóa ngoại, và kiểm tra bảng có MaCoSo hay cần JOIN.

## Trung tâm (15 mục)

| Menu | Mã menu | Bảng dữ liệu liên quan (dự kiến) |
|---|---|---|
| Trang chủ | tt-trang-chu | Tạo sau |
| Học viên | tt-hoc-vien | HocVien |
| Tuyển sinh | tt-tuyen-sinh | KhachTiemNang |
| Đăng ký khóa học | tt-dang-ky | DangKyKhoaHoc (đối chiếu tên thực tế) |
| Môn học | tt-mon-hoc | MonHoc |
| Khóa học | tt-khoa-hoc | KhoaHoc |
| Lớp học | tt-lop-hoc | LopTrungTam |
| Lịch học | tt-lich-hoc | LichHocLop |
| Điểm danh | tt-diem-danh | Điểm danh Trung tâm (đối chiếu bảng thực tế) |
| Học phí | tt-hoc-phi | KhoanPhaiThu |
| Phiếu thu | tt-phieu-thu | PhieuThu, ChiTietPhieuThu |
| Nhân sự / Giáo viên | tt-nhan-su | NhanVien, NhanVienCoSo |
| Kho - Dụng cụ | tt-kho | HangHoa, PhieuKho |
| Báo cáo | tt-bao-cao | Nhiều bảng |
| Hệ thống | tt-he-thong | TaiKhoan, VaiTro, TaiKhoanCoSo |

## Mầm non (13 mục)

| Menu | Mã menu | Bảng dữ liệu liên quan (dự kiến) |
|---|---|---|
| Trang chủ | mn-trang-chu | Tạo sau |
| Học sinh | mn-hoc-sinh | HocVien |
| Tuyển sinh | mn-tuyen-sinh | KhachTiemNang |
| Năm học | mn-nam-hoc | NamHoc |
| Lớp Mầm non | mn-lop-hoc | LopMamNon |
| Xếp lớp | mn-xep-lop | XepLopMamNon |
| Điểm danh | mn-diem-danh | DiemDanhMamNon |
| Học phí & Tiền ăn | mn-hoc-phi | KhoanPhaiThu, DonGiaTienAn |
| Phiếu thu | mn-phieu-thu | PhieuThu, ChiTietPhieuThu |
| Nhân sự / Giáo viên | mn-nhan-su | NhanVien, NhanVienCoSo |
| Kho - Dụng cụ | mn-kho | HangHoa, PhieuKho |
| Báo cáo | mn-bao-cao | Nhiều bảng |
| Hệ thống | mn-he-thong | TaiKhoan, VaiTro, TaiKhoanCoSo |

**Không xóa bảng SQL chỉ vì không còn mục menu riêng.** Các nghiệp vụ bổ sung có thể gắn vào tab bên trong JPanel của một nhóm chức năng.
