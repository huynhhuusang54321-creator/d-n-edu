
package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.CoSo;

public class CoSoRepository {

    public int timMaCoSo(CoSo coSo) throws SQLException {

        Objects.requireNonNull(coSo);

        String sql = """
            SELECT MaCoSo
            FROM dbo.CoSo
            WHERE LoaiCoSo = ?
              AND TrangThai = 1
            """;

        try (Connection conn = Database.moKetNoi();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, coSo.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("MaCoSo");
                }
            }
        }

        throw new SQLException(
            "Không tìm thấy cơ sở: " + coSo.name()
        );
    }
}
