package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinKho;
import vn.vinabacus.repository.KhoRepository;

/**
 * Data/DAO – module Kho, nhập/xuất, kiểm kê (thanh vien 6).
 * Cac bang SQL lien quan: HangHoa, PhieuKho, KyKho, KiemKeKho.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuKho implements KhoRepository {
    @Override
    public List<ThongTinKho> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module kho");
    }
}
