package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.ChuongTrinhGiamGia;

/** Chỉ thao tác ưu đãi thuộc cơ sở Trung tâm; không sửa cấu trúc SQL. */
public final class ChuongTrinhGiamGiaRepository {
    public static final String THIEU_SCHEMA = "CSDL chưa có cấu trúc Giai đoạn 3. "
            + "Hãy xem script migration đề xuất trước khi dùng chương trình giảm giá.";

    public boolean sanSang() throws SQLException {
        try (Connection c = Database.moKetNoi()) { return sanSang(c); }
    }

    public static boolean sanSang(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT CASE WHEN COL_LENGTH('dbo.ChuongTrinhGiamGia','MoTa') IS NOT NULL
                           AND COL_LENGTH('dbo.ChuongTrinhGiamGia','ChoPhepKetHop') IS NOT NULL
                           AND COL_LENGTH('dbo.ChuongTrinhGiamGia','ApDungTatCaMon') IS NOT NULL
                           AND OBJECT_ID('dbo.GiamGiaMonHoc','U') IS NOT NULL
                           AND OBJECT_ID('dbo.ChiTietGiamGiaDangKy','U') IS NOT NULL
                            THEN 1 ELSE 0 END
                """); ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 1;
        }
    }

    public List<ChuongTrinhGiamGia> layDanhSach(int maCoSo) throws SQLException {
        try (Connection c = Database.moKetNoi()) {
            canSchema(c);
            return layDanhSach(c, maCoSo, false);
        }
    }

    public List<ChuongTrinhGiamGia> layDanhSach(Connection c, int maCoSo,
            boolean khoaDong) throws SQLException {
        canSchema(c);
        if (khoaDong) {
            try (PreparedStatement lock = c.prepareStatement("""
                    SELECT MaChuongTrinh FROM dbo.ChuongTrinhGiamGia WITH (UPDLOCK,HOLDLOCK)
                    WHERE MaCoSo=? AND PhamViApDung='KHOA_HOC'
                    ORDER BY MaChuongTrinh
                    """)) {
                lock.setInt(1, maCoSo);
                try (ResultSet ignored = lock.executeQuery()) {
                    while (ignored.next()) { /* giữ khóa đến lúc commit */ }
                }
            }
        }
        Map<Integer, Set<Integer>> mon = danhSachLienKet(c, "dbo.GiamGiaMonHoc",
                "MaMonHoc", maCoSo);
        Map<Integer, Set<Integer>> khoa = danhSachLienKet(c, "dbo.GiamGiaKhoaHoc",
                "MaKhoaHoc", maCoSo);
        String sql = """
                SELECT MaChuongTrinh, MaCoSo, TenChuongTrinh, MoTa, LoaiGiam,
                       GiaTriGiam, TuNgay, DenNgay, ApDungTatCaMon,
                       ChoPhepKetHop, TrangThai
                FROM dbo.ChuongTrinhGiamGia %s
                WHERE MaCoSo=? AND PhamViApDung='KHOA_HOC'
                ORDER BY MaChuongTrinh DESC
                """.formatted(khoaDong ? "WITH (UPDLOCK, HOLDLOCK)" : "");
        List<ChuongTrinhGiamGia> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("MaChuongTrinh");
                    out.add(new ChuongTrinhGiamGia(id, rs.getInt("MaCoSo"),
                            rs.getString("TenChuongTrinh"), rs.getString("MoTa"),
                            rs.getString("LoaiGiam"), rs.getBigDecimal("GiaTriGiam"),
                            rs.getDate("TuNgay").toLocalDate(),
                            rs.getDate("DenNgay").toLocalDate(),
                            rs.getBoolean("ApDungTatCaMon"),
                            rs.getBoolean("ChoPhepKetHop"), rs.getBoolean("TrangThai"),
                            mon.getOrDefault(id, Set.of()), khoa.getOrDefault(id, Set.of())));
                }
            }
        }
        return out;
    }

    private static Map<Integer, Set<Integer>> danhSachLienKet(Connection c,
            String bang, String cot, int maCoSo) throws SQLException {
        // Tên bảng/cột chỉ đến từ hằng số nội bộ, không nhận từ giao diện.
        Map<Integer, Set<Integer>> out = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT MaChuongTrinh," + cot + " FROM " + bang + " WHERE MaCoSo=?")) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.computeIfAbsent(rs.getInt(1), ignored ->
                        new HashSet<>()).add(rs.getInt(2));
            }
        }
        return out;
    }

    public int luu(ChuongTrinhGiamGia ct) throws SQLException {
        try (Connection c = Database.moKetNoi()) {
            canSchema(c);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                int id = ct.maChuongTrinh();
                if (id == 0) {
                    try (PreparedStatement ps = c.prepareStatement("""
                            INSERT dbo.ChuongTrinhGiamGia
                                (MaCoSo,TenChuongTrinh,MoTa,PhamViApDung,LoaiGiam,
                                 GiaTriGiam,TuNgay,DenNgay,ApDungTatCaMon,
                                 ChoPhepKetHop,TrangThai)
                            VALUES (?,?,?,'KHOA_HOC',?,?,?,?,?,?,?)
                            """, Statement.RETURN_GENERATED_KEYS)) {
                        dien(ps, ct, 1);
                        ps.executeUpdate();
                        try (ResultSet key = ps.getGeneratedKeys()) {
                            if (!key.next()) throw new SQLException("Không lấy được mã chương trình mới");
                            id = key.getInt(1);
                        }
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement("""
                            UPDATE dbo.ChuongTrinhGiamGia SET TenChuongTrinh=?, MoTa=?,
                                LoaiGiam=?, GiaTriGiam=?, TuNgay=?, DenNgay=?,
                                ApDungTatCaMon=?, ChoPhepKetHop=?, TrangThai=?
                            WHERE MaChuongTrinh=? AND MaCoSo=? AND PhamViApDung='KHOA_HOC'
                            """)) {
                        ps.setString(1, ct.ten().trim()); ps.setString(2, ct.moTa());
                        ps.setString(3, ct.loaiGiam()); ps.setBigDecimal(4, ct.giaTri());
                        ps.setDate(5, Date.valueOf(ct.tuNgay()));
                        ps.setDate(6, Date.valueOf(ct.denNgay()));
                        ps.setBoolean(7, ct.apDungTatCaMon());
                        ps.setBoolean(8, ct.choPhepKetHop()); ps.setBoolean(9, ct.kichHoat());
                        ps.setInt(10, id); ps.setInt(11, ct.maCoSo());
                        if (ps.executeUpdate() != 1) throw new SQLException(
                                "Không tìm thấy chương trình thuộc Trung tâm");
                    }
                    xoaLienKet(c, "dbo.GiamGiaMonHoc", id, ct.maCoSo());
                    xoaLienKet(c, "dbo.GiamGiaKhoaHoc", id, ct.maCoSo());
                }
                if (!ct.apDungTatCaMon()) {
                    for (int maMon : ct.maMonHoc()) {
                        try (PreparedStatement ps = c.prepareStatement("""
                                INSERT dbo.GiamGiaMonHoc(MaChuongTrinh,MaMonHoc,MaCoSo)
                                SELECT ?,m.MaMonHoc,? FROM dbo.MonHoc m
                                WHERE m.MaMonHoc=? AND m.MaCoSo=?
                                """)) {
                            ps.setInt(1,id); ps.setInt(2,ct.maCoSo());
                            ps.setInt(3,maMon); ps.setInt(4,ct.maCoSo());
                            if (ps.executeUpdate()!=1) throw new SQLException("Môn áp dụng không thuộc Trung tâm");
                        }
                    }
                    for (int maKhoa : ct.maKhoaHoc()) {
                        try (PreparedStatement ps = c.prepareStatement("""
                                INSERT dbo.GiamGiaKhoaHoc(MaChuongTrinh,MaKhoaHoc,MaCoSo)
                                SELECT ?,k.MaKhoaHoc,? FROM dbo.KhoaHoc k
                                WHERE k.MaKhoaHoc=? AND k.MaCoSo=? AND k.MaMonHoc IN
                                    (SELECT MaMonHoc FROM dbo.GiamGiaMonHoc
                                     WHERE MaChuongTrinh=? AND MaCoSo=?)
                                """)) {
                            ps.setInt(1,id); ps.setInt(2,ct.maCoSo());
                            ps.setInt(3,maKhoa); ps.setInt(4,ct.maCoSo());
                            ps.setInt(5,id); ps.setInt(6,ct.maCoSo());
                            if (ps.executeUpdate()!=1) throw new SQLException("Khóa áp dụng không thuộc môn đã chọn");
                        }
                    }
                }
                c.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }

    private static void dien(PreparedStatement ps, ChuongTrinhGiamGia ct,
            int start) throws SQLException {
        ps.setInt(start, ct.maCoSo()); ps.setString(start+1, ct.ten().trim());
        ps.setString(start+2, ct.moTa()); ps.setString(start+3, ct.loaiGiam());
        ps.setBigDecimal(start+4, ct.giaTri());
        ps.setDate(start+5, Date.valueOf(ct.tuNgay()));
        ps.setDate(start+6, Date.valueOf(ct.denNgay()));
        ps.setBoolean(start+7, ct.apDungTatCaMon());
        ps.setBoolean(start+8, ct.choPhepKetHop());
        ps.setBoolean(start+9, ct.kichHoat());
    }

    private static void xoaLienKet(Connection c, String bang, int id,
            int maCoSo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + bang + " WHERE MaChuongTrinh=? AND MaCoSo=?")) {
            ps.setInt(1,id); ps.setInt(2,maCoSo); ps.executeUpdate();
        }
    }

    public void doiTrangThai(int maCoSo, int id, boolean kichHoat) throws SQLException {
        try (Connection c = Database.moKetNoi()) {
            canSchema(c);
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE dbo.ChuongTrinhGiamGia SET TrangThai=?
                    WHERE MaChuongTrinh=? AND MaCoSo=? AND PhamViApDung='KHOA_HOC'
                    """)) {
                ps.setBoolean(1,kichHoat); ps.setInt(2,id); ps.setInt(3,maCoSo);
                if (ps.executeUpdate()!=1) throw new SQLException("Không tìm thấy chương trình thuộc Trung tâm");
            }
        }
    }

    private static void canSchema(Connection c) throws SQLException {
        if (!sanSang(c)) throw new SQLException(THIEU_SCHEMA);
    }
}
