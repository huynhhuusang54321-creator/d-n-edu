package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.LichHocLop;

/** SQL Server: LichHocLop -> LopTrungTam -> KhoaHoc -> MonHoc, gioi han MaCoSo. */
public final class LichHocLopRepository {

    /** Một dòng lịch cho một thứ; nhiều dòng có thể thuộc cùng một lớp. */
    public record BuoiTrongTuan(int thu, LocalTime batDau, LocalTime ketThuc) { }


    public List<LichHocLop> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT lh.MaLichHoc, lh.MaLop, k.MaKhoaHoc, m.MaMonHoc,
                       m.TenMonHoc, k.TenKhoaHoc, l.TenLop,
                       lh.ThuTrongTuan, lh.GioBatDau, lh.GioKetThuc,
                       lh.TuNgay, lh.DenNgay, lh.TrangThai
                FROM dbo.LichHocLop lh
                INNER JOIN dbo.LopTrungTam l ON l.MaLop = lh.MaLop
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc = l.MaKhoaHoc AND k.MaCoSo = l.MaCoSo
                INNER JOIN dbo.MonHoc m
                    ON m.MaMonHoc = k.MaMonHoc AND m.MaCoSo = k.MaCoSo
                WHERE l.MaCoSo = ?
                ORDER BY lh.TuNgay DESC, lh.MaLichHoc DESC
                """;
        List<LichHocLop> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time bd = rs.getTime("GioBatDau");
                    Time kt = rs.getTime("GioKetThuc");
                    Date tu = rs.getDate("TuNgay");
                    Date den = rs.getDate("DenNgay");
                    ds.add(new LichHocLop(
                            rs.getInt("MaLichHoc"), rs.getInt("MaLop"),
                            rs.getInt("MaKhoaHoc"), rs.getInt("MaMonHoc"),
                            rs.getString("TenMonHoc"), rs.getString("TenKhoaHoc"),
                            rs.getString("TenLop"), rs.getInt("ThuTrongTuan"),
                            bd == null ? null : bd.toLocalTime(),
                            kt == null ? null : kt.toLocalTime(),
                            tu.toLocalDate(), den == null ? null : den.toLocalDate(),
                            rs.getString("TrangThai")));
                }
            }
        }
        return ds;
    }

    /** Thêm 1-7 thứ học trong cùng một giao dịch, tránh lưu dở khi một thứ bị trùng. */
    public List<Integer> themNhieu(int maCoSo, int maLop, List<BuoiTrongTuan> cacBuoi,
                               LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.LichHocLop
                    (MaLop, ThuTrongTuan, GioBatDau, GioKetThuc, TuNgay, DenNgay, TrangThai)
                SELECT l.MaLop, ?, ?, ?, ?, ?, ?
                FROM dbo.LopTrungTam l
                WHERE l.MaLop = ? AND l.MaCoSo = ? AND l.TrangThai <> 'DA_DONG'
                """;
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                List<Integer> ids = new ArrayList<>();
                for (BuoiTrongTuan b : cacBuoi) {
                    kiemTraKhongTrung(con, maCoSo, -1, maLop, b.thu(), b.batDau(),
                            b.ketThuc(), tuNgay, denNgay, trangThai);
                    try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, b.thu());
                        setGio(ps, 2, b.batDau());
                        setGio(ps, 3, b.ketThuc());
                        ps.setDate(4, Date.valueOf(tuNgay));
                        setNgay(ps, 5, denNgay);
                        ps.setString(6, trangThai);
                        ps.setInt(7, maLop);
                        ps.setInt(8, maCoSo);
                        if (ps.executeUpdate() != 1)
                            throw new SQLException("Không tìm thấy lớp đang sử dụng thuộc Trung tâm");
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Không nhận được mã lịch sau khi thêm");
                            ids.add(keys.getInt(1));
                        }
                    }
                }
                con.commit();
                return ids;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public int them(int maCoSo, int maLop, int thu,
                   LocalTime batDau, LocalTime ketThuc,
                   LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        // INSERT SELECT dam bao MaLop la lop thuoc TRUNG_TAM hien tai.
        String sql = """
                INSERT INTO dbo.LichHocLop
                    (MaLop, ThuTrongTuan, GioBatDau, GioKetThuc, TuNgay, DenNgay, TrangThai)
                SELECT l.MaLop, ?, ?, ?, ?, ?, ?
                FROM dbo.LopTrungTam l
                WHERE l.MaLop = ? AND l.MaCoSo = ?
                """;
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                kiemTraKhongTrung(con, maCoSo, -1, maLop, thu, batDau,
                        ketThuc, tuNgay, denNgay, trangThai);
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, thu);
                    setGio(ps, 2, batDau);
                    setGio(ps, 3, ketThuc);
                    ps.setDate(4, Date.valueOf(tuNgay));
                    setNgay(ps, 5, denNgay);
                    ps.setString(6, trangThai);
                    ps.setInt(7, maLop);
                    ps.setInt(8, maCoSo);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Lớp học không thuộc cơ sở Trung tâm");
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int maMoi = rs.getInt(1);
                            con.commit();
                            return maMoi;
                        }
                    }
                }
                throw new SQLException("Không lấy được mã lịch học vừa thêm");
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public void sua(int maCoSo, int maLichHoc, int thu,
                    LocalTime batDau, LocalTime ketThuc,
                    LocalDate tuNgay, LocalDate denNgay, String trangThai)
            throws SQLException {
        // Khong sua MaLop tren lich co san, vi BuoiHoc co the dang tham chieu lich+lop.
        String sql = """
                UPDATE lh
                   SET lh.ThuTrongTuan=?, lh.GioBatDau=?, lh.GioKetThuc=?,
                       lh.TuNgay=?, lh.DenNgay=?, lh.TrangThai=?
                FROM dbo.LichHocLop lh
                INNER JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                WHERE lh.MaLichHoc=? AND l.MaCoSo=?
                """;
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                // Lay lop trong cung transaction va khong cho thay doi ma lop.
                int maLop = -1;
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT lh.MaLop FROM dbo.LichHocLop lh
                        INNER JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                        WHERE lh.MaLichHoc=? AND l.MaCoSo=?
                        """)) {
                    ps.setInt(1, maLichHoc);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) maLop = rs.getInt(1);
                    }
                }
                if (maLop <= 0) throw new SQLException("Không tìm thấy lịch học thuộc Trung tâm");
                kiemTraKhongTrung(con, maCoSo, maLichHoc, maLop, thu,
                        batDau, ketThuc, tuNgay, denNgay, trangThai);
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, thu);
                    setGio(ps, 2, batDau);
                    setGio(ps, 3, ketThuc);
                    ps.setDate(4, Date.valueOf(tuNgay));
                    setNgay(ps, 5, denNgay);
                    ps.setString(6, trangThai);
                    ps.setInt(7, maLichHoc);
                    ps.setInt(8, maCoSo);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không cập nhật được lịch học");
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /** Chan hai lich HIEU_LUC trung thu, giao thoi gian va khoang ngay trong cung lop.
     * CAST tham so JDBC thanh TIME de tranh JDBC setTime gui duoi dang DATETIME. */
    private static void kiemTraKhongTrung(Connection con, int maCoSo, int maHienTai,
                                          int maLop, int thu, LocalTime batDau,
                                          LocalTime ketThuc, LocalDate tuNgay,
                                          LocalDate denNgay, String trangThai) throws SQLException {
        if (!"HIEU_LUC".equals(trangThai)) return;
        String sql = """
                SELECT TOP 1 lh.MaLichHoc
                FROM dbo.LichHocLop lh WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.LopTrungTam l ON l.MaLop = lh.MaLop
                WHERE l.MaCoSo = ? AND lh.MaLop = ? AND lh.MaLichHoc <> ?
                  AND lh.TrangThai = 'HIEU_LUC' AND lh.ThuTrongTuan = ?
                  AND lh.TuNgay <= ?
                  AND (lh.DenNgay IS NULL OR lh.DenNgay >= ?)
                  AND (lh.GioBatDau IS NULL OR lh.GioKetThuc IS NULL
                       OR ? IS NULL OR ? IS NULL
                       OR (lh.GioBatDau < CAST(? AS time(0))
                           AND lh.GioKetThuc > CAST(? AS time(0))))
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            ps.setInt(2, maLop);
            ps.setInt(3, maHienTai);
            ps.setInt(4, thu);
            ps.setDate(5, Date.valueOf(denNgay == null ? LocalDate.of(9999, 12, 31) : denNgay));
            ps.setDate(6, Date.valueOf(tuNgay));
            setGio(ps, 7, batDau);
            setGio(ps, 8, ketThuc);
            setGio(ps, 9, ketThuc);
            setGio(ps, 10, batDau);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException(
                        "Lịch trùng thứ, khoảng ngày và giờ với lịch đang hiệu lực trong lớp này");
            }
        }
    }

    private static void setGio(PreparedStatement ps, int i, LocalTime t) throws SQLException {
        if (t == null) ps.setNull(i, Types.TIME);
        else ps.setTime(i, Time.valueOf(t));
    }
    private static void setNgay(PreparedStatement ps, int i, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(i, Types.DATE);
        else ps.setDate(i, Date.valueOf(d));
    }
}
