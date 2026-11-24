package vn.vinabacus.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.service.GiamGiaService;
import vn.vinabacus.service.PhanBoThanhToanService;

class GiamGiaNghiepVuTest {
    private static final LocalDate NGAY = LocalDate.of(2026, 11, 22);
    private static final KhoaHoc TOAN = new KhoaHoc(1, 1, 10, "Toán",
            "Toán tư duy 2", 32, new BigDecimal("2000000"), true);
    private static final KhoaHoc ANH = new KhoaHoc(2, 1, 20, "Tiếng Anh",
            "Tiếng Anh 1", 36, new BigDecimal("2500000"), true);

    private static ChuongTrinhGiamGia chuongTrinh(int id, String loai,
            String giaTri, boolean tatCa, boolean ketHop, Set<Integer> mon) {
        return new ChuongTrinhGiamGia(id, 1, "CT" + id, "",
                loai, new BigDecimal(giaTri), NGAY.minusDays(1),
                NGAY.plusDays(1), tatCa, ketHop, true, mon, Set.of());
    }

    private static void bang(String mongDoi, BigDecimal thucTe) {
        assertEquals(0, new BigDecimal(mongDoi).compareTo(thucTe));
    }

    @Test void haiKhoaHaiChuongTrinhVaThuMotPhan() {
        var bangGia = GiamGiaService.tinh(List.of(TOAN, ANH), List.of(
                chuongTrinh(1, "PHAN_TRAM", "10", true, true, Set.of()),
                chuongTrinh(2, "SO_TIEN", "200000", false, true, Set.of(20))), NGAY);
        bang("4500000", bangGia.hocPhiGoc());
        bang("650000", bangGia.tongGiam());
        bang("3850000", bangGia.phaiThu());
        bang("1800000", bangGia.choKhoa(1).phaiThu());
        bang("2050000", bangGia.choKhoa(2).phaiThu());
        var thu = PhanBoThanhToanService.tinh(
                Map.of(1, bangGia.choKhoa(1).phaiThu(),
                       2, bangGia.choKhoa(2).phaiThu()), new BigDecimal("2000000"));
        bang("2000000", thu.get(1).add(thu.get(2)));
        bang("1850000", bangGia.phaiThu().subtract(thu.get(1)).subtract(thu.get(2)));
    }

    @Test void xungDotKetHopTrenCungKhoa() {
        var khongKetHop = chuongTrinh(2, "SO_TIEN", "200000",
                false, false, Set.of(20));
        var apDungChung = chuongTrinh(1, "PHAN_TRAM", "10", true, true, Set.of());
        assertThrows(IllegalArgumentException.class, () -> GiamGiaService.tinh(
                List.of(TOAN, ANH), List.of(khongKetHop, apDungChung), NGAY));
    }

    @Test void khongGiamHayThuVuotHocPhi() {
        var bangGia = GiamGiaService.tinh(List.of(TOAN, ANH), List.of(
                chuongTrinh(3, "SO_TIEN", "9999999", true, true, Set.of())), NGAY);
        bang("0", bangGia.phaiThu());
        assertThrows(IllegalArgumentException.class, () ->
                PhanBoThanhToanService.tinh(Map.of(1, BigDecimal.TEN),
                        new BigDecimal("11")));
    }

    @Test void chuongTrinhChuaBatDauHoacDaNgungKhongDuocApDung() {
        var sapBatDau = new ChuongTrinhGiamGia(4, 1, "Sắp bắt đầu", "",
                "PHAN_TRAM", BigDecimal.TEN, NGAY.plusDays(1), NGAY.plusDays(10),
                true, true, true, Set.of(), Set.of());
        var daNgung = new ChuongTrinhGiamGia(5, 1, "Đã ngừng", "",
                "PHAN_TRAM", BigDecimal.TEN, NGAY.minusDays(1), NGAY.plusDays(10),
                true, true, false, Set.of(), Set.of());
        assertThrows(IllegalArgumentException.class, () ->
                GiamGiaService.tinh(List.of(TOAN), List.of(sapBatDau), NGAY));
        assertThrows(IllegalArgumentException.class, () ->
                GiamGiaService.tinh(List.of(TOAN), List.of(daNgung), NGAY));
    }
}
