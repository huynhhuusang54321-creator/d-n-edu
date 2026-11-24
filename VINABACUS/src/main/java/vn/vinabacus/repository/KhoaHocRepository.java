package vn.vinabacus.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.KhoaHoc;

/** Chuyen truy van KhoaHoc, luon khoa theo MaCoSo de tranh lan co so. */
public final class KhoaHocRepository {

    public List<KhoaHoc> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT k.MaKhoaHoc, k.MaCoSo, k.MaMonHoc, m.TenMonHoc,
                       k.TenKhoaHoc, k.TongSoBuoi, k.HocPhiMacDinh, k.TrangThai
                FROM dbo.KhoaHoc k
                INNER JOIN dbo.MonHoc m
                  ON m.MaMonHoc = k.MaMonHoc AND m.MaCoSo = k.MaCoSo
                WHERE k.MaCoSo = ?
                ORDER BY k.MaKhoaHoc DESC
                """;
        List<KhoaHoc> ketQua = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ketQua.add(new KhoaHoc(
                            rs.getInt("MaKhoaHoc"), rs.getInt("MaCoSo"),
                            rs.getInt("MaMonHoc"), rs.getString("TenMonHoc"),
                            rs.getString("TenKhoaHoc"), rs.getInt("TongSoBuoi"),
                            rs.getBigDecimal("HocPhiMacDinh"), rs.getBoolean("TrangThai")));
                }
            }
        }
        return ketQua;
    }

    public int them(int maCoSo, int maMonHoc, String tenKhoaHoc,
                   int tongSoBuoi, BigDecimal hocPhiMacDinh, boolean trangThai)
            throws SQLException {
        // INSERT SELECT bao dam MonHoc thuoc dung co so va dang hoat dong.
        String sql = """
                INSERT INTO dbo.KhoaHoc
                  (MaCoSo, MaMonHoc, TenKhoaHoc, TongSoBuoi, HocPhiMacDinh, TrangThai)
                SELECT ?, m.MaMonHoc, ?, ?, ?, ?
                FROM dbo.MonHoc m
                WHERE m.MaMonHoc = ? AND m.MaCoSo = ? AND m.TrangThai = 1
                """;
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maCoSo);
            ps.setString(2, tenKhoaHoc);
            ps.setInt(3, tongSoBuoi);
            ps.setBigDecimal(4, hocPhiMacDinh);
            ps.setBoolean(5, trangThai);
            ps.setInt(6, maMonHoc);
            ps.setInt(7, maCoSo);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Mon hoc khong ton tai, khong hoat dong hoac sai co so");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Khong lay duoc ma khoa hoc vua tao");
    }

    public void sua(int maCoSo, int maKhoaHoc, int maMonHoc, String tenKhoaHoc,
                   int tongSoBuoi, BigDecimal hocPhiMacDinh, boolean trangThai)
            throws SQLException {
        // Cho sua khoa thuoc mon hoc da ngung su dung NEU giu nguyen mon cu.
        // Khi doi sang mon khac, mon moi phai dang hoat dong.
        String sql = """
                UPDATE k
                SET k.MaMonHoc = ?, k.TenKhoaHoc = ?, k.TongSoBuoi = ?,
                    k.HocPhiMacDinh = ?, k.TrangThai = ?
                FROM dbo.KhoaHoc k
                INNER JOIN dbo.MonHoc m
                  ON m.MaMonHoc = ? AND m.MaCoSo = ?
                WHERE k.MaKhoaHoc = ? AND k.MaCoSo = ?
                  AND (m.TrangThai = 1 OR m.MaMonHoc = k.MaMonHoc)
                """;
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maMonHoc);
            ps.setString(2, tenKhoaHoc);
            ps.setInt(3, tongSoBuoi);
            ps.setBigDecimal(4, hocPhiMacDinh);
            ps.setBoolean(5, trangThai);
            ps.setInt(6, maMonHoc);
            ps.setInt(7, maCoSo);
            ps.setInt(8, maKhoaHoc);
            ps.setInt(9, maCoSo);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong tim thay khoa hoc hoac mon hoc khong hop le");
            }
        }
    }
}
