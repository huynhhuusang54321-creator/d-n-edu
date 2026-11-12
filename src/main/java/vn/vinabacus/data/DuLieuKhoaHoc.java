package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinKhoaHoc;
import vn.vinabacus.repository.KhoaHocRepository;

/**
 * Data/DAO – module Khóa học và gói năm (thanh vien 3).
 * Cac bang SQL lien quan: KhoaHoc, DangKyGoiNam, DangKyKhoaHoc, ChuongTrinhGiamGia.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuKhoaHoc implements KhoaHocRepository {
    @Override
    public List<ThongTinKhoaHoc> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module khoahoc");
    }
}
