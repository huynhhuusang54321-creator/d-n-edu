package vn.vinabacus.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Các bước ghi sổ cho đăng ký Trung tâm. Người gọi sở hữu transaction SERIALIZABLE;
 * lớp này không mở connection, commit hoặc rollback để đăng ký và thu tiền là một giao dịch.
 */
public final class TaiChinhDangKyRepository {
    /** maKhoanThu=0 khi học phí sau giảm bằng 0, không phát sinh khoản phải thu. */
    public record Khoan(int maKhoanThu, int maDangKy, int maHocVien,
                       BigDecimal phaiThu) { }
    public record PhanBo(int maKhoanThu, BigDecimal soTien) { }

    private static void giaoDichHopLe(Connection c) throws SQLException {
        if (c == null || c.getAutoCommit()
                || c.getTransactionIsolation() != Connection.TRANSACTION_SERIALIZABLE) {
            throw new IllegalArgumentException("Cần dùng cùng giao dịch SERIALIZABLE với đăng ký khóa");
        }
    }

    private static BigDecimal tien(BigDecimal value, boolean choPhepKhong)
            throws IllegalArgumentException {
        if (value == null || value.signum() < 0 || (!choPhepKhong && value.signum() == 0)) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }
        try {
            BigDecimal ketQua = value.setScale(2, RoundingMode.UNNECESSARY);
            if (ketQua.precision() > 18)
                throw new IllegalArgumentException("Số tiền vượt giới hạn lưu trữ");
            return ketQua;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Số tiền chỉ có tối đa hai chữ số thập phân", ex);
        }
    }

    /** Chặn trùng khoản thu và dùng đúng số học phí đã chốt trên từng đăng ký. */
    public Khoan lapKhoan(Connection c, int maCoSo, int maDangKy,
                         LocalDate hanThanhToan) throws SQLException {
        giaoDichHopLe(c);
        if (maCoSo <= 0 || maDangKy <= 0)
            throw new IllegalArgumentException("Cơ sở hoặc đăng ký không hợp lệ");
        int maHocVien;
        BigDecimal phaiThu;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT MaHocVien,MaCoSo,HocPhiSauGiam,TrangThai
                FROM dbo.DangKyKhoaHoc WITH (UPDLOCK,HOLDLOCK)
                WHERE MaDangKy=?
                """)) {
            ps.setInt(1, maDangKy);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("MaCoSo") != maCoSo
                        || "HUY".equals(rs.getString("TrangThai")))
                    throw new SQLException("Đăng ký không thuộc cơ sở hoặc đã hủy");
                maHocVien = rs.getInt("MaHocVien");
                phaiThu = tien(rs.getBigDecimal("HocPhiSauGiam"), true);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT MaKhoanThu,MaHocVien,MaCoSo,LoaiKhoan,SoTienGoc,TrangThai
                FROM dbo.KhoanPhaiThu WITH (UPDLOCK,HOLDLOCK)
                WHERE MaDangKy=?
                """)) {
            ps.setInt(1, maDangKy);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maKhoan = rs.getInt("MaKhoanThu");
                    boolean dungNguon = rs.getInt("MaHocVien") == maHocVien
                            && rs.getInt("MaCoSo") == maCoSo
                            && "HOC_PHI_KHOA".equals(rs.getString("LoaiKhoan"))
                            && "HIEU_LUC".equals(rs.getString("TrangThai"))
                            && phaiThu.compareTo(rs.getBigDecimal("SoTienGoc")) == 0;
                    if (!dungNguon || rs.next())
                        throw new SQLException("Khoản phải thu của đăng ký không nhất quán; không tạo trùng");
                    return new Khoan(maKhoan, maDangKy, maHocVien, phaiThu);
                }
            }
        }
        if (phaiThu.signum() == 0) return new Khoan(0, maDangKy, maHocVien, phaiThu);
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT dbo.KhoanPhaiThu
                    (MaHocVien,MaCoSo,LoaiKhoan,MaDangKy,SoTienGoc,HanThanhToan)
                VALUES (?,?,'HOC_PHI_KHOA',?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            ps.setInt(3, maDangKy);
            ps.setBigDecimal(4, phaiThu);
            if (hanThanhToan == null) ps.setNull(5, Types.DATE);
            else ps.setDate(5, Date.valueOf(hanThanhToan));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return new Khoan(rs.getInt(1), maDangKy, maHocVien, phaiThu);
            }
        }
        throw new SQLException("Không lấy được mã khoản phải thu mới");
    }

    /**
     * Không tự chia tiền giữa các khóa: giao diện/nghiệp vụ phải cung cấp số tiền
     * cho từng khoản. Cùng token UUID có thể thử lại nhưng không tạo phiếu thứ hai.
     */
    public int lapPhieu(Connection c, int maCoSo, int maHocVien,
                       BigDecimal soTienThucThu, List<PhanBo> phanBo,
                       String hinhThuc, int maTaiKhoan, String ghiChu,
                       UUID maGiaoDich) throws SQLException {
        giaoDichHopLe(c);
        if (maCoSo <= 0 || maHocVien <= 0 || maTaiKhoan <= 0 || maGiaoDich == null)
            throw new IllegalArgumentException("Thiếu cơ sở, học viên, tài khoản hoặc mã giao dịch");
        if (!List.of("TIEN_MAT", "CHUYEN_KHOAN", "KHAC").contains(hinhThuc))
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ");
        String note = ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
        if (note != null && note.length() > 500)
            throw new IllegalArgumentException("Ghi chú không được quá 500 ký tự");
        BigDecimal tong = tien(soTienThucThu, false);
        List<PhanBo> ds = kiemTraPhanBo(tong, phanBo);
        String token = maGiaoDich.toString();

        Integer daCo = phieuTheoToken(c, token, maCoSo, maHocVien,
                tong, ds, hinhThuc, maTaiKhoan, note);
        if (daCo != null) return daCo;
        kiemTraTaiKhoan(c, maCoSo, maTaiKhoan);
        for (PhanBo item : ds) kiemTraSoDu(c, maCoSo, maHocVien, item);

        int maPhieu;
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT dbo.PhieuThu
                    (SoPhieuThu,MaHocVien,MaCoSo,TongTien,HinhThucThanhToan,
                     MaTaiKhoan,TrangThai,MaGiaoDichNghiepVu,GhiChu)
                VALUES (?,?,?,?,?,?,'XAC_NHAN',?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "PT-" + token.replace("-", "").toUpperCase());
            ps.setInt(2, maHocVien);
            ps.setInt(3, maCoSo);
            ps.setBigDecimal(4, tong);
            ps.setString(5, hinhThuc);
            ps.setInt(6, maTaiKhoan);
            ps.setString(7, token);
            ps.setString(8, note);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Không lấy được mã phiếu thu mới");
                maPhieu = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT dbo.ChiTietPhieuThu
                    (MaPhieuThu,MaKhoanThu,MaHocVien,MaCoSo,SoTienPhanBo,
                     LoaiBienDong,MaGiaoDichNghiepVu)
                VALUES (?,?,?,?,?,'PHAN_BO',?)
                """)) {
            for (PhanBo item : ds) {
                ps.setInt(1, maPhieu);
                ps.setInt(2, item.maKhoanThu());
                ps.setInt(3, maHocVien);
                ps.setInt(4, maCoSo);
                ps.setBigDecimal(5, item.soTien());
                ps.setString(6, UUID.randomUUID().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return maPhieu;
    }

    /** Ràng buộc tổng phân bổ và số tiền thực thu trước khi đụng CSDL. */
    public static List<PhanBo> kiemTraPhanBo(BigDecimal soTienThucThu,
                                            List<PhanBo> phanBo) {
        BigDecimal tong = tien(soTienThucThu, false);
        if (phanBo == null || phanBo.isEmpty())
            throw new IllegalArgumentException("Cần phân bổ số tiền thực thu cho ít nhất một khóa");
        List<PhanBo> result = new ArrayList<>();
        Set<Integer> maKhoan = new HashSet<>();
        BigDecimal daChia = BigDecimal.ZERO;
        for (PhanBo item : phanBo) {
            if (item == null || item.maKhoanThu() <= 0 || !maKhoan.add(item.maKhoanThu()))
                throw new IllegalArgumentException("Khoản thu phân bổ trùng hoặc không hợp lệ");
            BigDecimal amount = tien(item.soTien(), false);
            daChia = daChia.add(amount);
            result.add(new PhanBo(item.maKhoanThu(), amount));
        }
        if (daChia.compareTo(tong) != 0)
            throw new IllegalArgumentException("Tổng phân bổ phải bằng số tiền thực thu");
        result.sort(Comparator.comparingInt(PhanBo::maKhoanThu));
        return List.copyOf(result);
    }

    private static Integer phieuTheoToken(Connection c, String token, int maCoSo,
                                          int maHocVien, BigDecimal tong,
                                          List<PhanBo> ds, String hinhThuc,
                                          int maTaiKhoan, String note)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT MaPhieuThu,MaCoSo,MaHocVien,TongTien,HinhThucThanhToan,
                       MaTaiKhoan,TrangThai,GhiChu
                FROM dbo.PhieuThu WITH (UPDLOCK,HOLDLOCK)
                WHERE MaGiaoDichNghiepVu=?
                """)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int maPhieu = rs.getInt("MaPhieuThu");
                boolean trung = rs.getInt("MaCoSo") == maCoSo
                        && rs.getInt("MaHocVien") == maHocVien
                        && tong.compareTo(rs.getBigDecimal("TongTien")) == 0
                        && hinhThuc.equals(rs.getString("HinhThucThanhToan"))
                        && rs.getInt("MaTaiKhoan") == maTaiKhoan
                        && "XAC_NHAN".equals(rs.getString("TrangThai"))
                        && Objects.equals(note, rs.getString("GhiChu"));
                if (!trung || !phanBoDaLuu(c, maPhieu).equals(ds))
                    throw new SQLException("Mã giao dịch đã dùng cho nội dung khác; không lập phiếu trùng");
                return maPhieu;
            }
        }
    }

    private static List<PhanBo> phanBoDaLuu(Connection c, int maPhieu)
            throws SQLException {
        List<PhanBo> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT MaKhoanThu,SoTienPhanBo,LoaiBienDong
                FROM dbo.ChiTietPhieuThu WHERE MaPhieuThu=?
                ORDER BY MaKhoanThu
                """)) {
            ps.setInt(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!"PHAN_BO".equals(rs.getString("LoaiBienDong"))) return List.of();
                    result.add(new PhanBo(rs.getInt("MaKhoanThu"),
                            tien(rs.getBigDecimal("SoTienPhanBo"), false)));
                }
            }
        }
        return result;
    }

    private static void kiemTraTaiKhoan(Connection c, int maCoSo, int maTaiKhoan)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM dbo.TaiKhoanCoSo cs WITH (HOLDLOCK)
                JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=cs.MaTaiKhoan
                WHERE cs.MaCoSo=? AND cs.MaTaiKhoan=?
                  AND cs.TrangThai=1 AND t.TrangThai=1
                """)) {
            ps.setInt(1, maCoSo);
            ps.setInt(2, maTaiKhoan);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Tài khoản thu không hoạt động tại cơ sở");
            }
        }
    }

    private static void kiemTraSoDu(Connection c, int maCoSo, int maHocVien,
                                     PhanBo item) throws SQLException {
        BigDecimal goc;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT MaHocVien,MaCoSo,LoaiKhoan,SoTienGoc,TrangThai
                FROM dbo.KhoanPhaiThu WITH (UPDLOCK,HOLDLOCK)
                WHERE MaKhoanThu=?
                """)) {
            ps.setInt(1, item.maKhoanThu());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("MaHocVien") != maHocVien
                        || rs.getInt("MaCoSo") != maCoSo
                        || !"HOC_PHI_KHOA".equals(rs.getString("LoaiKhoan"))
                        || !"HIEU_LUC".equals(rs.getString("TrangThai")))
                    throw new SQLException("Khoản học phí không hợp lệ hoặc không thuộc học viên");
                goc = rs.getBigDecimal("SoTienGoc");
            }
        }
        BigDecimal dieuChinh;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(SoTienDieuChinh),0)
                FROM dbo.DieuChinhKhoanThu
                WHERE MaKhoanThu=? AND TrangThai='XAC_NHAN'
                """)) {
            ps.setInt(1, item.maKhoanThu());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                dieuChinh = rs.getBigDecimal(1);
            }
        }
        BigDecimal daThu;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(CASE WHEN ct.LoaiBienDong='PHAN_BO'
                                         THEN ct.SoTienPhanBo ELSE -ct.SoTienPhanBo END),0)
                FROM dbo.ChiTietPhieuThu ct
                JOIN dbo.PhieuThu p ON p.MaPhieuThu=ct.MaPhieuThu AND p.MaCoSo=ct.MaCoSo
                WHERE ct.MaKhoanThu=? AND ct.MaCoSo=? AND p.TrangThai='XAC_NHAN'
                """)) {
            ps.setInt(1, item.maKhoanThu());
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                daThu = rs.getBigDecimal(1);
            }
        }
        BigDecimal conNo = goc.add(dieuChinh).subtract(daThu);
        if (conNo.signum() < 0)
            throw new SQLException("Khoản thu đã vượt phải thu; cần kiểm tra dữ liệu trước khi thu thêm");
        if (item.soTien().compareTo(conNo) > 0)
            throw new SQLException("Số tiền phân bổ vượt công nợ của khoản #"
                    + item.maKhoanThu() + ": " + conNo.toPlainString());
    }
}
