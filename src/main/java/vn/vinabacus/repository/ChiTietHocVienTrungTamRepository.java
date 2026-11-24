package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.ChiTietHocVienTrungTam;
import vn.vinabacus.model.ChiTietHocVienTrungTam.Khoa;
import vn.vinabacus.model.ChiTietHocVienTrungTam.SuKien;
import vn.vinabacus.model.ChiTietHocVienTrungTam.TongQuan;
import vn.vinabacus.model.DangKyKhoaHoc;

/** Chi doc du lieu da luu; khong suy dien diem danh hay lich su khong ton tai. */
public final class ChiTietHocVienTrungTamRepository {
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    @FunctionalInterface public interface KetNoi { Connection mo() throws SQLException; }
    private final KetNoi ketNoi;
    public ChiTietHocVienTrungTamRepository() { this(Database::moKetNoi); }
    public ChiTietHocVienTrungTamRepository(KetNoi ketNoi) {
        this.ketNoi = Objects.requireNonNull(ketNoi);
    }

    private static final class Gom {
        final List<String> tenKhoa = new ArrayList<>();
        int daXep;
        int dangCho;
        boolean hoSoTam;

        TongQuan ketQua() {
            return new TongQuan(List.copyOf(tenKhoa), tenKhoa.size(), daXep,
                    dangCho, hoSoTam);
        }
    }

    /** Tom tat tat ca hoc vien de bo loc khong nham "chua dang ky" voi "cho lop". */
    public Map<Integer, TongQuan> tongQuan(int maCoSo) throws SQLException {
        String sql = """
                SELECT d.MaHocVien, k.TenKhoaHoc, d.TrangThai,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.XepLopTrungTam x
                           WHERE x.MaDangKy=d.MaDangKy AND x.MaCoSo=d.MaCoSo
                             AND x.TrangThai<>'HUY'
                       ) THEN 1 ELSE 0 END AS CoLop
                FROM dbo.DangKyKhoaHoc d
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                WHERE d.MaCoSo=? AND d.TrangThai<>'HUY'
                ORDER BY d.MaDangKy
                """;
        Map<Integer, Gom> gom = new HashMap<>();
        try (Connection c = ketNoi.mo()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, maCoSo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Gom g = gom.computeIfAbsent(rs.getInt("MaHocVien"), id -> new Gom());
                        g.tenKhoa.add(rs.getString("TenKhoaHoc"));
                        if (rs.getInt("CoLop") == 1
                                && !"CHO_XEP_LOP".equals(rs.getString("TrangThai"))) g.daXep++;
                        else g.dangCho++;
                    }
                }
            }
            // Dang ky co FK HocVien nen ho so cho xep lop co the tao dong HocVien tam.
            // Dong nay chi duoc hien trong Quan ly hoc vien sau khi chuyen doi ro rang.
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT DISTINCT y.MaHocVien
                    FROM dbo.YeuCauXepLop y
                    INNER JOIN dbo.KhachTiemNang k
                        ON k.MaKhach=y.MaKhach AND k.MaCoSo=y.MaCoSo
                    WHERE y.MaCoSo=? AND k.TrangThai<>'DA_CHUYEN_DOI'
                    """)) {
                ps.setInt(1, maCoSo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        gom.computeIfAbsent(rs.getInt(1), id -> new Gom()).hoSoTam = true;
                    }
                }
            }
        }
        Map<Integer, TongQuan> ketQua = new HashMap<>();
        gom.forEach((id, g) -> ketQua.put(id, g.ketQua()));
        return ketQua;
    }

    public ChiTietHocVienTrungTam tai(int maCoSo, int maHocVien) throws SQLException {
        List<Khoa> cacKhoa = new ArrayList<>();
        List<SuKien> suKien = new ArrayList<>();
        try (Connection c = ketNoi.mo()) {
            boolean coNoiDungHoc;
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT COL_LENGTH('dbo.BuoiHoc','NoiDungHoc')");
                 ResultSet rs = check.executeQuery()) {
                rs.next(); coNoiDungHoc = rs.getObject(1) != null;
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT 1 FROM dbo.HocVien WHERE MaHocVien=? AND MaCoSo=?
                    """)) {
                ps.setInt(1, maHocVien);
                ps.setInt(2, maCoSo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không tìm thấy học viên Trung tâm");
                }
            }
            String dieuKienHocBu = coNoiDungHoc ? """
                    AND (sx.LoaiSapXep<>'HOC_BU' OR
                         (src.MaSapXep IS NOT NULL AND srcdd.TrangThai IN
                          ('VANG_CO_PHEP','VANG_KHONG_PHEP')
                          AND srcb.TrangThai='DA_DIEN_RA'
                          AND srcb.NoiDungHoc IS NOT NULL AND b.NoiDungHoc IS NOT NULL
                          AND LOWER(LTRIM(RTRIM(srcb.NoiDungHoc)))=
                              LOWER(LTRIM(RTRIM(b.NoiDungHoc)))))
                    """ : " AND sx.LoaiSapXep<>'HOC_BU' ";
            String sql = """
                    SELECT d.MaDangKy,d.MaHocVien,h.HoTen,d.MaKhoaHoc,
                           m.TenMonHoc,k.TenKhoaHoc,d.TongSoBuoiDangKy,
                           d.HocPhiThoaThuan,d.SoTienGiam,d.HocPhiSauGiam,
                           COALESCE(thu.DaDong,0) AS DaDong,
                           d.NgayDangKy,d.NgayBatDau,
                           d.NgayKetThucDuKien,d.NgayKetThucThucTe,d.TrangThai,
                           x.MaLop,l.TenLop,l.NgayKhaiGiang,nv.HoTen AS TenGiaoVien,
                           (SELECT COUNT(DISTINCT CASE WHEN sx.LoaiSapXep IN ('HOC_BU','DOI_BUOI')
                                        THEN sx.MaSapXepNguon ELSE sx.MaSapXep END)
                            FROM dbo.SapXepBuoiHoc sx
                            INNER JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=sx.MaBuoiHoc
                            INNER JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=sx.MaSapXep
                            LEFT JOIN dbo.SapXepBuoiHoc src ON src.MaSapXep=sx.MaSapXepNguon
                            LEFT JOIN dbo.DiemDanhTrungTam srcdd ON srcdd.MaSapXep=src.MaSapXep
                            LEFT JOIN dbo.BuoiHoc srcb ON srcb.MaBuoiHoc=src.MaBuoiHoc
                            WHERE sx.MaDangKy=d.MaDangKy AND sx.TrangThai='HOAN_THANH'
                              AND b.TrangThai='DA_DIEN_RA' AND dd.TrangThai='CO_MAT'
                    """ + dieuKienHocBu + """
                           ) AS SoBuoiHoanThanh,
                           (SELECT COUNT(*) FROM dbo.SapXepBuoiHoc sx
                            JOIN dbo.DiemDanhTrungTam dd ON dd.MaSapXep=sx.MaSapXep
                            JOIN dbo.BuoiHoc b ON b.MaBuoiHoc=sx.MaBuoiHoc
                            WHERE sx.MaDangKy=d.MaDangKy
                              AND sx.TrangThai<>'HUY'
                              AND b.TrangThai='DA_DIEN_RA') AS SoDiemDanhDaGhi
                    FROM dbo.DangKyKhoaHoc d
                    INNER JOIN dbo.HocVien h
                        ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                    INNER JOIN dbo.KhoaHoc k
                        ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                    INNER JOIN dbo.MonHoc m
                        ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                    OUTER APPLY (
                        SELECT TOP (1) xl.MaLop
                        FROM dbo.XepLopTrungTam xl
                        WHERE xl.MaDangKy=d.MaDangKy AND xl.MaCoSo=d.MaCoSo
                          AND xl.TrangThai<>'HUY'
                        ORDER BY CASE WHEN xl.TrangThai='HIEU_LUC'
                                         AND (xl.DenNgay IS NULL
                                              OR xl.DenNgay>=CONVERT(date, GETDATE()))
                                      THEN 0 ELSE 1 END, xl.MaXepLop DESC
                    ) x
                    LEFT JOIN dbo.LopTrungTam l
                        ON l.MaLop=x.MaLop AND l.MaCoSo=d.MaCoSo
                    LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien=l.MaGiaoVien
                    OUTER APPLY (
                        SELECT SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO'
                                        THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END) AS DaDong
                        FROM dbo.KhoanPhaiThu kt
                        INNER JOIN dbo.ChiTietPhieuThu ct
                            ON ct.MaKhoanThu=kt.MaKhoanThu AND ct.MaCoSo=kt.MaCoSo
                        INNER JOIN dbo.PhieuThu p
                            ON p.MaPhieuThu=ct.MaPhieuThu AND p.MaCoSo=ct.MaCoSo
                           AND p.TrangThai='XAC_NHAN'
                        WHERE kt.MaDangKy=d.MaDangKy AND kt.MaCoSo=d.MaCoSo
                          AND kt.TrangThai='HIEU_LUC'
                    ) thu
                    WHERE d.MaHocVien=? AND d.MaCoSo=?
                    ORDER BY d.MaDangKy DESC
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql);
                 PreparedStatement lich = c.prepareStatement("""
                         SELECT ThuTrongTuan,GioBatDau,GioKetThuc,TuNgay,DenNgay
                         FROM dbo.LichHocLop
                         WHERE MaLop=? AND TrangThai='HIEU_LUC'
                         ORDER BY ThuTrongTuan,GioBatDau,TuNgay
                         """)) {
                ps.setInt(1, maHocVien);
                ps.setInt(2, maCoSo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Integer maLop = soNguyen(rs, "MaLop");
                        LocalDate ngayKhaiGiang = ngay(rs, "NgayKhaiGiang");
                        int hoanThanh = rs.getInt("SoBuoiHoanThanh");
                        Integer soBuoiTinCay = null;
                        if (hoanThanh > 0 || rs.getInt("SoDiemDanhDaGhi") > 0)
                            soBuoiTinCay = hoanThanh;
                        else if (maLop != null && ngayKhaiGiang != null
                                && ngayKhaiGiang.isAfter(LocalDate.now())) soBuoiTinCay = 0;
                        DangKyKhoaHoc dk = new DangKyKhoaHoc(rs.getInt("MaDangKy"),
                                rs.getInt("MaHocVien"), null, rs.getString("HoTen"),
                                rs.getInt("MaKhoaHoc"), rs.getString("TenMonHoc"),
                                rs.getString("TenKhoaHoc"), rs.getInt("TongSoBuoiDangKy"),
                                rs.getBigDecimal("HocPhiSauGiam"),
                                ngay(rs, "NgayBatDau"), rs.getString("TrangThai"),
                                maLop, rs.getString("TenLop"));
                        cacKhoa.add(new Khoa(dk, rs.getString("TenGiaoVien"),
                                List.of(),
                                ngayKhaiGiang, ngay(rs, "NgayKetThucDuKien"),
                                ngay(rs, "NgayKetThucThucTe"), soBuoiTinCay,
                                rs.getBigDecimal("HocPhiThoaThuan"),
                                rs.getBigDecimal("SoTienGiam"),
                                rs.getBigDecimal("DaDong")));
                        suKien.add(new SuKien(ngay(rs, "NgayDangKy"),
                                "Đăng ký khóa", dk.tenKhoaHoc() + " (#" + dk.maDangKy() + ")"));
                        if ("HOAN_THANH".equals(dk.trangThai())
                                && ngay(rs, "NgayKetThucThucTe") != null) {
                            suKien.add(new SuKien(ngay(rs, "NgayKetThucThucTe"),
                                    "Hoàn thành khóa", dk.tenKhoaHoc()));
                        }
                    }
                }
                // Khong mo ResultSet thu hai tren cung ket noi khi ResultSet chinh con mo
                // (cau hinh SQL Server hien tai khong bat MARS).
                for (int i = 0; i < cacKhoa.size(); i++) {
                    Khoa k = cacKhoa.get(i);
                    Integer maLop = k.dangKy().maLop();
                    if (maLop == null) continue;
                    cacKhoa.set(i, new Khoa(k.dangKy(), k.tenGiaoVien(),
                            lich(lich, maLop), k.ngayKhaiGiang(),
                            k.ngayKetThucDuKien(), k.ngayKetThucThucTe(),
                            k.soBuoiHoanThanh(), k.hocPhiGoc(),
                            k.soTienGiam(), k.daDong()));
                }
            }
            lichSuXepLop(c, maCoSo, maHocVien, suKien);
            lichSuChuyenLop(c, maCoSo, maHocVien, suKien);
        }
        suKien.sort(Comparator.comparing(SuKien::ngay,
                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        return new ChiTietHocVienTrungTam(List.copyOf(cacKhoa), List.copyOf(suKien));
    }

    private static List<String> lich(PreparedStatement ps, int maLop)
            throws SQLException {
        ps.setInt(1, maLop);
        List<String> ds = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Time dau = rs.getTime("GioBatDau"), cuoi = rs.getTime("GioKetThuc");
                LocalDate tu = ngay(rs, "TuNgay"), den = ngay(rs, "DenNgay");
                String gio = dau == null || cuoi == null ? "chưa xác định giờ"
                        : dau.toLocalTime() + "–" + cuoi.toLocalTime();
                ds.add("Thứ " + rs.getInt("ThuTrongTuan") + " · " + gio
                        + " · từ " + (tu == null ? "?" : NGAY.format(tu))
                        + (den == null ? "" : " đến " + NGAY.format(den)));
            }
        }
        return ds;
    }

    private static void lichSuXepLop(Connection c, int maCoSo, int maHocVien,
                                     List<SuKien> suKien) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT x.TuNgay,l.TenLop,k.TenKhoaHoc
                FROM dbo.XepLopTrungTam x
                INNER JOIN dbo.DangKyKhoaHoc d
                    ON d.MaDangKy=x.MaDangKy AND d.MaCoSo=x.MaCoSo
                INNER JOIN dbo.LopTrungTam l
                    ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                WHERE d.MaHocVien=? AND d.MaCoSo=? AND x.TrangThai<>'HUY'
                """)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) suKien.add(new SuKien(ngay(rs, "TuNgay"),
                        "Xếp lớp", rs.getString("TenKhoaHoc") + " → "
                        + rs.getString("TenLop")));
            }
        }
    }

    private static void lichSuChuyenLop(Connection c, int maCoSo, int maHocVien,
                                        List<SuKien> suKien) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT cl.NgayChuyen,cl.LoaiChuyen,cl.LyDo
                FROM dbo.ChuyenLopTrungTam cl
                INNER JOIN dbo.DangKyKhoaHoc d ON d.MaDangKy=cl.MaDangKyCu
                WHERE d.MaHocVien=? AND cl.MaCoSo=?
                """)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) suKien.add(new SuKien(ngay(rs, "NgayChuyen"),
                        "Chuyển lớp", rs.getString("LoaiChuyen") + " · "
                        + rs.getString("LyDo")));
            }
        }
    }

    private static LocalDate ngay(ResultSet rs, String cot) throws SQLException {
        Date d = rs.getDate(cot);
        return d == null ? null : d.toLocalDate();
    }

    private static Integer soNguyen(ResultSet rs, String cot) throws SQLException {
        Number n = (Number) rs.getObject(cot);
        return n == null ? null : n.intValue();
    }
}
