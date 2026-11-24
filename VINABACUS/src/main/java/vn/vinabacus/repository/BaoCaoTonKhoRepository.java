package vn.vinabacus.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.BaoCaoTonKho;

/**
 * Tinh ton kho lien tuc qua cac thang, khong can chep TON_DAU sang thang moi.
 * Bao cao tu tinh lai khi cac phieu XAC_NHAN cua ky chua chot thay doi.
 */
public class BaoCaoTonKhoRepository {

    private static final BigDecimal KHONG = BigDecimal.ZERO;

    private static class SoLuong {
        BigDecimal tonDau = KHONG;
        BigDecimal nhap = KHONG;
        BigDecimal xuatHV = KHONG;
        BigDecimal xuatCS = KHONG;
        BigDecimal dieuChinh = KHONG;
        boolean daCoTonDau;

        BigDecimal tonCuoi() {
            return tonDau.add(nhap).subtract(xuatHV)
                    .subtract(xuatCS).add(dieuChinh);
        }
    }

    public List<BaoCaoTonKho> layBaoCao(int maCoSo, int nam, int thang)
            throws SQLException {
        try (Connection conn = Database.moKetNoi()) {
            int kyCanXem = nam * 100 + thang;

            // Neu ky nay da chot va co snapshot thi giu nguyen bao cao da chot.
            Integer maKyCanXem = null;
            String trangThai = "MO";
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT MaKyKho, TrangThai FROM dbo.KyKho
                    WHERE MaCoSo=? AND Nam=? AND Thang=?
                    """)) {
                ps.setInt(1, maCoSo);
                ps.setInt(2, nam);
                ps.setInt(3, thang);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        maKyCanXem = rs.getInt("MaKyKho");
                        trangThai = rs.getString("TrangThai");
                    }
                }
            }
            if ("CHOT".equals(trangThai) && maKyCanXem != null) {
                List<BaoCaoTonKho> daChot = docSoLieuChot(conn, maCoSo, maKyCanXem);
                if (!daChot.isEmpty()) return daChot;
            }

            // Lay so du cua ky da chot gan nhat lam moc (neu co).
            // Cac ky MO sau moc nay se duoc tinh lai tu phieu thuc te.
            Map<Integer, SoLuong> soLieu = new HashMap<>();
            int moc = 0;
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT TOP 1 k.MaKyKho, k.Nam, k.Thang
                    FROM dbo.KyKho k
                    WHERE k.MaCoSo=? AND k.TrangThai='CHOT'
                      AND k.Nam*100+k.Thang < ?
                      AND EXISTS (SELECT 1 FROM dbo.SoLieuChotKho s
                                  WHERE s.MaCoSo=k.MaCoSo AND s.MaKyKho=k.MaKyKho)
                    ORDER BY k.Nam DESC, k.Thang DESC
                    """)) {
                ps.setInt(1, maCoSo);
                ps.setInt(2, kyCanXem);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int maKyChot = rs.getInt("MaKyKho");
                        moc = rs.getInt("Nam") * 100 + rs.getInt("Thang");
                        try (PreparedStatement ps2 = conn.prepareStatement("""
                                SELECT MaHangHoa, TonCuoi FROM dbo.SoLieuChotKho
                                WHERE MaCoSo=? AND MaKyKho=?
                                """)) {
                            ps2.setInt(1, maCoSo);
                            ps2.setInt(2, maKyChot);
                            try (ResultSet d = ps2.executeQuery()) {
                                while (d.next()) {
                                    SoLuong sl = soLieu.computeIfAbsent(
                                            d.getInt("MaHangHoa"), id -> new SoLuong());
                                    sl.tonDau = d.getBigDecimal("TonCuoi");
                                    sl.daCoTonDau = true;
                                }
                            }
                        }
                    }
                }
            }

            // Doc cac phieu da XAC_NHAN tu moc chot den ky dang xem.
            // Khong can ton tai KyKho cua thang moi neu chua co giao dich.
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT k.Nam, k.Thang, c.MaHangHoa, p.LoaiPhieu,
                           SUM(c.SoLuong) AS SoLuong
                    FROM dbo.KyKho k
                    JOIN dbo.PhieuKho p
                      ON p.MaKyKho=k.MaKyKho AND p.MaCoSo=k.MaCoSo
                    JOIN dbo.ChiTietPhieuKho c
                      ON c.MaPhieuKho=p.MaPhieuKho AND c.MaCoSo=p.MaCoSo
                    WHERE k.MaCoSo=? AND p.TrangThai='XAC_NHAN'
                      AND k.Nam*100+k.Thang > ?
                      AND k.Nam*100+k.Thang <= ?
                    GROUP BY k.Nam, k.Thang, c.MaHangHoa, p.LoaiPhieu
                    ORDER BY k.Nam, k.Thang, c.MaHangHoa, p.LoaiPhieu
                    """)) {
                ps.setInt(1, maCoSo);
                ps.setInt(2, moc);
                ps.setInt(3, kyCanXem);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int ky = rs.getInt("Nam") * 100 + rs.getInt("Thang");
                        boolean trongThang = ky == kyCanXem;
                        int maHangHoa = rs.getInt("MaHangHoa");
                        String loai = rs.getString("LoaiPhieu");
                        BigDecimal soLuong = rs.getBigDecimal("SoLuong");
                        SoLuong sl = soLieu.computeIfAbsent(
                                maHangHoa, id -> new SoLuong());

                        switch (loai) {
                            case "TON_DAU" -> {
                                // Chi nhan so du khoi tao MOT LAN cho moi mat hang.
                                // Khong cong lai TON_DAU o moi thang.
                                if (!sl.daCoTonDau) {
                                    sl.tonDau = sl.tonDau.add(soLuong);
                                    sl.daCoTonDau = true;
                                }
                            }
                            case "NHAP" -> {
                                if (trongThang) sl.nhap = sl.nhap.add(soLuong);
                                else sl.tonDau = sl.tonDau.add(soLuong);
                            }
                            case "XUAT_HS" -> {
                                if (trongThang) sl.xuatHV = sl.xuatHV.add(soLuong);
                                else sl.tonDau = sl.tonDau.subtract(soLuong);
                            }
                            case "XUAT_CS" -> {
                                if (trongThang) sl.xuatCS = sl.xuatCS.add(soLuong);
                                else sl.tonDau = sl.tonDau.subtract(soLuong);
                            }
                            case "DIEU_CHINH" -> {
                                if (trongThang) sl.dieuChinh = sl.dieuChinh.add(soLuong);
                                else sl.tonDau = sl.tonDau.add(soLuong);
                            }
                            default -> { /* Bo qua loai khong dung */ }
                        }
                    }
                }
            }

            // Hien thi day du hang hoa, ke ca thang moi khong co giao dich.
            List<BaoCaoTonKho> ketQua = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT MaHangHoa, MaSanPham, TenHangHoa, DonViTinh,
                           DonGiaXuatBan
                    FROM dbo.HangHoa WHERE MaCoSo=? ORDER BY MaHangHoa
                    """)) {
                ps.setInt(1, maCoSo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        SoLuong sl = soLieu.getOrDefault(
                                rs.getInt("MaHangHoa"), new SoLuong());
                        ketQua.add(new BaoCaoTonKho(
                                rs.getInt("MaHangHoa"), rs.getString("MaSanPham"),
                                rs.getString("TenHangHoa"), rs.getString("DonViTinh"),
                                sl.tonDau, sl.nhap, sl.xuatHV, sl.xuatCS,
                                sl.dieuChinh, sl.tonCuoi(),
                                rs.getBigDecimal("DonGiaXuatBan")
                        ));
                    }
                }
            }
            return ketQua;
        }
    }

    private List<BaoCaoTonKho> docSoLieuChot(
            Connection conn, int maCoSo, int maKyKho) throws SQLException {
        List<BaoCaoTonKho> ds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT MaHangHoa, MaSanPham, TenHangHoa, DonViTinh,
                       DonGiaXuatBan, TonDau, Nhap, XuatHS, XuatCS,
                       DieuChinhKiemKe, TonCuoi
                FROM dbo.SoLieuChotKho
                WHERE MaCoSo=? AND MaKyKho=? ORDER BY MaHangHoa
                """)) {
            ps.setInt(1, maCoSo);
            ps.setInt(2, maKyKho);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(new BaoCaoTonKho(
                            rs.getInt("MaHangHoa"), rs.getString("MaSanPham"),
                            rs.getString("TenHangHoa"), rs.getString("DonViTinh"),
                            rs.getBigDecimal("TonDau"), rs.getBigDecimal("Nhap"),
                            rs.getBigDecimal("XuatHS"), rs.getBigDecimal("XuatCS"),
                            rs.getBigDecimal("DieuChinhKiemKe"),
                            rs.getBigDecimal("TonCuoi"),
                            rs.getBigDecimal("DonGiaXuatBan")
                    ));
                }
            }
        }
        return ds;
    }
}
