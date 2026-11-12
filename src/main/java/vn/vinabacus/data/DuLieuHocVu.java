package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinHocVu;
import vn.vinabacus.repository.HocVuRepository;

/**
 * Data/DAO – module Học bù, đổi buổi, chuyển lớp (thanh vien 4).
 * Cac bang SQL lien quan: ChuyenLopTrungTam, SapXepBuoiHoc.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuHocVu implements HocVuRepository {
    @Override
    public List<ThongTinHocVu> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module hocvu");
    }
}
