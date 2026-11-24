package vn.vinabacus.service;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.NhanVienRepository;

class NhanVienServiceTest {
    @Test
    void ngungCongTacTrungTamPhaiQuaKiemTraLop() throws SQLException {
        Repo repo = new Repo();
        NhanVienService service = new NhanVienService(repo, new CoSoGia());

        assertTrue(service.ngungCongTac(CoSo.TRUNG_TAM, 7));
        assertTrue(repo.daKiemTraLop);
        assertFalse(repo.daDungDuongMamNon);
    }

    @Test
    void ngungCongTacMamNonGiuDuongXuLyCu() throws SQLException {
        Repo repo = new Repo();
        NhanVienService service = new NhanVienService(repo, new CoSoGia());

        assertTrue(service.ngungCongTac(CoSo.MAM_NON, 7));
        assertFalse(repo.daKiemTraLop);
        assertTrue(repo.daDungDuongMamNon);
    }

    private static final class CoSoGia extends CoSoRepository {
        @Override public int timMaCoSo(CoSo coSo) { return coSo == CoSo.TRUNG_TAM ? 2 : 1; }
    }

    private static final class Repo extends NhanVienRepository {
        boolean daKiemTraLop;
        boolean daDungDuongMamNon;

        @Override public boolean ngungCongTacTrungTam(int maCoSo, int maNhanVien) {
            daKiemTraLop = true;
            assertEquals(2, maCoSo);
            assertEquals(7, maNhanVien);
            return true;
        }

        @Override public boolean ngungCongTac(int maCoSo, int maNhanVien) {
            daDungDuongMamNon = true;
            assertEquals(1, maCoSo);
            assertEquals(7, maNhanVien);
            return true;
        }
    }
}
