package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinHocPhi;
import vn.vinabacus.repository.HocPhiRepository;

/**
 * Data/DAO – module Học phí, giảm giá, công nợ (thanh vien 5).
 * Cac bang SQL lien quan: KhoanPhaiThu, PhieuThu, ChiTietPhieuThu, PhatSinhTinDungHocPhi.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuHocPhi implements HocPhiRepository {
    @Override
    public List<ThongTinHocPhi> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module hocphi");
    }
}
