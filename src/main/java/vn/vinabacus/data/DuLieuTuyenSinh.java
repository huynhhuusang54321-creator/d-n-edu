package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinTuyenSinh;
import vn.vinabacus.repository.TuyenSinhRepository;

/**
 * Data/DAO – module Tuyển sinh (thanh vien 2).
 * Cac bang SQL lien quan: KhachTiemNang, YeuCauXepLop.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuTuyenSinh implements TuyenSinhRepository {
    @Override
    public List<ThongTinTuyenSinh> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module tuyensinh");
    }
}
