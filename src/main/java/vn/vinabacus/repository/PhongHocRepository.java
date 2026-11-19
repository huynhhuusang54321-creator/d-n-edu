package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.PhongHoc;

/** Truy vấn các phòng đang hoạt động thuộc một cơ sở. */
public final class PhongHocRepository {
    public List<PhongHoc> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT MaPhong, MaCoSo, TenPhong, TrangThai
                FROM dbo.PhongHoc
                WHERE MaCoSo=? AND TrangThai=1
                ORDER BY MaPhong
                """;
        List<PhongHoc> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ds.add(new PhongHoc(rs.getInt("MaPhong"),
                        rs.getInt("MaCoSo"), rs.getString("TenPhong"),
                        rs.getBoolean("TrangThai")));
            }
        }
        return ds;
    }
}
