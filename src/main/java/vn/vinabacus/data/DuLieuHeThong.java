package vn.vinabacus.data;

import java.util.List;
import vn.vinabacus.model.ThongTinHeThong;
import vn.vinabacus.repository.HeThongRepository;

/**
 * Data/DAO – module Nhân viên, tài khoản, cơ sở (thanh vien 6).
 * Cac bang SQL lien quan: NhanVien, TaiKhoan, VaiTro, CoSo.
 * TODO: JDBC PreparedStatement, loc theo co so va quyen truy cap.
 */
public class DuLieuHeThong implements HeThongRepository {
    @Override
    public List<ThongTinHeThong> layDanhSach(int maCoSo) {
        throw new UnsupportedOperationException("Chua ket noi SQL Server - module hethong");
    }
}
