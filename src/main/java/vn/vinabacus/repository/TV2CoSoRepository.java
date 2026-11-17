package vn.vinabacus.repository;
import java.sql.*;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.CoSo;
/** KHONG doan MaCoSo=1/2; lay MaCoSo thuc tu SQL Server. */
public final class TV2CoSoRepository {
    public int layMaCoSo(CoSo coSo) throws SQLException {
        String sql = "SELECT MaCoSo FROM dbo.CoSo WHERE LoaiCoSo=? AND TrangThai=1";
        try (Connection con = Database.moKetNoi(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, coSo.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Không tìm thấy cơ sở hợp lệ trong dbo.CoSo: " + coSo);
    }
}
