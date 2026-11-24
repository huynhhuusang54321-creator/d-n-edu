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

/** Xep hoc bu tung hoc vien; khong thay doi lop chinh va dang ky. */
public final class HocBuTrungTamRepository {
    private final DiemDanhTrungTamRepository.KetNoi ketNoi;
    public HocBuTrungTamRepository() { this(Database::moKetNoi); }
    public HocBuTrungTamRepository(DiemDanhTrungTamRepository.KetNoi ketNoi) {
        this.ketNoi = Objects.requireNonNull(ketNoi);
    }
    public record BuoiVang(int maSapXep, int maDangKy, int maHocVien,
                            String hocVien, String khoa, String lop, LocalDate ngay,
                            int soCa, String trangThai, String noiDung) { }
    public record BuoiDich(int maBuoi, String lop, String khoa, LocalDate ngay,
                            int soCa, String phong, int sucChua, int siSoToiDa,
                            int daGiuCho, String noiDung) {
        @Override public String toString() {
            return ngay + " · ca " + soCa + " · " + lop + " · " + phong
                    + " (" + daGiuCho + "/" + Math.min(sucChua, siSoToiDa) + ")";
        }
    }
    public record LichHocBu(int maSapXep, int maNguon, int maHocVien,
                             String hocVien, String lop, LocalDate ngay,
                             int soCa, String trangThai, String lyDoHuy) { }

    public List<LichHocBu> layLichHocBu(int maCoSo) throws SQLException {
        List<LichHocBu> ds = new ArrayList<>();
        try (Connection c=ketNoi.mo()) {
            String lyDo = coCot(c,"SapXepBuoiHoc","LyDoHuy")
                    ? "s.LyDoHuy" : "CAST(NULL AS nvarchar(500))";
            String sql="""
                SELECT s.MaSapXep,s.MaSapXepNguon,s.MaHocVien,h.HoTen,l.TenLop,
                       b.NgayHoc,b.SoCa,s.TrangThai,
                """+lyDo+"""
                 AS LyDoHuy
                FROM dbo.SapXepBuoiHoc s
                JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=s.MaDangKy
                     AND d.MaHocVien=s.MaHocVien AND d.MaCoSo=?
                JOIN dbo.HocVien h ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=s.MaBuoiHoc
                JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop AND l.MaCoSo=d.MaCoSo
                WHERE s.LoaiSapXep='HOC_BU'
                ORDER BY b.NgayHoc DESC,s.MaSapXep DESC
                """;
            try (PreparedStatement ps=c.prepareStatement(sql)) {
                ps.setInt(1,maCoSo);
                try (ResultSet rs=ps.executeQuery()) {
                    while (rs.next()) ds.add(new LichHocBu(rs.getInt(1),rs.getInt(2),
                            rs.getInt(3),rs.getString(4),rs.getString(5),
                            rs.getDate(6).toLocalDate(),rs.getInt(7),rs.getString(8),
                            rs.getString(9)));
                }
            }
        }
        return ds;
    }

    public List<BuoiVang> layBuoiVang(int maCoSo) throws SQLException {
        List<BuoiVang> ds = new ArrayList<>();
        try (Connection c = ketNoi.mo()) {
            String noiDung = coCot(c, "BuoiHoc", "NoiDungHoc")
                    ? "b.NoiDungHoc" : "CAST(NULL AS nvarchar(500))";
            String sql = """
                    SELECT s.MaSapXep,s.MaDangKy,s.MaHocVien,h.HoTen,k.TenKhoaHoc,
                           l.TenLop,b.NgayHoc,b.SoCa,dd.TrangThai,
                    """ + noiDung + """
                     AS NoiDung FROM dbo.SapXepBuoiHoc s
                    JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=s.MaSapXep
                    JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=s.MaBuoiHoc
                    JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop AND l.MaCoSo=?
                    JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=s.MaDangKy
                        AND d.MaHocVien=s.MaHocVien AND d.MaCoSo=l.MaCoSo
                    JOIN dbo.HocVien h ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                    JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                    WHERE dd.TrangThai IN ('VANG_CO_PHEP','VANG_KHONG_PHEP')
                      AND b.TrangThai='DA_DIEN_RA' AND s.TrangThai<>'HUY'
                      AND s.LoaiSapXep='CHINH_THUC'
                      AND d.TrangThai NOT IN ('HUY','DA_CHUYEN')
                    ORDER BY b.NgayHoc DESC,h.HoTen,s.MaSapXep
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, maCoSo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ds.add(new BuoiVang(rs.getInt(1),rs.getInt(2),
                            rs.getInt(3),rs.getString(4),rs.getString(5),
                            rs.getString(6),rs.getDate(7).toLocalDate(),
                            rs.getInt(8),rs.getString(9),rs.getString(10)));
                }
            }
        }
        return ds;
    }

    public List<BuoiDich> layBuoiDich(int maCoSo, int maNguon) throws SQLException {
        List<BuoiDich> ds = new ArrayList<>();
        try (Connection c = ketNoi.mo()) {
            if (!coCot(c, "BuoiHoc", "NoiDungHoc"))
                throw new SQLException("Chưa áp dụng migration GD6: thiếu nội dung buổi học. "
                        + "Có thể xem buổi vắng nhưng chưa xếp học bù trên CSDL này.");
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT b.MaBuoiHoc,l.TenLop,k.TenKhoaHoc,b.NgayHoc,b.SoCa,
                           p.TenPhong,p.SucChua,l.SiSoToiDa,
                           (SELECT COUNT(DISTINCT xd.MaHocVien)
                            FROM dbo.XepLopTrungTam x
                            JOIN dbo.DangKyKhoaHoc xd ON xd.MaDangKy=x.MaDangKy
                            WHERE x.MaLop=l.MaLop AND x.TrangThai='HIEU_LUC'
                              AND x.TuNgay<=b.NgayHoc
                              AND (x.DenNgay IS NULL OR x.DenNgay>=b.NgayHoc))
                           +(SELECT COUNT(*) FROM dbo.SapXepBuoiHoc sx
                             WHERE sx.MaBuoiHoc=b.MaBuoiHoc AND sx.LoaiSapXep='HOC_BU'
                               AND sx.TrangThai IN ('DA_XEP','HOAN_THANH')) AS DaGiuCho,
                           b.NoiDungHoc
                    FROM dbo.SapXepBuoiHoc src
                    JOIN dbo.BuoiHoc sb ON sb.MaBuoiHoc=src.MaBuoiHoc
                    JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=src.MaDangKy AND d.MaCoSo=?
                    JOIN dbo.LopTrungTam sl ON sl.MaLop=sb.MaLop
                    JOIN dbo.BuoiHoc b ON b.MaBuoiHoc<>sb.MaBuoiHoc
                           AND b.NgayHoc>sb.NgayHoc AND b.NgayHoc>=CONVERT(date,GETDATE())
                           AND b.TrangThai='DU_KIEN'
                    JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop AND l.MaCoSo=d.MaCoSo
                           AND l.MaLop<>sl.MaLop AND l.TrangThai='DANG_MO'
                    JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                    LEFT JOIN dbo.LichHocLop lh ON lh.MaLichHoc=b.MaLichHoc
                    LEFT JOIN dbo.PhongHoc p ON p.MaPhong=COALESCE(b.MaPhong,lh.MaPhong)
                           AND p.MaCoSo=l.MaCoSo
                    WHERE src.MaSapXep=? AND src.MaHocVien=d.MaHocVien
                      AND src.LoaiSapXep='CHINH_THUC'
                      AND p.TrangThai='HOAT_DONG'
                      AND NOT EXISTS (
                          SELECT 1 FROM dbo.SapXepBuoiHoc daDat
                          WHERE daDat.MaSapXepNguon=src.MaSapXep
                            AND daDat.LoaiSapXep='HOC_BU'
                            AND daDat.TrangThai IN ('DA_XEP','HOAN_THANH'))
                      AND NOT EXISTS (
                          SELECT 1 FROM dbo.XepLopTrungTam x
                          JOIN dbo.DangKyKhoaHoc dx ON dx.MaDangKy=x.MaDangKy
                          JOIN dbo.BuoiHoc bx ON bx.MaLop=x.MaLop
                          WHERE dx.MaHocVien=src.MaHocVien
                            AND x.TrangThai='HIEU_LUC'
                            AND x.TuNgay<=b.NgayHoc
                            AND (x.DenNgay IS NULL OR x.DenNgay>=b.NgayHoc)
                            AND bx.NgayHoc=b.NgayHoc AND bx.SoCa=b.SoCa
                            AND bx.TrangThai<>'HUY')
                      AND NOT EXISTS (
                          SELECT 1 FROM dbo.SapXepBuoiHoc sx
                          JOIN dbo.BuoiHoc bx ON bx.MaBuoiHoc=sx.MaBuoiHoc
                          WHERE sx.MaHocVien=src.MaHocVien
                            AND sx.TrangThai IN ('DA_XEP','HOAN_THANH')
                            AND bx.NgayHoc=b.NgayHoc AND bx.SoCa=b.SoCa
                            AND bx.TrangThai<>'HUY')
                      AND k.MaMonHoc=(SELECT MaMonHoc FROM dbo.KhoaHoc
                                       WHERE MaKhoaHoc=d.MaKhoaHoc AND MaCoSo=d.MaCoSo)
                      AND sb.NoiDungHoc IS NOT NULL AND b.NoiDungHoc IS NOT NULL
                      AND LTRIM(RTRIM(sb.NoiDungHoc))<>N''
                      AND LOWER(LTRIM(RTRIM(sb.NoiDungHoc)))=LOWER(LTRIM(RTRIM(b.NoiDungHoc)))
                    ORDER BY b.NgayHoc,b.SoCa,l.TenLop
                    """)) {
                ps.setInt(1, maCoSo); ps.setInt(2, maNguon);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Number suc = (Number) rs.getObject(7);
                        Number siSo = (Number) rs.getObject(8);
                        if (suc == null || siSo == null || suc.intValue()<1
                                || siSo.intValue()<1 || siSo.intValue()>suc.intValue())
                            continue;
                        int daGiu = rs.getInt(9);
                        if (daGiu >= Math.min(suc.intValue(),siSo.intValue())) continue;
                        ds.add(new BuoiDich(rs.getInt(1),rs.getString(2),rs.getString(3),
                                rs.getDate(4).toLocalDate(),rs.getInt(5),rs.getString(6),
                                suc.intValue(),siSo.intValue(),daGiu,rs.getString(10)));
                    }
                }
            }
        }
        return ds;
    }

    /** Giu khoa tren buoi nguon, buoi dich va dang ky trong mot transaction. */
    public int xepHocBu(int maCoSo, int maNguon, int maDich, int maTaiKhoan)
            throws SQLException {
        try (Connection c = ketNoi.mo()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                kiemTraMigration(c);
                kiemTraTaiKhoan(c,maCoSo,maTaiKhoan);
                int maBuoiNguon;
                try (PreparedStatement ps=c.prepareStatement("""
                        SELECT MaBuoiHoc FROM dbo.SapXepBuoiHoc WITH (UPDLOCK,HOLDLOCK)
                        WHERE MaSapXep=?
                        """)) {
                    ps.setInt(1,maNguon);
                    try (ResultSet rs=ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Khong tim thay luot vang");
                        maBuoiNguon=rs.getInt(1);
                    }
                }
                khoaBuoi(c,Math.min(maBuoiNguon,maDich));
                if (maBuoiNguon!=maDich) khoaBuoi(c,Math.max(maBuoiNguon,maDich));
                int dk, hv, lopNguon, khoa;
                LocalDate ngayNguon;
                String noiDung;
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT s.MaDangKy,s.MaHocVien,b.MaLop,d.MaKhoaHoc,
                               b.NgayHoc,b.NoiDungHoc
                        FROM dbo.SapXepBuoiHoc s WITH (UPDLOCK,HOLDLOCK)
                        JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=s.MaBuoiHoc
                        JOIN dbo.DangKyKhoaHoc d WITH (UPDLOCK,HOLDLOCK)
                             ON d.MaDangKy=s.MaDangKy AND d.MaHocVien=s.MaHocVien
                        JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop AND l.MaCoSo=d.MaCoSo
                        JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=s.MaSapXep
                        WHERE s.MaSapXep=? AND d.MaCoSo=? AND s.TrangThai<>'HUY'
                          AND s.LoaiSapXep='CHINH_THUC'
                          AND dd.TrangThai IN ('VANG_CO_PHEP','VANG_KHONG_PHEP')
                          AND b.TrangThai='DA_DIEN_RA'
                          AND d.TrangThai NOT IN ('HUY','DA_CHUYEN')
                        """)) {
                    ps.setInt(1,maNguon); ps.setInt(2,maCoSo);
                    try (ResultSet rs=ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Buoi vang khong hop le");
                        dk=rs.getInt(1); hv=rs.getInt(2); lopNguon=rs.getInt(3);
                        khoa=rs.getInt(4); ngayNguon=rs.getDate(5).toLocalDate();
                        noiDung=rs.getString(6);
                    }
                }
                if (noiDung==null || noiDung.isBlank())
                    throw new SQLException("Can ghi noi dung buoi vang truoc khi xep hoc bu");
                try (PreparedStatement ps=c.prepareStatement("""
                        SELECT 1 FROM dbo.SapXepBuoiHoc WITH (UPDLOCK,HOLDLOCK)
                        WHERE MaDangKy=? AND MaSapXepNguon=? AND LoaiSapXep='HOC_BU'
                          AND TrangThai IN ('DA_XEP','HOAN_THANH')
                        """)) {
                    ps.setInt(1,dk); ps.setInt(2,maNguon);
                    try (ResultSet rs=ps.executeQuery()) {
                        if (rs.next()) throw new SQLException("Buoi vang da co lich hoc bu hoat dong");
                    }
                }
                int lopDich, caDich, gioiHan;
                LocalDate ngayDich;
                try (PreparedStatement ps=c.prepareStatement("""
                        SELECT b.MaLop,b.NgayHoc,b.SoCa,b.TrangThai,b.NoiDungHoc,
                               l.SiSoToiDa,p.SucChua,l.TrangThai AS TrangThaiLop,
                               k.MaMonHoc,
                               (SELECT MaMonHoc FROM dbo.KhoaHoc WHERE MaKhoaHoc=?) AS MonNguon,
                               p.TrangThai AS TrangThaiPhong
                        FROM dbo.BuoiHoc b
                        JOIN dbo.LopTrungTam l ON l.MaLop=b.MaLop AND l.MaCoSo=?
                        JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=l.MaKhoaHoc AND k.MaCoSo=l.MaCoSo
                        LEFT JOIN dbo.LichHocLop lh ON lh.MaLichHoc=b.MaLichHoc
                        LEFT JOIN dbo.PhongHoc p ON p.MaPhong=COALESCE(b.MaPhong,lh.MaPhong)
                                              AND p.MaCoSo=l.MaCoSo
                        WHERE b.MaBuoiHoc=?
                        """)) {
                    ps.setInt(1,khoa);ps.setInt(2,maCoSo);ps.setInt(3,maDich);
                    try (ResultSet rs=ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Buoi dich khong thuoc Trung tam");
                        lopDich=rs.getInt(1); ngayDich=rs.getDate(2).toLocalDate();
                        caDich=rs.getInt(3);
                        if (lopDich==lopNguon || !"DU_KIEN".equals(rs.getString(4))
                                || !"DANG_MO".equals(rs.getString(8))
                                || !"HOAT_DONG".equals(rs.getString(11)))
                            throw new SQLException("Buoi dich phai o lop khac dang mo va chua dien ra");
                        String ndDich=rs.getString(5);
                        if (ndDich==null || !noiDung.trim().equalsIgnoreCase(ndDich.trim())
                                || rs.getInt(9)!=rs.getInt(10))
                            throw new SQLException("Mon hoc hoac noi dung buoi dich khong tuong duong");
                        Number siSo=(Number)rs.getObject(6), suc=(Number)rs.getObject(7);
                        if (siSo==null || suc==null || siSo.intValue()<1
                                || suc.intValue()<1 || siSo.intValue()>suc.intValue())
                            throw new SQLException("Lop dich can si so toi da va phong co suc chua hop le");
                        gioiHan=Math.min(siSo.intValue(),suc.intValue());
                    }
                }
                if (!ngayDich.isAfter(ngayNguon) || ngayDich.isBefore(LocalDate.now()))
                    throw new SQLException("Buoi hoc bu phai sau buoi vang va chua dien ra");
                if (ngayDich.equals(LocalDate.now())
                        && !LocalTime.now().isBefore(
                                CaHocTrungTam.layTheoSoCa(caDich).batDau()))
                    throw new SQLException("Ca hoc bu hom nay da bat dau");
                try (PreparedStatement ps=c.prepareStatement("""
                        SELECT COUNT(DISTINCT d.MaHocVien) FROM dbo.XepLopTrungTam x WITH (HOLDLOCK)
                        JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=x.MaDangKy
                        WHERE x.MaLop=? AND x.TrangThai='HIEU_LUC'
                          AND x.TuNgay<=? AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                        """)) {
                    ps.setInt(1,lopDich);ps.setDate(2,Date.valueOf(ngayDich));
                    ps.setDate(3,Date.valueOf(ngayDich));
                    try (ResultSet rs=ps.executeQuery()) {
                        rs.next();
                        int chinh=rs.getInt(1);
                        try (PreparedStatement bu=c.prepareStatement("""
                                SELECT COUNT(*) FROM dbo.SapXepBuoiHoc WITH (UPDLOCK,HOLDLOCK)
                                WHERE MaBuoiHoc=? AND LoaiSapXep='HOC_BU'
                                  AND TrangThai IN ('DA_XEP','HOAN_THANH')
                                """)) {
                            bu.setInt(1,maDich);
                            try (ResultSet br=bu.executeQuery()) {
                                br.next();
                                if (chinh+br.getInt(1)>=gioiHan)
                                    throw new SQLException("Lop hoac phong da du si so");
                            }
                        }
                    }
                }
                try (PreparedStatement ps=c.prepareStatement("""
                        SELECT 1 FROM dbo.XepLopTrungTam x WITH (HOLDLOCK)
                        JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=x.MaDangKy
                        JOIN dbo.BuoiHoc b ON b.MaLop=x.MaLop
                        WHERE d.MaHocVien=? AND x.TrangThai='HIEU_LUC'
                          AND x.TuNgay<=? AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                          AND b.NgayHoc=? AND b.SoCa=? AND b.TrangThai<>'HUY'
                        UNION ALL
                        SELECT 1 FROM dbo.SapXepBuoiHoc s WITH (HOLDLOCK)
                        JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=s.MaBuoiHoc
                        WHERE s.MaHocVien=? AND s.TrangThai IN ('DA_XEP','HOAN_THANH')
                          AND b.NgayHoc=? AND b.SoCa=? AND b.TrangThai<>'HUY'
                        """)) {
                    ps.setInt(1,hv);ps.setDate(2,Date.valueOf(ngayDich));
                    ps.setDate(3,Date.valueOf(ngayDich));
                    ps.setDate(4,Date.valueOf(ngayDich));ps.setInt(5,caDich);
                    ps.setInt(6,hv);ps.setDate(7,Date.valueOf(ngayDich));ps.setInt(8,caDich);
                    try (ResultSet rs=ps.executeQuery()) {
                        if (rs.next()) throw new SQLException("Hoc vien trung ca voi lop chinh hoac lich khac");
                    }
                }
                int maMoi;
                try (PreparedStatement ps=c.prepareStatement("""
                        INSERT dbo.SapXepBuoiHoc
                          (MaDangKy,MaHocVien,MaBuoiHoc,LoaiSapXep,MaSapXepNguon,TrangThai)
                        VALUES (?,?,?,'HOC_BU',?,'DA_XEP')
                        """,Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1,dk);ps.setInt(2,hv);ps.setInt(3,maDich);ps.setInt(4,maNguon);
                    if (ps.executeUpdate()!=1) throw new SQLException("Khong xep duoc hoc bu");
                    try (ResultSet rs=ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Thieu ma luot hoc bu");
                        maMoi=rs.getInt(1);
                    }
                }
                c.commit();
                return maMoi;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public void huyHocBu(int maCoSo,int maSapXep,int maTaiKhoan,String lyDo)
            throws SQLException {
        if (lyDo==null || lyDo.isBlank() || lyDo.length()>500)
            throw new IllegalArgumentException("Can ly do huy hoc bu (toi da 500 ky tu)");
        try (Connection c=ketNoi.mo()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                kiemTraMigration(c); kiemTraTaiKhoan(c,maCoSo,maTaiKhoan);
                try (PreparedStatement ps=c.prepareStatement("""
                        SELECT b.NgayHoc,b.TrangThai,b.SoCa
                        FROM dbo.SapXepBuoiHoc s WITH (UPDLOCK,HOLDLOCK)
                        JOIN dbo.BuoiHoc b WITH (UPDLOCK,HOLDLOCK) ON b.MaBuoiHoc=s.MaBuoiHoc
                        JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=s.MaDangKy AND d.MaCoSo=?
                        WHERE s.MaSapXep=? AND s.LoaiSapXep='HOC_BU' AND s.TrangThai='DA_XEP'
                          AND NOT EXISTS (SELECT 1 FROM dbo.DiemDanhTrungTam dd
                                          WHERE dd.MaSapXep=s.MaSapXep)
                        """)) {
                    ps.setInt(1,maCoSo);ps.setInt(2,maSapXep);
                    try (ResultSet rs=ps.executeQuery()) {
                        if (!rs.next() || rs.getDate(1).toLocalDate().isBefore(LocalDate.now())
                                || !"DU_KIEN".equals(rs.getString(2))
                                || (rs.getDate(1).toLocalDate().equals(LocalDate.now())
                                    && !LocalTime.now().isBefore(
                                        CaHocTrungTam.layTheoSoCa(rs.getInt(3)).batDau())))
                            throw new SQLException("Chi duoc huy lich hoc bu chua dien ra, chua diem danh");
                    }
                }
                try (PreparedStatement ps=c.prepareStatement("""
                        UPDATE dbo.SapXepBuoiHoc SET TrangThai='HUY',LyDoHuy=?,
                              NgayHuy=SYSUTCDATETIME(),MaTaiKhoanHuy=?
                        WHERE MaSapXep=? AND TrangThai='DA_XEP'
                        """)) {
                    ps.setString(1,lyDo.trim());ps.setInt(2,maTaiKhoan);ps.setInt(3,maSapXep);
                    if (ps.executeUpdate()!=1) throw new SQLException("Lich hoc bu da thay doi");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    private static void khoaBuoi(Connection c,int maBuoi) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("""
                SELECT 1 FROM dbo.BuoiHoc WITH (UPDLOCK,HOLDLOCK) WHERE MaBuoiHoc=?
                """)) {
            ps.setInt(1,maBuoi);
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Buoi hoc khong ton tai");
            }
        }
    }
    private static void kiemTraTaiKhoan(Connection c,int coSo,int taiKhoan)
            throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("""
                SELECT 1 FROM dbo.TaiKhoan t
                JOIN dbo.TaiKhoanCoSo tc ON tc.MaTaiKhoan=t.MaTaiKhoan
                WHERE t.MaTaiKhoan=? AND tc.MaCoSo=? AND t.TrangThai=1 AND tc.TrangThai=1
                """)) {
            ps.setInt(1,taiKhoan);ps.setInt(2,coSo);
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Tai khoan khong thuoc co so");
            }
        }
    }
    private static void kiemTraMigration(Connection c) throws SQLException {
        if (!coCot(c,"BuoiHoc","NoiDungHoc")
                || !coCot(c,"SapXepBuoiHoc","LyDoHuy")
                || !coCot(c,"SapXepBuoiHoc","MaTaiKhoanHuy"))
            throw new SQLException("Chua du migration GD6; khong ghi hoc bu vao CSDL nay");
    }
    private static boolean coCot(Connection c,String bang,String cot) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("SELECT COL_LENGTH(?,?)")) {
            ps.setString(1,"dbo."+bang);ps.setString(2,cot);
            try (ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getObject(1)!=null; }
        }
    }
}
