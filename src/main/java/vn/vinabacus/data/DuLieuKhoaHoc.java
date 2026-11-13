
package vn.vinabacus.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vn.vinabacus.config.KetNoiSQLServer;
import vn.vinabacus.model.ThongTinKhoaHoc;
import vn.vinabacus.repository.KhoaHocRepository;

public class DuLieuKhoaHoc implements KhoaHocRepository {

    @Override
    public List<ThongTinKhoaHoc> layDanhSach(int maCoSo) {

        if (maCoSo <= 0) {
            throw new IllegalArgumentException(
                "Ma co so khong hop le"
            );
        }

        List<ThongTinKhoaHoc> danhSach =
                new ArrayList<>();

        String sql = """
            SELECT MaKhoaHoc, TenKhoaHoc, TrangThai
            FROM dbo.KhoaHoc
            WHERE MaCoSo = ?
            ORDER BY TenKhoaHoc
            """;

        try (Connection conn =
                KetNoiSQLServer.getConnection()) {

            if (conn == null) {
                throw new SQLException(
                    "Khong mo duoc ket noi SQL Server"
                );
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {

                ps.setInt(1, maCoSo);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        String ma = String.valueOf(
                            rs.getInt("MaKhoaHoc")
                        );

                        String ten =
                            rs.getString("TenKhoaHoc");

                        String trangThai =
                            rs.getBoolean("TrangThai")
                            ? "Hoat dong"
                            : "Ngung hoat dong";

                        ThongTinKhoaHoc khoaHoc =
                            new ThongTinKhoaHoc(
                                ma, ten, trangThai
                            );

                        danhSach.add(khoaHoc);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Loi lay danh sach khoa hoc", e
            );
        }

        return danhSach;
    }
}
