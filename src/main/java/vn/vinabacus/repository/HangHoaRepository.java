package vn.vinabacus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.sql.Statement;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.HangHoa;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
public class HangHoaRepository {

    public List<HangHoa> timTheoCoSo(int maCoSo)
            throws SQLException {

        List<HangHoa> danhSach = new ArrayList<>();

        String sql = """
            SELECT
                hh.MaHangHoa,
                hh.MaCoSo,
                hh.MaSanPham,
                hh.TenHangHoa,
                hh.DonViTinh,
                hh.DonGiaSi,
                hh.DonGiaXuatBan,
                hh.TrangThai,
                COALESCE(ton.SoLuongTon, 0) AS SoLuongTon

            FROM dbo.HangHoa hh

            LEFT JOIN (
                SELECT
                    ct.MaHangHoa,
                    ct.MaCoSo,
                    SUM(
                        CASE
                            WHEN pk.LoaiPhieu IN ('TON_DAU', 'NHAP')
                                THEN ct.SoLuong
                    
                            WHEN pk.LoaiPhieu IN ('XUAT_HS', 'XUAT_CS')
                                THEN -ct.SoLuong
                    
                            WHEN pk.LoaiPhieu = 'DIEU_CHINH'
                                THEN ct.SoLuong
                    
                            ELSE 0
                        END
                    ) AS SoLuongTon

                FROM dbo.ChiTietPhieuKho ct

                INNER JOIN dbo.PhieuKho pk
                    ON ct.MaPhieuKho = pk.MaPhieuKho
                    AND ct.MaCoSo = pk.MaCoSo

                WHERE pk.TrangThai = 'XAC_NHAN'

                GROUP BY
                    ct.MaHangHoa,
                    ct.MaCoSo
            ) ton
                ON hh.MaHangHoa = ton.MaHangHoa
                AND hh.MaCoSo = ton.MaCoSo

            WHERE hh.MaCoSo = ?

            ORDER BY hh.MaHangHoa
            """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps
                = conn.prepareStatement(sql)) {

            ps.setInt(1, maCoSo);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    HangHoa hangHoa = new HangHoa(
                            rs.getInt("MaHangHoa"),
                            rs.getInt("MaCoSo"),
                            rs.getString("MaSanPham"),
                            rs.getString("TenHangHoa"),
                            rs.getString("DonViTinh"),
                            rs.getBigDecimal("DonGiaSi"),
                            rs.getBigDecimal("DonGiaXuatBan"),
                            rs.getBoolean("TrangThai"),
                            rs.getBigDecimal("SoLuongTon")
                    );

                    danhSach.add(hangHoa);
                }
            }
        }

        return danhSach;
    }

    public int themHangHoa(
            int maCoSo,
            String maSanPham,
            String tenHangHoa,
            String donViTinh,
            BigDecimal donGiaSi,
            BigDecimal donGiaXuatBan) throws SQLException {

        String sql = """
        INSERT INTO dbo.HangHoa
        (MaCoSo, MaSanPham, TenHangHoa,
         DonViTinh, DonGiaSi, DonGiaXuatBan)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, maCoSo);
            ps.setString(2, maSanPham);
            ps.setString(3, tenHangHoa);
            ps.setString(4, donViTinh);
            ps.setBigDecimal(5, donGiaSi);
            ps.setBigDecimal(6, donGiaXuatBan);

            int soDong = ps.executeUpdate();

            if (soDong == 0) {
                throw new SQLException(
                        "Không thể thêm hàng hóa!"
                );
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }

                throw new SQLException(
                        "Không lấy được mã hàng hóa mới!"
                );
            }
        }
    }

    public boolean suaHangHoa(
            int maHangHoa,
            int maCoSo,
            String maSanPham,
            String tenHangHoa,
            String donViTinh,
            BigDecimal donGiaSi,
            BigDecimal donGiaXuatBan) throws SQLException {

        String sql = """
        UPDATE dbo.HangHoa
        SET
            MaSanPham = ?,
            TenHangHoa = ?,
            DonViTinh = ?,
            DonGiaSi = ?,
            DonGiaXuatBan = ?
        WHERE MaHangHoa = ?
          AND MaCoSo = ?
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maSanPham);
            ps.setString(2, tenHangHoa);
            ps.setString(3, donViTinh);
            ps.setBigDecimal(4, donGiaSi);
            ps.setBigDecimal(5, donGiaXuatBan);
            ps.setInt(6, maHangHoa);
            ps.setInt(7, maCoSo);

            int soDongCapNhat = ps.executeUpdate();

            return soDongCapNhat == 1;
        }
    }

    public boolean capNhatTrangThai(
            int maHangHoa,
            int maCoSo,
            boolean trangThai) throws SQLException {

        String sql = """
        UPDATE dbo.HangHoa
        SET TrangThai = ?
        WHERE MaHangHoa = ?
          AND MaCoSo = ?
        """;

        try (Connection conn = Database.moKetNoi(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, trangThai);
            ps.setInt(2, maHangHoa);
            ps.setInt(3, maCoSo);

            int soDong = ps.executeUpdate();

            return soDong == 1;
        }
    }

public int nhapKho(
        int maCoSo,
        int maHangHoa,
        BigDecimal soLuong,
        int maTaiKhoan,
        String ghiChu) throws SQLException {

    // ===== KIEM TRA DU LIEU =====

    if (maCoSo <= 0 || maHangHoa <= 0 || maTaiKhoan <= 0) {
        throw new IllegalArgumentException(
            "Thông tin cơ sở, hàng hóa hoặc tài khoản không hợp lệ!"
        );
    }

    if (soLuong == null
            || soLuong.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            "Số lượng nhập phải lớn hơn 0!"
        );
    }

    BigDecimal soLuongNhap = soLuong.stripTrailingZeros();

    if (soLuongNhap.scale() > 3
            || soLuongNhap.precision()
               - soLuongNhap.scale() > 15) {
        throw new IllegalArgumentException(
            "Số lượng nhập không hợp lệ!"
        );
    }

    LocalDate homNay = LocalDate.now(
        ZoneId.of("Asia/Ho_Chi_Minh")
    );

    try (Connection conn = Database.moKetNoi()) {

        // Su dung transaction de tranh luu thieu du lieu
        conn.setAutoCommit(false);

        try {

            // ===== 1. TIM KY KHO DANG MO =====

            int maKyKho;

            String sqlKyKho = """
                SELECT MaKyKho
                FROM dbo.KyKho WITH (UPDLOCK, HOLDLOCK)
                WHERE MaCoSo = ?
                  AND Nam = ?
                  AND Thang = ?
                  AND TrangThai = 'MO'
                """;

            try (PreparedStatement ps =
                    conn.prepareStatement(sqlKyKho)) {

                ps.setInt(1, maCoSo);
                ps.setInt(2, homNay.getYear());
                ps.setInt(3, homNay.getMonthValue());

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                            "Chưa có kỳ kho đang mở cho tháng hiện tại!"
                        );
                    }

                    maKyKho = rs.getInt("MaKyKho");
                }
            }

            // ===== 2. LAY GIA HANG HOA =====

            BigDecimal giaSi;
            BigDecimal giaXuatBan;

            String sqlHangHoa = """
                SELECT DonGiaSi, DonGiaXuatBan
                FROM dbo.HangHoa
                WHERE MaHangHoa = ?
                  AND MaCoSo = ?
                  AND TrangThai = 1
                """;

            try (PreparedStatement ps =
                    conn.prepareStatement(sqlHangHoa)) {

                ps.setInt(1, maHangHoa);
                ps.setInt(2, maCoSo);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                            "Hàng hóa không tồn tại, sai cơ sở "
                            + "hoặc đã ngừng sử dụng!"
                        );
                    }

                    giaSi = rs.getBigDecimal("DonGiaSi");
                    giaXuatBan =
                        rs.getBigDecimal("DonGiaXuatBan");
                }
            }

            // ===== 3. TAO PHIEU NHAP KHO =====

            int maPhieuKho;

            String maGiaoDich =
                "NHAP-" + UUID.randomUUID().toString();

            String sqlPhieu = """
                INSERT INTO dbo.PhieuKho
                (
                    MaCoSo,
                    MaKyKho,
                    LoaiPhieu,
                    MaTaiKhoan,
                    TrangThai,
                    MaGiaoDichNghiepVu,
                    GhiChu
                )
                VALUES (?, ?, 'NHAP', ?, 'XAC_NHAN', ?, ?)
                """;

            try (PreparedStatement ps =
                    conn.prepareStatement(
                        sqlPhieu,
                        Statement.RETURN_GENERATED_KEYS
                    )) {

                ps.setInt(1, maCoSo);
                ps.setInt(2, maKyKho);
                ps.setInt(3, maTaiKhoan);
                ps.setString(4, maGiaoDich);
                ps.setString(5, ghiChu);

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (!rs.next()) {
                        throw new SQLException(
                            "Không lấy được mã phiếu nhập kho!"
                        );
                    }

                    maPhieuKho = rs.getInt(1);
                }
            }

            // ===== 4. THEM CHI TIET PHIEU KHO =====

            String sqlChiTiet = """
                INSERT INTO dbo.ChiTietPhieuKho
                (
                    MaPhieuKho,
                    MaCoSo,
                    MaHangHoa,
                    SoLuong,
                    DonGiaSiTaiThoiDiem,
                    DonGiaXuatBanTaiThoiDiem,
                    GhiChu
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement ps =
                    conn.prepareStatement(sqlChiTiet)) {

                ps.setInt(1, maPhieuKho);
                ps.setInt(2, maCoSo);
                ps.setInt(3, maHangHoa);
                ps.setBigDecimal(4, soLuongNhap);
                ps.setBigDecimal(5, giaSi);
                ps.setBigDecimal(6, giaXuatBan);
                ps.setString(7, ghiChu);

                ps.executeUpdate();
            }

            // ===== 5. LUU GIAO DICH =====

            conn.commit();

            return maPhieuKho;

        } catch (SQLException | RuntimeException e) {

            try {
                conn.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }

            throw e;
        }
    }
}

}
