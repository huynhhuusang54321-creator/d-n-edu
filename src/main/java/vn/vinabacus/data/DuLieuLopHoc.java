package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinLopHoc;
import vn.vinabacus.repository.LopHocRepository;

/**
 * Data/DAO – module Lớp học và phân công (thanh vien 3).
 * Cac bang SQL lien quan: LopTrungTam, LopMamNon.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuLopHoc implements LopHocRepository {
    @Override
    public List<ThongTinLopHoc> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module lophoc");
    }
}
