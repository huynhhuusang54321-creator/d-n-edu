package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import vn.vinabacus.config.Database;
import vn.vinabacus.service.CaHocTrungTam;

/** Diem danh Trung tam theo BuoiHoc va SapXepBuoiHoc hien co. */
public final class DiemDanhTrungTamRepository {
    @FunctionalInterface public interface KetNoi { Connection mo() throws SQLException; }
    private final KetNoi ketNoi;
    public DiemDanhTrungTamRepository() { this(Database::moKetNoi); }
    public DiemDanhTrungTamRepository(KetNoi ketNoi) {
        this.ketNoi = Objects.requireNonNull(ketNoi);
    }

    public record TaiKhoan(int ma, String ten) {
        @Override public String toString() { return ten; }
    }
    public record Buoi(int maBuoi, int maLop, int maKhoa, int maMon,
                       String tenLop, String tenKhoa, String tenMon,
                       String giaoVien, String phong, LocalDate ngay,
                       int soCa, String trangThai, String noiDung) {
        @Override public String toString() {
            return tenLop + " · " + CaHocTrungTam.layTheoSoCa(soCa)
                    + " · " + trangThai;
        }
    }
    public record HocVienBuoi(int maDangKy, int maHocVien, Integer maSapXep,
                              String hoTen, String tenLopChinh, String tenKhoa,
                              boolean hocBu, String loaiSapXep,
                              String trangThai, String ghiChu) { }
    public record LichSuSua(String truoc, String sau, String lyDo,
                            String taiKhoan, String ngaySua) { }
    public record ThayDoi(HocVienBuoi hocVien, String trangThai,
                          String ghiChu, String lyDoSua) { }

    public List<LichSuSua> layLichSuSua(int maCoSo, int maSapXep)
            throws SQLException {
        List<LichSuSua> ds = new ArrayList<>();
        try (Connection c = ketNoi.mo()) {
            if (!coBang(c, "LichSuSuaDiemDanhTrungTam")) return ds;
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT ls.TrangThaiTruoc,ls.TrangThaiSau,ls.LyDo,
                           t.TenDangNhap,CONVERT(varchar(19),ls.NgaySua,120)
                    FROM dbo.LichSuSuaDiemDanhTrungTam ls
                    JOIN dbo.DiemDanhTrungTam dd ON dd.MaDiemDanh=ls.MaDiemDanh
                    JOIN dbo.SapXepBuoiHoc s ON s.MaSapXep=dd.MaSapXep
                    JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=s.MaDangKy
                         AND d.MaHocVien=s.MaHocVien AND d.MaCoSo=?
                    JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=ls.MaTaiKhoan
                    WHERE s.MaSapXep=? ORDER BY ls.NgaySua DESC,ls.MaLichSu DESC
                    """)) {
                ps.setInt(1, maCoSo);ps.setInt(2, maSapXep);
                try (ResultSet rs=ps.executeQuery()) {
                    while (rs.next()) ds.add(new LichSuSua(rs.getString(1),
                            rs.getString(2),rs.getString(3),rs.getString(4),
                            rs.getString(5)));
                }
            }
        }
        return ds;
    }

    public List<TaiKhoan> layTaiKhoan(int maCoSo) throws SQLException {
        List<TaiKhoan> ds = new ArrayList<>();
        try (Connection c = ketNoi.mo(); PreparedStatement ps = c.prepareStatement("""
                SELECT t.MaTaiKhoan,t.TenDangNhap FROM dbo.TaiKhoan t
                INNER JOIN dbo.TaiKhoanCoSo tc ON tc.MaTaiKhoan=t.MaTaiKhoan
                WHERE tc.MaCoSo=? AND tc.TrangThai=1 AND t.TrangThai=1
                ORDER BY t.TenDangNhap
                """)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ds.add(new TaiKhoan(rs.getInt(1), rs.getString(2)));
            }
        }
        return ds;
    }

    public List<Buoi> layBuoi(int maCoSo, LocalDate ngay) throws SQLException {
        List<Buoi> ds = new ArrayList<>();
        try (Connection c = ketNoi.mo()) {
            boolean coNoiDung = coCot(c, "BuoiHoc", "NoiDungHoc");
            String sql = """
                    SELECT b.MaBuoiHoc,b.MaLop,k.MaKhoaHoc,m.MaMonHoc,
                           l.TenLop,k.TenKhoaHoc,m.TenMonHoc,
                           nv.HoTen AS GiaoVien,p.TenPhong,b.NgayHoc,b.SoCa,b.TrangThai,
                    """ + (coNoiDung ? "b.NoiDungHoc" : "CAST(NULL AS nvarchar(500))")
                    + " AS NoiDung FROM dbo.BuoiHoc b " + """
                    INNER JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop
                    INNER JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                    INNER JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                    LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien=l.MaGiaoVien
                    LEFT JOIN dbo.LichHocLop lh ON lh.MaLichHoc=b.MaLichHoc
                    LEFT JOIN dbo.PhongHoc p ON p.MaPhong=COALESCE(b.MaPhong,lh.MaPhong)
                                           AND p.MaCoSo=l.MaCoSo
                    WHERE l.MaCoSo=? AND b.NgayHoc=? AND b.TrangThai<>'HUY'
                    ORDER BY b.SoCa,l.TenLop,b.MaBuoiHoc
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, maCoSo);
                ps.setDate(2, Date.valueOf(ngay));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ds.add(new Buoi(rs.getInt("MaBuoiHoc"),
                            rs.getInt("MaLop"), rs.getInt("MaKhoaHoc"),
                            rs.getInt("MaMonHoc"), rs.getString("TenLop"),
                            rs.getString("TenKhoaHoc"), rs.getString("TenMonHoc"),
                            rs.getString("GiaoVien"), rs.getString("TenPhong"),
                            rs.getDate("NgayHoc").toLocalDate(), rs.getInt("SoCa"),
                            rs.getString("TrangThai"), rs.getString("NoiDung")));
                }
            }
        }
        return ds;
    }

    /** Lịch tuần có hiệu lực nhưng chưa phát sinh buổi thực tế, chỉ đọc và cảnh báo. */
    public int demLichChuaCoBuoi(int maCoSo,LocalDate ngay) throws SQLException {
        try(Connection c=ketNoi.mo();PreparedStatement ps=c.prepareStatement("""
                SELECT COUNT(*) FROM dbo.LichHocLop lh
                JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                WHERE l.MaCoSo=? AND l.TrangThai='DANG_MO'
                  AND lh.TrangThai='HIEU_LUC' AND lh.ThuTrongTuan=?
                  AND lh.TuNgay<=? AND (lh.DenNgay IS NULL OR lh.DenNgay>=?)
                  AND NOT EXISTS(SELECT 1 FROM dbo.BuoiHoc b
                                 WHERE b.MaLichHoc=lh.MaLichHoc AND b.NgayHoc=?)
                """)) {
            ps.setInt(1,maCoSo);
            ps.setInt(2,ngay.getDayOfWeek().getValue()+1);
            for(int i=3;i<=5;i++)ps.setDate(i,Date.valueOf(ngay));
            try(ResultSet rs=ps.executeQuery()) {rs.next();return rs.getInt(1);}
        }
    }

    public List<HocVienBuoi> layHocVien(int maCoSo, int maBuoi) throws SQLException {
        List<HocVienBuoi> ds = new ArrayList<>();
        try (Connection c = ketNoi.mo()) {
            Buoi buoi = khoaBuoi(c, maCoSo, maBuoi, false);
            String sql = """
                    SELECT d.MaDangKy,d.MaHocVien,h.HoTen,s.MaSapXep,
                           l.TenLop AS LopChinh,k.TenKhoaHoc,
                           dd.TrangThai,dd.GhiChu,s.LoaiSapXep
                    FROM dbo.XepLopTrungTam x
                    INNER JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=x.MaDangKy
                         AND d.MaCoSo=x.MaCoSo AND d.MaKhoaHoc=x.MaKhoaHoc
                    INNER JOIN dbo.HocVien h ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                    INNER JOIN dbo.LopTrungTam l ON l.MaLop=x.MaLop
                    INNER JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                    LEFT JOIN dbo.SapXepBuoiHoc s ON s.MaDangKy=d.MaDangKy
                         AND s.MaBuoiHoc=? AND s.TrangThai<>'HUY'
                    LEFT JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=s.MaSapXep
                    WHERE x.MaLop=? AND x.MaCoSo=? AND x.TrangThai IN ('HIEU_LUC','KET_THUC')
                      AND x.TuNgay<=? AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                      AND d.TrangThai<>'HUY'
                    ORDER BY h.HoTen,d.MaDangKy
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, maBuoi); ps.setInt(2, buoi.maLop()); ps.setInt(3, maCoSo);
                ps.setDate(4, Date.valueOf(buoi.ngay()));
                ps.setDate(5, Date.valueOf(buoi.ngay()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ds.add(docHocVien(rs, false));
                }
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT d.MaDangKy,d.MaHocVien,h.HoTen,s.MaSapXep,
                           l.TenLop AS LopChinh,k.TenKhoaHoc,dd.TrangThai,dd.GhiChu,
                           s.LoaiSapXep
                    FROM dbo.SapXepBuoiHoc s
                    INNER JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=s.MaDangKy
                         AND d.MaHocVien=s.MaHocVien AND d.MaCoSo=?
                    INNER JOIN dbo.HocVien h ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                    INNER JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                    LEFT JOIN dbo.XepLopTrungTam x ON x.MaDangKy=d.MaDangKy
                         AND x.MaCoSo=d.MaCoSo AND x.TrangThai='HIEU_LUC'
                    LEFT JOIN dbo.LopTrungTam l ON l.MaLop=x.MaLop
                    LEFT JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=s.MaSapXep
                    WHERE s.MaBuoiHoc=? AND s.LoaiSapXep<>'CHINH_THUC'
                      AND s.TrangThai<>'HUY'
                      AND NOT EXISTS (
                          SELECT 1 FROM dbo.XepLopTrungTam xe
                          WHERE xe.MaDangKy=d.MaDangKy AND xe.MaLop=?
                            AND xe.TrangThai IN ('HIEU_LUC','KET_THUC')
                            AND xe.TuNgay<=? AND (xe.DenNgay IS NULL OR xe.DenNgay>=?))
                    ORDER BY h.HoTen,s.MaSapXep
                    """)) {
                ps.setInt(1, maCoSo); ps.setInt(2, maBuoi);
                ps.setInt(3, buoi.maLop());
                ps.setDate(4, Date.valueOf(buoi.ngay()));
                ps.setDate(5, Date.valueOf(buoi.ngay()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ds.add(docHocVien(rs, true));
                }
            }
        }
        return ds;
    }

    /** Noi dung phai duoc xac nhan truoc khi xep hoc bu; khong sua sau diem danh. */
    public void luuNoiDung(int maCoSo, int maBuoi, String noiDung)
            throws SQLException {
        if (noiDung == null || noiDung.isBlank() || noiDung.length() > 500)
            throw new IllegalArgumentException("Nội dung buổi học phải có 1–500 ký tự");
        try (Connection c = ketNoi.mo()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                if (!coCot(c, "BuoiHoc", "NoiDungHoc"))
                    throw new SQLException("Chưa có cột nội dung buổi học GD6");
                khoaBuoi(c, maCoSo, maBuoi, true);
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT NoiDungHoc FROM dbo.BuoiHoc WITH (UPDLOCK,HOLDLOCK)
                        WHERE MaBuoiHoc=?
                        """)) {
                    ps.setInt(1, maBuoi);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy buổi học");
                        String daLuu = rs.getString(1);
                        if (daLuu != null && !daLuu.trim().equals(noiDung.trim()))
                            throw new SQLException("Nội dung đã xác nhận; không thể ghi đè lịch sử");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE dbo.BuoiHoc SET NoiDungHoc=?
                        WHERE MaBuoiHoc=? AND NoiDungHoc IS NULL
                        """)) {
                    ps.setString(1, noiDung.trim()); ps.setInt(2, maBuoi);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
        }
    }

    private static HocVienBuoi docHocVien(ResultSet rs, boolean hocBu) throws SQLException {
        Number maSapXep = (Number) rs.getObject("MaSapXep");
        return new HocVienBuoi(rs.getInt("MaDangKy"), rs.getInt("MaHocVien"),
                maSapXep == null ? null : maSapXep.intValue(),
                rs.getString("HoTen"), rs.getString("LopChinh"),
                rs.getString("TenKhoaHoc"), hocBu,
                rs.getString("LoaiSapXep") == null ? "CHINH_THUC"
                        : rs.getString("LoaiSapXep"),
                rs.getString("TrangThai"), rs.getString("GhiChu"));
    }

    /** Chi ghi trang thai duoc nhan vien chon. Sua ban ghi cu can lich su GD6. */
    public void luuDiemDanh(int maCoSo, int maBuoi, int maDangKy, int maHocVien,
                            String trangThai, String ghiChu, int maTaiKhoan,
                            String lyDoSua) throws SQLException {
        luuNhieu(maCoSo, maBuoi, List.of(new ThayDoi(
                new HocVienBuoi(maDangKy, maHocVien, null, "", "", "", false,
                        "CHINH_THUC", null, null), trangThai, ghiChu, lyDoSua)), maTaiKhoan);
    }

    public void luuNhieu(int maCoSo, int maBuoi, List<ThayDoi> thayDoi,
                         int maTaiKhoan) throws SQLException {
        if (thayDoi == null || thayDoi.isEmpty()) return;
        try (Connection c = ketNoi.mo()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                for (ThayDoi d : thayDoi) {
                    if (d.hocVien() == null) throw new IllegalArgumentException("Thiếu học viên");
                    luuTrongTransaction(c, maCoSo, maBuoi, d.hocVien().maDangKy(),
                            d.hocVien().maHocVien(), d.trangThai(), d.ghiChu(),
                            maTaiKhoan, d.lyDoSua());
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
        }
    }

    private void luuTrongTransaction(Connection c, int maCoSo, int maBuoi,
            int maDangKy, int maHocVien, String trangThai, String ghiChu,
            int maTaiKhoan, String lyDoSua) throws SQLException {
        if (!List.of("CO_MAT", "VANG_CO_PHEP", "VANG_KHONG_PHEP",
                "CHUA_DIEM_DANH").contains(trangThai))
            throw new IllegalArgumentException("Trang thai diem danh khong hop le");
                Buoi buoi = khoaBuoi(c, maCoSo, maBuoi, true);
                if ("HUY".equals(buoi.trangThai()))
                    throw new SQLException("Buoi da huy, khong the diem danh");
                if (buoi.ngay().isAfter(LocalDate.now())
                        || (buoi.ngay().equals(LocalDate.now())
                        && LocalTime.now().isBefore(CaHocTrungTam.layTheoSoCa(
                                buoi.soCa()).ketThuc())))
                    throw new SQLException("Buoi chua ket thuc; khong the diem danh truoc");
                kiemTraTaiKhoan(c, maCoSo, maTaiKhoan);
                Integer maSapXep = null;
                String loai = null;
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT MaSapXep,LoaiSapXep FROM dbo.SapXepBuoiHoc WITH (UPDLOCK,HOLDLOCK)
                        WHERE MaBuoiHoc=? AND MaDangKy=? AND MaHocVien=? AND TrangThai<>'HUY'
                        """)) {
                    ps.setInt(1, maBuoi); ps.setInt(2, maDangKy); ps.setInt(3, maHocVien);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) { maSapXep = rs.getInt(1); loai = rs.getString(2); }
                    }
                }
                if (maSapXep == null) {
                    if (!laHocVienChinh(c, buoi, maDangKy, maHocVien, maCoSo))
                        throw new SQLException("Hoc vien khong thuoc lop va chua duoc xep hoc bu");
                    try (PreparedStatement ps = c.prepareStatement("""
                            INSERT dbo.SapXepBuoiHoc
                              (MaDangKy,MaHocVien,MaBuoiHoc,LoaiSapXep,TrangThai)
                            VALUES (?,?,?,'CHINH_THUC','DA_XEP')
                            """, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, maDangKy); ps.setInt(2, maHocVien); ps.setInt(3, maBuoi);
                        if (ps.executeUpdate() != 1) throw new SQLException("Khong tao duoc luot hoc");
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Thieu ma luot hoc");
                            maSapXep = keys.getInt(1);
                        }
                    }
                }
                Integer maDiemDanh = null;
                String trangThaiCu = null, ghiChuCu = null;
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT MaDiemDanh,TrangThai,GhiChu
                        FROM dbo.DiemDanhTrungTam WITH (UPDLOCK,HOLDLOCK)
                        WHERE MaSapXep=?
                        """)) {
                    ps.setInt(1, maSapXep);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) { maDiemDanh = rs.getInt(1);
                            trangThaiCu = rs.getString(2); ghiChuCu = rs.getString(3); }
                    }
                }
                if (maDiemDanh != null) {
                    if (trangThai.equals(trangThaiCu)
                            && Objects.equals(ghiChu, ghiChuCu)) return;
                    if ("CO_MAT".equals(trangThai)) {
                        try (PreparedStatement ps = c.prepareStatement("""
                                SELECT 1 FROM dbo.SapXepBuoiHoc WITH (UPDLOCK,HOLDLOCK)
                                WHERE MaSapXepNguon=? AND LoaiSapXep='HOC_BU'
                                  AND TrangThai IN ('DA_XEP','HOAN_THANH')
                                """)) {
                            ps.setInt(1, maSapXep);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) throw new SQLException(
                                        "Buổi vắng đã có học bù; hủy lịch bù trước khi sửa thành có mặt");
                            }
                        }
                    }
                    if (!coBang(c, "LichSuSuaDiemDanhTrungTam"))
                        throw new SQLException("Chua co bang lich su sua diem danh GD6; "
                                + "giu nguyen ban ghi cu cho den khi migration duoc duyet");
                    if (lyDoSua == null || lyDoSua.isBlank())
                        throw new SQLException("Sua diem danh cu phai ghi ly do");
                    try (PreparedStatement ps = c.prepareStatement("""
                            UPDATE dbo.DiemDanhTrungTam SET TrangThai=?,GhiChu=?,
                                   MaTaiKhoan=?,NgayCapNhat=SYSUTCDATETIME()
                            WHERE MaDiemDanh=?
                            """)) {
                        ps.setString(1, trangThai); ps.setString(2, ghiChu);
                        ps.setInt(3, maTaiKhoan); ps.setInt(4, maDiemDanh);
                        if (ps.executeUpdate() != 1) throw new SQLException("Khong sua duoc diem danh");
                    }
                    try (PreparedStatement ps = c.prepareStatement("""
                            INSERT dbo.LichSuSuaDiemDanhTrungTam
                              (MaDiemDanh,TrangThaiTruoc,TrangThaiSau,GhiChuTruoc,
                               GhiChuSau,LyDo,MaTaiKhoan)
                            VALUES (?,?,?,?,?,?,?)
                            """)) {
                        ps.setInt(1, maDiemDanh); ps.setString(2, trangThaiCu);
                        ps.setString(3, trangThai); ps.setString(4, ghiChuCu);
                        ps.setString(5, ghiChu); ps.setString(6, lyDoSua.trim());
                        ps.setInt(7, maTaiKhoan);
                        ps.executeUpdate();
                    }
                } else if (!"CHUA_DIEM_DANH".equals(trangThai)) {
                    try (PreparedStatement ps = c.prepareStatement("""
                            INSERT dbo.DiemDanhTrungTam
                              (MaSapXep,TrangThai,GhiChu,MaTaiKhoan,NgayCapNhat)
                            VALUES (?,?,?,?,SYSUTCDATETIME())
                            """)) {
                        ps.setInt(1, maSapXep); ps.setString(2, trangThai);
                        ps.setString(3, ghiChu); ps.setInt(4, maTaiKhoan);
                        if (ps.executeUpdate() != 1) throw new SQLException("Khong luu duoc diem danh");
                    }
                }
                if ("CO_MAT".equals(trangThai) && "HOC_BU".equals(loai))
                    kiemTraNoiDungHocBu(c, maSapXep);
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE dbo.SapXepBuoiHoc SET TrangThai=? WHERE MaSapXep=?
                        """)) {
                    ps.setString(1, "CO_MAT".equals(trangThai) ? "HOAN_THANH" : "DA_XEP");
                    ps.setInt(2, maSapXep); ps.executeUpdate();
                }
                if (!"CHUA_DIEM_DANH".equals(trangThai)) {
                    try (PreparedStatement ps = c.prepareStatement("""
                            UPDATE dbo.BuoiHoc SET TrangThai='DA_DIEN_RA'
                            WHERE MaBuoiHoc=? AND TrangThai='DU_KIEN'
                            """)) {
                        ps.setInt(1, maBuoi); ps.executeUpdate();
                    }
                }
                capNhatHoanThanh(c, maCoSo, maDangKy);
    }

    /** Chi dao trang thai do GD6 tu dong quan ly, khong ghi de hoan thanh thu cong cu. */
    private static void capNhatHoanThanh(Connection c, int maCoSo,
                                         int maDangKy) throws SQLException {
        if (!coCot(c, "DangKyKhoaHoc", "HoanThanhTuDongGD6")) return;
        int tong;
        boolean tuDong;
        String trangThai;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT TongSoBuoiDangKy,TrangThai,HoanThanhTuDongGD6
                FROM dbo.DangKyKhoaHoc WITH (UPDLOCK,HOLDLOCK)
                WHERE MaDangKy=? AND MaCoSo=?
                """)) {
            ps.setInt(1, maDangKy); ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Đăng ký không thuộc Trung tâm");
                tong = rs.getInt(1); trangThai = rs.getString(2);
                tuDong = rs.getBoolean(3);
            }
        }
        if ("HUY".equals(trangThai) || "DA_CHUYEN".equals(trangThai)) return;
        String dieuKienHocBu = coCot(c, "BuoiHoc", "NoiDungHoc") ? """
                AND (sx.LoaiSapXep<>'HOC_BU' OR
                    (srcdd.TrangThai IN ('VANG_CO_PHEP','VANG_KHONG_PHEP')
                     AND srcb.TrangThai='DA_DIEN_RA'
                     AND srcb.NoiDungHoc IS NOT NULL AND b.NoiDungHoc IS NOT NULL
                     AND LOWER(LTRIM(RTRIM(srcb.NoiDungHoc)))=
                         LOWER(LTRIM(RTRIM(b.NoiDungHoc)))))
                """ : " AND sx.LoaiSapXep<>'HOC_BU' ";
        int daHoc;
        Date ngayCuoi;
        String sql = """
                SELECT COUNT(DISTINCT CASE WHEN sx.LoaiSapXep IN ('HOC_BU','DOI_BUOI')
                           THEN sx.MaSapXepNguon ELSE sx.MaSapXep END),
                       MAX(b.NgayHoc)
                FROM dbo.SapXepBuoiHoc sx
                JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=sx.MaBuoiHoc
                JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=sx.MaSapXep
                LEFT JOIN dbo.SapXepBuoiHoc src ON src.MaSapXep=sx.MaSapXepNguon
                LEFT JOIN dbo.DiemDanhTrungTam srcdd ON srcdd.MaSapXep=src.MaSapXep
                LEFT JOIN dbo.BuoiHoc srcb ON srcb.MaBuoiHoc=src.MaBuoiHoc
                WHERE sx.MaDangKy=? AND sx.TrangThai='HOAN_THANH'
                  AND b.TrangThai='DA_DIEN_RA' AND dd.TrangThai='CO_MAT'
                """ + dieuKienHocBu;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDangKy);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); daHoc = rs.getInt(1); ngayCuoi = rs.getDate(2);
            }
        }
        if (daHoc >= tong && ngayCuoi != null
                && ("DANG_HOC".equals(trangThai) || tuDong)) {
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE dbo.DangKyKhoaHoc
                    SET TrangThai='HOAN_THANH',NgayKetThucThucTe=?,
                        HoanThanhTuDongGD6=1
                    WHERE MaDangKy=? AND MaCoSo=?
                    """)) {
                ps.setDate(1, ngayCuoi); ps.setInt(2, maDangKy);
                ps.setInt(3, maCoSo); ps.executeUpdate();
            }
        } else if (daHoc < tong && tuDong && "HOAN_THANH".equals(trangThai)) {
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE dbo.DangKyKhoaHoc
                    SET TrangThai='DANG_HOC',NgayKetThucThucTe=NULL,
                        HoanThanhTuDongGD6=0
                    WHERE MaDangKy=? AND MaCoSo=?
                    """)) {
                ps.setInt(1, maDangKy); ps.setInt(2, maCoSo);
                ps.executeUpdate();
            }
        }
    }

    private static void kiemTraNoiDungHocBu(Connection c, int maSapXep)
            throws SQLException {
        if (!coCot(c, "BuoiHoc", "NoiDungHoc"))
            throw new SQLException("Chua co cot noi dung buoi hoc GD6");
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT src.NoiDungHoc,tar.NoiDungHoc
                FROM dbo.SapXepBuoiHoc s
                INNER JOIN dbo.BuoiHoc tar ON tar.MaBuoiHoc=s.MaBuoiHoc
                INNER JOIN dbo.SapXepBuoiHoc nguon ON nguon.MaSapXep=s.MaSapXepNguon
                INNER JOIN dbo.BuoiHoc src ON src.MaBuoiHoc=nguon.MaBuoiHoc
                WHERE s.MaSapXep=? AND s.LoaiSapXep='HOC_BU'
                """)) {
            ps.setInt(1, maSapXep);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getString(1) == null || rs.getString(2) == null
                        || !rs.getString(1).trim().equalsIgnoreCase(rs.getString(2).trim()))
                    throw new SQLException("Noi dung buoi hoc bu khong khop buoi vang");
            }
        }
    }

    private static boolean laHocVienChinh(Connection c, Buoi buoi,
                                          int maDangKy, int maHocVien, int maCoSo)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM dbo.XepLopTrungTam x WITH (HOLDLOCK)
                INNER JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=x.MaDangKy
                     AND d.MaKhoaHoc=x.MaKhoaHoc AND d.MaCoSo=x.MaCoSo
                WHERE x.MaLop=? AND x.MaCoSo=? AND x.MaDangKy=?
                  AND d.MaHocVien=? AND x.TrangThai IN ('HIEU_LUC','KET_THUC')
                  AND x.TuNgay<=? AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                """)) {
            ps.setInt(1, buoi.maLop()); ps.setInt(2, maCoSo);
            ps.setInt(3, maDangKy); ps.setInt(4, maHocVien);
            ps.setDate(5, Date.valueOf(buoi.ngay()));
            ps.setDate(6, Date.valueOf(buoi.ngay()));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static Buoi khoaBuoi(Connection c, int maCoSo, int maBuoi,
                                 boolean ghi) throws SQLException {
        String sql = """
                SELECT b.MaBuoiHoc,b.MaLop,k.MaKhoaHoc,m.MaMonHoc,
                       l.TenLop,k.TenKhoaHoc,m.TenMonHoc,
                       nv.HoTen AS GiaoVien,p.TenPhong,b.NgayHoc,b.SoCa,b.TrangThai
                FROM dbo.BuoiHoc b WITH (UPDLOCK,HOLDLOCK)
                INNER JOIN dbo.LopTrungTam l WITH (UPDLOCK,HOLDLOCK) ON l.MaLop=b.MaLop
                INNER JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                INNER JOIN dbo.MonHoc m ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien=l.MaGiaoVien
                LEFT JOIN dbo.LichHocLop lh ON lh.MaLichHoc=b.MaLichHoc
                LEFT JOIN dbo.PhongHoc p ON p.MaPhong=COALESCE(b.MaPhong,lh.MaPhong)
                                       AND p.MaCoSo=l.MaCoSo
                WHERE l.MaCoSo=? AND b.MaBuoiHoc=?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maCoSo); ps.setInt(2, maBuoi);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Khong tim thay buoi cua Trung tam");
                int soCa = rs.getInt("SoCa");
                if (soCa < 1 || soCa > 8) throw new SQLException("Buoi khong thuoc 8 ca chuan");
                return new Buoi(maBuoi, rs.getInt("MaLop"), rs.getInt("MaKhoaHoc"),
                        rs.getInt("MaMonHoc"), rs.getString("TenLop"),
                        rs.getString("TenKhoaHoc"), rs.getString("TenMonHoc"),
                        rs.getString("GiaoVien"), rs.getString("TenPhong"),
                        rs.getDate("NgayHoc").toLocalDate(), soCa,
                        rs.getString("TrangThai"), null);
            }
        }
    }

    private static void kiemTraTaiKhoan(Connection c, int maCoSo, int taiKhoan)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM dbo.TaiKhoan t WITH (HOLDLOCK)
                INNER JOIN dbo.TaiKhoanCoSo tc ON tc.MaTaiKhoan=t.MaTaiKhoan
                WHERE t.MaTaiKhoan=? AND tc.MaCoSo=?
                  AND t.TrangThai=1 AND tc.TrangThai=1
                """)) {
            ps.setInt(1, taiKhoan); ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Tai khoan khong duoc phep diem danh Trung tam");
            }
        }
    }

    private static boolean coCot(Connection c, String bang, String cot) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COL_LENGTH(?,?)")) {
            ps.setString(1, "dbo." + bang); ps.setString(2, cot);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getObject(1) != null; }
        }
    }
    private static boolean coBang(Connection c, String bang) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT OBJECT_ID(?, 'U')")) {
            ps.setString(1, "dbo." + bang);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getObject(1) != null; }
        }
    }
}
