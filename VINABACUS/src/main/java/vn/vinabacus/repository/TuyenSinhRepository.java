package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.KhachTiemNang;

/**
 * Repository quản lý dữ liệu tuyển sinh trong dbo.KhachTiemNang.
 * Truy vấn SQL Server, quản lý cache theo MaCoSo, thêm/sửa và chuyển đổi hồ sơ trong transaction.
 */
public final class TuyenSinhRepository {

    private static final String SQL_LAY_TAT_CA =
            "SELECT MaKhach, MaCoSo, HoTen, SoDienThoai, "
            + "TenNguoiLienHe, NhuCau, TrangThai, DaHocThu, GhiChu "
            + "FROM dbo.KhachTiemNang "
            + "WHERE MaCoSo = ? "
            + "ORDER BY MaKhach DESC";

    private static final String SQL_THEM =
            "INSERT INTO dbo.KhachTiemNang "
            + "(MaCoSo, HoTen, SoDienThoai, TenNguoiLienHe, "
            + "NhuCau, TrangThai, DaHocThu, GhiChu) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SUA =
            "UPDATE dbo.KhachTiemNang SET "
            + "HoTen = ?, "
            + "SoDienThoai = ?, "
            + "TenNguoiLienHe = ?, "
            + "NhuCau = ?, "
            + "TrangThai = ?, "
            + "DaHocThu = ?, "
            + "GhiChu = ? "
            + "WHERE MaKhach = ? "
            + "AND MaCoSo = ? "
            + "AND TrangThai <> 'DA_CHUYEN_DOI'";

    private static final String SQL_KHOA_HO_SO =
            "SELECT HoTen, TenNguoiLienHe, SoDienThoai, NhuCau, TrangThai "
            + "FROM dbo.KhachTiemNang WITH (UPDLOCK, ROWLOCK) "
            + "WHERE MaKhach = ? AND MaCoSo = ?";

    private static final String SQL_THEM_HOC_VIEN =
            "INSERT INTO dbo.HocVien "
            + "(MaCoSo, HoTen, TenNguoiLienHe, SoDienThoaiLienHe, GhiChu) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_THEM_YEU_CAU_XEP_LOP =
            "INSERT INTO dbo.YeuCauXepLop "
            + "(MaKhach, MaHocVien, MaCoSo, NhuCau) "
            + "VALUES (?, ?, ?, ?)";

    private static final String SQL_CAP_NHAT_CHUYEN_DOI =
            "UPDATE dbo.KhachTiemNang "
            + "SET TrangThai = 'DA_CHUYEN_DOI' "
            + "WHERE MaKhach = ? "
            + "AND MaCoSo = ? "
            + "AND TrangThai <> 'DA_CHUYEN_DOI' "
            + "AND TrangThai <> 'KHONG_DANG_KY'";

    private record HoSoChuyenDoi(
            String hoTen,
            String tenNguoiLienHe,
            String soDienThoai,
            String nhuCau,
            String trangThai) {
    }

    private static final Map<Integer, List<KhachTiemNang>> CACHE = new HashMap<>();
    private static final Object KHOA = new Object();

    public List<KhachTiemNang> timKiem(int maCoSo, String tuKhoa, String trangThai) throws SQLException {
        if (maCoSo <= 0) {
            throw new SQLException("Mã cơ sở không hợp lệ");
        }

        List<KhachTiemNang> nguon = layCache(maCoSo);
        String tk = tuKhoa == null ? "" : tuKhoa.trim();
        String tkKhongDau = chuanHoa(tk);
        String tt = trangThai == null || trangThai.isBlank() ? "TAT_CA" : trangThai.trim();
        boolean coTuKhoa = !tk.isEmpty();

        List<KhachTiemNang> ketQua = new ArrayList<>();
        for (KhachTiemNang khach : nguon) {
            if (!"TAT_CA".equalsIgnoreCase(tt) && !tt.equalsIgnoreCase(khach.trangThai())) {
                continue;
            }
            if (coTuKhoa && !khop(khach, tk, tkKhongDau)) {
                continue;
            }
            ketQua.add(khach);
        }
        return ketQua;
    }

    private static boolean khop(KhachTiemNang khach, String tuKhoa, String tuKhoaKhongDau) {
        if (String.valueOf(khach.maKhach()).contains(tuKhoa)) {
            return true;
        }
        if (chuanHoa(khach.hoTen()).contains(tuKhoaKhongDau)) {
            return true;
        }
        return khach.soDienThoai() != null && khach.soDienThoai().contains(tuKhoa);
    }

    private List<KhachTiemNang> layCache(int maCoSo) throws SQLException {
        synchronized (KHOA) {
            List<KhachTiemNang> daCo = CACHE.get(maCoSo);
            if (daCo == null) {
                daCo = List.copyOf(taiTuCSDL(maCoSo));
                CACHE.put(maCoSo, daCo);
            }
            return daCo;
        }
    }

    private List<KhachTiemNang> taiTuCSDL(int maCoSo) throws SQLException {
        List<KhachTiemNang> danhSach = new ArrayList<>();
        try (Connection con = Database.moKetNoi();
             PreparedStatement ps = con.prepareStatement(SQL_LAY_TAT_CA)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    danhSach.add(doc(rs));
                }
            }
        }
        return danhSach;
    }

    public int them(KhachTiemNang khach) throws SQLException {
        if (khach == null) {
            throw new SQLException("Thông tin hồ sơ không được để trống");
        }

        synchronized (KHOA) {
            try (Connection con = Database.moKetNoi();
                 PreparedStatement ps = con.prepareStatement(SQL_THEM, Statement.RETURN_GENERATED_KEYS)) {
                ganDuLieuThem(ps, khach);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Không thể thêm hồ sơ tuyển sinh");
                }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Không lấy được mã hồ sơ tuyển sinh");
                    }
                    int maMoi = rs.getInt(1);
                    CACHE.remove(khach.maCoSo());
                    return maMoi;
                }
            }
        }
    }

    public boolean sua(KhachTiemNang khach) throws SQLException {
        if (khach == null) {
            throw new SQLException("Thông tin hồ sơ không được để trống");
        }
        if (khach.maKhach() <= 0 || khach.maCoSo() <= 0) {
            throw new SQLException("Mã hồ sơ hoặc mã cơ sở không hợp lệ");
        }

        synchronized (KHOA) {
            try (Connection con = Database.moKetNoi();
                 PreparedStatement ps = con.prepareStatement(SQL_SUA)) {
                ganDuLieuSua(ps, khach);
                boolean thanhCong = ps.executeUpdate() == 1;
                if (thanhCong) {
                    CACHE.remove(khach.maCoSo());
                }
                return thanhCong;
            }
        }
    }

    public int chuyenDoi(int maKhach, int maCoSo) throws SQLException {
        if (maKhach <= 0 || maCoSo <= 0) {
            throw new SQLException("Mã khách hoặc mã cơ sở không hợp lệ");
        }

        synchronized (KHOA) {
            int maHocVien;
            try (Connection con = Database.moKetNoi()) {
                con.setAutoCommit(false);
                try {
                    HoSoChuyenDoi hoSo = layHoSoChuyenDoi(con, maKhach, maCoSo);
                    kiemTraChuyenDoi(hoSo);
                    maHocVien = themHocVienTuTuyenSinh(con, hoSo, maKhach, maCoSo);
                    themYeuCauXepLop(con, maKhach, maHocVien, maCoSo, hoSo.nhuCau());
                    capNhatTrangThaiChuyenDoi(con, maKhach, maCoSo);
                    con.commit();
                } catch (SQLException | RuntimeException ex) {
                    try {
                        con.rollback();
                    } catch (SQLException rollbackError) {
                        ex.addSuppressed(rollbackError);
                    }
                    throw ex;
                }
            }

            CACHE.remove(maCoSo);
            HocVienRepository.xoaCache(maCoSo);
            return maHocVien;
        }
    }

    private HoSoChuyenDoi layHoSoChuyenDoi(Connection con, int maKhach, int maCoSo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_KHOA_HO_SO)) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Không tìm thấy hồ sơ tuyển sinh thuộc cơ sở này");
                }
                return new HoSoChuyenDoi(
                        rs.getString("HoTen"),
                        rs.getString("TenNguoiLienHe"),
                        rs.getString("SoDienThoai"),
                        rs.getString("NhuCau"),
                        rs.getString("TrangThai"));
            }
        }
    }

    private void kiemTraChuyenDoi(HoSoChuyenDoi hoSo) throws SQLException {
        if ("DA_CHUYEN_DOI".equals(hoSo.trangThai())) {
            throw new SQLException("Hồ sơ đã chuyển đổi thành học viên trước đó");
        }
        if ("KHONG_DANG_KY".equals(hoSo.trangThai())) {
            throw new SQLException("Hồ sơ Không đăng ký không thể chuyển đổi");
        }
        if (hoSo.hoTen() == null || hoSo.hoTen().isBlank()) {
            throw new SQLException("Hồ sơ chưa có họ tên hợp lệ");
        }
    }

    private int themHocVienTuTuyenSinh(Connection con, HoSoChuyenDoi hoSo, int maKhach, int maCoSo)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_THEM_HOC_VIEN, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maCoSo);
            ps.setString(2, hoSo.hoTen());
            ps.setString(3, hoSo.tenNguoiLienHe());
            ps.setString(4, hoSo.soDienThoai());
            ps.setString(5, "Tuyển sinh từ khách #" + maKhach);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Không thể tạo học viên từ hồ sơ tuyển sinh");
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Không lấy được mã học viên vừa tạo");
                }
                return rs.getInt(1);
            }
        }
    }

    private void themYeuCauXepLop(Connection con, int maKhach, int maHocVien, int maCoSo, String nhuCau)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_THEM_YEU_CAU_XEP_LOP)) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maHocVien);
            ps.setInt(3, maCoSo);
            ps.setString(4, nhuCau);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Không thể tạo yêu cầu xếp lớp");
            }
        }
    }

    private void capNhatTrangThaiChuyenDoi(Connection con, int maKhach, int maCoSo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_CAP_NHAT_CHUYEN_DOI)) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Không thể cập nhật trạng thái hồ sơ. Hồ sơ có thể đã được chuyển đổi hoặc không còn hợp lệ.");
            }
        }
    }

    private static void ganDuLieuThem(PreparedStatement ps, KhachTiemNang khach) throws SQLException {
        ps.setInt(1, khach.maCoSo());
        ps.setString(2, khach.hoTen());
        ps.setString(3, khach.soDienThoai());
        ps.setString(4, khach.tenNguoiLienHe());
        ps.setString(5, khach.nhuCau());
        ps.setString(6, khach.trangThai());
        ps.setBoolean(7, khach.daHocThu());
        ps.setString(8, khach.ghiChu());
    }

    private static void ganDuLieuSua(PreparedStatement ps, KhachTiemNang khach) throws SQLException {
        ps.setString(1, khach.hoTen());
        ps.setString(2, khach.soDienThoai());
        ps.setString(3, khach.tenNguoiLienHe());
        ps.setString(4, khach.nhuCau());
        ps.setString(5, khach.trangThai());
        ps.setBoolean(6, khach.daHocThu());
        ps.setString(7, khach.ghiChu());
        ps.setInt(8, khach.maKhach());
        ps.setInt(9, khach.maCoSo());
    }

    public static void xoaCache(int maCoSo) {
        synchronized (KHOA) {
            CACHE.remove(maCoSo);
        }
    }

    public static void xoaToanBoCache() {
        synchronized (KHOA) {
            CACHE.clear();
        }
    }

    private static KhachTiemNang doc(ResultSet rs) throws SQLException {
        return new KhachTiemNang(
                rs.getInt("MaKhach"),
                rs.getInt("MaCoSo"),
                rs.getString("HoTen"),
                rs.getString("SoDienThoai"),
                rs.getString("TenNguoiLienHe"),
                rs.getString("NhuCau"),
                rs.getString("TrangThai"),
                rs.getBoolean("DaHocThu"),
                rs.getString("GhiChu"));
    }

    private static String chuanHoa(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String khongDau = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return khongDau.replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}