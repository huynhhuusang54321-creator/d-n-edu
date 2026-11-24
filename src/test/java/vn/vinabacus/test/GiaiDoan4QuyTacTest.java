package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.service.KhoaHocService;
import vn.vinabacus.service.LopTrungTamService;
import vn.vinabacus.service.MonHocService;

/** Kiểm tra dữ liệu sai bị chặn trước khi mở kết nối CSDL thật. */
class GiaiDoan4QuyTacTest {
    @Test
    void monKhongCoTenBiTuChoi() {
        MonHocService service = new MonHocService(CoSo.TRUNG_TAM);
        assertThrows(IllegalArgumentException.class,
                () -> service.them("  ", null, true));
    }

    @Test
    void khoaKhongDuocCoSoBuoiBangKhongHoacHocPhiAm() {
        KhoaHocService service = new KhoaHocService(CoSo.TRUNG_TAM);
        assertThrows(IllegalArgumentException.class,
                () -> service.them(1, "Khóa", 0, BigDecimal.ZERO, true));
        assertThrows(IllegalArgumentException.class,
                () -> service.them(1, "Khóa", 24, new BigDecimal("-1"), true));
    }

    @Test
    void lopMoiPhaiCoSiSoToiDa() {
        LopTrungTamService service = new LopTrungTamService(CoSo.TRUNG_TAM);
        assertThrows(IllegalArgumentException.class,
                () -> service.them(1, "Lớp", null, null, "DANG_MO", 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.them(1, "Lớp", null, null, "DANG_MO", -1));
    }

    @Test
    void chuyenGiaoVienSaiNgayHoacMaBiTuChoiTruocKhiMoSql() {
        LopTrungTamService service = new LopTrungTamService(CoSo.TRUNG_TAM);
        assertThrows(IllegalArgumentException.class,
                () -> service.chuyenGiaoVien(0, 2, LocalDate.now(), "Lý do"));
        assertThrows(IllegalArgumentException.class,
                () -> service.chuyenGiaoVien(1, 2, null, "Lý do"));
        assertThrows(IllegalArgumentException.class,
                () -> service.chuyenGiaoVien(1, 2, LocalDate.now().plusDays(1), "Lý do"));
        assertThrows(IllegalArgumentException.class,
                () -> service.chuyenGiaoVien(1, 2, LocalDate.now(), " "));
    }

    @Test
    void khaiBaoChuyenMonSaiDuLieuBiTuChoiTruocKhiMoSql() {
        LopTrungTamService service = new LopTrungTamService(CoSo.TRUNG_TAM);
        assertThrows(IllegalArgumentException.class,
                () -> service.themChuyenMonGiaoVien(0, 1, LocalDate.now(), "Lý do"));
        assertThrows(IllegalArgumentException.class,
                () -> service.themChuyenMonGiaoVien(1, 0, LocalDate.now(), "Lý do"));
        assertThrows(IllegalArgumentException.class,
                () -> service.themChuyenMonGiaoVien(1, 1, LocalDate.now().minusDays(1), "Lý do"));
        assertThrows(IllegalArgumentException.class,
                () -> service.themChuyenMonGiaoVien(1, 1, LocalDate.now(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.ketThucChuyenMonGiaoVien(1, 1, LocalDate.now(), ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.ketThucChuyenMonGiaoVien(1, 1,
                        LocalDate.now().plusDays(1), "Lý do"));
    }
}
