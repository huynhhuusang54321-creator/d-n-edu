package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.vinabacus.repository.LichHocLopRepository;
import vn.vinabacus.service.LichHocLopService;

/** Ngày hiệu lực sửa lịch cũ và giao dịch tách lịch trên DB TEST riêng. */
class LichHocSuaTuNgayTest {
    private static final String TEST_DB = "VINABACUS_GD6_TEST_20261123";
    private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 24);

    @Test void lichCuMoFormDeXuatHomNay() {
        LocalDate tuCu = LocalDate.of(2026, 11, 18);
        assertEquals(HOM_NAY, LichHocLopService.ngaySuaMacDinh(tuCu, HOM_NAY));
        LichHocLopService.kiemTraNgaySua(tuCu, null, HOM_NAY, HOM_NAY);
        assertEquals(LocalDate.of(2026, 12, 1), LichHocLopService.ngaySuaMacDinh(
                LocalDate.of(2026, 12, 1), HOM_NAY));
    }

    @Test void ngayQuaKhuVaNgoaiThoiHanBaoLyDoCuThe() {
        LocalDate tuCu = LocalDate.of(2026, 11, 18);
        var quaKhu = assertThrows(IllegalArgumentException.class, () ->
                LichHocLopService.kiemTraNgaySua(tuCu, null, tuCu, HOM_NAY));
        assertTrue(quaKhu.getMessage().contains("2026-11-24"));
        var hetHan = assertThrows(IllegalArgumentException.class, () ->
                LichHocLopService.kiemTraNgaySua(tuCu, LocalDate.of(2026, 11, 23),
                        HOM_NAY, HOM_NAY));
        assertTrue(hetHan.getMessage().contains("đã kết thúc"));
    }

    @Test
    @EnabledIfSystemProperty(named = "vinabacus.sualich.sqltest", matches = "true")
    void tachLichCuGiuNguyenLichSuVaRollbackTrenTest() throws Exception {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("db.local.properties"))) { p.load(in); }
        String urlGoc = p.getProperty("db.url", "");
        if (!urlGoc.matches("(?is).*databaseName=VINABACUS(?:;.*)?"))
            throw new IllegalStateException("Cấu hình gốc không trỏ đúng VINABACUS");
        String url = urlGoc.replaceFirst("(?i)(databaseName=)VINABACUS(?=;|$)", "$1" + TEST_DB);
        LocalDate ngayHieuLuc = LocalDate.now();
        int maMoi;
        try (Connection real = DriverManager.getConnection(url,
                p.getProperty("db.user"), p.getProperty("db.password"))) {
            assertEquals(TEST_DB, real.getCatalog());
            real.setAutoCommit(false);
            Connection khongCommit = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("commit".equals(method.getName()) || "close".equals(method.getName()))
                            return null;
                        try { return method.invoke(real, args); }
                        catch (InvocationTargetException ex) { throw ex.getCause(); }
                    });
            try {
                LichHocLopRepository repo = new LichHocLopRepository(() -> khongCommit);
                maMoi = repo.suaTuNgay(2, 8, 3, 2, LocalTime.of(10, 0),
                        LocalTime.of(11, 30), ngayHieuLuc, null, "HIEU_LUC");
                try (Statement s = real.createStatement(); ResultSet rs = s.executeQuery(
                        "SELECT DenNgay,GioBatDau FROM dbo.LichHocLop WHERE MaLichHoc=8")) {
                    assertTrue(rs.next());
                    assertEquals(ngayHieuLuc.minusDays(1), rs.getDate(1).toLocalDate());
                    assertEquals(LocalTime.of(8, 0), rs.getTime(2).toLocalTime());
                }
                try (Statement s = real.createStatement(); ResultSet rs = s.executeQuery(
                        "SELECT TuNgay,GioBatDau,MaPhong FROM dbo.LichHocLop WHERE MaLichHoc=" + maMoi)) {
                    assertTrue(rs.next());
                    assertEquals(ngayHieuLuc, rs.getDate(1).toLocalDate());
                    assertEquals(LocalTime.of(10, 0), rs.getTime(2).toLocalTime());
                    assertEquals(3, rs.getInt(3));
                }
            } finally { real.rollback(); }
        }
        try (Connection c = DriverManager.getConnection(url,
                p.getProperty("db.user"), p.getProperty("db.password"))) {
            assertEquals(TEST_DB, c.getCatalog());
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT DenNgay,GioBatDau FROM dbo.LichHocLop WHERE MaLichHoc=8")) {
                assertTrue(rs.next());
                assertNull(rs.getDate(1));
                assertEquals(LocalTime.of(8, 0), rs.getTime(2).toLocalTime());
            }
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) FROM dbo.LichHocLop WHERE MaLichHoc=" + maMoi)) {
                assertTrue(rs.next());assertEquals(0, rs.getInt(1));
            }
        }
    }
}
