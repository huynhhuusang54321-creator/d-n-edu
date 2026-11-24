package vn.vinabacus.controller;

import java.sql.SQLException;
import java.util.Map;
import vn.vinabacus.model.ChiTietHocVienTrungTam;
import vn.vinabacus.model.ChiTietHocVienTrungTam.TongQuan;
import vn.vinabacus.service.ChiTietHocVienTrungTamService;

public final class ChiTietHocVienTrungTamController {
    private final ChiTietHocVienTrungTamService service =
            new ChiTietHocVienTrungTamService();

    public Map<Integer, TongQuan> tongQuan() throws SQLException {
        return service.tongQuan();
    }

    public ChiTietHocVienTrungTam tai(int maHocVien) throws SQLException {
        return service.tai(maHocVien);
    }
}
