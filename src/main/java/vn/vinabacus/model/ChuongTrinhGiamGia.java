package vn.vinabacus.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** Cấu hình ưu đãi Trung tâm; các tập ID xác định phạm vi trên dữ liệu SQL. */
public record ChuongTrinhGiamGia(int maChuongTrinh, int maCoSo, String ten,
        String moTa, String loaiGiam, BigDecimal giaTri, LocalDate tuNgay,
        LocalDate denNgay, boolean apDungTatCaMon, boolean choPhepKetHop,
        boolean kichHoat, Set<Integer> maMonHoc, Set<Integer> maKhoaHoc) {
    public ChuongTrinhGiamGia {
        maMonHoc = Set.copyOf(maMonHoc == null ? Set.of() : maMonHoc);
        maKhoaHoc = Set.copyOf(maKhoaHoc == null ? Set.of() : maKhoaHoc);
    }

    public boolean hieuLuc(LocalDate ngay) {
        return kichHoat && ngay != null && !ngay.isBefore(tuNgay)
                && !ngay.isAfter(denNgay);
    }

    public boolean apDung(KhoaHoc khoa) {
        if (khoa == null) return false;
        if (apDungTatCaMon) return true;
        if (!maMonHoc.contains(khoa.maMonHoc())) return false;
        return maKhoaHoc.isEmpty() || maKhoaHoc.contains(khoa.maKhoaHoc());
    }
}
