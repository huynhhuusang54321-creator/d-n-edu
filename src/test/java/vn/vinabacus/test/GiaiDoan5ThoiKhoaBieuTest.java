package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.service.CaHocTrungTam;
import vn.vinabacus.service.DuKienKetThucLop;
import vn.vinabacus.service.QuyTacTrungLich;
import vn.vinabacus.service.LichHocLopService;
import vn.vinabacus.repository.LichHocLopRepository.HocVienTheoLop;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.view.ThoiKhoaBieuTuanPanel;

class GiaiDoan5ThoiKhoaBieuTest {
    private static final LocalDate THU_HAI = LocalDate.of(2026, 11, 23);

    private static LichHocLop lich(int id, int thu, int ca, LocalDate tu,
                                   LocalDate den, String trangThai) {
        CaHocTrungTam.Ca gio = CaHocTrungTam.layTheoSoCa(ca);
        return new LichHocLop(id, 1, 1, 1, "Toán", "Khóa 1", "Lớp A",
                thu, gio.batDau(), gio.ketThuc(), tu, den, trangThai, 1, "Phòng 1");
    }

    @Test void tamCaCoDinhLienTucVaMoiCaChinMuoiPhut() {
        assertEquals(8, CaHocTrungTam.DANH_SACH.size());
        assertEquals(LocalTime.of(7, 0), CaHocTrungTam.layTheoSoCa(1).batDau());
        assertEquals(LocalTime.of(19, 0), CaHocTrungTam.layTheoSoCa(8).ketThuc());
        for (int soCa = 1; soCa <= 8; soCa++) {
            CaHocTrungTam.Ca ca = CaHocTrungTam.layTheoSoCa(soCa);
            assertEquals(90, Duration.between(ca.batDau(), ca.ketThuc()).toMinutes());
            if (soCa < 8)
                assertEquals(ca.ketThuc(), CaHocTrungTam.layTheoSoCa(soCa + 1).batDau());
        }
    }

    @Test void duKienDemTungCaVaKhongDemLichDaHuy() {
        List<LichHocLop> lich = List.of(
                lich(1, 2, 1, THU_HAI, null, "HIEU_LUC"),
                lich(2, 2, 2, THU_HAI, null, "HIEU_LUC"),
                lich(3, 3, 1, THU_HAI, null, "HUY"));
        assertEquals(THU_HAI, DuKienKetThucLop.tinh(THU_HAI, 2, lich).orElseThrow());
        assertEquals(THU_HAI.plusWeeks(1),
                DuKienKetThucLop.tinh(THU_HAI, 3, lich).orElseThrow());
    }

    @Test void duKienTonTrongNgayHieuLucVaBaoThieuBuoi() {
        List<LichHocLop> lich = List.of(
                lich(1, 2, 1, THU_HAI.plusWeeks(1), THU_HAI.plusWeeks(2), "HIEU_LUC"));
        assertEquals(THU_HAI.plusWeeks(2),
                DuKienKetThucLop.tinh(THU_HAI, 2, lich).orElseThrow());
        assertTrue(DuKienKetThucLop.tinh(THU_HAI, 3, lich).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> DuKienKetThucLop.tinh(THU_HAI, 0, lich));
    }

    @Test void dangKyVaTheLopDungCungMotPhepTinh() {
        LichHocLop oLich = lich(1, 4, 3, THU_HAI, null, "HIEU_LUC");
        DuKienKetThucLop.Lich lichDangKy = new DuKienKetThucLop.Lich(
                oLich.maLichHoc(), oLich.thuTrongTuan(), oLich.gioBatDau(),
                oLich.gioKetThuc(), oLich.tuNgay(), oLich.denNgay(), oLich.trangThai());
        assertEquals(DuKienKetThucLop.tinh(THU_HAI, 5, List.of(oLich)),
                DuKienKetThucLop.tinhTuLich(THU_HAI, 5, List.of(lichDangKy)));
    }

    @Test void lichHocVienChiHienTrongDotXepLop() {
        LichHocLop lich = lich(7, 2, 1, THU_HAI, null, "HIEU_LUC");
        HocVienTheoLop daXep = new HocVienTheoLop(9, "Học viên", 1,
                THU_HAI.plusWeeks(1), THU_HAI.plusWeeks(3));
        LichHocLop daLoc = LichHocLopService.gioiHanTheoXep(lich, daXep);
        assertEquals(THU_HAI.plusWeeks(1), daLoc.tuNgay());
        assertEquals(THU_HAI.plusWeeks(3), daLoc.denNgay());
        assertEquals(lich.maLichHoc(), daLoc.maLichHoc());
        assertEquals(null, LichHocLopService.gioiHanTheoXep(lich,
                new HocVienTheoLop(9, "Học viên", 2, THU_HAI, null)));
        assertEquals(null, LichHocLopService.gioiHanTheoXep(
                lich(7, 2, 1, THU_HAI, THU_HAI, "HIEU_LUC"), daXep));
    }

    @Test void khongTachLichVeQuaKhuHoacDungCaKhongChuan() {
        LichHocLopService service = new LichHocLopService(CoSo.TRUNG_TAM);
        assertThrows(IllegalArgumentException.class, () -> service.suaTuNgay(
                1, 1, 2, LocalTime.of(7, 0), LocalTime.of(8, 30),
                LocalDate.now().minusDays(1), null, "HIEU_LUC"));
        assertThrows(IllegalArgumentException.class, () -> service.suaTuNgay(
                1, 1, 2, LocalTime.of(7, 15), LocalTime.of(8, 45),
                LocalDate.now(), null, "HIEU_LUC"));
    }

    @Test void buoiHuyVaHocBuThayDoiNgayDuKienKhongDemTrung() {
        DuKienKetThucLop.Lich thuHai = new DuKienKetThucLop.Lich(
                1, 2, LocalTime.of(7, 0), LocalTime.of(8, 30),
                THU_HAI, null, "HIEU_LUC");
        var huy = new DuKienKetThucLop.BuoiDieuChinh(10, 1, 1,
                THU_HAI.plusWeeks(1), 1, "HUY");
        var hocBu = new DuKienKetThucLop.BuoiDieuChinh(11, 1, null,
                THU_HAI.plusDays(9), 2, "DU_KIEN");
        assertEquals(THU_HAI.plusWeeks(3), DuKienKetThucLop.tinhTuLich(
                THU_HAI, 3, List.of(thuHai), List.of(huy), Set.of()).orElseThrow());
        assertEquals(THU_HAI.plusWeeks(2), DuKienKetThucLop.tinhTuLich(
                THU_HAI, 3, List.of(thuHai), List.of(huy, hocBu), Set.of()).orElseThrow());
        var trung = new DuKienKetThucLop.BuoiDieuChinh(12, 1, 1,
                THU_HAI, 1, "DU_KIEN");
        assertEquals(THU_HAI.plusWeeks(2), DuKienKetThucLop.tinhTuLich(
                THU_HAI, 3, List.of(thuHai), List.of(trung), Set.of()).orElseThrow());
        assertEquals(THU_HAI.plusWeeks(2), DuKienKetThucLop.tinhTuLich(
                THU_HAI, 3, List.of(thuHai), List.of(hocBu),
                Set.of(THU_HAI.plusWeeks(1))).orElseThrow());
    }

    @Test void doiLichTuNgayHieuLucTinhLaiNgayKetThuc() {
        var lichCu = new DuKienKetThucLop.Lich(1, 2,
                LocalTime.of(7, 0), LocalTime.of(8, 30),
                THU_HAI, THU_HAI.plusWeeks(1), "HIEU_LUC");
        var lichMoi = new DuKienKetThucLop.Lich(2, 4,
                LocalTime.of(8, 30), LocalTime.of(10, 0),
                THU_HAI.plusWeeks(2), null, "HIEU_LUC");
        assertEquals(THU_HAI.plusDays(16), DuKienKetThucLop.tinhTuLich(
                THU_HAI, 3, List.of(lichCu, lichMoi)).orElseThrow());
        assertEquals(THU_HAI.plusDays(15), DuKienKetThucLop.tinhTuLich(
                THU_HAI, 3, List.of(lichCu, lichMoi,
                        new DuKienKetThucLop.Lich(3, 3,
                                LocalTime.of(10, 0), LocalTime.of(11, 30),
                                THU_HAI.plusWeeks(2), null, "HIEU_LUC"))).orElseThrow());
    }

    @Test void huyLichTuongLaiVaBoSungBuoiBuKhongDeNgayDuKienGia() {
        var lichCu = new DuKienKetThucLop.Lich(1, 2,
                LocalTime.of(7, 0), LocalTime.of(8, 30),
                THU_HAI, THU_HAI.plusWeeks(1), "HIEU_LUC");
        var lichDaHuy = new DuKienKetThucLop.Lich(2, 2,
                LocalTime.of(7, 0), LocalTime.of(8, 30),
                THU_HAI.plusWeeks(2), null, "HUY");
        assertTrue(DuKienKetThucLop.tinhTuLich(THU_HAI, 3,
                List.of(lichCu, lichDaHuy)).isEmpty());
        var hocBu = new DuKienKetThucLop.BuoiDieuChinh(10, 1, null,
                THU_HAI.plusDays(16), 2, "DU_KIEN");
        assertEquals(THU_HAI.plusDays(16), DuKienKetThucLop.tinhTuLich(
                THU_HAI, 3, List.of(lichCu, lichDaHuy),
                List.of(hocBu), Set.of()).orElseThrow());
    }

    @Test void lichTuanLoaiBuoiHuyVaTinhBuoiBu() {
        ThoiKhoaBieuTuanPanel panel = new ThoiKhoaBieuTuanPanel(THU_HAI, x -> { });
        panel.datThongTinLop(List.of(new LopTrungTam(1, 2, 1, 1,
                "Toán", "Khóa 1", "Lớp A", 2, "Giáo viên",
                THU_HAI, "DANG_MO", 10, 0, 0, 20, "Phòng 1")));
        panel.datDanhSach(List.of(lich(1, 2, 1, THU_HAI, null, "HIEU_LUC")));
        var huy = new DuKienKetThucLop.BuoiDieuChinh(11, 1, 1,
                THU_HAI, 1, "HUY", 1, "Phòng 1");
        var bu = new DuKienKetThucLop.BuoiDieuChinh(12, 1, null,
                THU_HAI.plusDays(1), 2, "DU_KIEN", 1, "Phòng 1");
        panel.datBuoiDieuChinh(List.of(huy));
        assertTrue(thongKeLichTuan(panel).contains("0 buổi học"));
        panel.datBuoiDieuChinh(List.of(huy, bu));
        assertTrue(thongKeLichTuan(panel).contains("1 buổi học"));
    }

    private static String thongKeLichTuan(java.awt.Container p) {
        for (java.awt.Component c : p.getComponents()) {
            if (c instanceof javax.swing.JLabel l && l.getText().startsWith("Tuần này:"))
                return l.getText();
            if (c instanceof java.awt.Container nested) {
                String text = thongKeLichTuan(nested);
                if (!text.isEmpty()) return text;
            }
        }
        return "";
    }

    @Test void tamCaSatGioKhongTrungNhungCaGiaoNhauTrung() {
        LocalTime bay = LocalTime.of(7, 0);
        LocalTime tamRuoi = LocalTime.of(8, 30);
        LocalTime chin = LocalTime.of(9, 0);
        assertFalse(QuyTacTrungLich.giaoGio(bay, tamRuoi,
                tamRuoi, LocalTime.of(10, 0)));
        assertTrue(QuyTacTrungLich.giaoGio(bay, chin,
                tamRuoi, LocalTime.of(10, 0)));
        assertFalse(QuyTacTrungLich.trungLichDinhKy(2, THU_HAI, THU_HAI,
                bay, tamRuoi, 2, THU_HAI.plusDays(1), null,
                bay, tamRuoi));
    }
}
