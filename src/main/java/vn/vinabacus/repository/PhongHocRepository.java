package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.service.PhongHocService;

/** CRUD phong hoc: giu nguyen MaPhong identity, khong xoa vat ly. */
public final class PhongHocRepository {
    /** Một lớp từng được xếp lịch định kỳ hoặc buổi học tại phòng này. */
    public record LopSuDung(int maPhong, int maLop, String tenLop,
                            String tenMonHoc, String tenKhoaHoc, String tenGiaoVien,
                            int siSoDangHoc, Integer siSoToiDa, String trangThaiLop,
                            boolean dangSuDung) { }

    public List<PhongHoc> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT MaPhong, MaCoSo, TenPhong, SucChua, TrangThai, GhiChu
                FROM dbo.PhongHoc WHERE MaCoSo = ?
                ORDER BY TenPhong, MaPhong
                """;
        List<PhongHoc> ketQua = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ketQua.add(new PhongHoc(
                        rs.getInt("MaPhong"), rs.getInt("MaCoSo"),
                        rs.getString("TenPhong"), rs.getInt("SucChua"),
                        rs.getString("TrangThai"), rs.getString("GhiChu")));
            }
        }
        return ketQua;
    }

    /** Kể cả lịch cũ để xem lịch sử; cờ dangSuDung chỉ tính lịch còn hiệu lực/tương lai. */
    public List<LopSuDung> layLopSuDung(int maCoSo) throws SQLException {
        String sql = """
                SELECT p.MaPhong, l.MaLop, l.TenLop, m.TenMonHoc, k.TenKhoaHoc,
                       nv.HoTen AS TenGiaoVien, l.SiSoToiDa, l.TrangThai AS TrangThaiLop,
                       (SELECT COUNT(DISTINCT dk.MaHocVien)
                          FROM dbo.XepLopTrungTam x
                          JOIN dbo.DangKyKhoaHoc dk
                            ON dk.MaDangKy=x.MaDangKy AND dk.MaKhoaHoc=x.MaKhoaHoc
                           AND dk.MaCoSo=x.MaCoSo
                         WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo
                           AND x.TrangThai='HIEU_LUC'
                           AND x.TuNgay<=CONVERT(date,GETDATE())
                           AND (x.DenNgay IS NULL OR x.DenNgay>=CONVERT(date,GETDATE()))) AS SiSoDangHoc,
                       CASE WHEN EXISTS (
                            SELECT 1 FROM dbo.LichHocLop lh
                             WHERE lh.MaLop=l.MaLop AND lh.MaPhong=p.MaPhong
                               AND lh.TrangThai='HIEU_LUC'
                               AND (lh.DenNgay IS NULL OR lh.DenNgay>=CONVERT(date,GETDATE())))
                            OR EXISTS (
                            SELECT 1 FROM dbo.BuoiHoc b
                             WHERE b.MaLop=l.MaLop AND b.MaPhong=p.MaPhong
                               AND b.TrangThai='DU_KIEN'
                               AND b.NgayHoc>=CONVERT(date,GETDATE()))
                            THEN CAST(1 AS bit) ELSE CAST(0 AS bit) END AS DangSuDung
                  FROM dbo.PhongHoc p
                  JOIN dbo.LopTrungTam l ON l.MaCoSo=p.MaCoSo
                  JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                  JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                  LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien=l.MaGiaoVien
                 WHERE p.MaCoSo=? AND (
                       EXISTS (SELECT 1 FROM dbo.LichHocLop lh
                                WHERE lh.MaLop=l.MaLop AND lh.MaPhong=p.MaPhong)
                       OR EXISTS (SELECT 1 FROM dbo.BuoiHoc b
                                   WHERE b.MaLop=l.MaLop AND b.MaPhong=p.MaPhong))
                 ORDER BY p.MaPhong, l.MaLop
                """;
        List<LopSuDung> ketQua = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Number gioiHan = (Number) rs.getObject("SiSoToiDa");
                    ketQua.add(new LopSuDung(rs.getInt("MaPhong"), rs.getInt("MaLop"),
                            rs.getString("TenLop"), rs.getString("TenMonHoc"),
                            rs.getString("TenKhoaHoc"), rs.getString("TenGiaoVien"),
                            rs.getInt("SiSoDangHoc"), gioiHan == null ? null : gioiHan.intValue(),
                            rs.getString("TrangThaiLop"), rs.getBoolean("DangSuDung")));
                }
            }
        }
        return ketQua;
    }

    public int them(int maCoSo, String ma, String ten, int sucChua,
                    String trangThai, String ghiChu) throws SQLException {
        String sql = """
                INSERT INTO dbo.PhongHoc (MaCoSo, TenPhong, SucChua, TrangThai, GhiChu)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                kiemTraMaKhongTrung(con, maCoSo, -1, ma);
                int id;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, maCoSo);
                    ps.setString(2, PhongHocService.tenLuu(ma, ten));
                    ps.setInt(3, sucChua);
                    ps.setString(4, trangThai);
                    ps.setString(5, ghiChu);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không thể thêm phòng học");
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Không nhận được mã phòng mới");
                        id = rs.getInt(1);
                    }
                }
                con.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public void sua(int maCoSo, int id, String ma, String ten, int sucChua,
                    String trangThai, String ghiChu) throws SQLException {
        String sql = """
                UPDATE dbo.PhongHoc
                SET TenPhong=?, SucChua=?, TrangThai=?, GhiChu=?
                WHERE MaPhong=? AND MaCoSo=?
                """;
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                PhongHoc cu = layMotPhongKhoa(con, maCoSo, id);
                kiemTraMaKhongTrung(con, maCoSo, id, ma);
                kiemTraAnhHuong(con, maCoSo, id, cu.sucChua(), sucChua,
                        cu.trangThai(), trangThai);
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, PhongHocService.tenLuu(ma, ten));
                    ps.setInt(2, sucChua);
                    ps.setString(3, trangThai);
                    ps.setString(4, ghiChu);
                    ps.setInt(5, id);
                    ps.setInt(6, maCoSo);
                    if (ps.executeUpdate() != 1) throw new SQLException("Phòng học không còn tồn tại");
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public void doiTrangThai(int maCoSo, int id, String trangThai) throws SQLException {
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                PhongHoc cu = layMotPhongKhoa(con, maCoSo, id);
                kiemTraAnhHuong(con, maCoSo, id, cu.sucChua(), cu.sucChua(),
                        cu.trangThai(), trangThai);
                try (PreparedStatement ps = con.prepareStatement("""
                        UPDATE dbo.PhongHoc SET TrangThai = ?
                        WHERE MaPhong = ? AND MaCoSo = ?
                        """)) {
                    ps.setString(1, trangThai);
                    ps.setInt(2, id);
                    ps.setInt(3, maCoSo);
                    if (ps.executeUpdate() != 1) throw new SQLException("Phòng học không còn tồn tại");
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    private static PhongHoc layMotPhongKhoa(Connection con, int maCoSo, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT MaPhong, MaCoSo, TenPhong, SucChua, TrangThai, GhiChu
                FROM dbo.PhongHoc WITH (UPDLOCK, HOLDLOCK)
                WHERE MaCoSo = ? AND MaPhong = ?
                """)) {
            ps.setInt(1, maCoSo);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không tìm thấy phòng học thuộc Trung tâm");
                return new PhongHoc(rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getInt(4), rs.getString(5), rs.getString(6));
            }
        }
    }

    private static void kiemTraMaKhongTrung(Connection con, int maCoSo, int boQuaId,
                                            String ma) throws SQLException {
        // Lock theo MaCoSo duoi SERIALIZABLE de hai thao tac khong tao trung ma hien thi.
        String sql = """
                SELECT MaPhong, TenPhong FROM dbo.PhongHoc WITH (UPDLOCK, HOLDLOCK)
                WHERE MaCoSo = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt(1) == boQuaId) continue;
                    String daCo = PhongHocService.maHienThi(new PhongHoc(
                            rs.getInt(1), maCoSo, rs.getString(2), 1,
                            PhongHocService.HOAT_DONG));
                    if (daCo.equalsIgnoreCase(ma.trim()))
                        throw new SQLException("Mã phòng " + ma.toUpperCase(Locale.ROOT)
                                + " đã tồn tại trong Trung tâm");
                }
            }
        }
    }

    /**
     * Khong tat phong dang co lich tuong lai; khong giam suc chua thap hon
     * si so thuc te/gioi han si so cua lop da duoc gan vao phong.
     */
    private static void kiemTraAnhHuong(Connection con, int maCoSo, int maPhong,
                                        int sucChuaCu, int sucChuaMoi,
                                        String trangThaiCu, String trangThaiMoi) throws SQLException {
        boolean khoaPhong = PhongHocService.HOAT_DONG.equals(trangThaiCu)
                && !PhongHocService.HOAT_DONG.equals(trangThaiMoi);
        boolean giamCho = sucChuaMoi < sucChuaCu;
        if (!khoaPhong && !giamCho) return;

        String sql = """
                SELECT DISTINCT l.MaLop, l.TenLop, ISNULL(l.SiSoToiDa, 0) AS SiSoToiDa,
                       (SELECT COUNT(DISTINCT dk.MaHocVien)
                        FROM dbo.XepLopTrungTam x
                        INNER JOIN dbo.DangKyKhoaHoc dk
                             ON dk.MaDangKy = x.MaDangKy
                            AND dk.MaKhoaHoc = x.MaKhoaHoc
                            AND dk.MaCoSo = x.MaCoSo
                        WHERE x.MaLop = l.MaLop AND x.MaCoSo = l.MaCoSo
                          AND x.TrangThai = 'HIEU_LUC'
                          AND (x.DenNgay IS NULL OR x.DenNgay >= CONVERT(date, GETDATE()))) AS SiSoDangHoc
                FROM dbo.LopTrungTam l WITH (HOLDLOCK)
                WHERE l.MaCoSo = ? AND (
                    EXISTS (SELECT 1 FROM dbo.LichHocLop lh WITH (HOLDLOCK)
                            WHERE lh.MaLop = l.MaLop AND lh.MaPhong = ?
                              AND lh.TrangThai = 'HIEU_LUC'
                              AND (lh.DenNgay IS NULL OR lh.DenNgay >= CONVERT(date, GETDATE())))
                    OR EXISTS (SELECT 1 FROM dbo.BuoiHoc b WITH (HOLDLOCK)
                               WHERE b.MaLop = l.MaLop AND b.MaPhong = ?
                                 AND b.TrangThai = 'DU_KIEN'
                                 AND b.NgayHoc >= CONVERT(date, GETDATE()))
                )
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            ps.setInt(2, maPhong);
            ps.setInt(3, maPhong);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tenLop = rs.getString("TenLop");
                    if (khoaPhong)
                        throw new SQLException("Phòng đang được lớp '" + tenLop +
                                "' sử dụng trong lịch sắp tới. Hãy chuyển/hủy lịch trước khi đổi trạng thái phòng.");
                    int yeuCau = Math.max(rs.getInt("SiSoToiDa"), rs.getInt("SiSoDangHoc"));
                    if (giamCho && yeuCau > sucChuaMoi)
                        throw new SQLException("Sức chứa mới " + sucChuaMoi
                                + " thấp hơn sĩ số/giới hạn " + yeuCau
                                + " của lớp '" + tenLop + "' đang dùng phòng.");
                }
            }
        }
    }
}
