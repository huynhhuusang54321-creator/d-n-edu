package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.KhachTiemNang;

/** Read-only detail data for a Center admission record. */
public final class TuyenSinhChiTietRepository {
    public record SuKien(LocalDateTime thoiDiem, boolean chiCoNgay,
                         String loai, String noiDung) { }
    public record ChiTiet(KhachTiemNang hoSo, HocVien hocVien,
                          List<SuKien> lichSu) { }

    public ChiTiet lay(int maKhach, int maCoSo) throws SQLException {
        try (Connection c = Database.moKetNoi()) {
            KhachTiemNang hoSo;
            List<SuKien> lichSu = new ArrayList<>();
            boolean coTruongMoi;
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT CASE WHEN COL_LENGTH('dbo.KhachTiemNang','NgaySinh') IS NOT NULL AND COL_LENGTH('dbo.KhachTiemNang','GioiTinh') IS NOT NULL AND COL_LENGTH('dbo.KhachTiemNang','DiaChi') IS NOT NULL THEN 1 ELSE 0 END")){
                r.next();coTruongMoi=r.getInt(1)==1;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT HoTen,SoDienThoai,TenNguoiLienHe,NhuCau,TrangThai,"
                    + "DaHocThu,GhiChu,NgayTiepNhan,"+
                    (coTruongMoi?"NgaySinh,GioiTinh,DiaChi":"CAST(NULL AS DATE) AS NgaySinh,CAST(NULL AS VARCHAR(15)) AS GioiTinh,CAST(NULL AS NVARCHAR(400)) AS DiaChi")+
                    " FROM dbo.KhachTiemNang "
                    + "WHERE MaKhach=? AND MaCoSo=?")) {
                ps.setInt(1, maKhach);
                ps.setInt(2, maCoSo);
                try (ResultSet r = ps.executeQuery()) {
                    if (!r.next()) throw new SQLException("Không tìm thấy hồ sơ tuyển sinh.");
                    hoSo = new KhachTiemNang(maKhach, maCoSo, r.getString("HoTen"),
                            r.getString("SoDienThoai"), r.getString("TenNguoiLienHe"),
                            r.getString("NhuCau"), r.getString("TrangThai"),
                            r.getBoolean("DaHocThu"), r.getString("GhiChu"),
                            r.getDate("NgaySinh")==null?null:r.getDate("NgaySinh").toLocalDate(),
                            r.getString("GioiTinh"),r.getString("DiaChi"));
                    them(lichSu, r.getTimestamp("NgayTiepNhan"),
                            "Tiếp nhận hồ sơ", "Hồ sơ tuyển sinh #" + maKhach);
                }
            }

            Integer maHocVien = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT DISTINCT MaHocVien FROM dbo.YeuCauXepLop "
                    + "WHERE MaKhach=? AND MaCoSo=?")) {
                ps.setInt(1, maKhach);
                ps.setInt(2, maCoSo);
                try (ResultSet r = ps.executeQuery()) {
                    while (r.next()) {
                        int id = r.getInt(1);
                        if (maHocVien != null && maHocVien != id)
                            throw new SQLException("Hồ sơ tuyển sinh liên kết nhiều học viên; cần kiểm tra dữ liệu.");
                        maHocVien = id;
                    }
                }
            }
            HocVien hocVien = maHocVien == null ? null : layHocVien(c, maHocVien, maCoSo);
            layTuVan(c, maKhach, lichSu);
            layYeuCau(c, maKhach, maCoSo, lichSu);
            layDangKy(c, maKhach, maCoSo, lichSu);
            layXepLop(c, maKhach, maCoSo, lichSu);
            lichSu.sort(Comparator.comparing(SuKien::thoiDiem).reversed());
            return new ChiTiet(hoSo, hocVien, List.copyOf(lichSu));
        }
    }

    private HocVien layHocVien(Connection c, int maHocVien, int maCoSo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT HoTen,NgaySinh,GioiTinh,TenNguoiLienHe,SoDienThoaiLienHe,"
                + "DiaChi,GhiChu,TrangThaiHoSo FROM dbo.HocVien "
                + "WHERE MaHocVien=? AND MaCoSo=?")) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            try (ResultSet r = ps.executeQuery()) {
                if (!r.next()) throw new SQLException("Không tìm thấy học viên liên kết.");
                Date ngay = r.getDate("NgaySinh");
                return new HocVien(maHocVien, maCoSo, r.getString("HoTen"),
                        ngay == null ? null : ngay.toLocalDate(), r.getString("GioiTinh"),
                        r.getString("TenNguoiLienHe"), r.getString("SoDienThoaiLienHe"),
                        r.getString("DiaChi"), r.getString("GhiChu"),
                        r.getString("TrangThaiHoSo"));
            }
        }
    }

    private void layTuVan(Connection c, int maKhach, List<SuKien> out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT NgayTuVan,NoiDung,KetQua FROM dbo.LichSuTuVan WHERE MaKhach=?")) {
            ps.setInt(1, maKhach);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    String noiDung = r.getString("NoiDung");
                    String ketQua = r.getString("KetQua");
                    them(out, r.getTimestamp("NgayTuVan"), "Tư vấn",
                            noiDung + (ketQua == null || ketQua.isBlank() ? "" : " · " + ketQua));
                }
            }
        }
    }

    private void layYeuCau(Connection c, int maKhach, int maCoSo,
                            List<SuKien> out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT y.MaYeuCau,y.NgayXacNhan,y.NgayHoanTat,k.TenKhoaHoc "
                + "FROM dbo.YeuCauXepLop y LEFT JOIN dbo.KhoaHoc k "
                + "ON k.MaKhoaHoc=y.MaKhoaHoc AND k.MaCoSo=y.MaCoSo "
                + "WHERE y.MaKhach=? AND y.MaCoSo=?")) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    String khoa = r.getString("TenKhoaHoc");
                    String moTa = "Yêu cầu #" + r.getInt("MaYeuCau")
                            + (khoa == null ? "" : " · " + khoa);
                    them(out, r.getTimestamp("NgayXacNhan"), "Yêu cầu xếp lớp", moTa);
                    them(out, r.getTimestamp("NgayHoanTat"), "Hoàn tất yêu cầu", moTa);
                }
            }
        }
    }

    private void layDangKy(Connection c, int maKhach, int maCoSo,
                            List<SuKien> out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT d.MaDangKy,d.NgayDangKy,k.TenKhoaHoc "
                + "FROM dbo.DangKyKhoaHoc d JOIN dbo.YeuCauXepLop y "
                + "ON y.MaYeuCau=d.MaYeuCau AND y.MaCoSo=d.MaCoSo "
                + "JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo "
                + "WHERE y.MaKhach=? AND y.MaCoSo=?")) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    Date ngay = r.getDate("NgayDangKy");
                    themNgay(out, ngay == null ? null : ngay.toLocalDate(), "Đăng ký khóa",
                            r.getString("TenKhoaHoc") + " · đăng ký #" + r.getInt("MaDangKy"));
                }
            }
        }
    }

    private void layXepLop(Connection c, int maKhach, int maCoSo,
                            List<SuKien> out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT x.TuNgay,x.DenNgay,x.TrangThai,l.TenLop,k.TenKhoaHoc "
                + "FROM dbo.XepLopTrungTam x JOIN dbo.DangKyKhoaHoc d "
                + "ON d.MaDangKy=x.MaDangKy AND d.MaCoSo=x.MaCoSo "
                + "JOIN dbo.YeuCauXepLop y ON y.MaYeuCau=d.MaYeuCau "
                + "AND y.MaCoSo=d.MaCoSo "
                + "JOIN dbo.LopTrungTam l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo "
                + "JOIN dbo.KhoaHoc k ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo "
                + "WHERE y.MaKhach=? AND y.MaCoSo=?")) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    String moTa = r.getString("TenKhoaHoc") + " · " + r.getString("TenLop");
                    Date tuNgay = r.getDate("TuNgay");
                    Date denNgay = r.getDate("DenNgay");
                    themNgay(out, tuNgay == null ? null : tuNgay.toLocalDate(),
                            "Bắt đầu xếp lớp", moTa);
                    if (denNgay != null)
                        themNgay(out, denNgay.toLocalDate(), "Kết thúc xếp lớp", moTa);
                }
            }
        }
    }

    private static void them(List<SuKien> out, Timestamp ngay,
                             String loai, String noiDung) {
        if (ngay != null) out.add(new SuKien(ngay.toLocalDateTime(), false, loai, noiDung));
    }

    private static void themNgay(List<SuKien> out, LocalDate ngay,
                                 String loai, String noiDung) {
        if (ngay != null) out.add(new SuKien(ngay.atStartOfDay(), true, loai, noiDung));
    }
}
