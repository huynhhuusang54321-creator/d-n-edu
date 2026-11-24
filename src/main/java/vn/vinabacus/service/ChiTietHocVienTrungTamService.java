package vn.vinabacus.service;

import java.sql.SQLException;
import java.util.Map;
import vn.vinabacus.model.ChiTietHocVienTrungTam;
import vn.vinabacus.model.ChiTietHocVienTrungTam.TongQuan;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.repository.ChiTietHocVienTrungTamRepository;
import vn.vinabacus.repository.TV2CoSoRepository;

public final class ChiTietHocVienTrungTamService {
    private final ChiTietHocVienTrungTamRepository repository =
            new ChiTietHocVienTrungTamRepository();
    private final TV2CoSoRepository coSo = new TV2CoSoRepository();

    public Map<Integer, TongQuan> tongQuan() throws SQLException {
        return repository.tongQuan(coSo.layMaCoSo(CoSo.TRUNG_TAM));
    }

    public ChiTietHocVienTrungTam tai(int maHocVien) throws SQLException {
        if (maHocVien <= 0) throw new IllegalArgumentException("Mã học viên không hợp lệ");
        return repository.tai(coSo.layMaCoSo(CoSo.TRUNG_TAM), maHocVien);
    }
}
