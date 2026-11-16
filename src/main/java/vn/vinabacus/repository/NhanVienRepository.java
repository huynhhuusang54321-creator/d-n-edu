
package vn.vinabacus.repository;

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

        try (Connection conn = Database.moKetNoi();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
}
