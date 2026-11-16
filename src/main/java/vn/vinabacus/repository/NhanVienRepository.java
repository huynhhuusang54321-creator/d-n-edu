package vn.vinabacus.repository;

import java.sql.Statement;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vn.vinabacus.config.Database;
import vn.vinabacus.model.NhanVien;

public class NhanVienRepository {

    public List<NhanVien> timTheoCoSo(int maCoSo)
            throws SQLException {

        List<NhanVien> danhSach = new ArrayList<>();

        String sql = """
            SELECT nv.MaNhanVien,
                   nv.HoTen,
                   nv.NgaySinh,
                   nv.GioiTinh,
                   nv.SoDienThoai,
                   nv.Email,
                   nv.TrangThai,
                   nvcs.TrangThai AS TrangThaiTaiCoSo
            FROM dbo.NhanVien nv
            INNER JOIN dbo.NhanVienCoSo nvcs
                ON nv.MaNhanVien = nvcs.MaNhanVien
            WHERE nvcs.MaCoSo = ?
            ORDER BY nv.HoTen
            """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, maCoSo);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Date ngaySinh = rs.getDate("NgaySinh");

                    NhanVien nv = new NhanVien(
                            rs.getInt("MaNhanVien"),
                            rs.getString("HoTen"),
                            ngaySinh == null
                                    ? null
                                    : ngaySinh.toLocalDate(),
                            rs.getString("GioiTinh"),
                            rs.getString("SoDienThoai"),
                            rs.getString("Email"),
                            rs.getBoolean("TrangThai"),
                            rs.getBoolean("TrangThaiTaiCoSo")
                    );

                    danhSach.add(nv);
                }
            }
        }

        return danhSach;
    }

    public int themNhanVien(
            int maCoSo,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        String sqlNhanVien = """
        INSERT INTO dbo.NhanVien
        (HoTen, NgaySinh, GioiTinh, SoDienThoai, Email)
        VALUES (?, ?, ?, ?, ?)
        """;

        String sqlCoSo = """
        INSERT INTO dbo.NhanVienCoSo
        (MaNhanVien, MaCoSo)
        VALUES (?, ?)
        """;

        try (Connection conn = Database.moKetNoi()) {

            conn.setAutoCommit(false);

            try {
                int maNhanVien;

                // Them thong tin nhan vien
                try (PreparedStatement ps = conn.prepareStatement(
                        sqlNhanVien,
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, hoTen);

                    if (ngaySinh == null) {
                        ps.setDate(2, null);
                    } else {
                        ps.setDate(2, java.sql.Date.valueOf(ngaySinh));
                    }

                    ps.setString(3, gioiTinh);
                    ps.setString(4, soDienThoai);
                    ps.setString(5, email);

                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Khong lay duoc ma nhan vien"
                            );
                        }

                        maNhanVien = rs.getInt(1);
                    }
                }

                // Gan nhan vien vao co so
                try (PreparedStatement ps = conn.prepareStatement(sqlCoSo)) {

                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);

                    ps.executeUpdate();
                }

                // Luu ca hai bang
                conn.commit();

                return maNhanVien;

            } catch (SQLException e) {

                conn.rollback();
                throw e;
            }
        }
    }

    public boolean suaNhanVien(
            int maCoSo,
            int maNhanVien,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        String sql = """
        UPDATE dbo.NhanVien
        SET HoTen = ?,
            NgaySinh = ?,
            GioiTinh = ?,
            SoDienThoai = ?,
            Email = ?
        WHERE MaNhanVien = ?
          AND EXISTS (
              SELECT 1
              FROM dbo.NhanVienCoSo
              WHERE MaNhanVien = ?
                AND MaCoSo = ?
          )
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hoTen);

            if (ngaySinh == null) {
                ps.setDate(2, null);
            } else {
                ps.setDate(2, java.sql.Date.valueOf(ngaySinh));
            }

            ps.setString(3, gioiTinh);
            ps.setString(4, soDienThoai);
            ps.setString(5, email);

            ps.setInt(6, maNhanVien);
            ps.setInt(7, maNhanVien);
            ps.setInt(8, maCoSo);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean ngungCongTac(
            int maCoSo,
            int maNhanVien) throws SQLException {

        String sql = """
        UPDATE dbo.NhanVienCoSo
        SET TrangThai = 0
        WHERE MaNhanVien = ?
          AND MaCoSo = ?
          AND TrangThai = 1
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, maNhanVien);
            ps.setInt(2, maCoSo);

            int soDongCapNhat = ps.executeUpdate();

            return soDongCapNhat > 0;
        }
    }

    public boolean lamViecTroLai(
            int maCoSo,
            int maNhanVien) throws SQLException {

        String sql = """
        UPDATE dbo.NhanVienCoSo
        SET TrangThai = 1
        WHERE MaNhanVien = ?
          AND MaCoSo = ?
          AND TrangThai = 0
          AND EXISTS (
              SELECT 1
              FROM dbo.NhanVien
              WHERE MaNhanVien = ?
                AND TrangThai = 1
          )
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, maNhanVien);
            ps.setInt(2, maCoSo);
            ps.setInt(3, maNhanVien);

            int soDongCapNhat = ps.executeUpdate();

            return soDongCapNhat > 0;
        }
    }

}
