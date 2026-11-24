package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.LopTrungTam;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Types;

/** SQL Server: LopTrungTam -> KhoaHoc / NhanVienCoSo, rang buoc MaCoSo. */
public final class LopTrungTamRepository {
    @FunctionalInterface public interface KetNoi { Connection mo() throws SQLException; }
    private final KetNoi ketNoi;
    public LopTrungTamRepository() { this(Database::moKetNoi); }
    public LopTrungTamRepository(KetNoi ketNoi) {
        this.ketNoi=Objects.requireNonNull(ketNoi);
    }
    public record GiaoVien(int maNhanVien, String hoTen) {
        @Override public String toString() { return hoTen; }
    }
    public record HocVienTrongLop(int maHocVien, String hoTen, String tenKhoaHoc,
                                 LocalDate tuNgay, LocalDate denNgay,
                                 String trangThai) { }
    public record LichSuPhanCong(long maPhanCong, String giaoVienTruoc,
                                String giaoVien, LocalDate tuNgay, LocalDate denNgay,
                                LocalDateTime ghiNhan, String lyDo, String nguon) { }

    /** Đọc lịch sử đã lưu; bản ghi trước migration không giả định ngày bắt đầu. */
    public List<LichSuPhanCong> layLichSuPhanCong(int maCoSo, int maLop)
            throws SQLException {
        String sql = """
                SELECT pc.MaPhanCong, pc.TuNgay, pc.DenNgay,
                       pc.ThoiDiemGhiNhan, pc.GhiChu, pc.NguonDuLieu,
                       nv.HoTen AS GiaoVien, cu.HoTen AS GiaoVienTruoc
                FROM dbo.PhanCongGiaoVienTrungTam pc
                JOIN dbo.NhanVien nv ON nv.MaNhanVien=pc.MaNhanVien
                LEFT JOIN dbo.NhanVien cu ON cu.MaNhanVien=pc.MaNhanVienTruoc
                WHERE pc.MaCoSo=? AND pc.MaLop=?
                ORDER BY pc.MaPhanCong DESC
                """;
        List<LichSuPhanCong> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi()) {
            if (!daCoBangLichSuPhanCong(con))
                throw new SQLException("Chưa triển khai bảng lịch sử phân công giáo viên");
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, maCoSo);
                ps.setInt(2, maLop);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date tu = rs.getDate("TuNgay");
                        Date den = rs.getDate("DenNgay");
                        java.sql.Timestamp ghi = rs.getTimestamp("ThoiDiemGhiNhan");
                        ds.add(new LichSuPhanCong(rs.getLong("MaPhanCong"),
                                rs.getString("GiaoVienTruoc"), rs.getString("GiaoVien"),
                                tu == null ? null : tu.toLocalDate(),
                                den == null ? null : den.toLocalDate(),
                                ghi == null ? null : ghi.toLocalDateTime(),
                                rs.getString("GhiChu"), rs.getString("NguonDuLieu")));
                    }
                }
            }
        }
        return ds;
    }

    /** Đọc cả lịch sử xếp lớp; không tạo danh sách học viên độc lập. */
    public List<HocVienTrongLop> layHocVien(int maCoSo, int maLop) throws SQLException {
        String sql = """
                SELECT d.MaHocVien, h.HoTen, k.TenKhoaHoc,
                       x.TuNgay, x.DenNgay, x.TrangThai
                FROM dbo.XepLopTrungTam x
                INNER JOIN dbo.DangKyKhoaHoc d
                    ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
                   AND d.MaCoSo=x.MaCoSo
                INNER JOIN dbo.HocVien h
                    ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                WHERE x.MaLop=? AND x.MaCoSo=?
                ORDER BY x.TuNgay DESC, d.MaHocVien
                """;
        List<HocVienTrongLop> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maLop);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date tu = rs.getDate("TuNgay");
                    Date den = rs.getDate("DenNgay");
                    ds.add(new HocVienTrongLop(rs.getInt("MaHocVien"),
                            rs.getString("HoTen"), rs.getString("TenKhoaHoc"),
                            tu == null ? null : tu.toLocalDate(),
                            den == null ? null : den.toLocalDate(),
                            rs.getString("TrangThai")));
                }
            }
        }
        return ds;
    }

    public List<LopTrungTam> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT l.MaLop, l.MaCoSo, l.MaKhoaHoc,
                       k.MaMonHoc, m.TenMonHoc, k.TenKhoaHoc,
                       l.TenLop, l.MaGiaoVien, nv.HoTen AS TenGiaoVien,
                       l.NgayKhaiGiang, l.TrangThai, l.SiSoToiDa,
                       phong.SucChuaPhong, phong.TenPhong,
                       (SELECT COUNT(DISTINCT d.MaHocVien)
                        FROM dbo.XepLopTrungTam x
                        INNER JOIN dbo.DangKyKhoaHoc d
                          ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
                         AND d.MaCoSo=x.MaCoSo
                        WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo
                          AND x.TrangThai='HIEU_LUC'
                          AND x.TuNgay<=CONVERT(date, GETDATE())
                          AND (x.DenNgay IS NULL OR x.DenNgay>=CONVERT(date, GETDATE())))
                          AS SiSoHienTai,
                       (SELECT COUNT(DISTINCT d.MaHocVien)
                        FROM dbo.XepLopTrungTam x
                        INNER JOIN dbo.DangKyKhoaHoc d
                          ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
                         AND d.MaCoSo=x.MaCoSo
                        WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo
                          AND x.TrangThai='HIEU_LUC'
                          AND (x.DenNgay IS NULL OR x.DenNgay>=CONVERT(date, GETDATE())))
                          AS SiSoDaDat
                FROM dbo.LopTrungTam l
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc = l.MaKhoaHoc AND k.MaCoSo = l.MaCoSo
                INNER JOIN dbo.MonHoc m
                    ON m.MaMonHoc = k.MaMonHoc AND m.MaCoSo = k.MaCoSo
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = l.MaGiaoVien
                OUTER APPLY (
                    SELECT MIN(r.SucChua) AS SucChuaPhong,
                           STRING_AGG(CAST(r.TenPhong AS nvarchar(max)), N', ') AS TenPhong
                    FROM (
                        SELECT p.MaPhong, p.TenPhong, p.SucChua
                        FROM dbo.LichHocLop lh
                        INNER JOIN dbo.PhongHoc p
                          ON p.MaPhong=lh.MaPhong AND p.MaCoSo=l.MaCoSo
                        WHERE lh.MaLop=l.MaLop AND lh.TrangThai='HIEU_LUC'
                          AND (lh.DenNgay IS NULL OR lh.DenNgay>=CONVERT(date, GETDATE()))
                        UNION
                        SELECT p.MaPhong, p.TenPhong, p.SucChua
                        FROM dbo.BuoiHoc b
                        INNER JOIN dbo.PhongHoc p
                          ON p.MaPhong=b.MaPhong AND p.MaCoSo=l.MaCoSo
                        WHERE b.MaLop=l.MaLop AND b.TrangThai='DU_KIEN'
                          AND b.NgayHoc>=CONVERT(date, GETDATE())
                    ) r
                ) phong
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
                    Number siSo = (Number) rs.getObject("SiSoToiDa");
                    Number sucChua = (Number) rs.getObject("SucChuaPhong");
                    Date ngay = rs.getDate("NgayKhaiGiang");
                    ds.add(new LopTrungTam(
                            rs.getInt("MaLop"), rs.getInt("MaCoSo"),
                            rs.getInt("MaKhoaHoc"), rs.getInt("MaMonHoc"),
                            rs.getString("TenMonHoc"), rs.getString("TenKhoaHoc"),
                            rs.getString("TenLop"), gv == null ? null : ((Number) gv).intValue(),
                            rs.getString("TenGiaoVien"), ngay == null ? null : ngay.toLocalDate(),
                            rs.getString("TrangThai"),
                            siSo == null ? null : siSo.intValue(),
                            rs.getInt("SiSoHienTai"),
                            rs.getInt("SiSoDaDat"),
                            sucChua == null ? null : sucChua.intValue(),
                            rs.getString("TenPhong")));
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

    /** Chỉ liệt kê giáo viên có chuyên môn hiệu lực cho môn được chọn. */
    public List<GiaoVien> layGiaoVienChuyenMon(int maCoSo, int maMonHoc,
                                               LocalDate ngay) throws SQLException {
        String sql = """
                SELECT DISTINCT nv.MaNhanVien, nv.HoTen
                FROM dbo.GiaoVienMonHoc gm
                JOIN dbo.NhanVienCoSo ncs
                  ON ncs.MaNhanVien=gm.MaNhanVien AND ncs.MaCoSo=gm.MaCoSo
                JOIN dbo.NhanVien nv ON nv.MaNhanVien=gm.MaNhanVien
                WHERE gm.MaCoSo=? AND gm.MaMonHoc=? AND gm.TuNgay<=?
                  AND (gm.DenNgay IS NULL OR gm.DenNgay>=?)
                  AND ncs.TrangThai=1 AND nv.TrangThai=1
                ORDER BY nv.HoTen, nv.MaNhanVien
                """;
        List<GiaoVien> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi()) {
            if (!daCoBangLichSuPhanCong(con) || !daCoBangChuyenMon(con))
                throw new SQLException("Chưa triển khai bảng lịch sử giáo viên; "
                        + "chưa thể chuyển phân công.");
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, maCoSo);
                ps.setInt(2, maMonHoc);
                ps.setDate(3, Date.valueOf(ngay));
                ps.setDate(4, Date.valueOf(ngay));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        ds.add(new GiaoVien(rs.getInt("MaNhanVien"), rs.getString("HoTen")));
                }
            }
        }
        return ds;
    }

    /** Khai báo chuyên môn mới từ hôm nay, giữ nguyên mọi đợt cũ đã kết thúc. */
    public void themChuyenMonGiaoVien(int maCoSo, int maNhanVien, int maMonHoc,
                                      LocalDate ngay, String lyDo) throws SQLException {
        if (maCoSo <= 0 || maNhanVien <= 0 || maMonHoc <= 0
                || ngay == null || !ngay.equals(LocalDate.now()))
            throw new IllegalArgumentException("Chỉ khai báo chuyên môn hợp lệ từ hôm nay");
        lyDo = lyDoHopLe(lyDo);
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                if (!daCoBangChuyenMon(con))
                    throw new SQLException("Chưa triển khai bảng lịch sử chuyên môn giáo viên");
                khoaGiaoVienHoatDong(con, maCoSo, maNhanVien);
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT 1 FROM dbo.MonHoc WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaMonHoc=? AND MaCoSo=? AND TrangThai=1
                        """)) {
                    ps.setInt(1, maMonHoc);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Môn học không còn hoạt động tại Trung tâm");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT TOP (1) 1 FROM dbo.GiaoVienMonHoc WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaNhanVien=? AND MaCoSo=? AND MaMonHoc=?
                          AND (DenNgay IS NULL OR DenNgay>=?)
                        """)) {
                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);
                    ps.setInt(3, maMonHoc);
                    ps.setDate(4, Date.valueOf(ngay));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next())
                            throw new SQLException("Giáo viên đã có chuyên môn hiệu lực "
                                    + "hoặc đợt dự kiến cho môn này");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT dbo.GiaoVienMonHoc
                            (MaNhanVien, MaCoSo, MaMonHoc, TuNgay, GhiChu)
                        VALUES (?, ?, ?, ?, ?)
                        """)) {
                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);
                    ps.setInt(3, maMonHoc);
                    ps.setDate(4, Date.valueOf(ngay));
                    ps.setString(5, lyDo);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Không lưu được chuyên môn giáo viên");
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }

    /** Kết thúc đợt chuyên môn cũ mà không xóa dòng lịch sử. */
    public void ketThucChuyenMonGiaoVien(int maCoSo, int maNhanVien, int maMonHoc,
                                         LocalDate ngay, String lyDo) throws SQLException {
        if (maCoSo <= 0 || maNhanVien <= 0 || maMonHoc <= 0
                || ngay == null || !ngay.equals(LocalDate.now()))
            throw new IllegalArgumentException("Chỉ kết thúc chuyên môn hợp lệ từ hôm nay");
        lyDo = lyDoHopLe(lyDo);
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                if (!daCoBangChuyenMon(con))
                    throw new SQLException("Chưa triển khai bảng lịch sử chuyên môn giáo viên");
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT 1 FROM dbo.NhanVienCoSo WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaNhanVien=? AND MaCoSo=?
                        """)) {
                    ps.setInt(1, maNhanVien);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            throw new SQLException("Giáo viên không thuộc Trung tâm");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT TOP (1) 1
                        FROM dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
                        JOIN dbo.KhoaHoc k WITH (HOLDLOCK)
                          ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                        WHERE l.MaCoSo=? AND l.MaGiaoVien=?
                          AND k.MaMonHoc=? AND l.TrangThai<>'DA_DONG'
                        """)) {
                    ps.setInt(1, maCoSo);
                    ps.setInt(2, maNhanVien);
                    ps.setInt(3, maMonHoc);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new SQLException("Giáo viên còn phụ trách "
                                + "lớp chưa đóng của môn này; hãy xử lý phân công lớp trước");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("""
                        UPDATE dbo.GiaoVienMonHoc
                        SET DenNgay=?, LyDoKetThuc=?,
                            ThoiDiemKetThucGhiNhan=SYSUTCDATETIME()
                        WHERE MaNhanVien=? AND MaCoSo=? AND MaMonHoc=?
                          AND DenNgay IS NULL AND TuNgay<?
                        """)) {
                    ps.setDate(1, Date.valueOf(ngay.minusDays(1)));
                    ps.setString(2, lyDo);
                    ps.setInt(3, maNhanVien);
                    ps.setInt(4, maCoSo);
                    ps.setInt(5, maMonHoc);
                    ps.setDate(6, Date.valueOf(ngay));
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Không có đợt chuyên môn đang mở từ trước hôm nay");
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }

    public int them(int maCoSo, int maKhoaHoc, String tenLop, Integer maGiaoVien,
                   LocalDate ngayKhaiGiang, String trangThai,
                   int siSoToiDa) throws SQLException {
        return themVoiLich(maCoSo,maKhoaHoc,tenLop,maGiaoVien,ngayKhaiGiang,
                trangThai,siSoToiDa,List.of(),null);
    }

    public int themVoiLich(int maCoSo,int maKhoaHoc,String tenLop,Integer maGiaoVien,
            LocalDate ngayKhaiGiang,String trangThai,int siSoToiDa,
            List<LichHocLopRepository.BuoiCoPhong> cacBuoi,LocalDate tuNgay)
            throws SQLException {
        if (cacBuoi == null) throw new IllegalArgumentException("Danh sách lịch không hợp lệ");
        if (!cacBuoi.isEmpty() && (tuNgay == null || maGiaoVien == null
                || !"DANG_MO".equals(trangThai)))
            throw new IllegalArgumentException("Xếp lịch ngay cần ngày hiệu lực, giáo viên và lớp đang mở");
        try (Connection con = ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                int maLop=themTrongTransaction(con,maCoSo,maKhoaHoc,tenLop,maGiaoVien,
                        ngayKhaiGiang,trangThai,siSoToiDa);
                if (!cacBuoi.isEmpty()) new LichHocLopRepository().themNhieuTheoPhong(
                        con,maCoSo,maLop,cacBuoi,tuNgay,null,"HIEU_LUC");
                con.commit();return maLop;
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }

    private int themTrongTransaction(Connection con,int maCoSo,int maKhoaHoc,
            String tenLop,Integer maGiaoVien,LocalDate ngayKhaiGiang,
            String trangThai,int siSoToiDa) throws SQLException {
        // INSERT SELECT: khong the tao lop tu khoa/mon thuoc co so khac hay da ngung.
        // Nhan vien phai ton tai trong NhanVienCoSo dung Trung tam (hoac chua phan cong).
        String sql = """
                INSERT INTO dbo.LopTrungTam
                    (MaCoSo, MaKhoaHoc, TenLop, MaGiaoVien, NgayKhaiGiang,
                     TrangThai, SiSoToiDa)
                SELECT ?, k.MaKhoaHoc, ?, ?, ?, ?, ?
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
                boolean ghiLichSu = maGiaoVien != null && daCoBangLichSuPhanCong(con);
                if (maGiaoVien != null)
                    khoaGiaoVienHoatDong(con, maCoSo, maGiaoVien);
                if (ghiLichSu)
                    kiemTraChuyenMon(con, maCoSo, maGiaoVien,
                            maMonHocCuaKhoa(con, maCoSo, maKhoaHoc), LocalDate.now());
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, maCoSo);
                    ps.setString(2, tenLop);
                    if (maGiaoVien == null) ps.setNull(3, Types.INTEGER);
                    else ps.setInt(3, maGiaoVien);
                    ps.setDate(4, ngayKhaiGiang == null ? null : Date.valueOf(ngayKhaiGiang));
                    ps.setString(5, trangThai);
                    ps.setInt(6, siSoToiDa);
                    ps.setInt(7, maKhoaHoc);
                    ps.setInt(8, maCoSo);
                    ps.setInt(9, maGiaoVien == null ? 0 : maGiaoVien);
                    ps.setInt(10, maGiaoVien == null ? 0 : maGiaoVien);
                    ps.setInt(11, maCoSo);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Khóa/môn đã ngừng hoặc giáo viên không thuộc Trung tâm");
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Không lấy được mã lớp vừa thêm");
                        int maLop = rs.getInt(1);
                        if (ghiLichSu)
                            ghiPhanCongMoi(con, maLop, maKhoaHoc, maCoSo,
                                    maGiaoVien, null, LocalDate.now(), null);
                        return maLop;
                    }
                }
    }

    public void sua(int maCoSo, int maLop, int maKhoaHoc, String tenLop,
                   Integer maGiaoVien, LocalDate ngayKhaiGiang, String trangThai,
                   Integer siSoToiDa)
            throws SQLException {
        suaVoiLich(maCoSo,maLop,maKhoaHoc,tenLop,maGiaoVien,ngayKhaiGiang,
                trangThai,siSoToiDa,List.of(),null);
    }

    public void suaVoiLich(int maCoSo,int maLop,int maKhoaHoc,String tenLop,
            Integer maGiaoVien,LocalDate ngayKhaiGiang,String trangThai,
            Integer siSoToiDa,List<LichHocLopRepository.BuoiCoPhong> cacBuoi,
            LocalDate tuNgay) throws SQLException {
        if (cacBuoi == null) throw new IllegalArgumentException("Danh sách lịch không hợp lệ");
        if (!cacBuoi.isEmpty() && (tuNgay == null || maGiaoVien == null
                || !"DANG_MO".equals(trangThai)))
            throw new IllegalArgumentException("Bổ sung lịch cần ngày hiệu lực, giáo viên và lớp đang mở");
        try (Connection con=ketNoi.mo()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                suaTrongTransaction(con,maCoSo,maLop,maKhoaHoc,tenLop,maGiaoVien,
                        ngayKhaiGiang,trangThai,siSoToiDa);
                if (!cacBuoi.isEmpty()) new LichHocLopRepository().themNhieuTheoPhong(
                        con,maCoSo,maLop,cacBuoi,tuNgay,null,"HIEU_LUC");
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch(SQLException rollback) {ex.addSuppressed(rollback);}
                throw ex;
            }
        }
    }

    private void suaTrongTransaction(Connection con,int maCoSo,int maLop,
            int maKhoaHoc,String tenLop,Integer maGiaoVien,LocalDate ngayKhaiGiang,
            String trangThai,Integer siSoToiDa) throws SQLException {
        // Khóa lớp và các liên kết trong một transaction để tránh đổi khóa/GV hoặc
        // giảm sĩ số trên dữ liệu đã phát sinh giữa lúc kiểm tra và lúc cập nhật.
        String sql = """
                UPDATE l
                   SET MaKhoaHoc=?, TenLop=?, MaGiaoVien=?,
                       NgayKhaiGiang=?, TrangThai=?, SiSoToiDa=?
                FROM dbo.LopTrungTam l
                INNER JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=? AND k.MaCoSo=?
                INNER JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                WHERE l.MaLop=? AND l.MaCoSo=?
                  AND (l.MaKhoaHoc=k.MaKhoaHoc OR (k.TrangThai=1 AND m.TrangThai=1))
                  AND (?<>'DANG_MO' OR l.TrangThai='DANG_MO'
                       OR (k.TrangThai=1 AND m.TrangThai=1))
                  AND (?=0 OR l.MaGiaoVien=? OR EXISTS (
                        SELECT 1 FROM dbo.NhanVienCoSo ncs
                        INNER JOIN dbo.NhanVien nv ON nv.MaNhanVien=ncs.MaNhanVien
                        WHERE ncs.MaNhanVien=? AND ncs.MaCoSo=?
                          AND ncs.TrangThai=1 AND nv.TrangThai=1))
                """;
                if (maGiaoVien != null)
                    khoaGiaoVienHoatDong(con, maCoSo, maGiaoVien);
                int khoaCu;
                Integer gvCu;
                Integer siSoCu;
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT MaKhoaHoc, MaGiaoVien, SiSoToiDa
                        FROM dbo.LopTrungTam WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaLop=? AND MaCoSo=?
                        """)) {
                    ps.setInt(1, maLop);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy lớp thuộc Trung tâm");
                        khoaCu = rs.getInt("MaKhoaHoc");
                        Number gv = (Number) rs.getObject("MaGiaoVien");
                        gvCu = gv == null ? null : gv.intValue();
                        Number siSo = (Number) rs.getObject("SiSoToiDa");
                        siSoCu = siSo == null ? null : siSo.intValue();
                    }
                }
                boolean coXep = coLienKet(con, """
                        SELECT TOP (1) 1 FROM dbo.XepLopTrungTam WITH (HOLDLOCK)
                        WHERE MaLop=? AND MaCoSo=?
                        """, maLop, maCoSo);
                boolean coLich = coLienKet(con, """
                        SELECT TOP (1) 1 FROM dbo.LichHocLop WITH (HOLDLOCK)
                        WHERE MaLop=?
                        """, maLop);
                boolean coBuoi = coLienKet(con, """
                        SELECT TOP (1) 1 FROM dbo.BuoiHoc WITH (HOLDLOCK)
                        WHERE MaLop=?
                        """, maLop);
                if (khoaCu != maKhoaHoc && (coXep || coLich || coBuoi)) {
                    throw new SQLException("Lớp đã có học viên hoặc lịch; không thể đổi khóa. "
                            + "Hãy tạo lớp mới để giữ đúng lịch sử đăng ký.");
                }
                if (khoaCu != maKhoaHoc && gvCu != null)
                    throw new SQLException("Lớp đã phân công giáo viên; không thể đổi khóa "
                            + "vì sẽ làm sai lịch sử môn được phân công.");
                if (gvCu != null && !Objects.equals(gvCu, maGiaoVien)) {
                    throw new SQLException("Chưa thể thay hoặc bỏ giáo viên đã phân công: "
                            + "CSDL chưa lưu lịch sử thay đổi. Hãy chờ quy trình chuyển phân công.");
                }
                if (gvCu == null && maGiaoVien != null && (coXep || coLich || coBuoi)) {
                    throw new SQLException("Lớp đã có học viên hoặc lịch/buổi học. "
                            + "Chưa thể phân công giáo viên lần đầu mà không ghi lịch sử.");
                }
                boolean ghiPhanCongDau = gvCu == null && maGiaoVien != null
                        && daCoBangLichSuPhanCong(con);
                if (ghiPhanCongDau)
                    kiemTraChuyenMon(con, maCoSo, maGiaoVien,
                            maMonHocCuaKhoa(con, maCoSo, maKhoaHoc), LocalDate.now());
                if (siSoToiDa == null && siSoCu != null)
                    throw new SQLException("Không thể xóa giới hạn sĩ số đã đặt. "
                            + "Hãy nhập giới hạn mới hợp lệ.");
                int soHocVien = soHocVienChuaKetThuc(con, maLop, maCoSo);
                if (siSoToiDa != null && siSoToiDa < soHocVien)
                    throw new SQLException("Sĩ số tối đa mới thấp hơn " + soHocVien
                            + " học viên đã được xếp lớp.");
                if ("DA_DONG".equals(trangThai) && soHocVien > 0)
                    throw new SQLException("Lớp còn học viên hiệu lực; hãy kết thúc/chuyển "
                            + "xếp lớp trước khi đóng lớp.");
                Integer sucChua = sucChuaPhongDangDung(con, maLop, maCoSo);
                if (sucChua != null && siSoToiDa != null && siSoToiDa > sucChua)
                    throw new SQLException("Sĩ số tối đa vượt sức chứa phòng đang dùng ("
                            + sucChua + "). Hãy chọn phòng phù hợp trong Lịch học trước.");
                if (ngayKhaiGiang != null && coXep) {
                    LocalDate ngayXepSomNhat = ngayXepSomNhat(con, maLop, maCoSo);
                    if (ngayXepSomNhat != null && ngayKhaiGiang.isAfter(ngayXepSomNhat))
                        throw new SQLException("Ngày khai giảng mới sau ngày đã xếp học viên; "
                                + "không thể làm sai lịch sử xếp lớp.");
                }
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, maKhoaHoc);
                    ps.setString(2, tenLop);
                    if (maGiaoVien == null) ps.setNull(3, Types.INTEGER);
                    else ps.setInt(3, maGiaoVien);
                    ps.setDate(4, ngayKhaiGiang == null ? null : Date.valueOf(ngayKhaiGiang));
                    ps.setString(5, trangThai);
                    if (siSoToiDa == null) ps.setNull(6, Types.INTEGER);
                    else ps.setInt(6, siSoToiDa);
                    ps.setInt(7, maKhoaHoc);
                    ps.setInt(8, maCoSo);
                    ps.setInt(9, maLop);
                    ps.setInt(10, maCoSo);
                    ps.setString(11, trangThai);
                    ps.setInt(12, maGiaoVien == null ? 0 : maGiaoVien);
                    ps.setInt(13, maGiaoVien == null ? 0 : maGiaoVien);
                    ps.setInt(14, maGiaoVien == null ? 0 : maGiaoVien);
                    ps.setInt(15, maCoSo);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Không sửa được lớp: khóa hoặc giáo viên không hợp lệ");
                }
                if (ghiPhanCongDau)
                    ghiPhanCongMoi(con, maLop, maKhoaHoc, maCoSo,
                            maGiaoVien, null, LocalDate.now(), null);
    }

    private static boolean daCoBangLichSuPhanCong(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT OBJECT_ID(N'dbo.PhanCongGiaoVienTrungTam', N'U')");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getObject(1) != null;
        }
    }

    private static boolean daCoBangChuyenMon(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT OBJECT_ID(N'dbo.GiaoVienMonHoc', N'U')");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getObject(1) != null;
        }
    }

    private static String lyDoHopLe(String lyDo) {
        if (lyDo == null || lyDo.isBlank() || lyDo.trim().length() > 500)
            throw new IllegalArgumentException("Lý do phải có từ 1 đến 500 ký tự");
        return lyDo.trim();
    }

    private static int maMonHocCuaKhoa(Connection con, int maCoSo, int maKhoaHoc)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT MaMonHoc FROM dbo.KhoaHoc WITH (HOLDLOCK)
                WHERE MaKhoaHoc=? AND MaCoSo=?
                """)) {
            ps.setInt(1, maKhoaHoc);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Khóa học không thuộc Trung tâm");
                return rs.getInt(1);
            }
        }
    }

    private static void kiemTraChuyenMon(Connection con, int maCoSo,
                                         int maGiaoVien, int maMonHoc,
                                         LocalDate ngay) throws SQLException {
        if (!daCoBangChuyenMon(con))
            throw new SQLException("Thiếu bảng lịch sử chuyên môn giáo viên");
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT TOP (1) 1 FROM dbo.GiaoVienMonHoc WITH (HOLDLOCK)
                WHERE MaNhanVien=? AND MaCoSo=? AND MaMonHoc=?
                  AND TuNgay<=? AND (DenNgay IS NULL OR DenNgay>=?)
                """)) {
            ps.setInt(1, maGiaoVien);
            ps.setInt(2, maCoSo);
            ps.setInt(3, maMonHoc);
            ps.setDate(4, Date.valueOf(ngay));
            ps.setDate(5, Date.valueOf(ngay));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new SQLException("Giáo viên chưa có chuyên môn hiệu lực cho môn học");
            }
        }
    }

    private static void ghiPhanCongMoi(Connection con, int maLop, int maKhoaHoc,
                                       int maCoSo, int maGiaoVien,
                                       Integer maGiaoVienTruoc, LocalDate ngay,
                                       String lyDo)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT dbo.PhanCongGiaoVienTrungTam
                    (MaLop, MaKhoaHoc, MaCoSo, MaNhanVien, MaNhanVienTruoc,
                     TuNgay, NguonDuLieu, GhiChu)
                VALUES (?, ?, ?, ?, ?, ?, 'PHAT_SINH', ?)
                """)) {
            ps.setInt(1, maLop);
            ps.setInt(2, maKhoaHoc);
            ps.setInt(3, maCoSo);
            ps.setInt(4, maGiaoVien);
            if (maGiaoVienTruoc == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, maGiaoVienTruoc);
            ps.setDate(6, Date.valueOf(ngay));
            ps.setString(7, lyDo);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Không ghi được lịch sử phân công giáo viên");
        }
    }

    /**
     * Chuyển phân công hành chính của lớp chưa từng phát sinh lịch/học viên.
     * Lớp đã hoạt động cần quy trình hiệu lực theo buổi riêng để không sửa
     * ngược lịch sử giảng dạy. Mọi thay đổi dưới đây cùng một transaction.
     */
    public void chuyenGiaoVien(int maCoSo, int maLop, int maGiaoVienMoi,
                               LocalDate ngayChuyen, String lyDo) throws SQLException {
        if (maCoSo <= 0 || maLop <= 0 || maGiaoVienMoi <= 0
                || ngayChuyen == null || !ngayChuyen.equals(LocalDate.now()))
            throw new IllegalArgumentException("Chỉ chuyển giáo viên hợp lệ trong ngày hiện tại");
        lyDo = lyDoHopLe(lyDo);
        try (Connection con = Database.moKetNoi()) {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT OBJECT_ID(N'dbo.GiaoVienMonHoc', N'U'),
                               OBJECT_ID(N'dbo.PhanCongGiaoVienTrungTam', N'U')
                        """);
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getObject(1) == null || rs.getObject(2) == null)
                        throw new SQLException("Chưa triển khai hai bảng lịch sử giáo viên; "
                                + "không thể chuyển phân công.");
                }
                // Cùng thứ tự khóa với thao tác ngừng công tác: nhân sự rồi lớp.
                khoaGiaoVienHoatDong(con, maCoSo, maGiaoVienMoi);
                int gvCu;
                int maKhoaHoc;
                int maMonHoc;
                try (PreparedStatement ps = con.prepareStatement("""
                        SELECT l.MaGiaoVien, l.MaKhoaHoc, k.MaMonHoc, l.TrangThai
                        FROM dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
                        JOIN dbo.KhoaHoc k WITH (HOLDLOCK)
                          ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                        WHERE l.MaLop=? AND l.MaCoSo=?
                        """)) {
                    ps.setInt(1, maLop);
                    ps.setInt(2, maCoSo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy lớp Trung tâm");
                        Number gv = (Number) rs.getObject("MaGiaoVien");
                        if (gv == null)
                            throw new SQLException("Lớp chưa có giáo viên cũ để chuyển phân công");
                        gvCu = gv.intValue();
                        maKhoaHoc = rs.getInt("MaKhoaHoc");
                        maMonHoc = rs.getInt("MaMonHoc");
                        if ("DA_DONG".equals(rs.getString("TrangThai")))
                            throw new SQLException("Không chuyển giáo viên của lớp đã đóng");
                    }
                }
                if (gvCu == maGiaoVienMoi)
                    throw new SQLException("Giáo viên mới trùng giáo viên hiện tại");
                if (coLienKet(con, """
                        SELECT TOP (1) 1 FROM dbo.XepLopTrungTam WITH (HOLDLOCK)
                        WHERE MaLop=? AND MaCoSo=?
                        """, maLop, maCoSo)
                        || coLienKet(con, """
                        SELECT TOP (1) 1 FROM dbo.LichHocLop WITH (HOLDLOCK)
                        WHERE MaLop=?
                        """, maLop)
                        || coLienKet(con, """
                        SELECT TOP (1) 1 FROM dbo.BuoiHoc WITH (HOLDLOCK)
                        WHERE MaLop=?
                        """, maLop)) {
                    throw new SQLException("Lớp đã có lịch hoặc học viên; cần quy trình "
                            + "chuyển giáo viên theo buổi để giữ lịch sử.");
                }
                kiemTraChuyenMon(con, maCoSo, maGiaoVienMoi, maMonHoc, ngayChuyen);
                try (PreparedStatement ps = con.prepareStatement("""
                        UPDATE dbo.PhanCongGiaoVienTrungTam
                        SET DenNgay=?
                        WHERE MaLop=? AND MaCoSo=? AND MaKhoaHoc=?
                          AND MaNhanVien=? AND DenNgay IS NULL
                          AND (TuNgay IS NULL OR TuNgay<?)
                        """)) {
                    ps.setDate(1, Date.valueOf(ngayChuyen.minusDays(1)));
                    ps.setInt(2, maLop);
                    ps.setInt(3, maCoSo);
                    ps.setInt(4, maKhoaHoc);
                    ps.setInt(5, gvCu);
                    ps.setDate(6, Date.valueOf(ngayChuyen));
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Phân công cũ không khớp ảnh chụp lịch sử");
                }
                try (PreparedStatement ps = con.prepareStatement("""
                        UPDATE dbo.LopTrungTam SET MaGiaoVien=?
                        WHERE MaLop=? AND MaCoSo=? AND MaGiaoVien=?
                        """)) {
                    ps.setInt(1, maGiaoVienMoi);
                    ps.setInt(2, maLop);
                    ps.setInt(3, maCoSo);
                    ps.setInt(4, gvCu);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Phân công lớp đã thay đổi; hãy tải lại");
                }
                ghiPhanCongMoi(con, maLop, maKhoaHoc, maCoSo,
                        maGiaoVienMoi, gvCu, ngayChuyen, lyDo);
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }

    private static void khoaGiaoVienHoatDong(Connection con, int maCoSo,
                                             int maGiaoVien) throws SQLException {
        String sql = """
                SELECT 1
                FROM dbo.NhanVienCoSo ncs WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.NhanVien nv WITH (HOLDLOCK)
                  ON nv.MaNhanVien=ncs.MaNhanVien
                WHERE ncs.MaCoSo=? AND ncs.MaNhanVien=?
                  AND ncs.TrangThai=1 AND nv.TrangThai=1
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo); ps.setInt(2, maGiaoVien);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new SQLException("Giáo viên không còn đang công tác tại Trung tâm");
            }
        }
    }

    private static boolean coLienKet(Connection con, String sql, int... ids) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < ids.length; i++) ps.setInt(i + 1, ids[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static int soHocVienChuaKetThuc(Connection con, int maLop, int maCoSo)
            throws SQLException {
        String sql = """
                SELECT COUNT(DISTINCT d.MaHocVien)
                FROM dbo.XepLopTrungTam x WITH (HOLDLOCK)
                INNER JOIN dbo.DangKyKhoaHoc d
                  ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
                 AND d.MaCoSo=x.MaCoSo
                WHERE x.MaLop=? AND x.MaCoSo=? AND x.TrangThai='HIEU_LUC'
                  AND (x.DenNgay IS NULL OR x.DenNgay>=CONVERT(date, GETDATE()))
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maLop); ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static Integer sucChuaPhongDangDung(Connection con, int maLop, int maCoSo)
            throws SQLException {
        String sql = """
                SELECT MIN(r.SucChua) FROM (
                    SELECT p.SucChua FROM dbo.LichHocLop lh WITH (HOLDLOCK)
                    INNER JOIN dbo.PhongHoc p ON p.MaPhong=lh.MaPhong AND p.MaCoSo=?
                    WHERE lh.MaLop=? AND lh.TrangThai='HIEU_LUC'
                      AND (lh.DenNgay IS NULL OR lh.DenNgay>=CONVERT(date, GETDATE()))
                    UNION ALL
                    SELECT p.SucChua FROM dbo.BuoiHoc b WITH (HOLDLOCK)
                    INNER JOIN dbo.PhongHoc p ON p.MaPhong=b.MaPhong AND p.MaCoSo=?
                    WHERE b.MaLop=? AND b.TrangThai='DU_KIEN'
                      AND b.NgayHoc>=CONVERT(date, GETDATE())
                ) r
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maCoSo); ps.setInt(2, maLop);
            ps.setInt(3, maCoSo); ps.setInt(4, maLop);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); Number n = (Number) rs.getObject(1);
                return n == null ? null : n.intValue();
            }
        }
    }

    private static LocalDate ngayXepSomNhat(Connection con, int maLop, int maCoSo)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT MIN(TuNgay) FROM dbo.XepLopTrungTam WITH (HOLDLOCK)
                WHERE MaLop=? AND MaCoSo=?
                """)) {
            ps.setInt(1, maLop); ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); Date d = rs.getDate(1);
                return d == null ? null : d.toLocalDate();
            }
        }
    }
}
