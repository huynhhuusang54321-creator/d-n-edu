package vn.vinabacus.model;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

/** Du lieu thuc te cua tung khoa hoc va lich su da luu cho hoc vien Trung tam. */
public record ChiTietHocVienTrungTam(List<Khoa> khoaHoc, List<SuKien> lichSu) {

    public record TongQuan(List<String> tenKhoa, int tongKhoa, int daXep,
                            int dangCho, boolean hoSoTam) {
        public String tinhTrangXepLop() {
            if (tongKhoa == 0) return "Chưa đăng ký khóa";
            if (daXep == 0) return "Chưa xếp lớp";
            if (dangCho > 0) return "Xếp lớp một phần";
            return "Đã xếp đủ";
        }
    }

    public record Khoa(DangKyKhoaHoc dangKy, String tenGiaoVien,
                       List<String> lichHoc, LocalDate ngayKhaiGiang,
                       LocalDate ngayKetThucDuKien, LocalDate ngayKetThucThucTe,
                       Integer soBuoiHoanThanh, BigDecimal hocPhiGoc,
                       BigDecimal soTienGiam, BigDecimal daDong) {
        public boolean dangChoXepLop() {
            return "CHO_XEP_LOP".equals(dangKy.trangThai()) || dangKy.maLop() == null;
        }
        public BigDecimal conNo() { return dangKy.hocPhiSauGiam().subtract(daDong); }
    }

    public record SuKien(LocalDate ngay, String loai, String noiDung) { }
}
