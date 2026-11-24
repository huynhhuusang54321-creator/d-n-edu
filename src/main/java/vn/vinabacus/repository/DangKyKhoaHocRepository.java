package vn.vinabacus.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.DangKyKhoaHoc.KetQua;
import vn.vinabacus.model.DangKyKhoaHoc.LuaChon;
import vn.vinabacus.service.QuyTacTrungLich;
import vn.vinabacus.service.GiamGiaService;
import vn.vinabacus.service.PhanBoThanhToanService;
import vn.vinabacus.service.QuyTacSiSoLop;
import vn.vinabacus.service.DuKienKetThucLop;

/** Giao dịch tuyển sinh, đăng ký khóa và xếp lớp của Trung tâm. */
public final class DangKyKhoaHocRepository {
    private record HoSo(String hoTen, String tenLienHe, String soDienThoai,
                        String nhuCau, String trangThai, LocalDate ngaySinh,
                        String gioiTinh,String diaChi) { }
    private record Khoa(int maMonHoc, int soBuoi, BigDecimal hocPhi) { }
    private final TaiChinhDangKyRepository taiChinhRepo = new TaiChinhDangKyRepository();
    private final ChuongTrinhGiamGiaRepository giamGiaRepo = new ChuongTrinhGiamGiaRepository();
    private record DangKy(int maHocVien, int maKhoaHoc, Integer maYeuCau,
                          LocalDate ngayBatDau, String trangThai) { }
    private record Lich(int thu, LocalDate tuNgay, LocalDate denNgay,
                        LocalTime batDau, LocalTime ketThuc) { }
    private record LopCoBan(int maLop, int maKhoaHoc, String tenLop,
                            String tenGiaoVien, String tenPhong, int soDaGiuCho,
                            Integer siSoToiDa, Integer sucChuaPhong,
                            boolean phongKhongHoatDong, boolean lichThieuPhong,
                            LocalDate ngayKhaiGiang, String trangThai) { }

    /** Danh sách lớp kèm lịch và sĩ số hiện tại để giao diện chọn theo khóa. */
    public List<DangKyKhoaHoc.ThongTinLop> thongTinLop(int maCoSo)
            throws SQLException {
        String sql = """
                SELECT l.MaLop, l.MaKhoaHoc, l.TenLop,
                       nv.HoTen AS TenGiaoVien, l.NgayKhaiGiang,
                       l.TrangThai, l.SiSoToiDa,
                       phong.SucChuaPhong, phong.PhongKhongHoatDong,phong.TenPhong,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.LichHocLop lp
                           WHERE lp.MaLop=l.MaLop AND lp.TrangThai='HIEU_LUC'
                             AND lp.MaPhong IS NULL
                             AND (lp.DenNgay IS NULL OR lp.DenNgay>=CONVERT(date,GETDATE())))
                         OR EXISTS (
                           SELECT 1 FROM dbo.BuoiHoc b
                           WHERE b.MaLop=l.MaLop AND b.TrangThai='DU_KIEN'
                             AND b.MaPhong IS NULL
                             AND b.NgayHoc>=CONVERT(date,GETDATE()))
                         THEN CAST(1 AS bit) ELSE CAST(0 AS bit) END AS LichThieuPhong,
                       (SELECT COUNT(DISTINCT d.MaHocVien)
                          FROM dbo.XepLopTrungTam x
                          INNER JOIN dbo.DangKyKhoaHoc d
                            ON d.MaDangKy=x.MaDangKy AND d.MaCoSo=x.MaCoSo
                         WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo
                           AND x.TrangThai='HIEU_LUC'
                           AND (x.DenNgay IS NULL OR x.DenNgay>=CONVERT(date,GETDATE()))) AS SoDaGiuCho,
                       lh.ThuTrongTuan, lh.GioBatDau, lh.GioKetThuc
                FROM dbo.LopTrungTam l
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien=l.MaGiaoVien
                LEFT JOIN dbo.LichHocLop lh
                  ON lh.MaLop=l.MaLop AND lh.TrangThai='HIEU_LUC'
                 AND lh.GioBatDau IS NOT NULL AND lh.GioKetThuc IS NOT NULL
                 AND (lh.DenNgay IS NULL OR lh.DenNgay>=CONVERT(date,GETDATE()))
                OUTER APPLY (
                    SELECT MIN(p.SucChua) AS SucChuaPhong,
                           STRING_AGG(CONVERT(nvarchar(max),p.TenPhong),N', ') AS TenPhong,
                           MAX(CASE WHEN p.TrangThai='HOAT_DONG' THEN 0 ELSE 1 END)
                               AS PhongKhongHoatDong
                    FROM dbo.PhongHoc p
                    WHERE p.MaCoSo=l.MaCoSo AND (
                        EXISTS (SELECT 1 FROM dbo.LichHocLop lp
                                WHERE lp.MaLop=l.MaLop AND lp.MaPhong=p.MaPhong
                                  AND lp.TrangThai='HIEU_LUC'
                                  AND (lp.DenNgay IS NULL OR lp.DenNgay>=CONVERT(date,GETDATE())))
                        OR EXISTS (SELECT 1 FROM dbo.BuoiHoc b
                                   WHERE b.MaLop=l.MaLop AND b.MaPhong=p.MaPhong
                                     AND b.TrangThai='DU_KIEN'
                                     AND b.NgayHoc>=CONVERT(date,GETDATE()))
                    )
                ) phong
                WHERE l.MaCoSo=?
                ORDER BY l.MaLop, lh.ThuTrongTuan, lh.GioBatDau
                """;
        Map<Integer, LopCoBan> cacLop = new LinkedHashMap<>();
        Map<Integer, List<String>> lich = new LinkedHashMap<>();
        try (Connection c = Database.moKetNoi();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int maLop = rs.getInt("MaLop");
                    Date ngay = rs.getDate("NgayKhaiGiang");
                    int sucChuaPhong = rs.getInt("SucChuaPhong");
                    Integer gioiHanPhong = rs.wasNull() ? null : sucChuaPhong;
                    Number siSoLop = (Number) rs.getObject("SiSoToiDa");
                    cacLop.putIfAbsent(maLop, new LopCoBan(maLop,
                            rs.getInt("MaKhoaHoc"), rs.getString("TenLop"),
                            rs.getString("TenGiaoVien"), rs.getString("TenPhong"),
                            rs.getInt("SoDaGiuCho"),
                            siSoLop == null ? null : siSoLop.intValue(), gioiHanPhong,
                            rs.getInt("PhongKhongHoatDong") == 1,
                            rs.getBoolean("LichThieuPhong"),
                            ngay == null ? null : ngay.toLocalDate(),
                            rs.getString("TrangThai")));
                    if (rs.getObject("ThuTrongTuan") != null) {
                        String ca = "Thứ " + rs.getInt("ThuTrongTuan") + " "
                                + rs.getTime("GioBatDau").toLocalTime()
                                + "–" + rs.getTime("GioKetThuc").toLocalTime();
                        lich.computeIfAbsent(maLop, ignored -> new ArrayList<>()).add(ca);
                    }
                }
            }
        }
        List<DangKyKhoaHoc.ThongTinLop> ketQua = new ArrayList<>();
        for (LopCoBan lop : cacLop.values()) {
            List<String> ca = lich.getOrDefault(lop.maLop(), List.of());
            QuyTacSiSoLop.KetQua siSo = QuyTacSiSoLop.danhGia(
                    lop.siSoToiDa(), lop.sucChuaPhong(), lop.soDaGiuCho());
            String trangThaiNhan;
            if (!"DANG_MO".equals(lop.trangThai())) trangThaiNhan = "Không nhận học viên";
            else if (lop.siSoToiDa() == null || lop.siSoToiDa() <= 0)
                trangThaiNhan = siSo.trangThai();
            else if (ca.isEmpty()) trangThaiNhan = "Chưa có lịch hợp lệ";
            else if (lop.lichThieuPhong()) trangThaiNhan = "Lịch chưa gán đủ phòng";
            else if (lop.phongKhongHoatDong()) trangThaiNhan = "Phòng không hoạt động";
            else trangThaiNhan = siSo.trangThai();
            ketQua.add(new DangKyKhoaHoc.ThongTinLop(lop.maLop(), lop.maKhoaHoc(),
                    lop.tenLop(), lop.tenGiaoVien(),
                    ca.isEmpty() ? "Chưa có lịch hợp lệ" : String.join("; ", ca),
                    lop.soDaGiuCho(), lop.siSoToiDa(), lop.sucChuaPhong(),
                    siSo.gioiHan(), lop.ngayKhaiGiang(),
                    trangThaiNhan,lop.tenPhong()));
        }
        return ketQua;
    }

    public List<DangKyKhoaHoc> layDanhSach(int maCoSo) throws SQLException {
        String sql = """
                SELECT d.MaDangKy, d.MaHocVien, y.MaKhach, h.HoTen,
                       d.MaKhoaHoc, m.TenMonHoc, k.TenKhoaHoc,
                       d.TongSoBuoiDangKy, d.HocPhiSauGiam, d.NgayBatDau,
                       d.TrangThai, x.MaLop, l.TenLop
                FROM dbo.DangKyKhoaHoc d
                INNER JOIN dbo.HocVien h
                    ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo
                INNER JOIN dbo.KhoaHoc k
                    ON k.MaKhoaHoc=d.MaKhoaHoc AND k.MaCoSo=d.MaCoSo
                INNER JOIN dbo.MonHoc m
                    ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                LEFT JOIN dbo.YeuCauXepLop y
                    ON y.MaYeuCau=d.MaYeuCau AND y.MaCoSo=d.MaCoSo
                OUTER APPLY (
                    SELECT TOP (1) xl.MaLop
                    FROM dbo.XepLopTrungTam xl
                    WHERE xl.MaDangKy=d.MaDangKy AND xl.MaCoSo=d.MaCoSo
                      AND ((d.TrangThai='DANG_HOC'
                            AND xl.TrangThai='HIEU_LUC'
                            AND (xl.DenNgay IS NULL OR xl.DenNgay>=CONVERT(date, GETDATE())))
                        OR (d.TrangThai='HOAN_THANH'
                            AND xl.TrangThai IN ('HIEU_LUC','KET_THUC')))
                    ORDER BY CASE WHEN xl.TrangThai='HIEU_LUC'
                                       AND (xl.DenNgay IS NULL OR xl.DenNgay>=CONVERT(date, GETDATE()))
                                  THEN 0 ELSE 1 END,
                             xl.MaXepLop DESC
                ) x
                LEFT JOIN dbo.LopTrungTam l
                    ON l.MaLop=x.MaLop AND l.MaCoSo=d.MaCoSo
                WHERE d.MaCoSo=?
                ORDER BY d.MaDangKy DESC
                """;
        List<DangKyKhoaHoc> ketQua = new ArrayList<>();
        try (Connection c = Database.moKetNoi();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date ngay = rs.getDate("NgayBatDau");
                    ketQua.add(new DangKyKhoaHoc(
                            rs.getInt("MaDangKy"), rs.getInt("MaHocVien"),
                            soNguyenHoacNull(rs, "MaKhach"), rs.getString("HoTen"),
                            rs.getInt("MaKhoaHoc"), rs.getString("TenMonHoc"),
                            rs.getString("TenKhoaHoc"), rs.getInt("TongSoBuoiDangKy"),
                            rs.getBigDecimal("HocPhiSauGiam"),
                            ngay == null ? null : ngay.toLocalDate(),
                            rs.getString("TrangThai"), soNguyenHoacNull(rs, "MaLop"),
                            rs.getString("TenLop")));
                }
            }
        }
        return ketQua;
    }

    /** Một hồ sơ có thể đăng ký nhiều khóa; mọi bản ghi được ghi cùng giao dịch. */
    public KetQua dangKyTuHoSo(int maCoSo, int maKhach, LocalDate ngayBatDau,
                               List<LuaChon> luaChon) throws SQLException {
        return dangKyTuHoSo(maCoSo, maKhach, ngayBatDau, luaChon, null);
    }

    /** Bản Giai đoạn 3 chốt giảm giá, công nợ và tiền thực thu cùng giao dịch. */
    public KetQua dangKyTuHoSo(int maCoSo, int maKhach, LocalDate ngayBatDau,
            List<LuaChon> luaChon, DangKyKhoaHoc.TaiChinh taiChinh)
            throws SQLException {
        if (maCoSo <= 0 || maKhach <= 0 || ngayBatDau == null
                || luaChon == null || luaChon.isEmpty()) {
            throw new IllegalArgumentException("Chọn hồ sơ, ngày bắt đầu và ít nhất một khóa học");
        }
        Set<Integer> cacKhoa = new HashSet<>();
        for (LuaChon chon : luaChon) {
            if (chon == null || chon.maKhoaHoc() <= 0
                    || (chon.maLop() != null && chon.maLop() <= 0)) {
                throw new IllegalArgumentException("Khóa học hoặc lớp học được chọn không hợp lệ");
            }
            if (!cacKhoa.add(chon.maKhoaHoc())) {
                throw new IllegalArgumentException("Một khóa học chỉ được chọn một lần");
            }
        }
        // Cùng thứ tự khóa giúp các giao dịch nhiều khóa tránh khóa vòng nhau.
        List<LuaChon> theoThuTuKhoa = new ArrayList<>(luaChon);
        theoThuTuKhoa.sort(Comparator.comparingInt(LuaChon::maKhoaHoc));
        try (Connection c = Database.moKetNoi()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                HoSo hoSo = khoaHoSo(c, maCoSo, maKhach);
                if ("KHONG_DANG_KY".equals(hoSo.trangThai())) {
                    throw new SQLException("Hồ sơ Không đăng ký không thể đăng ký khóa học");
                }
                Integer daCoHocVien = timHocVienCuaHoSo(c, maCoSo, maKhach);
                if (daCoHocVien == null && "DA_CHUYEN_DOI".equals(hoSo.trangThai())) {
                    throw new SQLException("Hồ sơ đã chuyển đổi nhưng thiếu liên kết học viên; cần kiểm tra dữ liệu");
                }
                int maHocVien = daCoHocVien == null
                        ? taoHocVien(c, maCoSo, maKhach, hoSo)
                        : daCoHocVien;
                kiemTraHocVien(c, maCoSo, maHocVien);
                dongBoThongTinCaNhan(c,maCoSo,maHocVien,hoSo);

                Map<Integer, ChuongTrinhGiamGia> giamGiaDaChon = chuongTrinhDaChon(
                        c, maCoSo, taiChinh == null ? List.of() : taiChinh.maChuongTrinh());
                Map<Integer, Khoa> giaKhoa = khoaDuocChon(c, maCoSo, theoThuTuKhoa);
                GiamGiaService.BangGia bangGia = chotBangGia(c, maCoSo,
                        theoThuTuKhoa, giaKhoa, taiChinh, giamGiaDaChon);
                Map<Integer, TaiChinhDangKyRepository.Khoan> khoanThu = new LinkedHashMap<>();
                int soXepNgay = 0;
                for (LuaChon chon : theoThuTuKhoa) {
                    Khoa khoa = giaKhoa.get(chon.maKhoaHoc());
                    kiemTraTrungDangKy(c, maCoSo, maHocVien, chon.maKhoaHoc());
                    int maYeuCau = taoYeuCau(c, maCoSo, maKhach, maHocVien,
                            chon.maKhoaHoc(), hoSo.nhuCau());
                    int maDangKy = taoDangKy(c, maCoSo, maHocVien, maYeuCau,
                            chon.maKhoaHoc(), ngayBatDau, khoa,
                            bangGia == null ? null : bangGia.choKhoa(chon.maKhoaHoc()));
                    if (bangGia != null) {
                        luuChiTietGiam(c, maCoSo, maDangKy,
                                bangGia.choKhoa(chon.maKhoaHoc()), giamGiaDaChon);
                        khoanThu.put(chon.maKhoaHoc(),
                                taiChinhRepo.lapKhoan(c, maCoSo, maDangKy, null));
                    }
                    if (chon.maLop() != null) {
                        xepTrongGiaoDich(c, maCoSo, maDangKy, maHocVien,
                                chon.maKhoaHoc(), maYeuCau, chon.maLop(), ngayBatDau);
                        soXepNgay++;
                    }
                }
                if (taiChinh != null) ghiThanhToan(c, maCoSo, maHocVien,
                        khoanThu, bangGia, taiChinh);
                // Hồ sơ chuyển đổi bằng luồng cũ có thể có yêu cầu chưa gắn khóa;
                // giữ trạng thái đã chuyển đổi, không đưa ngược về chờ xếp lớp.
                boolean chinhThuc = "DA_CHUYEN_DOI".equals(hoSo.trangThai());
                datTrangThaiHoSo(c, maCoSo, maKhach,
                        chinhThuc ? "DA_CHUYEN_DOI" : "CHO_XEP_LOP");
                c.commit();
                return new KetQua(maHocVien, luaChon.size(), soXepNgay, chinhThuc);
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Xếp một đăng ký đang chờ vào lớp cùng khóa/cơ sở. */
    public void xepLop(int maCoSo, int maDangKy, int maLop,
                       LocalDate ngayXep) throws SQLException {
        if (maCoSo <= 0 || maDangKy <= 0 || maLop <= 0 || ngayXep == null) {
            throw new IllegalArgumentException("Chọn đăng ký, lớp học và ngày xếp lớp hợp lệ");
        }
        try (Connection c = Database.moKetNoi()) {
            // Đọc liên kết trước giao dịch để cùng khóa hồ sơ theo thứ tự với dangKyTuHoSo.
            Integer maKhach = timMaKhachCuaDangKy(c, maCoSo, maDangKy);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                if (maKhach != null) khoaHoSo(c, maCoSo, maKhach);
                DangKy dangKy = khoaDangKy(c, maCoSo, maDangKy);
                if (!"CHO_XEP_LOP".equals(dangKy.trangThai())) {
                    throw new SQLException("Chỉ có thể xếp lớp cho đăng ký đang chờ xếp lớp");
                }
                if (ngayXep.isBefore(dangKy.ngayBatDau())) {
                    throw new SQLException("Ngày xếp lớp không được trước ngày bắt đầu đăng ký");
                }
                kiemTraHocVien(c, maCoSo, dangKy.maHocVien());
                if (dangKy.maYeuCau() != null) {
                    Integer maKhachThuc = timMaKhachCuaYeuCau(c, maCoSo,
                            dangKy.maYeuCau(), dangKy.maHocVien(), dangKy.maKhoaHoc());
                    if (!java.util.Objects.equals(maKhach, maKhachThuc)) {
                        throw new SQLException("Liên kết hồ sơ tuyển sinh đã thay đổi; hãy tải lại");
                    }
                }
                xepTrongGiaoDich(c, maCoSo, maDangKy, dangKy.maHocVien(),
                        dangKy.maKhoaHoc(), dangKy.maYeuCau(), maLop, ngayXep);
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Đăng ký thêm khóa cho học viên đã chính thức, không tạo hồ sơ tuyển sinh mới. */
    public KetQua dangKyChoHocVien(int maCoSo, int maHocVien,
                                  LocalDate ngayBatDau, List<LuaChon> luaChon)
            throws SQLException {
        return dangKyChoHocVien(maCoSo, maHocVien, ngayBatDau, luaChon, null);
    }

    public KetQua dangKyChoHocVien(int maCoSo, int maHocVien,
            LocalDate ngayBatDau, List<LuaChon> luaChon,
            DangKyKhoaHoc.TaiChinh taiChinh) throws SQLException {
        if (maCoSo <= 0 || maHocVien <= 0 || ngayBatDau == null
                || luaChon == null || luaChon.isEmpty()) {
            throw new IllegalArgumentException("Chọn học viên, ngày bắt đầu và ít nhất một khóa học");
        }
        Set<Integer> cacKhoa = new HashSet<>();
        for (LuaChon chon : luaChon) {
            if (chon == null || chon.maKhoaHoc() <= 0
                    || (chon.maLop() != null && chon.maLop() <= 0)) {
                throw new IllegalArgumentException("Khóa học hoặc lớp học được chọn không hợp lệ");
            }
            if (!cacKhoa.add(chon.maKhoaHoc())) {
                throw new IllegalArgumentException("Một khóa học chỉ được chọn một lần");
            }
        }
        List<LuaChon> theoThuTuKhoa = new ArrayList<>(luaChon);
        theoThuTuKhoa.sort(Comparator.comparingInt(LuaChon::maKhoaHoc));
        try (Connection c = Database.moKetNoi()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                kiemTraHocVien(c, maCoSo, maHocVien);
                kiemTraHocVienChinhThuc(c, maCoSo, maHocVien);
                Map<Integer, ChuongTrinhGiamGia> giamGiaDaChon = chuongTrinhDaChon(
                        c, maCoSo, taiChinh == null ? List.of() : taiChinh.maChuongTrinh());
                Map<Integer, Khoa> giaKhoa = khoaDuocChon(c, maCoSo, theoThuTuKhoa);
                GiamGiaService.BangGia bangGia = chotBangGia(c, maCoSo,
                        theoThuTuKhoa, giaKhoa, taiChinh, giamGiaDaChon);
                Map<Integer, TaiChinhDangKyRepository.Khoan> khoanThu = new LinkedHashMap<>();
                int soXepNgay = 0;
                for (LuaChon chon : theoThuTuKhoa) {
                    Khoa khoa = giaKhoa.get(chon.maKhoaHoc());
                    kiemTraTrungDangKy(c, maCoSo, maHocVien, chon.maKhoaHoc());
                    int maDangKy = taoDangKy(c, maCoSo, maHocVien, null,
                            chon.maKhoaHoc(), ngayBatDau, khoa,
                            bangGia == null ? null : bangGia.choKhoa(chon.maKhoaHoc()));
                    if (bangGia != null) {
                        luuChiTietGiam(c, maCoSo, maDangKy,
                                bangGia.choKhoa(chon.maKhoaHoc()), giamGiaDaChon);
                        khoanThu.put(chon.maKhoaHoc(),
                                taiChinhRepo.lapKhoan(c, maCoSo, maDangKy, null));
                    }
                    if (chon.maLop() != null) {
                        xepTrongGiaoDich(c, maCoSo, maDangKy, maHocVien,
                                chon.maKhoaHoc(), null, chon.maLop(), ngayBatDau);
                        soXepNgay++;
                    }
                }
                if (taiChinh != null) ghiThanhToan(c, maCoSo, maHocVien,
                        khoanThu, bangGia, taiChinh);
                c.commit();
                return new KetQua(maHocVien, luaChon.size(), soXepNgay, true);
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Tách xác nhận nhập học khỏi thao tác lưu đăng ký/xếp lớp. */
    public int chuyenThanhHocVien(int maCoSo, int maKhach) throws SQLException {
        if (maCoSo <= 0 || maKhach <= 0)
            throw new IllegalArgumentException("Hãy chọn hồ sơ tuyển sinh hợp lệ");
        try (Connection c = Database.moKetNoi()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                HoSo hoSo = khoaHoSo(c, maCoSo, maKhach);
                if ("KHONG_DANG_KY".equals(hoSo.trangThai()))
                    throw new SQLException("Hồ sơ Không đăng ký không thể chuyển thành học viên");
                if (!"CHO_XEP_LOP".equals(hoSo.trangThai())
                        && !"DA_CHUYEN_DOI".equals(hoSo.trangThai()))
                    throw new SQLException("Hồ sơ cần đăng ký khóa và xếp lớp trước khi chuyển thành học viên");
                Integer maHocVien = timHocVienCuaHoSo(c, maCoSo, maKhach);
                if (maHocVien == null)
                    throw new SQLException("Hồ sơ chưa có học viên liên kết với đăng ký khóa");
                kiemTraHocVien(c, maCoSo, maHocVien);
                dongBoThongTinCaNhan(c,maCoSo,maHocVien,hoSo);
                if (!"DA_CHUYEN_DOI".equals(hoSo.trangThai())
                        && !daCoXepLopCuaHoSo(c, maCoSo, maKhach))
                    throw new SQLException("Cần ít nhất một đăng ký hợp lệ đã được xếp lớp");
                if (!"DA_CHUYEN_DOI".equals(hoSo.trangThai()))
                    datTrangThaiHoSo(c, maCoSo, maKhach, "DA_CHUYEN_DOI");
                c.commit();
                return maHocVien;
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Mã học viên được tạo để giữ khóa ngoại đăng ký nhưng chưa được xác nhận nhập học. */
    public Set<Integer> layMaHocVienTam(int maCoSo) throws SQLException {
        String sql = """
                SELECT DISTINCT y.MaHocVien
                FROM dbo.YeuCauXepLop y
                INNER JOIN dbo.KhachTiemNang k
                    ON k.MaKhach=y.MaKhach AND k.MaCoSo=y.MaCoSo
                WHERE y.MaCoSo=? AND k.TrangThai<>'DA_CHUYEN_DOI'
                """;
        Set<Integer> ketQua = new HashSet<>();
        try (Connection c = Database.moKetNoi();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ketQua.add(rs.getInt(1));
            }
        }
        return ketQua;
    }

    private static HoSo khoaHoSo(Connection c, int maCoSo, int maKhach)
            throws SQLException {
        boolean coTruongMoi;
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT CASE WHEN COL_LENGTH('dbo.KhachTiemNang','NgaySinh') IS NOT NULL AND COL_LENGTH('dbo.KhachTiemNang','GioiTinh') IS NOT NULL AND COL_LENGTH('dbo.KhachTiemNang','DiaChi') IS NOT NULL THEN 1 ELSE 0 END")){
            r.next();coTruongMoi=r.getInt(1)==1;
        }
        String sql = "SELECT HoTen,TenNguoiLienHe,SoDienThoai,NhuCau,TrangThai,"+
            (coTruongMoi?"NgaySinh,GioiTinh,DiaChi":"CAST(NULL AS DATE) AS NgaySinh,CAST(NULL AS VARCHAR(15)) AS GioiTinh,CAST(NULL AS NVARCHAR(400)) AS DiaChi")+
            " FROM dbo.KhachTiemNang WITH (UPDLOCK,HOLDLOCK) WHERE MaKhach=? AND MaCoSo=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new HoSo(rs.getString("HoTen"), rs.getString("TenNguoiLienHe"),
                            rs.getString("SoDienThoai"), rs.getString("NhuCau"),
                            rs.getString("TrangThai"),rs.getDate("NgaySinh")==null?null:rs.getDate("NgaySinh").toLocalDate(),
                            rs.getString("GioiTinh"),rs.getString("DiaChi"));
                }
            }
        }
        throw new SQLException("Không tìm thấy hồ sơ tuyển sinh thuộc Trung tâm này");
    }

    private static Integer timHocVienCuaHoSo(Connection c, int maCoSo,
                                             int maKhach) throws SQLException {
        String sql = """
                SELECT MaHocVien FROM dbo.YeuCauXepLop WITH (UPDLOCK, HOLDLOCK)
                WHERE MaKhach=? AND MaCoSo=?
                """;
        Integer maHocVien = null;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    if (maHocVien != null && maHocVien != id) {
                        throw new SQLException("Một hồ sơ liên kết nhiều học viên; cần kiểm tra dữ liệu");
                    }
                    maHocVien = id;
                }
            }
        }
        return maHocVien;
    }

    private static int taoHocVien(Connection c, int maCoSo, int maKhach,
                                  HoSo hoSo) throws SQLException {
        String sql = """
                INSERT dbo.HocVien(MaCoSo, HoTen, TenNguoiLienHe,
                                   SoDienThoaiLienHe, GhiChu,NgaySinh,GioiTinh,DiaChi)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maCoSo);
            ps.setString(2, hoSo.hoTen());
            ps.setString(3, hoSo.tenLienHe());
            ps.setString(4, hoSo.soDienThoai());
            ps.setString(5, "Tuyển sinh từ khách #" + maKhach);
            if(hoSo.ngaySinh()==null)ps.setNull(6,Types.DATE);else ps.setDate(6,Date.valueOf(hoSo.ngaySinh()));
            ps.setString(7,hoSo.gioiTinh());ps.setString(8,hoSo.diaChi());
            if (ps.executeUpdate() != 1) throw new SQLException("Không tạo được học viên");
            return maVuaTao(ps, "học viên");
        }
    }

    private static void dongBoThongTinCaNhan(Connection c,int maCoSo,int maHocVien,HoSo hoSo)throws SQLException{
        String sql="SELECT NgaySinh,GioiTinh,DiaChi FROM dbo.HocVien WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=?";
        try(PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,maHocVien);p.setInt(2,maCoSo);
            try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("Không tìm thấy học viên liên kết.");
                LocalDate birth=r.getDate(1)==null?null:r.getDate(1).toLocalDate();
                if(birth!=null&&hoSo.ngaySinh()!=null&&!birth.equals(hoSo.ngaySinh())
                    ||r.getString(2)!=null&&hoSo.gioiTinh()!=null&&!r.getString(2).equals(hoSo.gioiTinh())
                    ||r.getString(3)!=null&&hoSo.diaChi()!=null&&!r.getString(3).equals(hoSo.diaChi()))
                    throw new SQLException("Thông tin cá nhân khác học viên đã liên kết; hãy đối chiếu trước khi chuyển đổi.");
            }
        }
        try(PreparedStatement p=c.prepareStatement("UPDATE dbo.HocVien SET NgaySinh=COALESCE(NgaySinh,?),GioiTinh=COALESCE(GioiTinh,?),DiaChi=COALESCE(DiaChi,?) WHERE MaHocVien=? AND MaCoSo=?")){
            if(hoSo.ngaySinh()==null)p.setNull(1,Types.DATE);else p.setDate(1,Date.valueOf(hoSo.ngaySinh()));
            p.setString(2,hoSo.gioiTinh());p.setString(3,hoSo.diaChi());p.setInt(4,maHocVien);p.setInt(5,maCoSo);
            p.executeUpdate();
        }
    }

    private static void kiemTraHocVien(Connection c, int maCoSo, int maHocVien)
            throws SQLException {
        String sql = """
                SELECT TrangThaiHoSo FROM dbo.HocVien WITH (UPDLOCK, HOLDLOCK)
                WHERE MaHocVien=? AND MaCoSo=?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không tìm thấy học viên của Trung tâm này");
                if (!"HOAT_DONG".equals(rs.getString(1))) {
                    throw new SQLException("Học viên đã ngừng sử dụng; cần kích hoạt lại trước khi đăng ký/xếp lớp");
                }
            }
        }
    }

    private static void kiemTraHocVienChinhThuc(Connection c, int maCoSo,
                                                int maHocVien) throws SQLException {
        String sql = """
                SELECT TOP (1) k.MaKhach
                FROM dbo.YeuCauXepLop y WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.KhachTiemNang k WITH (HOLDLOCK)
                    ON k.MaKhach=y.MaKhach AND k.MaCoSo=y.MaCoSo
                WHERE y.MaHocVien=? AND y.MaCoSo=?
                  AND k.TrangThai<>'DA_CHUYEN_DOI'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    throw new SQLException("Hồ sơ này chưa được xác nhận thành học viên chính thức");
            }
        }
    }

    private static Khoa khoaKhoaHoc(Connection c, int maCoSo, int maKhoaHoc)
            throws SQLException {
        String sql = """
                SELECT k.MaMonHoc, k.TongSoBuoi, k.HocPhiMacDinh
                FROM dbo.KhoaHoc k WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.MonHoc m WITH (HOLDLOCK)
                    ON m.MaMonHoc=k.MaMonHoc AND m.MaCoSo=k.MaCoSo
                WHERE k.MaKhoaHoc=? AND k.MaCoSo=?
                  AND k.TrangThai=1 AND m.TrangThai=1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maKhoaHoc);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Khoa(rs.getInt(1), rs.getInt(2),
                        rs.getBigDecimal(3));
            }
        }
        throw new SQLException("Khóa học hoặc môn học không hoạt động tại Trung tâm này");
    }

    private static void kiemTraTrungDangKy(Connection c, int maCoSo,
                                           int maHocVien, int maKhoaHoc)
            throws SQLException {
        String sql = """
                SELECT TOP (1) MaDangKy
                FROM dbo.DangKyKhoaHoc WITH (UPDLOCK, HOLDLOCK)
                WHERE MaHocVien=? AND MaCoSo=? AND MaKhoaHoc=? AND TrangThai<>'HUY'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            ps.setInt(3, maKhoaHoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException("Học viên đã đăng ký khóa học này");
            }
        }
    }

    private static int taoYeuCau(Connection c, int maCoSo, int maKhach,
                                 int maHocVien, int maKhoaHoc, String nhuCau)
            throws SQLException {
        String sql = """
                INSERT dbo.YeuCauXepLop(MaKhach, MaHocVien, MaCoSo,
                                         MaKhoaHoc, NhuCau, TrangThai)
                VALUES (?, ?, ?, ?, ?, 'CHO_XEP_LOP')
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maHocVien);
            ps.setInt(3, maCoSo);
            ps.setInt(4, maKhoaHoc);
            ps.setString(5, nhuCau);
            if (ps.executeUpdate() != 1) throw new SQLException("Không tạo được yêu cầu xếp lớp");
            return maVuaTao(ps, "yêu cầu xếp lớp");
        }
    }

    private static int taoDangKy(Connection c, int maCoSo, int maHocVien,
                                 Integer maYeuCau, int maKhoaHoc,
                                 LocalDate ngayBatDau, Khoa khoa,
                                 GiamGiaService.Dong dongGia) throws SQLException {
        String sql = """
                INSERT dbo.DangKyKhoaHoc
                    (MaHocVien, MaCoSo, MaKhoaHoc, MaYeuCau, NgayDangKy,
                     NgayBatDau, TongSoBuoiDangKy, HocPhiThoaThuan,
                     MaChuongTrinh, SoTienGiam, HocPhiSauGiam, TrangThai)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CHO_XEP_LOP')
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            ps.setInt(3, maKhoaHoc);
            if (maYeuCau == null) ps.setNull(4, java.sql.Types.INTEGER);
            else ps.setInt(4, maYeuCau);
            ps.setDate(5, Date.valueOf(LocalDate.now()));
            ps.setDate(6, Date.valueOf(ngayBatDau));
            ps.setInt(7, khoa.soBuoi());
            ps.setBigDecimal(8, khoa.hocPhi());
            Integer motChuongTrinh = dongGia == null
                    || dongGia.giamTheoChuongTrinh().size() != 1 ? null
                    : dongGia.giamTheoChuongTrinh().keySet().iterator().next();
            if (motChuongTrinh == null) ps.setNull(9, java.sql.Types.INTEGER);
            else ps.setInt(9, motChuongTrinh);
            ps.setBigDecimal(10, dongGia == null ? BigDecimal.ZERO : dongGia.tongGiam());
            ps.setBigDecimal(11, dongGia == null ? khoa.hocPhi() : dongGia.phaiThu());
            if (ps.executeUpdate() != 1) throw new SQLException("Không tạo được đăng ký khóa học");
            return maVuaTao(ps, "đăng ký khóa học");
        }
    }

    private static Map<Integer, Khoa> khoaDuocChon(Connection c, int maCoSo,
            List<LuaChon> luaChon) throws SQLException {
        Map<Integer, Khoa> out = new LinkedHashMap<>();
        for (LuaChon chon : luaChon) {
            out.put(chon.maKhoaHoc(), khoaKhoaHoc(c, maCoSo, chon.maKhoaHoc()));
        }
        return out;
    }

    private Map<Integer, ChuongTrinhGiamGia> chuongTrinhDaChon(Connection c,
            int maCoSo, List<Integer> ids) throws SQLException {
        Map<Integer, ChuongTrinhGiamGia> out = new LinkedHashMap<>();
        if (ids.isEmpty()) return out;
        Set<Integer> ma = new HashSet<>(ids);
        if (ma.size() != ids.size())
            throw new IllegalArgumentException("Chương trình giảm giá bị chọn trùng");
        for (ChuongTrinhGiamGia ct : giamGiaRepo.layDanhSach(c, maCoSo, true)) {
            if (ma.contains(ct.maChuongTrinh())) out.put(ct.maChuongTrinh(), ct);
        }
        if (out.size() != ids.size())
            throw new SQLException("Chương trình giảm giá đã thay đổi; hãy tải lại danh sách");
        return out;
    }

    private static GiamGiaService.BangGia chotBangGia(Connection c, int maCoSo,
            List<LuaChon> luaChon, Map<Integer, Khoa> giaKhoa,
            DangKyKhoaHoc.TaiChinh taiChinh,
            Map<Integer, ChuongTrinhGiamGia> giamGia) throws SQLException {
        if (taiChinh == null) return null;
        List<KhoaHoc> cacKhoa = new ArrayList<>();
        for (LuaChon chon : luaChon) {
            Khoa k = giaKhoa.get(chon.maKhoaHoc());
            cacKhoa.add(new KhoaHoc(chon.maKhoaHoc(), maCoSo, k.maMonHoc(),
                    "", "", k.soBuoi(), k.hocPhi(), true));
        }
        GiamGiaService.BangGia bang = GiamGiaService.tinh(cacKhoa,
                new ArrayList<>(giamGia.values()), LocalDate.now());
        if (taiChinh.phaiThuDuKien() == null
                || bang.phaiThu().compareTo(taiChinh.phaiThuDuKien()) != 0)
            throw new SQLException("Học phí hoặc chương trình đã thay đổi; hãy tải lại và kiểm tra trước khi lưu");
        if (taiChinh.soTienThu() == null || taiChinh.soTienThu().signum() < 0
                || taiChinh.soTienThu().compareTo(bang.phaiThu()) > 0)
            throw new IllegalArgumentException("Số tiền thực thu vượt học phí phải thu");
        return bang;
    }

    private static void luuChiTietGiam(Connection c, int maCoSo, int maDangKy,
            GiamGiaService.Dong dong,
            Map<Integer, ChuongTrinhGiamGia> giamGia) throws SQLException {
        if (dong.giamTheoChuongTrinh().isEmpty()) return;
        List<ChuongTrinhGiamGia> thuTu = new ArrayList<>(giamGia.values());
        thuTu.sort(Comparator.comparingInt((ChuongTrinhGiamGia ct) ->
                "PHAN_TRAM".equals(ct.loaiGiam()) ? 0 : 1)
                .thenComparingInt(ChuongTrinhGiamGia::maChuongTrinh));
        String sql = """
                INSERT dbo.ChiTietGiamGiaDangKy
                    (MaDangKy,MaChuongTrinh,MaCoSo,TenChuongTrinhSnapshot,
                     LoaiGiamSnapshot,GiaTriGiamSnapshot,SoTienGiam,
                     ThuTuApDung)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int thuTuApDung = 0;
            for (ChuongTrinhGiamGia ct : thuTu) {
                thuTuApDung++;
                BigDecimal soTien = dong.giamTheoChuongTrinh().get(ct.maChuongTrinh());
                if (soTien == null) continue;
                ps.setInt(1, maDangKy); ps.setInt(2, ct.maChuongTrinh());
                ps.setInt(3, maCoSo); ps.setString(4, ct.ten());
                ps.setString(5, ct.loaiGiam()); ps.setBigDecimal(6, ct.giaTri());
                ps.setBigDecimal(7, soTien); ps.setInt(8, thuTuApDung);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void ghiThanhToan(Connection c, int maCoSo, int maHocVien,
            Map<Integer, TaiChinhDangKyRepository.Khoan> khoanThu,
            GiamGiaService.BangGia bang, DangKyKhoaHoc.TaiChinh yeuCau)
            throws SQLException {
        if (yeuCau.soTienThu().signum() == 0) return;
        Map<Integer, BigDecimal> phaiThu = new LinkedHashMap<>();
        for (GiamGiaService.Dong d : bang.dong())
            phaiThu.put(d.maKhoaHoc(), d.phaiThu());
        Map<Integer, BigDecimal> tienTheoKhoa = PhanBoThanhToanService.tinh(
                phaiThu, yeuCau.soTienThu());
        List<TaiChinhDangKyRepository.PhanBo> phanBo = new ArrayList<>();
        for (GiamGiaService.Dong d : bang.dong()) {
            BigDecimal soTien = tienTheoKhoa.get(d.maKhoaHoc());
            if (soTien.signum() == 0) continue;
            TaiChinhDangKyRepository.Khoan k = khoanThu.get(d.maKhoaHoc());
            if (k == null || k.maKhoanThu() <= 0)
                throw new SQLException("Không có khoản phải thu để phân bổ thanh toán");
            phanBo.add(new TaiChinhDangKyRepository.PhanBo(k.maKhoanThu(), soTien));
        }
        taiChinhRepo.lapPhieu(c, maCoSo, maHocVien, yeuCau.soTienThu(),
                phanBo, yeuCau.hinhThuc(), yeuCau.maTaiKhoan(),
                yeuCau.ghiChu(), yeuCau.maGiaoDich());
    }

    private static DangKy khoaDangKy(Connection c, int maCoSo, int maDangKy)
            throws SQLException {
        String sql = """
                SELECT MaHocVien, MaKhoaHoc, MaYeuCau, NgayBatDau, TrangThai
                FROM dbo.DangKyKhoaHoc WITH (UPDLOCK, HOLDLOCK)
                WHERE MaDangKy=? AND MaCoSo=?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDangKy);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new DangKy(rs.getInt(1), rs.getInt(2),
                        soNguyenHoacNull(rs, "MaYeuCau"),
                        rs.getDate("NgayBatDau").toLocalDate(), rs.getString("TrangThai"));
            }
        }
        throw new SQLException("Không tìm thấy đăng ký khóa học thuộc Trung tâm này");
    }

    private static Integer timMaKhachCuaDangKy(Connection c, int maCoSo,
                                               int maDangKy) throws SQLException {
        String sql = """
                SELECT y.MaKhach
                FROM dbo.DangKyKhoaHoc d
                LEFT JOIN dbo.YeuCauXepLop y
                    ON y.MaYeuCau=d.MaYeuCau AND y.MaCoSo=d.MaCoSo
                WHERE d.MaDangKy=? AND d.MaCoSo=?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDangKy);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? soNguyenHoacNull(rs, "MaKhach") : null;
            }
        }
    }

    private static Integer timMaKhachCuaYeuCau(Connection c, int maCoSo,
                                                int maYeuCau, int maHocVien,
                                                int maKhoaHoc) throws SQLException {
        String sql = """
                SELECT MaKhach FROM dbo.YeuCauXepLop WITH (UPDLOCK, HOLDLOCK)
                WHERE MaYeuCau=? AND MaHocVien=? AND MaCoSo=?
                  AND (MaKhoaHoc=? OR MaKhoaHoc IS NULL)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maYeuCau);
            ps.setInt(2, maHocVien);
            ps.setInt(3, maCoSo);
            ps.setInt(4, maKhoaHoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Yêu cầu xếp lớp không khớp đăng ký khóa học");
    }

    private static void xepTrongGiaoDich(Connection c, int maCoSo, int maDangKy,
                                         int maHocVien, int maKhoaHoc,
                                         Integer maYeuCau, int maLop,
                                         LocalDate ngayXep) throws SQLException {
        String sqlLop = """
                SELECT MaKhoaHoc, TrangThai, SiSoToiDa, NgayKhaiGiang
                FROM dbo.LopTrungTam WITH (UPDLOCK, HOLDLOCK)
                WHERE MaLop=? AND MaCoSo=?
                """;
        int siSoToiDa;
        try (PreparedStatement ps = c.prepareStatement(sqlLop)) {
            ps.setInt(1, maLop);
            ps.setInt(2, maCoSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không tìm thấy lớp tại Trung tâm này");
                if (rs.getInt("MaKhoaHoc") != maKhoaHoc) {
                    throw new SQLException("Lớp được chọn không thuộc khóa học đăng ký");
                }
                if (!"DANG_MO".equals(rs.getString("TrangThai"))) {
                    throw new SQLException("Lớp được chọn không còn mở");
                }
                Date khaiGiang = rs.getDate("NgayKhaiGiang");
                if (khaiGiang != null && ngayXep.isBefore(khaiGiang.toLocalDate())) {
                    throw new SQLException("Ngày xếp lớp trước ngày khai giảng; hãy chọn ngày muộn hơn");
                }
                siSoToiDa = rs.getInt("SiSoToiDa");
            }
        }
        String sqlDaXep = """
                SELECT TOP (1) MaXepLop FROM dbo.XepLopTrungTam WITH (UPDLOCK, HOLDLOCK)
                WHERE MaDangKy=? AND MaCoSo=? AND TrangThai='HIEU_LUC'
                  AND (DenNgay IS NULL OR DenNgay>=?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sqlDaXep)) {
            ps.setInt(1, maDangKy);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(ngayXep));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException("Đăng ký đã được xếp lớp; hãy tải lại danh sách");
            }
        }
        kiemTraTrungLichHocVien(c, maCoSo, maHocVien, maLop, ngayXep);
        LocalDate ngayKetThucDuKien = duKienKetThuc(c, maCoSo, maDangKy,
                maKhoaHoc, maLop, ngayXep);
        if (siSoToiDa <= 0)
            throw new SQLException("Lớp chưa đặt sĩ số tối đa; hãy cập nhật lớp trước khi xếp học viên");
        int phong = gioiHanPhongDangDung(c, maCoSo, maLop, ngayXep);
        QuyTacSiSoLop.KetQua siSo = QuyTacSiSoLop.danhGia(siSoToiDa,
                phong == Integer.MAX_VALUE ? null : phong, 0);
        if (siSo.gioiHan() <= 0) throw new SQLException(siSo.trangThai()
                + "; hãy cập nhật lịch và phòng của lớp trước khi xếp học viên");
        int gioiHan = siSo.gioiHan();
        String sqlSiSo = """
                    SELECT COUNT(DISTINCT d.MaHocVien)
                    FROM dbo.XepLopTrungTam x WITH (UPDLOCK, HOLDLOCK)
                    INNER JOIN dbo.DangKyKhoaHoc d
                        ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
                       AND d.MaCoSo=x.MaCoSo
                    WHERE x.MaLop=? AND x.MaCoSo=?
                      AND x.TrangThai='HIEU_LUC'
                      AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                    """;
        try (PreparedStatement ps = c.prepareStatement(sqlSiSo)) {
            ps.setInt(1, maLop);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(ngayXep));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) >= gioiHan) {
                    throw new SQLException("Lớp đã đủ sĩ số theo giới hạn lớp/phòng học");
                }
            }
        }
        String sqlXep = """
                INSERT dbo.XepLopTrungTam
                    (MaDangKy, MaKhoaHoc, MaCoSo, MaLop, TuNgay, TrangThai)
                VALUES (?, ?, ?, ?, ?, 'HIEU_LUC')
                """;
        try (PreparedStatement ps = c.prepareStatement(sqlXep)) {
            ps.setInt(1, maDangKy);
            ps.setInt(2, maKhoaHoc);
            ps.setInt(3, maCoSo);
            ps.setInt(4, maLop);
            ps.setDate(5, Date.valueOf(ngayXep));
            if (ps.executeUpdate() != 1) throw new SQLException("Không xếp được học viên vào lớp");
        }
        String sqlTrangThai = """
                UPDATE dbo.DangKyKhoaHoc
                   SET TrangThai='DANG_HOC', NgayBatDau=?,
                       NgayKetThucDuKien=CASE
                           WHEN NgayKetThucDuKien IS NULL OR NgayKetThucDuKien<?
                           THEN ? ELSE NgayKetThucDuKien END
                WHERE MaDangKy=? AND MaHocVien=? AND MaCoSo=? AND TrangThai='CHO_XEP_LOP'
                """;
        try (PreparedStatement ps = c.prepareStatement(sqlTrangThai)) {
            ps.setDate(1, Date.valueOf(ngayXep));
            ps.setDate(2, Date.valueOf(ngayXep));
            if (ngayKetThucDuKien == null) ps.setNull(3, java.sql.Types.DATE);
            else ps.setDate(3, Date.valueOf(ngayKetThucDuKien));
            ps.setInt(4, maDangKy);
            ps.setInt(5, maHocVien);
            ps.setInt(6, maCoSo);
            if (ps.executeUpdate() != 1) throw new SQLException("Đăng ký không còn chờ xếp lớp");
        }
        if (maYeuCau != null) {
            String sqlYeuCau = """
                    UPDATE dbo.YeuCauXepLop
                       SET MaKhoaHoc=COALESCE(MaKhoaHoc, ?),
                           TrangThai='HOAN_TAT', NgayHoanTat=SYSUTCDATETIME()
                    WHERE MaYeuCau=? AND MaHocVien=? AND MaCoSo=?
                      AND (MaKhoaHoc=? OR MaKhoaHoc IS NULL)
                      AND TrangThai='CHO_XEP_LOP'
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlYeuCau)) {
                ps.setInt(1, maKhoaHoc);
                ps.setInt(2, maYeuCau);
                ps.setInt(3, maHocVien);
                ps.setInt(4, maCoSo);
                ps.setInt(5, maKhoaHoc);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Yêu cầu xếp lớp không còn ở trạng thái chờ");
                }
            }
        }
    }

    /** Tinh trong giao dich xep lop; khong doan ngay neu lich chua du so buoi. */
    static LocalDate duKienKetThuc(Connection c, int maCoSo, int maDangKy,
                                           int maKhoaHoc, int maLop, LocalDate ngayXep)
            throws SQLException {
        int tongSoBuoi;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT TongSoBuoiDangKy FROM dbo.DangKyKhoaHoc WITH (UPDLOCK, HOLDLOCK)
                WHERE MaDangKy=? AND MaCoSo=? AND MaKhoaHoc=?
                """)) {
            ps.setInt(1, maDangKy);
            ps.setInt(2, maCoSo);
            ps.setInt(3, maKhoaHoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không tìm thấy đăng ký khóa học để tính ngày kết thúc");
                tongSoBuoi = rs.getInt(1);
            }
        }
        List<DuKienKetThucLop.Lich> cacLich = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT MaLichHoc, ThuTrongTuan, GioBatDau, GioKetThuc,
                       TuNgay, DenNgay, TrangThai
                FROM dbo.LichHocLop WITH (HOLDLOCK)
                WHERE MaLop=? AND (DenNgay IS NULL OR DenNgay>=?)
                """)) {
            ps.setInt(1, maLop);
            ps.setDate(2, Date.valueOf(ngayXep));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date den = rs.getDate("DenNgay");
                    cacLich.add(new DuKienKetThucLop.Lich(
                            rs.getInt("MaLichHoc"), rs.getInt("ThuTrongTuan"),
                            rs.getTime("GioBatDau") == null ? null
                                    : rs.getTime("GioBatDau").toLocalTime(),
                            rs.getTime("GioKetThuc") == null ? null
                                    : rs.getTime("GioKetThuc").toLocalTime(),
                            rs.getDate("TuNgay").toLocalDate(),
                            den == null ? null : den.toLocalDate(),
                            rs.getString("TrangThai")));
                }
            }
        }
        List<DuKienKetThucLop.BuoiDieuChinh> dieuChinh = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT MaBuoiHoc, MaLichHoc, NgayHoc, SoCa, TrangThai
                FROM dbo.BuoiHoc WITH (HOLDLOCK)
                WHERE MaLop=? AND NgayHoc>=? AND TrangThai IN ('DU_KIEN','HUY')
                """)) {
            ps.setInt(1, maLop);
            ps.setDate(2, Date.valueOf(ngayXep));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Number maLich = (Number) rs.getObject("MaLichHoc");
                    dieuChinh.add(new DuKienKetThucLop.BuoiDieuChinh(
                            rs.getInt("MaBuoiHoc"), maLop,
                            maLich == null ? null : maLich.intValue(),
                            rs.getDate("NgayHoc").toLocalDate(),
                            rs.getInt("SoCa"), rs.getString("TrangThai")));
                }
            }
        }
        return DuKienKetThucLop.tinhTuLich(ngayXep, tongSoBuoi,
                cacLich, dieuChinh, Set.of()).orElse(null);
    }

    /** Lịch hai ca sát giờ nhau được chấp nhận; chỉ chặn phần thời gian giao nhau. */
    private static void kiemTraTrungLichHocVien(Connection c, int maCoSo,
                                                 int maHocVien, int maLopMoi,
                                                 LocalDate ngayXep) throws SQLException {
        String sqlMoi = """
                SELECT lh.ThuTrongTuan, lh.TuNgay, lh.DenNgay,
                       lh.GioBatDau, lh.GioKetThuc
                FROM dbo.LichHocLop lh WITH (HOLDLOCK)
                INNER JOIN dbo.LopTrungTam l ON l.MaLop=lh.MaLop
                WHERE lh.MaLop=? AND l.MaCoSo=? AND lh.TrangThai='HIEU_LUC'
                  AND lh.GioBatDau IS NOT NULL AND lh.GioKetThuc IS NOT NULL
                  AND (lh.DenNgay IS NULL OR lh.DenNgay>=?)
                """;
        List<Lich> lichMoi = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sqlMoi)) {
            ps.setInt(1, maLopMoi);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(ngayXep));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lichMoi.add(docLich(rs));
            }
        }
        if (lichMoi.isEmpty()) {
            throw new SQLException("Lớp chưa có lịch học hợp lệ để kiểm tra trùng lịch");
        }

        // Không bỏ qua lớp cũ chỉ vì lịch của lớp đó đang thiếu giờ hoặc đã
        // hết hiệu lực. Khi còn xếp lớp hiệu lực, thiếu lịch nghĩa là chưa đủ
        // dữ liệu để khẳng định học viên có thể học thêm lớp mới.
        String sqlThieuLichCu = """
                SELECT TOP (1) l.TenLop
                FROM dbo.XepLopTrungTam x WITH (HOLDLOCK)
                INNER JOIN dbo.DangKyKhoaHoc d
                  ON d.MaDangKy=x.MaDangKy AND d.MaCoSo=x.MaCoSo
                 AND d.MaKhoaHoc=x.MaKhoaHoc
                INNER JOIN dbo.LopTrungTam l
                  ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo
                WHERE d.MaHocVien=? AND d.MaCoSo=?
                  AND x.TrangThai='HIEU_LUC'
                  AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.LichHocLop lh WITH (HOLDLOCK)
                      WHERE lh.MaLop=x.MaLop AND lh.TrangThai='HIEU_LUC'
                        AND lh.GioBatDau IS NOT NULL AND lh.GioKetThuc IS NOT NULL
                        AND lh.TuNgay<=ISNULL(x.DenNgay,CONVERT(date,'99991231'))
                        AND (lh.DenNgay IS NULL OR lh.DenNgay>=
                             CASE WHEN x.TuNgay>? THEN x.TuNgay ELSE ? END))
                """;
        try (PreparedStatement ps = c.prepareStatement(sqlThieuLichCu)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(ngayXep));
            ps.setDate(4, Date.valueOf(ngayXep));
            ps.setDate(5, Date.valueOf(ngayXep));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException("Lớp '" + rs.getString(1)
                        + "' của học viên thiếu lịch hợp lệ; chưa thể kiểm tra trùng lịch.");
            }
        }

        String sqlCu = """
                SELECT x.TuNgay AS TuNgayXep, x.DenNgay AS DenNgayXep,
                       l.TenLop, lh.ThuTrongTuan, lh.TuNgay, lh.DenNgay,
                       lh.GioBatDau, lh.GioKetThuc
                FROM dbo.XepLopTrungTam x WITH (HOLDLOCK)
                INNER JOIN dbo.DangKyKhoaHoc d
                    ON d.MaDangKy=x.MaDangKy AND d.MaCoSo=x.MaCoSo
                INNER JOIN dbo.LopTrungTam l
                    ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo
                INNER JOIN dbo.LichHocLop lh WITH (HOLDLOCK)
                    ON lh.MaLop=l.MaLop
                WHERE d.MaHocVien=? AND d.MaCoSo=?
                  AND x.TrangThai='HIEU_LUC'
                  AND (x.DenNgay IS NULL OR x.DenNgay>=?)
                  AND lh.TrangThai='HIEU_LUC'
                  AND lh.GioBatDau IS NOT NULL AND lh.GioKetThuc IS NOT NULL
                  AND (lh.DenNgay IS NULL OR lh.DenNgay>=?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sqlCu)) {
            ps.setInt(1, maHocVien);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(ngayXep));
            ps.setDate(4, Date.valueOf(ngayXep));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Lich cu = docLich(rs);
                    LocalDate tuXepCu = rs.getDate("TuNgayXep").toLocalDate();
                    LocalDate denXepCu = ngayHoacNull(rs, "DenNgayXep");
                    LocalDate tuCu = max(tuXepCu, cu.tuNgay());
                    LocalDate denCu = min(denXepCu, cu.denNgay());
                    if (denCu != null && denCu.isBefore(tuCu)) continue;
                    for (Lich moi : lichMoi) {
                        LocalDate tuMoi = max(ngayXep, moi.tuNgay());
                        if (QuyTacTrungLich.trungLichDinhKy(
                                moi.thu(), tuMoi, moi.denNgay(),
                                moi.batDau(), moi.ketThuc(),
                                cu.thu(), tuCu, denCu,
                                cu.batDau(), cu.ketThuc())) {
                            throw new SQLException("Học viên đã có lịch trùng với lớp '"
                                    + rs.getString("TenLop") + "'");
                        }
                    }
                }
            }
        }
    }

    /** Lấy giới hạn nhỏ nhất của các phòng có lịch còn hiệu lực của lớp. */
    private static int gioiHanPhongDangDung(Connection c, int maCoSo, int maLop,
                                           LocalDate ngayXep) throws SQLException {
        LocalDate tuNgay = ngayXep.isAfter(LocalDate.now()) ? ngayXep : LocalDate.now();
        String sqlThieuPhong = """
                SELECT CASE WHEN EXISTS (
                    SELECT 1 FROM dbo.LichHocLop lh WITH (HOLDLOCK)
                    WHERE lh.MaLop=? AND lh.TrangThai='HIEU_LUC'
                      AND lh.MaPhong IS NULL
                      AND (lh.DenNgay IS NULL OR lh.DenNgay>=?))
                  OR EXISTS (
                    SELECT 1 FROM dbo.BuoiHoc b WITH (HOLDLOCK)
                    WHERE b.MaLop=? AND b.TrangThai='DU_KIEN'
                      AND b.MaPhong IS NULL AND b.NgayHoc>=?)
                  THEN 1 ELSE 0 END
                """;
        try (PreparedStatement ps = c.prepareStatement(sqlThieuPhong)) {
            ps.setInt(1, maLop);
            ps.setDate(2, Date.valueOf(tuNgay));
            ps.setInt(3, maLop);
            ps.setDate(4, Date.valueOf(tuNgay));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 1)
                    throw new SQLException("Lịch lớp còn buổi chưa gán phòng học");
            }
        }
        String sql = """
                SELECT DISTINCT p.MaPhong, p.SucChua, p.TrangThai
                FROM dbo.PhongHoc p WITH (HOLDLOCK)
                WHERE p.MaCoSo=? AND (
                    EXISTS (SELECT 1 FROM dbo.LichHocLop lh WITH (HOLDLOCK)
                            WHERE lh.MaLop=? AND lh.MaPhong=p.MaPhong
                              AND lh.TrangThai='HIEU_LUC'
                              AND (lh.DenNgay IS NULL OR lh.DenNgay>=?))
                    OR EXISTS (SELECT 1 FROM dbo.BuoiHoc b WITH (HOLDLOCK)
                               WHERE b.MaLop=? AND b.MaPhong=p.MaPhong
                                 AND b.TrangThai='DU_KIEN' AND b.NgayHoc>=?)
                )
                """;
        int gioiHan = Integer.MAX_VALUE;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maCoSo);
            ps.setInt(2, maLop);
            ps.setDate(3, Date.valueOf(tuNgay));
            ps.setInt(4, maLop);
            ps.setDate(5, Date.valueOf(tuNgay));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!"HOAT_DONG".equals(rs.getString("TrangThai"))) {
                        throw new SQLException("Lịch lớp dùng phòng đang bảo trì hoặc ngừng sử dụng");
                    }
                    gioiHan = Math.min(gioiHan, rs.getInt("SucChua"));
                }
            }
        }
        return gioiHan;
    }

    private static Lich docLich(ResultSet rs) throws SQLException {
        return new Lich(rs.getInt("ThuTrongTuan"),
                rs.getDate("TuNgay").toLocalDate(), ngayHoacNull(rs, "DenNgay"),
                rs.getTime("GioBatDau").toLocalTime(),
                rs.getTime("GioKetThuc").toLocalTime());
    }

    private static LocalDate ngayHoacNull(ResultSet rs, String tenCot)
            throws SQLException {
        Date d = rs.getDate(tenCot);
        return d == null ? null : d.toLocalDate();
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static boolean daCoXepLopCuaHoSo(Connection c, int maCoSo,
                                             int maKhach) throws SQLException {
        String sql = """
                SELECT TOP (1) 1
                FROM dbo.YeuCauXepLop y
                INNER JOIN dbo.DangKyKhoaHoc d
                    ON d.MaYeuCau=y.MaYeuCau AND d.MaCoSo=y.MaCoSo
                INNER JOIN dbo.XepLopTrungTam x
                    ON x.MaDangKy=d.MaDangKy AND x.MaCoSo=d.MaCoSo
                WHERE y.MaKhach=? AND y.MaCoSo=?
                  AND ((d.TrangThai='DANG_HOC'
                        AND x.TrangThai='HIEU_LUC'
                        AND (x.DenNgay IS NULL OR x.DenNgay>=?))
                    OR (d.TrangThai='HOAN_THANH'
                        AND x.TrangThai IN ('HIEU_LUC','KET_THUC')))
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maKhach);
            ps.setInt(2, maCoSo);
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void datTrangThaiHoSo(Connection c, int maCoSo,
                                         int maKhach, String trangThai)
            throws SQLException {
        String sql = "UPDATE dbo.KhachTiemNang SET TrangThai=? WHERE MaKhach=? AND MaCoSo=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trangThai);
            ps.setInt(2, maKhach);
            ps.setInt(3, maCoSo);
            if (ps.executeUpdate() != 1) throw new SQLException("Không cập nhật được trạng thái hồ sơ");
        }
    }

    private static int maVuaTao(PreparedStatement ps, String ten) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Không lấy được mã " + ten + " vừa tạo");
    }

    private static Integer soNguyenHoacNull(ResultSet rs, String tenCot)
            throws SQLException {
        Object giaTri = rs.getObject(tenCot);
        return giaTri == null ? null : ((Number) giaTri).intValue();
    }

    private static void rollback(Connection c, Exception ex) {
        try {
            c.rollback();
        } catch (SQLException loiRollback) {
            ex.addSuppressed(loiRollback);
        }
    }
}
