package vn.vinabacus.repository;

import java.sql.Statement;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vn.vinabacus.config.Database;
import vn.vinabacus.model.ChiTietGiaoVienTrungTam;
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
                   nvcs.TrangThai AS TrangThaiTaiCoSo,
                   CASE WHEN tk.TrangThai = 1 AND tkcs.TrangThai = 1
                        THEN vt.MaVaiTroChu ELSE NULL END AS VaiTroTaiKhoan
            FROM dbo.NhanVien nv
            INNER JOIN dbo.NhanVienCoSo nvcs
                ON nv.MaNhanVien = nvcs.MaNhanVien
            LEFT JOIN dbo.TaiKhoan tk ON tk.MaNhanVien = nv.MaNhanVien
            LEFT JOIN dbo.TaiKhoanCoSo tkcs
                ON tkcs.MaTaiKhoan = tk.MaTaiKhoan AND tkcs.MaCoSo = nvcs.MaCoSo
            LEFT JOIN dbo.VaiTro vt ON vt.MaVaiTro = tkcs.MaVaiTro
            WHERE nvcs.MaCoSo = ?
            ORDER BY nv.HoTen
            """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

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
                            rs.getBoolean("TrangThaiTaiCoSo"),
                            rs.getString("VaiTroTaiKhoan")
                    );

                    danhSach.add(nv);
                }
            }
        }

        return danhSach;
    }

    public int themNhanVien(
            int maCoSo,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        String sqlNhanVien = """
        INSERT INTO dbo.NhanVien
        (HoTen, NgaySinh, GioiTinh, SoDienThoai, Email)
        VALUES (?, ?, ?, ?, ?)
        """;

        String sqlCoSo = """
        INSERT INTO dbo.NhanVienCoSo
        (MaNhanVien, MaCoSo)
        VALUES (?, ?)
        """;

        try (Connection conn = Database.moKetNoi()) {

            conn.setAutoCommit(false);

            try {
                int maNhanVien;

                // Them thong tin nhan vien
                try (PreparedStatement ps = conn.prepareStatement(
                        sqlNhanVien,
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, hoTen);

                    if (ngaySinh == null) {
                        ps.setDate(2, null);
                    } else {
                        ps.setDate(2, java.sql.Date.valueOf(ngaySinh));
                    }

                    ps.setString(3, gioiTinh);
                    ps.setString(4, soDienThoai);
                    ps.setString(5, email);

                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Khong lay duoc ma nhan vien"
                            );
                        }

                        maNhanVien = rs.getInt(1);
                    }
                }

                // Gan nhan vien vao co so
                try (PreparedStatement ps = conn.prepareStatement(sqlCoSo)) {

                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);

                    ps.executeUpdate();
                }

                // Luu ca hai bang
                conn.commit();

                return maNhanVien;

            } catch (SQLException e) {

                conn.rollback();
                throw e;
            }
        }
    }

    public boolean suaNhanVien(
            int maCoSo,
            int maNhanVien,
            String hoTen,
            LocalDate ngaySinh,
            String gioiTinh,
            String soDienThoai,
            String email) throws SQLException {

        String sql = """
        UPDATE dbo.NhanVien
        SET HoTen = ?,
            NgaySinh = ?,
            GioiTinh = ?,
            SoDienThoai = ?,
            Email = ?
        WHERE MaNhanVien = ?
          AND EXISTS (
              SELECT 1
              FROM dbo.NhanVienCoSo
              WHERE MaNhanVien = ?
                AND MaCoSo = ?
          )
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hoTen);

            if (ngaySinh == null) {
                ps.setDate(2, null);
            } else {
                ps.setDate(2, java.sql.Date.valueOf(ngaySinh));
            }

            ps.setString(3, gioiTinh);
            ps.setString(4, soDienThoai);
            ps.setString(5, email);

            ps.setInt(6, maNhanVien);
            ps.setInt(7, maNhanVien);
            ps.setInt(8, maCoSo);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean ngungCongTac(
            int maCoSo,
            int maNhanVien) throws SQLException {

        String sql = """
        UPDATE dbo.NhanVienCoSo
        SET TrangThai = 0
        WHERE MaNhanVien = ?
          AND MaCoSo = ?
          AND TrangThai = 1
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, maNhanVien);
            ps.setInt(2, maCoSo);

            int soDongCapNhat = ps.executeUpdate();

            return soDongCapNhat > 0;
        }
    }

    /**
     * Trung tam: khong de nhan vien ngung cong tac khi van la giao vien cua
     * lop chua dong. Khoa ban ghi NhanVienCoSo va kiem tra trong cung giao dich
     * de tranh phan cong song song chen vao giua buoc kiem tra va cap nhat.
     */
    public boolean ngungCongTacTrungTam(int maCoSo, int maNhanVien) throws SQLException {
        try (Connection conn = Database.moKetNoi()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT TrangThai FROM dbo.NhanVienCoSo WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaNhanVien=? AND MaCoSo=?
                        """)) {
                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !rs.getBoolean(1)) {
                            conn.commit();
                            return false;
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT TOP (1) TenLop
                        FROM dbo.LopTrungTam WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaGiaoVien=? AND MaCoSo=? AND TrangThai<>'DA_DONG'
                        ORDER BY MaLop
                        """)) {
                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new SQLException(
                                "Nhân viên còn phụ trách lớp '" + rs.getString(1)
                                + "'. Hãy chuyển phân công hoặc đóng lớp theo quy trình trước khi ngừng công tác.");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE dbo.NhanVienCoSo SET TrangThai=0
                        WHERE MaNhanVien=? AND MaCoSo=? AND TrangThai=1
                        """)) {
                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);
                    boolean changed = ps.executeUpdate() == 1;
                    conn.commit();
                    return changed;
                }
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Chi doc: cac lop va lich hien duoc gan cho nhan vien Trung tam. */
    public ChiTietGiaoVienTrungTam chiTietPhanCongTrungTam(int maCoSo, int maNhanVien)
            throws SQLException {
        String sqlLop = """
                SELECT l.MaLop, l.TenLop, m.TenMonHoc, k.TenKhoaHoc,
                       l.NgayKhaiGiang, l.TrangThai
                FROM dbo.LopTrungTam l
                INNER JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                INNER JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                WHERE l.MaCoSo=? AND l.MaGiaoVien=?
                ORDER BY l.MaLop DESC
                """;
        String sqlLich = """
                SELECT lh.MaLichHoc, l.MaLop, l.TenLop, lh.ThuTrongTuan,
                       lh.GioBatDau, lh.GioKetThuc, lh.TuNgay, lh.DenNgay,
                       lh.TrangThai
                FROM dbo.LichHocLop lh
                INNER JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                WHERE l.MaCoSo=? AND l.MaGiaoVien=?
                ORDER BY l.MaLop DESC, lh.ThuTrongTuan, lh.GioBatDau
                """;
        List<ChiTietGiaoVienTrungTam.Lop> lop = new ArrayList<>();
        List<ChiTietGiaoVienTrungTam.Lich> lich = new ArrayList<>();
        try (Connection conn = Database.moKetNoi()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlLop)) {
                ps.setInt(1, maCoSo);
                ps.setInt(2, maNhanVien);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date ngay = rs.getDate("NgayKhaiGiang");
                        lop.add(new ChiTietGiaoVienTrungTam.Lop(
                                rs.getInt("MaLop"), rs.getString("TenLop"),
                                rs.getString("TenMonHoc"), rs.getString("TenKhoaHoc"),
                                ngay == null ? null : ngay.toLocalDate(),
                                rs.getString("TrangThai")));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlLich)) {
                ps.setInt(1, maCoSo);
                ps.setInt(2, maNhanVien);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date den = rs.getDate("DenNgay");
                        lich.add(new ChiTietGiaoVienTrungTam.Lich(
                                rs.getInt("MaLichHoc"), rs.getInt("MaLop"),
                                rs.getString("TenLop"), rs.getInt("ThuTrongTuan"),
                                rs.getTime("GioBatDau") == null ? null
                                        : rs.getTime("GioBatDau").toLocalTime(),
                                rs.getTime("GioKetThuc") == null ? null
                                        : rs.getTime("GioKetThuc").toLocalTime(),
                                rs.getDate("TuNgay").toLocalDate(),
                                den == null ? null : den.toLocalDate(),
                                rs.getString("TrangThai")));
                    }
                }
            }
        }
        return new ChiTietGiaoVienTrungTam(lop, lich);
    }

    public boolean lamViecTroLai(
            int maCoSo,
            int maNhanVien) throws SQLException {

        String sql = """
        UPDATE dbo.NhanVienCoSo
        SET TrangThai = 1
        WHERE MaNhanVien = ?
          AND MaCoSo = ?
          AND TrangThai = 0
          AND EXISTS (
              SELECT 1
              FROM dbo.NhanVien
              WHERE MaNhanVien = ?
                AND TrangThai = 1
          )
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, maNhanVien);
            ps.setInt(2, maCoSo);
            ps.setInt(3, maNhanVien);

            int soDongCapNhat = ps.executeUpdate();

            return soDongCapNhat > 0;
        }
    }

}
