package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.LopTrungTam;
import java.time.LocalDate;
import java.sql.Types;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;

/** SQL Server: LopTrungTam -> KhoaHoc / NhanVienCoSo, rang buoc MaCoSo. */
public final class LopTrungTamRepository {
    public record GiaoVien(int maNhanVien, String hoTen) {
        @Override public String toString() { return hoTen; }
    }

    public List<LopTrungTam> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT l.MaLop, l.MaCoSo, l.MaKhoaHoc,
                       k.MaMonHoc, m.TenMonHoc, k.TenKhoaHoc,
                       l.TenLop, l.MaGiaoVien, nv.HoTen AS TenGiaoVien,
                       l.NgayKhaiGiang, l.TrangThai
                FROM dbo.LopTrungTam l
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc = l.MaKhoaHoc AND k.MaCoSo = l.MaCoSo
                INNER JOIN dbo.MonHoc m
                    ON m.MaMonHoc = k.MaMonHoc AND m.MaCoSo = k.MaCoSo
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = l.MaGiaoVien
                WHERE l.MaCoSo = ?
                ORDER BY l.MaLop DESC
                """;
        List<LopTrungTam> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object gv = rs.getObject("MaGiaoVien");
                    Date ngay = rs.getDate("NgayKhaiGiang");
                    ds.add(new LopTrungTam(
                            rs.getInt("MaLop"), rs.getInt("MaCoSo"),
                            rs.getInt("MaKhoaHoc"), rs.getInt("MaMonHoc"),
                            rs.getString("TenMonHoc"), rs.getString("TenKhoaHoc"),
                            rs.getString("TenLop"), gv == null ? null : ((Number) gv).intValue(),
                            rs.getString("TenGiaoVien"), ngay == null ? null : ngay.toLocalDate(),
                            rs.getString("TrangThai")));
                }
            }
        }
        return ds;
    }

    public List<GiaoVien> layGiaoVien(int maCoSo) throws SQLException {
        // Khong suy doan NhanVien la giao vien: day la danh sach nhan vien co so.
        // Viec loc chuyen mon/phan quyen giao vien se ket noi voi TV1/TV6 khi co du lieu.
        String sql = """
                SELECT nv.MaNhanVien, nv.HoTen
                FROM dbo.NhanVien nv
                INNER JOIN dbo.NhanVienCoSo ncs ON ncs.MaNhanVien = nv.MaNhanVien
                WHERE ncs.MaCoSo = ? AND ncs.TrangThai = 1 AND nv.TrangThai = 1
                ORDER BY nv.HoTen, nv.MaNhanVien
                """;
        List<GiaoVien> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(new GiaoVien(rs.getInt("MaNhanVien"), rs.getString("HoTen")));
                }
            }
        }
        return ds;
    }

    public int them(int maCoSo, int maKhoaHoc, String tenLop, Integer maGiaoVien,
                   LocalDate ngayKhaiGiang, String trangThai) throws SQLException {
        try (Connection con = Database.moKetNoi()) {
            return them(con, maCoSo, maKhoaHoc, tenLop, maGiaoVien,
                    ngayKhaiGiang, trangThai);
        }
    }

    /** Tạo lớp và các buổi học trong cùng giao dịch để không lưu dở dữ liệu. */
    public int themKemLich(int maCoSo, int maKhoaHoc, String tenLop,
                           Integer maGiaoVien, LocalDate ngayKhaiGiang,
                           String trangThai, List<BuoiTrongTuan> cacBuoi) throws SQLException {
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                int maLop = them(con, maCoSo, maKhoaHoc, tenLop, maGiaoVien,
                        ngayKhaiGiang, trangThai);
                new LichHocLopRepository().themNhieu(con, maCoSo, maLop,
                        cacBuoi, ngayKhaiGiang, null, "HIEU_LUC");
                con.commit();
                return maLop;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    private int them(Connection con, int maCoSo, int maKhoaHoc, String tenLop,
                     Integer maGiaoVien, LocalDate ngayKhaiGiang,
                     String trangThai) throws SQLException {
        // INSERT SELECT: khong the tao lop tu khoa/mon thuoc co so khac hay da ngung.
        // Nhan vien phai ton tai trong NhanVienCoSo dung Trung tam (hoac chua phan cong).
        String sql = """
                INSERT INTO dbo.LopTrungTam
                    (MaCoSo, MaKhoaHoc, TenLop, MaGiaoVien, NgayKhaiGiang, TrangThai)
                SELECT ?, k.MaKhoaHoc, ?, ?, ?, ?
                FROM dbo.KhoaHoc k
                INNER JOIN dbo.MonHoc m
                    ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                WHERE k.MaKhoaHoc=? AND k.MaCoSo=?
                  AND k.TrangThai=1 AND m.TrangThai=1
                  AND (? = 0 OR EXISTS (
                        SELECT 1 FROM dbo.NhanVienCoSo ncs
                        INNER JOIN dbo.NhanVien nv ON nv.MaNhanVien=ncs.MaNhanVien
                        WHERE ncs.MaNhanVien=? AND ncs.MaCoSo=?
                          AND ncs.TrangThai=1 AND nv.TrangThai=1))
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maCoSo);
            ps.setString(2, tenLop);
            if (maGiaoVien == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, maGiaoVien);
            ps.setDate(4, ngayKhaiGiang == null ? null : Date.valueOf(ngayKhaiGiang));
            ps.setString(5, trangThai);
            ps.setInt(6, maKhoaHoc);
            ps.setInt(7, maCoSo);
            ps.setInt(8, maGiaoVien == null ? 0 : maGiaoVien);
            ps.setInt(9, maGiaoVien == null ? 0 : maGiaoVien);
            ps.setInt(10, maCoSo);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Khóa học/môn học không hoạt động hoặc giáo viên không thuộc Trung tâm");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Không lấy được mã lớp vừa thêm");
    }

    public void sua(int maCoSo, int maLop, int maKhoaHoc, String tenLop,
                   Integer maGiaoVien, LocalDate ngayKhaiGiang, String trangThai)
            throws SQLException {
        // Khi giu nguyen khoa cu/giao vien cu, cho sua lop du khoa/GV da ngung.
        // Khi chuyen khoa/GV, doi tuong moi phai dang hoat dong va cung co so.
        String sql = """
                UPDATE l
                   SET l.MaKhoaHoc=?, l.TenLop=?, l.MaGiaoVien=?,
                       l.NgayKhaiGiang=?, l.TrangThai=?
                FROM dbo.LopTrungTam l
                INNER JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=? AND k.MaCoSo=?
                INNER JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                WHERE l.MaLop=? AND l.MaCoSo=?
                  AND (l.MaKhoaHoc=k.MaKhoaHoc OR (k.TrangThai=1 AND m.TrangThai=1))
                  AND (?=0 OR l.MaGiaoVien=? OR EXISTS (
                        SELECT 1 FROM dbo.NhanVienCoSo ncs
                        INNER JOIN dbo.NhanVien nv ON nv.MaNhanVien=ncs.MaNhanVien
                        WHERE ncs.MaNhanVien=? AND ncs.MaCoSo=?
                          AND ncs.TrangThai=1 AND nv.TrangThai=1))
                """;
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maKhoaHoc);
            ps.setString(2, tenLop);
            if (maGiaoVien == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, maGiaoVien);
            ps.setDate(4, ngayKhaiGiang == null ? null : Date.valueOf(ngayKhaiGiang));
            ps.setString(5, trangThai);
            ps.setInt(6, maKhoaHoc);
            ps.setInt(7, maCoSo);
            ps.setInt(8, maLop);
            ps.setInt(9, maCoSo);
            ps.setInt(10, maGiaoVien == null ? 0 : maGiaoVien);
            ps.setInt(11, maGiaoVien == null ? 0 : maGiaoVien);
            ps.setInt(12, maGiaoVien == null ? 0 : maGiaoVien);
            ps.setInt(13, maCoSo);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Không sửa được lớp: sai cơ sở, khóa học hoặc giáo viên không hợp lệ");
        }
    }
}
