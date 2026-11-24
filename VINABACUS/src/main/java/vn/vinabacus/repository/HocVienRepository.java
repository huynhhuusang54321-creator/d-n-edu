package vn.vinabacus.repository;

import vn.vinabacus.repository.*;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.HocVien;
import java.sql.*;
import java.sql.Date;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

/**
 * Truy vấn dbo.HocVien có cache theo từng cơ sở (maCoSo).
 * Cache nằm trên RAM, được nạp lần đầu khi cần và bị xóa khi có thêm/sửa/xóa.
 * Mọi lệnh ghi phải gọi xoaCache(maCoSo) sau khi thành công.
 */
public final class HocVienRepository {

    private static final int LOI_RANG_BUOC_KHOA_NGOAI = 547;
    private static final String SQL_LAY_TAT_CA = "SELECT MaHocVien,MaCoSo,HoTen,NgaySinh,GioiTinh,TenNguoiLienHe,"
            + "SoDienThoaiLienHe,DiaChi,GhiChu,TrangThaiHoSo FROM dbo.HocVien "
            + "WHERE MaCoSo=? ORDER BY MaHocVien DESC";

    // Cache dùng chung cho mọi instance trong cùng JVM: maCoSo -> danh sách học viên
    private static final Map<Integer, List<HocVien>> CACHE = new HashMap<>();
    private static final Object KHOA = new Object();

    // ================= Đọc (có cache) =================

    /** Lọc trong cache theo trạng thái và từ khóa (mã, họ tên không dấu, SĐT người liên hệ). */
    public List<HocVien> timKiem(int maCoSo, String tuKhoa, String trangThai) throws SQLException {
        List<HocVien> nguon = layCache(maCoSo);
        String tk = tuKhoa == null ? "" : tuKhoa.trim();
        String tkKhongDau = chuanHoa(tk);
        boolean coTuKhoa = !tk.isEmpty();

        List<HocVien> ketQua = new ArrayList<>();
        for (HocVien h : nguon) {
            if (!"TAT_CA".equals(trangThai) && !trangThai.equals(h.trangThaiHoSo())) continue;
            if (coTuKhoa && !khop(h, tk, tkKhongDau)) continue;
            ketQua.add(h);
        }
        return ketQua;
    }

    private static boolean khop(HocVien h, String tk, String tkKhongDau) {
        if (String.valueOf(h.maHocVien()).contains(tk)) return true;
        if (chuanHoa(h.hoTen()).contains(tkKhongDau)) return true;
        return h.soDienThoaiLienHe() != null && h.soDienThoaiLienHe().contains(tk);
    }

    /** Trả về cache của cơ sở; nếu chưa có thì nạp từ CSDL. */
    private List<HocVien> layCache(int maCoSo) throws SQLException {
        synchronized (KHOA) {
            List<HocVien> da = CACHE.get(maCoSo);
            if (da == null) {
                da = List.copyOf(taiTuCSDL(maCoSo));
                CACHE.put(maCoSo, da);
            }
            return da;
        }
    }

    private List<HocVien> taiTuCSDL(int maCoSo) throws SQLException {
        List<HocVien> ds = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(SQL_LAY_TAT_CA)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ds.add(doc(rs));
            }
        }
        return ds;
    }

    // ================= Ghi (xóa cache sau khi thành công) =================

    public int them(HocVien h) throws SQLException {
        String sql = "INSERT dbo.HocVien(MaCoSo,HoTen,NgaySinh,GioiTinh,TenNguoiLienHe,"
                + "SoDienThoaiLienHe,DiaChi,GhiChu,TrangThaiHoSo) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, h.maCoSo());
            ps.setString(2, h.hoTen());
            setNgay(ps, 3, h.ngaySinh());
            ps.setString(4, h.gioiTinh());
            ps.setString(5, h.tenNguoiLienHe());
            ps.setString(6, h.soDienThoaiLienHe());
            ps.setString(7, h.diaChi());
            ps.setString(8, h.ghiChu());
            ps.setString(9, h.trangThaiHoSo());
            ps.executeUpdate();

            int maMoi;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được mã học viên vừa thêm");
                maMoi = keys.getInt(1);
            }
            xoaCache(h.maCoSo());
            return maMoi;
        }
    }

    public boolean sua(HocVien h) throws SQLException {
        String sql = "UPDATE dbo.HocVien SET HoTen=?,NgaySinh=?,GioiTinh=?,TenNguoiLienHe=?,"
                + "SoDienThoaiLienHe=?,DiaChi=?,GhiChu=?,TrangThaiHoSo=? WHERE MaHocVien=? AND MaCoSo=?";
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, h.hoTen());
            setNgay(ps, 2, h.ngaySinh());
            ps.setString(3, h.gioiTinh());
            ps.setString(4, h.tenNguoiLienHe());
            ps.setString(5, h.soDienThoaiLienHe());
            ps.setString(6, h.diaChi());
            ps.setString(7, h.ghiChu());
            ps.setString(8, h.trangThaiHoSo());
            ps.setInt(9, h.maHocVien());
            ps.setInt(10, h.maCoSo());
            boolean ok = ps.executeUpdate() == 1;
            if (ok) xoaCache(h.maCoSo());
            return ok;
        }
    }

    /**
     * Xóa cứng học viên. Nếu học viên đã có lớp, học phí hoặc dữ liệu liên quan
     * thì SQL Server chặn bằng khóa ngoại và ném lỗi có thông báo tiếng Việt.
     */
    public void xoa(int maCoSo, int maHocVien) throws SQLException {
        String sql = "DELETE FROM dbo.HocVien WHERE MaHocVien=? AND MaCoSo=?";
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Không tìm thấy học viên mã " + maHocVien + " trong cơ sở này");
            }
            xoaCache(maCoSo);
        } catch (SQLException e) {
            if (e.getErrorCode() == LOI_RANG_BUOC_KHOA_NGOAI) {
                // Không gắn cause để màn hình hiện thông báo tiếng Việt, không hiện lỗi SQL gốc
                throw new SQLException("Học viên đã có dữ liệu liên quan (lớp học, học phí...), "
                        + "không thể xóa. Hãy đổi trạng thái sang 'Ngừng sử dụng'.");
            }
            throw e;
        }
    }

    // ================= Quản lý cache =================

    /** Xóa cache của một cơ sở. Gọi sau mọi thay đổi bảng HocVien, kể cả từ repository khác. */
    public static void xoaCache(int maCoSo) {
        synchronized (KHOA) {
            CACHE.remove(maCoSo);
        }
    }

    /** Xóa cache của tất cả cơ sở. */
    public static void xoaToanBoCache() {
        synchronized (KHOA) {
            CACHE.clear();
        }
    }

    // ================= Tiện ích =================

    private HocVien doc(ResultSet rs) throws SQLException {
        Date d = rs.getDate("NgaySinh");
        LocalDate ngay = d == null ? null : d.toLocalDate();
        return new HocVien(rs.getInt("MaHocVien"), rs.getInt("MaCoSo"), rs.getString("HoTen"), ngay,
                rs.getString("GioiTinh"), rs.getString("TenNguoiLienHe"),
                rs.getString("SoDienThoaiLienHe"), rs.getString("DiaChi"),
                rs.getString("GhiChu"), rs.getString("TrangThaiHoSo"));
    }

    private static void setNgay(PreparedStatement ps, int idx, LocalDate date) throws SQLException {
        if (date == null) ps.setNull(idx, Types.DATE);
        else ps.setDate(idx, Date.valueOf(date));
    }

    /** Bỏ dấu, đổi đ thành d, chữ thường: so khớp giống collation Latin1 CI AI. */
    private static String chuanHoa(String s) {
        if (s == null) return "";
        String khongDau = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return khongDau.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
    public record LopDangHoc(int maLop, String tenLop, String tenKhoaHoc,
                         String tenMonHoc, String trangThaiLop, LocalDate tuNgay) { }

public List<LopDangHoc> layLopDangHoc(int maHocVien) throws SQLException {
    String sql = """
            SELECT l.MaLop, l.TenLop, k.TenKhoaHoc, m.TenMonHoc, l.TrangThai, xl.TuNgay
            FROM dbo.XepLopTrungTam xl
            JOIN dbo.DangKyKhoaHoc dk ON dk.MaDangKy = xl.MaDangKy
            JOIN dbo.LopTrungTam l ON l.MaLop = xl.MaLop
            JOIN dbo.KhoaHoc k ON k.MaKhoaHoc = l.MaKhoaHoc AND k.MaCoSo = l.MaCoSo
            JOIN dbo.MonHoc m ON m.MaMonHoc = k.MaMonHoc AND m.MaCoSo = k.MaCoSo
            WHERE dk.MaHocVien = ? AND xl.TrangThai = 'HIEU_LUC'
            ORDER BY xl.TuNgay DESC, l.MaLop DESC
            """;
    List<LopDangHoc> ds = new ArrayList<>();
    try (Connection con = Database.moKetNoi();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, maHocVien);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date d = rs.getDate("TuNgay");
                ds.add(new LopDangHoc(rs.getInt("MaLop"), rs.getString("TenLop"),
                        rs.getString("TenKhoaHoc"), rs.getString("TenMonHoc"),
                        rs.getString("TrangThai"), d == null ? null : d.toLocalDate()));
            }
        }
    }
    return ds;
}
/**
 * Tập mã học viên đang có lớp trong cơ sở.
 * Mầm non: XepLopMamNon còn hiệu lực. Trung tâm: XepLopTrungTam HIEU_LUC (cùng điều kiện với layLopDangHoc).
 */
public Set<Integer> layMaHocVienDangCoLop(int maCoSo, boolean mamNon) throws SQLException {
    String sql = mamNon ? """
            SELECT DISTINCT x.MaHocVien
            FROM dbo.XepLopMamNon x
            WHERE x.MaCoSo = ? AND x.TrangThai = 'HIEU_LUC'
              AND (x.DenNgay IS NULL OR x.DenNgay >= CAST(GETDATE() AS DATE))
            """ : """
            SELECT DISTINCT dk.MaHocVien
            FROM dbo.XepLopTrungTam xl
            JOIN dbo.DangKyKhoaHoc dk ON dk.MaDangKy = xl.MaDangKy
            JOIN dbo.LopTrungTam l ON l.MaLop = xl.MaLop
            WHERE l.MaCoSo = ? AND xl.TrangThai = 'HIEU_LUC'
            """;
    Set<Integer> tap = new HashSet<>();
    try (Connection con = Database.moKetNoi();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, maCoSo);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) tap.add(rs.getInt(1));
        }
    }
    return tap;
}
}