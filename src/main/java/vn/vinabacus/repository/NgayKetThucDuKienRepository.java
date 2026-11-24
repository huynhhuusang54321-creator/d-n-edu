package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Cap nhat ngay du kien cung giao dich ghi lich/buoi; khong cham vao hoc phi. */
final class NgayKetThucDuKienRepository {
    private NgayKetThucDuKienRepository() { }

    private record DangKyDangHoc(int maDangKy, int maKhoaHoc,
                                 LocalDate ngayBatDau, LocalDate tuNgayXep) { }

    static void tinhLaiChoLop(Connection con, int maCoSo, int maLop) throws SQLException {
        List<DangKyDangHoc> dangKy = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT d.MaDangKy, d.MaKhoaHoc, d.NgayBatDau, x.TuNgay
                FROM dbo.XepLopTrungTam x WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.DangKyKhoaHoc d WITH (UPDLOCK, HOLDLOCK)
                  ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
                 AND d.MaCoSo=x.MaCoSo
                WHERE x.MaLop=? AND x.MaCoSo=? AND x.TrangThai='HIEU_LUC'
                  AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                  AND d.TrangThai='DANG_HOC' AND d.NgayKetThucThucTe IS NULL
                ORDER BY d.MaDangKy
                """)) {
            ps.setInt(1, maLop);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) dangKy.add(new DangKyDangHoc(
                        rs.getInt("MaDangKy"), rs.getInt("MaKhoaHoc"),
                        rs.getDate("NgayBatDau").toLocalDate(),
                        rs.getDate("TuNgay").toLocalDate()));
            }
        }
        // Khong giu ResultSet mo khi tinh lai: SQL Server co the chua bat MARS.
        for (DangKyDangHoc d : dangKy) {
            LocalDate tuNgay = d.ngayBatDau().isAfter(d.tuNgayXep())
                    ? d.ngayBatDau() : d.tuNgayXep();
            LocalDate ketThuc = DangKyKhoaHocRepository.duKienKetThuc(
                    con, maCoSo, d.maDangKy(), d.maKhoaHoc(), maLop, tuNgay);
            try (PreparedStatement ps = con.prepareStatement("""
                    UPDATE dbo.DangKyKhoaHoc
                       SET NgayKetThucDuKien=?
                    WHERE MaDangKy=? AND MaKhoaHoc=? AND MaCoSo=?
                      AND TrangThai='DANG_HOC' AND NgayKetThucThucTe IS NULL
                    """)) {
                if (ketThuc == null) ps.setNull(1, Types.DATE);
                else ps.setDate(1, Date.valueOf(ketThuc));
                ps.setInt(2, d.maDangKy());
                ps.setInt(3, d.maKhoaHoc());
                ps.setInt(4, maCoSo);
                if (ps.executeUpdate() != 1)
                    throw new SQLException("Đăng ký đã đổi trạng thái khi tính lại ngày dự kiến");
            }
        }
    }
}
