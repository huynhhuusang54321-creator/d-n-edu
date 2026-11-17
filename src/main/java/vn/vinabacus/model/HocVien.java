package vn.vinabacus.model;
import java.time.LocalDate;
/** Ho so hoc vien theo dbo.HocVien - VINABACUS. */
public record HocVien(int maHocVien, int maCoSo, String hoTen, LocalDate ngaySinh,
        String gioiTinh, String tenNguoiLienHe, String soDienThoaiLienHe,
        String diaChi, String ghiChu, String trangThaiHoSo) { }
