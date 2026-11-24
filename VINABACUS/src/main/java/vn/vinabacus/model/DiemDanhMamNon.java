package vn.vinabacus.model;
import java.time.LocalDate;
/** Bản ghi điểm danh theo ngày; mã 0 nghĩa là chưa có bản ghi. */
public record DiemDanhMamNon(int maDiemDanh, int maHocVien, int maXepLop,
                              String hoTen, LocalDate ngayHoc, String trangThai, String ghiChu) { }
