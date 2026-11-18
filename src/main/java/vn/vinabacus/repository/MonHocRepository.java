package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MonHoc;

/** Khong hardcode MaCoSo, khong sua du lieu co so khac. */
public final class MonHocRepository {

    public int layMaCoSo(CoSo coSo) throws SQLException {
        String sql = "SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo = ? AND TrangThai = 1";
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, coSo.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Khong tim thay co so dang hoat dong: " + coSo);
    }

    public List<MonHoc> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT MaMonHoc, MaCoSo, TenMonHoc, GhiChu, TrangThai
                FROM dbo.MonHoc
                WHERE MaCoSo = ?
                ORDER BY MaMonHoc DESC
                """;
        List<MonHoc> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(new MonHoc(rs.getInt("MaMonHoc"), rs.getInt("MaCoSo"),
                            rs.getString("TenMonHoc"), rs.getString("GhiChu"),
                            rs.getBoolean("TrangThai")));
                }
            }
        }
        return ds;
    }

    public int them(int maCoSo, String ten, String ghiChu, boolean trangThai)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.MonHoc (MaCoSo, TenMonHoc, GhiChu, TrangThai)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maCoSo);
            ps.setString(2, ten);
            ps.setString(3, ghiChu);
            ps.setBoolean(4, trangThai);
            if (ps.executeUpdate() != 1) throw new SQLException("Khong them duoc mon hoc");
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Khong lay duoc MaMonHoc vua tao");
    }

    public void sua(int maCoSo, int maMonHoc, String ten, String ghiChu, boolean trangThai)
            throws SQLException {
        String sql = """
                UPDATE dbo.MonHoc
                   SET TenMonHoc = ?, GhiChu = ?, TrangThai = ?
                 WHERE MaMonHoc = ? AND MaCoSo = ?
                """;
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ten);
            ps.setString(2, ghiChu);
            ps.setBoolean(3, trangThai);
            ps.setInt(4, maMonHoc);
            ps.setInt(5, maCoSo);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong tim thay mon hoc thuoc Trung tam de cap nhat");
            }
        }
    }
}
