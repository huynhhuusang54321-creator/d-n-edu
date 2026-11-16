
package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import vn.vinabacus.config.Database;
import vn.vinabacus.model.LichSuKho;

public class LichSuKhoRepository {

    public List<LichSuKho> timTheoCoSo(int maCoSo)
            throws SQLException {

        List<LichSuKho> danhSach = new ArrayList<>();

        String sql = """
            SELECT
                pk.MaPhieuKho,
                pk.MaCoSo,
                ct.MaHangHoa,
                hh.MaSanPham,
                hh.TenHangHoa,
                hh.DonViTinh,
                pk.LoaiPhieu,
                ct.SoLuong,
                pk.NgayLap,
                pk.TrangThai,
                COALESCE(ct.GhiChu, pk.GhiChu) AS GhiChu

            FROM dbo.PhieuKho pk

            INNER JOIN dbo.ChiTietPhieuKho ct
                ON pk.MaPhieuKho = ct.MaPhieuKho
                AND pk.MaCoSo = ct.MaCoSo

            INNER JOIN dbo.HangHoa hh
                ON ct.MaHangHoa = hh.MaHangHoa
                AND ct.MaCoSo = hh.MaCoSo

            WHERE pk.MaCoSo = ?

            ORDER BY
                pk.NgayLap DESC,
                pk.MaPhieuKho DESC,
                ct.MaChiTiet DESC
            """;

        try (Connection conn = Database.moKetNoi();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setInt(1, maCoSo);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    LichSuKho lichSu = new LichSuKho(

                        rs.getInt("MaPhieuKho"),

                        rs.getInt("MaCoSo"),

                        rs.getInt("MaHangHoa"),

                        rs.getString("MaSanPham"),

                        rs.getString("TenHangHoa"),

                        rs.getString("DonViTinh"),

                        rs.getString("LoaiPhieu"),

                        rs.getBigDecimal("SoLuong"),

                        rs.getTimestamp("NgayLap")
                            .toLocalDateTime(),

                        rs.getString("TrangThai"),

                        rs.getString("GhiChu")
                    );

                    danhSach.add(lichSu);
                }
            }
        }

        return danhSach;
    }
}
