package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinBaoCao;
import vn.vinabacus.repository.BaoCaoRepository;

/**
 * Data/DAO – module Báo cáo, thống kê (thanh vien 6).
 * Cac bang SQL lien quan: BaoCaoTongHop.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuBaoCao implements BaoCaoRepository {
    @Override
    public List<ThongTinBaoCao> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module baocao");
    }
}
