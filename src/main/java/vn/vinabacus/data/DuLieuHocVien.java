package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinHocVien;
import vn.vinabacus.repository.HocVienRepository;

/**
 * Data/DAO – module Học viên (thanh vien 2).
 * Cac bang SQL lien quan: HocVien, XepLopMamNon, XepLopTrungTam.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuHocVien implements HocVienRepository {
    @Override
    public List<ThongTinHocVien> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module hocvien");
    }
}
