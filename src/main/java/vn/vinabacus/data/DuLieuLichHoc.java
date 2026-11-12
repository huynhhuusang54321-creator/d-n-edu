package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinLichHoc;
import vn.vinabacus.repository.LichHocRepository;

/**
 * Data/DAO – module Lịch lớp và buổi học (thanh vien 3).
 * Cac bang SQL lien quan: LichHocLop, BuoiHoc, SapXepBuoiHoc.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuLichHoc implements LichHocRepository {
    @Override
    public List<ThongTinLichHoc> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module lichhoc");
    }
}
