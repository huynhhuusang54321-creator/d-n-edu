package vn.vinabacus.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Du lieu chi doc ve cac lop va lich cua nhan vien dang phu trach tai Trung tam. */
public record ChiTietGiaoVienTrungTam(List<Lop> lop, List<Lich> lich) {
    public ChiTietGiaoVienTrungTam {
        lop = List.copyOf(lop);
        lich = List.copyOf(lich);
    }

    public record Lop(int maLop, String tenLop, String tenMon,
                      String tenKhoa, LocalDate ngayKhaiGiang, String trangThai) { }

    public record Lich(int maLich, int maLop, String tenLop,
                       int thuTrongTuan, LocalTime batDau, LocalTime ketThuc,
                       LocalDate tuNgay, LocalDate denNgay, String trangThai) { }
}
