package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinTienAn;
import vn.vinabacus.repository.TienAnRepository;

/**
 * Data/DAO – module Tiền ăn Mầm non (thanh vien 5).
 * Cac bang SQL lien quan: DonGiaTienAn, ChiTietTienAnMamNon, PhatSinhTienAn.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuTienAn implements TienAnRepository {
    @Override
    public List<ThongTinTienAn> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module tienan");
    }
}
