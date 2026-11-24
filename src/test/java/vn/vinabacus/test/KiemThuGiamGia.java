package vn.vinabacus.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.service.GiamGiaService;
import vn.vinabacus.service.PhanBoThanhToanService;

/** Chạy trực tiếp: java -cp target/test-classes;target/classes vn.vinabacus.test.KiemThuGiamGia */
public final class KiemThuGiamGia {
    private static final LocalDate NGAY = LocalDate.of(2026, 11, 22);
    private static final KhoaHoc TOAN = new KhoaHoc(1, 1, 10, "Toán",
            "Toán tư duy 2", 32, new BigDecimal("2000000"), true);
    private static final KhoaHoc ANH = new KhoaHoc(2, 1, 20, "Tiếng Anh",
            "Tiếng Anh 1", 36, new BigDecimal("2500000"), true);

    private static ChuongTrinhGiamGia ct(int id, String loai, String giaTri,
            boolean tatCa, boolean ketHop, Set<Integer> mon) {
        return new ChuongTrinhGiamGia(id, 1, "CT" + id, "",
                loai, new BigDecimal(giaTri), NGAY.minusDays(1),
                NGAY.plusDays(1), tatCa, ketHop, true, mon, Set.of());
    }

    private static void bang(BigDecimal thucTe, String mongDoi) {
        if (thucTe.compareTo(new BigDecimal(mongDoi)) != 0)
            throw new AssertionError("Mong " + mongDoi + ", nhan " + thucTe);
    }
    private static void tuChoi(Runnable task) {
        try { task.run(); throw new AssertionError("Phải từ chối dữ liệu không hợp lệ"); }
        catch (IllegalArgumentException expected) { }
    }

    public static void main(String[] args) {
        var pt = ct(1, "PHAN_TRAM", "10", true, true, Set.of());
        var tien = ct(2, "SO_TIEN", "200000", false, true, Set.of(20));
        var ketQua = GiamGiaService.tinh(List.of(TOAN, ANH), List.of(pt, tien), NGAY);
        bang(ketQua.hocPhiGoc(), "4500000");
        bang(ketQua.tongGiam(), "650000");
        bang(ketQua.phaiThu(), "3850000");
        bang(ketQua.choKhoa(TOAN.maKhoaHoc()).tongGiam(), "200000");
        bang(ketQua.choKhoa(ANH.maKhoaHoc()).tongGiam(), "450000");
        var khongKetHop = ct(3, "SO_TIEN", "100000", false, false, Set.of(20));
        tuChoi(() -> GiamGiaService.tinh(List.of(TOAN, ANH),
                List.of(pt, khongKetHop), NGAY));
        var khacMon = ct(4, "SO_TIEN", "100000", false, false, Set.of(10));
        var ketQuaRieng = GiamGiaService.tinh(List.of(TOAN, ANH),
                List.of(khacMon, khongKetHop), NGAY);
        bang(ketQuaRieng.tongGiam(), "200000");
        tuChoi(() -> GiamGiaService.tinh(List.of(TOAN), List.of(pt, pt), NGAY));
        tuChoi(() -> GiamGiaService.tinh(List.of(TOAN),
                List.of(ct(5, "PHAN_TRAM", "101", true, true, Set.of())), NGAY));
        tuChoi(() -> GiamGiaService.tinh(List.of(TOAN),
                List.of(new ChuongTrinhGiamGia(6, 1, "Het han", "", "SO_TIEN",
                        BigDecimal.ONE, NGAY.minusDays(5), NGAY.minusDays(1),
                        true, true, true, Set.of(), Set.of())), NGAY));
        var gioiHan = GiamGiaService.tinh(List.of(TOAN, ANH),
                List.of(ct(7, "SO_TIEN", "9999999", true, true, Set.of())), NGAY);
        bang(gioiHan.phaiThu(), "0");
        var phanBo = PhanBoThanhToanService.tinh(
                Map.of(1, new BigDecimal("1800000"), 2, new BigDecimal("2050000")),
                new BigDecimal("2000000"));
        bang(phanBo.get(1).add(phanBo.get(2)), "2000000");
        var xu = PhanBoThanhToanService.tinh(
                Map.of(1, new BigDecimal("0.01"), 2, new BigDecimal("0.02")),
                new BigDecimal("0.01"));
        bang(xu.get(1).add(xu.get(2)), "0.01");
        tuChoi(() -> PhanBoThanhToanService.tinh(
                Map.of(1, new BigDecimal("100")), new BigDecimal("101")));
        System.out.println("KiemThuGiamGia: PASS");
    }
}
