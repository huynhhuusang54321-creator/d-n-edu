package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinDiemDanh;
import vn.vinabacus.repository.DiemDanhRepository;

/**
 * Data/DAO – module Điểm danh (thanh vien 4).
 * Cac bang SQL lien quan: DiemDanhMamNon, DiemDanhTrungTam.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuDiemDanh implements DiemDanhRepository {
    @Override
    public List<ThongTinDiemDanh> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module diemdanh");
    }
}
