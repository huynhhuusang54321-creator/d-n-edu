package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.vinabacus.repository.LichHocLopRepository;
import vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;

/** Chi chay khi bat co GD5; moi ket noi tu choi database khac TEST co dinh. */
class GiaiDoan5SqlTest {
    private static final String TEST_DB = "VINABACUS_GD5_TEST_20261123";
    private static final int CO_SO = 2;
    private static final LocalTime BAY = LocalTime.of(7, 0);
    private static final LocalTime TAM_RUOI = LocalTime.of(8, 30);
    private static final LocalTime MUOI = LocalTime.of(10, 0);
    private static final LocalTime MUOI_MOT_RUOI = LocalTime.of(11, 30);

    private record Fixture(int a, int b, int c, int d, int e,
                           int dangKyA, int dangKyC, int xepA, int xepC) { }

    @Test
    @EnabledIfSystemProperty(named = "vinabacus.gd5.sqltest", matches = "true")
    void ghiLichChiTrenTestVaRollbackKhiLoi() throws Exception {
        LichHocLopRepository.KetNoi ketNoi = ketNoiTest();
        // Xac minh database truoc khi fixture duoc INSERT.
        try (Connection con = ketNoi.mo()) { assertEquals(TEST_DB, tenDb(con)); }
        Fixture f = taoFixture(ketNoi);
        LichHocLopRepository repo = new LichHocLopRepository(ketNoi);
        LocalDate tuNgay = LocalDate.of(2035, 1, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        try {
            int lichA = repo.them(CO_SO, f.a(), 1, 6, BAY, TAM_RUOI,
                    tuNgay, null, "HIEU_LUC");
            LocalDate duKienCu = ngayDuKien(ketNoi, f.dangKyA());
            assertNotNull(duKienCu);

            SQLException trungGv = assertThrows(SQLException.class,
                    () -> repo.them(CO_SO, f.b(), 2, 6, BAY, TAM_RUOI,
                            tuNgay, null, "HIEU_LUC"));
            assertTrue(trungGv.getMessage().contains("Giáo viên"));
            SQLException trungPhong = assertThrows(SQLException.class,
                    () -> repo.them(CO_SO, f.c(), 1, 6, BAY, TAM_RUOI,
                            tuNgay, null, "HIEU_LUC"));
            assertTrue(trungPhong.getMessage().contains("Phòng"));
            SQLException trungHv = assertThrows(SQLException.class,
                    () -> repo.them(CO_SO, f.c(), 2, 6, BAY, TAM_RUOI,
                            tuNgay, null, "HIEU_LUC"));
            assertTrue(trungHv.getMessage().contains("học viên"));

            // 08:30 bat dau dung luc ca 1 ket thuc: khong trung.
            repo.them(CO_SO, f.c(), 2, 6, TAM_RUOI, MUOI,
                    tuNgay, null, "HIEU_LUC");
            assertNotNull(ngayDuKien(ketNoi, f.dangKyC()));

            repo.sua(CO_SO, lichA, 1, 5, BAY, TAM_RUOI,
                    tuNgay, null, "HIEU_LUC");
            LocalDate duKienMoi = ngayDuKien(ketNoi, f.dangKyA());
            assertNotEquals(duKienCu, duKienMoi);

            int buoiHuy = repo.huyBuoi(CO_SO, lichA, tuNgay.plusDays(3),
                    "GD5 SQL TEST");
            assertTrue(buoiHuy > 0);
            assertThrows(SQLException.class, () -> repo.huyBuoi(CO_SO,
                    lichA, tuNgay.plusDays(3), "Khong duoc huy trung"));
            LocalDate duKienSauHuy = ngayDuKien(ketNoi, f.dangKyA());
            assertTrue(duKienSauHuy.isAfter(duKienMoi));
            int buoiBu = repo.themBuoiBu(CO_SO, f.a(), 1,
                    tuNgay.plusDays(8), 4, "GD5 SQL TEST");
            assertTrue(buoiBu > 0);
            var buoiTrenLich = repo.layBuoiDieuChinh(CO_SO);
            assertTrue(buoiTrenLich.stream().anyMatch(x ->
                    x.maBuoiHoc() == buoiHuy && "HUY".equals(x.trangThai())));
            assertTrue(buoiTrenLich.stream().anyMatch(x ->
                    x.maBuoiHoc() == buoiBu && "DU_KIEN".equals(x.trangThai())
                    && Integer.valueOf(1).equals(x.maPhong()) && x.tenPhong() != null));
            assertTrue(ngayDuKien(ketNoi, f.dangKyA()).isBefore(duKienSauHuy));
            LocalDate duKienSauBu = ngayDuKien(ketNoi, f.dangKyA());

            SQLException trungTrongDanhSach = assertThrows(SQLException.class,
                    () -> repo.themNhieuTheoPhong(CO_SO, f.b(), List.of(
                            new BuoiCoPhong(new BuoiTrongTuan(3, BAY, TAM_RUOI), 2),
                            new BuoiCoPhong(new BuoiTrongTuan(5, BAY, TAM_RUOI), 2)),
                            tuNgay, null, "HIEU_LUC"));
            assertTrue(trungTrongDanhSach.getMessage().contains("Giáo viên"));
            assertEquals(0, soLich(ketNoi, f.b()));

            // Gia lap loi khi UPDATE ngay du kien; INSERT lich phai rollback.
            LichHocLopRepository repoLoi = new LichHocLopRepository(
                    () -> chanCapNhatDuKien(ketNoi.mo()));
            int truoc = soLich(ketNoi, f.a());
            assertThrows(SQLException.class, () -> repoLoi.them(CO_SO, f.a(), 1,
                    6, LocalTime.of(11, 30), LocalTime.of(13, 0),
                    tuNgay, null, "HIEU_LUC"));
            assertEquals(truoc, soLich(ketNoi, f.a()));
            assertEquals(duKienSauBu, ngayDuKien(ketNoi, f.dangKyA()));
            int soBuoiTruoc = soBuoi(ketNoi, f.a());
            assertThrows(SQLException.class, () -> repoLoi.huyBuoi(CO_SO,
                    lichA, tuNgay.plusDays(10), "GD5 SQL TEST rollback"));
            assertEquals(soBuoiTruoc, soBuoi(ketNoi, f.a()));
            assertEquals(duKienSauBu, ngayDuKien(ketNoi, f.dangKyA()));
            assertThrows(SQLException.class, () -> repoLoi.themBuoiBu(CO_SO,
                    f.a(), 1, tuNgay.plusDays(15), 4, "GD5 SQL TEST rollback"));
            assertEquals(soBuoiTruoc, soBuoi(ketNoi, f.a()));
            assertEquals(duKienSauBu, ngayDuKien(ketNoi, f.dangKyA()));

            int lichMoi = repo.suaTuNgay(CO_SO, lichA, 1, 3, BAY, TAM_RUOI,
                    tuNgay.plusWeeks(2), null, "HIEU_LUC");
            assertTrue(lichMoi > lichA);
            assertEquals(tuNgay.plusWeeks(2).minusDays(1), denNgay(ketNoi, lichA));
            assertNotNull(ngayDuKien(ketNoi, f.dangKyA()));

            kiemTraDongThoi(ketNoi, f.d(), f.e(), tuNgay);
        } finally {
            donFixture(ketNoi, f);
        }
    }

    private static LichHocLopRepository.KetNoi ketNoiTest() throws Exception {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("db.local.properties"))) {
            p.load(in);
        }
        String urlGoc = p.getProperty("db.url", "");
        if (!urlGoc.matches("(?is).*databaseName=VINABACUS(?:;.*)?"))
            throw new IllegalStateException("Cau hinh goc khong tro dung VINABACUS");
        String urlTest = urlGoc.replaceFirst(
                "(?i)(databaseName=)VINABACUS(?=;|$)", "$1" + TEST_DB);
        return () -> {
            Connection con = DriverManager.getConnection(urlTest,
                    p.getProperty("db.user", ""), p.getProperty("db.password", ""));
            try {
                if (!TEST_DB.equals(tenDb(con)))
                    throw new SQLException("Tu choi ghi: ket noi khong phai CSDL TEST");
                return con;
            } catch (SQLException ex) {
                con.close();
                throw ex;
            }
        };
    }

    private static String tenDb(Connection con) throws SQLException {
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery("SELECT DB_NAME()")) {
            if (!rs.next()) throw new SQLException("Khong doc duoc ten CSDL");
            return rs.getString(1);
        }
    }

    private static Fixture taoFixture(LichHocLopRepository.KetNoi ketNoi)
            throws SQLException {
        String tag = "[GD5_SQL_TEST_" + UUID.randomUUID().toString().substring(0, 8) + "]";
        LocalDate ngay = LocalDate.of(2035, 1, 1);
        try (Connection con = ketNoi.mo()) {
            con.setAutoCommit(false);
            try {
                int a = themLop(con, 1, 2, tag + " A", ngay);
                int b = themLop(con, 1, 2, tag + " B", ngay);
                int c = themLop(con, 3, 3, tag + " C", ngay);
                int d = themLop(con, 1, 2, tag + " D", ngay);
                int e = themLop(con, 1, 2, tag + " E", ngay);
                int dkA = themDangKy(con, 1, 1, ngay, tag);
                int dkC = themDangKy(con, 1, 3, ngay, tag);
                int xA = themXep(con, dkA, 1, a, ngay);
                int xC = themXep(con, dkC, 3, c, ngay);
                con.commit();
                return new Fixture(a, b, c, d, e, dkA, dkC, xA, xC);
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    private static int themLop(Connection con, int khoa, int giaoVien,
                               String ten, LocalDate ngay) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT dbo.LopTrungTam
                    (MaCoSo, MaKhoaHoc, TenLop, MaGiaoVien,
                     NgayKhaiGiang, TrangThai, SiSoToiDa)
                VALUES (?, ?, ?, ?, ?, 'DANG_MO', 10)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, CO_SO);
            ps.setInt(2, khoa);
            ps.setString(3, ten);
            ps.setInt(4, giaoVien);
            ps.setDate(5, Date.valueOf(ngay));
            return themVaLayMa(ps);
        }
    }

    private static int themDangKy(Connection con, int hocVien, int khoa,
                                  LocalDate ngay, String tag) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT dbo.DangKyKhoaHoc
                    (MaHocVien, MaCoSo, MaKhoaHoc, NgayDangKy, NgayBatDau,
                     TongSoBuoiDangKy, HocPhiThoaThuan, SoTienGiam,
                     HocPhiSauGiam, TrangThai, GhiChu)
                VALUES (?, ?, ?, ?, ?, 4, 0, 0, 0, 'DANG_HOC', ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, hocVien);
            ps.setInt(2, CO_SO);
            ps.setInt(3, khoa);
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            ps.setDate(5, Date.valueOf(ngay));
            ps.setString(6, tag);
            return themVaLayMa(ps);
        }
    }

    private static int themXep(Connection con, int dangKy, int khoa,
                               int lop, LocalDate ngay) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT dbo.XepLopTrungTam
                    (MaDangKy, MaKhoaHoc, MaCoSo, MaLop, TuNgay, TrangThai)
                VALUES (?, ?, ?, ?, ?, 'HIEU_LUC')
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, dangKy);
            ps.setInt(2, khoa);
            ps.setInt(3, CO_SO);
            ps.setInt(4, lop);
            ps.setDate(5, Date.valueOf(ngay));
            return themVaLayMa(ps);
        }
    }

    private static int themVaLayMa(PreparedStatement ps) throws SQLException {
        if (ps.executeUpdate() != 1) throw new SQLException("Khong tao duoc fixture TEST");
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("Fixture TEST thieu ma sinh tu dong");
            return keys.getInt(1);
        }
    }

    private static int soLich(LichHocLopRepository.KetNoi ketNoi, int lop)
            throws SQLException {
        try (Connection con = ketNoi.mo();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM dbo.LichHocLop WHERE MaLop=?")) {
            ps.setInt(1, lop);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static int soBuoi(LichHocLopRepository.KetNoi ketNoi, int lop)
            throws SQLException {
        try (Connection con = ketNoi.mo();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM dbo.BuoiHoc WHERE MaLop=?")) {
            ps.setInt(1, lop);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static LocalDate ngayDuKien(LichHocLopRepository.KetNoi ketNoi,
                                        int dangKy) throws SQLException {
        try (Connection con = ketNoi.mo();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT NgayKetThucDuKien FROM dbo.DangKyKhoaHoc WHERE MaDangKy=?")) {
            ps.setInt(1, dangKy);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Thieu dang ky TEST");
                Date ngay = rs.getDate(1);
                return ngay == null ? null : ngay.toLocalDate();
            }
        }
    }

    private static LocalDate denNgay(LichHocLopRepository.KetNoi ketNoi,
                                     int maLich) throws SQLException {
        try (Connection con = ketNoi.mo();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT DenNgay FROM dbo.LichHocLop WHERE MaLichHoc=?")) {
            ps.setInt(1, maLich);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Thieu lich TEST");
                Date ngay = rs.getDate(1);
                return ngay == null ? null : ngay.toLocalDate();
            }
        }
    }

    private static Connection chanCapNhatDuKien(Connection real) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if ("prepareStatement".equals(method.getName())
                            && args != null && args.length > 0 && args[0] instanceof String sql
                            && sql.contains("SET NgayKetThucDuKien="))
                        throw new SQLException("Gia lap loi cap nhat ngay du kien");
                    try { return method.invoke(real, args); }
                    catch (InvocationTargetException ex) { throw ex.getCause(); }
                });
    }

    private static void kiemTraDongThoi(LichHocLopRepository.KetNoi ketNoi,
                                        int lopD, int lopE, LocalDate ngay) throws Exception {
        CountDownLatch batDau = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> thuD = () -> thuThemDongThoi(ketNoi, batDau, lopD, 2, ngay);
            Callable<Boolean> thuE = () -> thuThemDongThoi(ketNoi, batDau, lopE, 3, ngay);
            Future<Boolean> d = pool.submit(thuD);
            Future<Boolean> e = pool.submit(thuE);
            batDau.countDown();
            int thanhCong = (d.get(30, TimeUnit.SECONDS) ? 1 : 0)
                    + (e.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, thanhCong, "Hai nhan vien khong duoc cung luu lich trung GV");
            assertEquals(1, soLich(ketNoi, lopD) + soLich(ketNoi, lopE));
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean thuThemDongThoi(LichHocLopRepository.KetNoi ketNoi,
                                            CountDownLatch batDau, int lop,
                                            int phong, LocalDate ngay) throws Exception {
        batDau.await();
        try {
            new LichHocLopRepository(ketNoi).them(CO_SO, lop, phong,
                    6, MUOI, MUOI_MOT_RUOI, ngay, null, "HIEU_LUC");
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    private static void donFixture(LichHocLopRepository.KetNoi ketNoi,
                                   Fixture f) throws SQLException {
        try (Connection con = ketNoi.mo()) {
            con.setAutoCommit(false);
            try {
                xoa(con, "DELETE dbo.BuoiHoc WHERE MaLop IN (?,?,?,?,?)",
                        f.a(), f.b(), f.c(), f.d(), f.e());
                xoa(con, "DELETE dbo.XepLopTrungTam WHERE MaXepLop IN (?,?)",
                        f.xepA(), f.xepC());
                xoa(con, "DELETE dbo.DangKyKhoaHoc WHERE MaDangKy IN (?,?)",
                        f.dangKyA(), f.dangKyC());
                xoa(con, "DELETE dbo.LichHocLop WHERE MaLop IN (?,?,?,?,?)",
                        f.a(), f.b(), f.c(), f.d(), f.e());
                xoa(con, "DELETE dbo.LopTrungTam WHERE MaLop IN (?,?,?,?,?)",
                        f.a(), f.b(), f.c(), f.d(), f.e());
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    private static void xoa(Connection con, String sql, int... ids) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < ids.length; i++) ps.setInt(i + 1, ids[i]);
            ps.executeUpdate();
        }
    }
}
