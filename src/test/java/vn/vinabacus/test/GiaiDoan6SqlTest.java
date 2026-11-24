package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.vinabacus.repository.DiemDanhTrungTamRepository;
import vn.vinabacus.repository.HocBuTrungTamRepository;
import vn.vinabacus.repository.ChiTietHocVienTrungTamRepository;
import vn.vinabacus.repository.LopTrungTamRepository;
import vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;

/** Chi ghi vao TEST rieng sau khi xac nhan DB_NAME() moi lan mo ket noi. */
class GiaiDoan6SqlTest {
    private static final String TEST_DB="VINABACUS_GD6_TEST_20261123";
    private static final int CO_SO=2;
    private record Fixture(int nguon,int dich,int tongCu,String trangThaiCu,
                           Date ketThucCu) { }

    @Test
    @EnabledIfSystemProperty(named="vinabacus.new.sqltest",matches="true")
    void taoLopVaLichThanhCongTrongCungTransactionTest() throws Exception {
        var ketNoi=ketNoiTest();
        try(Connection real=ketNoi.mo()) {
            assertEquals(TEST_DB,tenDb(real));
            int phong;
            try(Statement s=real.createStatement();ResultSet rs=s.executeQuery(
                    "SELECT TOP (1) MaPhong FROM dbo.PhongHoc WHERE MaCoSo=2 AND TrangThai='HOAT_DONG' AND SucChua>=10 ORDER BY MaPhong")) {
                assertTrue(rs.next());phong=rs.getInt(1);
            }
            real.setAutoCommit(false);
            Connection giuTransaction=(Connection)Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),new Class<?>[]{Connection.class},
                    (proxy,method,args)->{
                        if("commit".equals(method.getName()) || "close".equals(method.getName()))
                            return null;
                        try{return method.invoke(real,args);}
                        catch(InvocationTargetException ex){throw ex.getCause();}
                    });
            try {
                LopTrungTamRepository repo=new LopTrungTamRepository(()->giuTransaction);
                LocalDate tuNgay=LocalDate.of(2041,1,2);
                var ca=new BuoiCoPhong(new BuoiTrongTuan(3,
                        java.time.LocalTime.of(7,0),java.time.LocalTime.of(8,30)),phong);
                String ten="[SQL TEST] ATOMIC-"+java.util.UUID.randomUUID();
                int maLop=repo.themVoiLich(CO_SO,1,ten,2,tuNgay,
                        "DANG_MO",10,List.of(ca),tuNgay);
                assertTrue(maLop>0);
                var caBoSung=new BuoiCoPhong(new BuoiTrongTuan(3,
                        java.time.LocalTime.of(8,30),java.time.LocalTime.of(10,0)),phong);
                repo.suaVoiLich(CO_SO,maLop,1,ten+" sửa",2,tuNgay,
                        "DANG_MO",10,List.of(caBoSung),tuNgay);
                try(PreparedStatement ps=real.prepareStatement("""
                        SELECT COUNT(*) FROM dbo.LichHocLop
                        WHERE MaLop=? AND MaPhong=? AND ThuTrongTuan=3
                        """)) {
                    ps.setInt(1,maLop);ps.setInt(2,phong);
                    try(ResultSet rs=ps.executeQuery()){rs.next();assertEquals(2,rs.getInt(1));}
                }
                try(PreparedStatement ps=real.prepareStatement(
                        "SELECT TenLop FROM dbo.LopTrungTam WHERE MaLop=?")) {
                    ps.setInt(1,maLop);
                    try(ResultSet rs=ps.executeQuery()){assertTrue(rs.next());assertEquals(ten+" sửa",rs.getString(1));}
                }
            } finally {real.rollback();}
        }
    }

    @Test
    @EnabledIfSystemProperty(named="vinabacus.new.sqltest",matches="true")
    void taoLopVaLichLoiThiKhongConLop() throws Exception {
        var ketNoi=ketNoiTest();
        try(Connection c=ketNoi.mo()) { assertEquals(TEST_DB,tenDb(c)); }
        String ten="[SQL TEST] ATOMIC-"+java.util.UUID.randomUUID();
        int phong;
        try(Connection c=ketNoi.mo();Statement s=c.createStatement();
                ResultSet rs=s.executeQuery("SELECT TOP (1) MaPhong FROM dbo.PhongHoc WHERE MaCoSo=2 AND TrangThai='HOAT_DONG' AND SucChua>=10 ORDER BY MaPhong")) {
            assertTrue(rs.next());phong=rs.getInt(1);
        }
        LocalDate tuNgay=LocalDate.of(2040,1,2);
        var ca=new BuoiCoPhong(new BuoiTrongTuan(3,
                java.time.LocalTime.of(7,0),java.time.LocalTime.of(8,30)),phong);
        LopTrungTamRepository repo=new LopTrungTamRepository(ketNoi::mo);
        SQLException trungLich=assertThrows(SQLException.class,
                ()->repo.themVoiLich(CO_SO,1,ten,2,tuNgay,
                        "DANG_MO",10,List.of(ca,ca),tuNgay));
        assertTrue(trungLich.getMessage().contains("đã có lịch giao nhau"),
                trungLich.getMessage());
        try(Connection c=ketNoi.mo();PreparedStatement ps=c.prepareStatement(
                "SELECT COUNT(*) FROM dbo.LopTrungTam WHERE MaCoSo=2 AND TenLop=?")) {
            ps.setString(1,ten);
            try(ResultSet rs=ps.executeQuery()) { rs.next();assertEquals(0,rs.getInt(1)); }
        }
    }

    @Test
    @EnabledIfSystemProperty(named="vinabacus.gd6.sqltest",matches="true")
    void luuNhieuDiemDanhRollbackToanBo() throws Exception {
        var ketNoi=ketNoiTest();
        try(Connection c=ketNoi.mo()) { assertEquals(TEST_DB,tenDb(c)); }
        Fixture f=taoFixture(ketNoi);
        try {
            DiemDanhTrungTamRepository repo=new DiemDanhTrungTamRepository(ketNoi);
            var hopLe=new DiemDanhTrungTamRepository.HocVienBuoi(2,6,null,
                    "Kiểm thử","","",false,"CHINH_THUC",null,null);
            var khongHopLe=new DiemDanhTrungTamRepository.HocVienBuoi(999999,999999,
                    null,"Không tồn tại","","",false,"CHINH_THUC",null,null);
            assertThrows(SQLException.class,()->repo.luuNhieu(CO_SO,f.nguon(),List.of(
                    new DiemDanhTrungTamRepository.ThayDoi(hopLe,"VANG_CO_PHEP","test",null),
                    new DiemDanhTrungTamRepository.ThayDoi(khongHopLe,"CO_MAT","test",null)),1));
            try(Connection c=ketNoi.mo();PreparedStatement ps=c.prepareStatement("""
                    SELECT COUNT(*) FROM dbo.SapXepBuoiHoc
                    WHERE MaBuoiHoc=? AND MaDangKy=2 AND MaHocVien=6
                    """)) {
                ps.setInt(1,f.nguon());
                try(ResultSet rs=ps.executeQuery()) { rs.next();assertEquals(0,rs.getInt(1)); }
            }
        } finally { donFixture(ketNoi,f); }
    }

    @Test
    @EnabledIfSystemProperty(named="vinabacus.gd6.sqltest",matches="true")
    void diemDanhHocBuRollbackVaDongThoi() throws Exception {
        var ketNoi=ketNoiTest();
        try (Connection c=ketNoi.mo()) {
            assertEquals(TEST_DB,tenDb(c));
            try (Statement s=c.createStatement(); ResultSet rs=s.executeQuery("""
                    SELECT COL_LENGTH('dbo.BuoiHoc','NoiDungHoc'),
                           OBJECT_ID('dbo.LichSuSuaDiemDanhTrungTam','U')
                    """)) {
                rs.next(); assertTrue(rs.getObject(1)!=null && rs.getObject(2)!=null);
            }
        }
        DiemDanhTrungTamRepository dd=new DiemDanhTrungTamRepository(ketNoi);
        HocBuTrungTamRepository hb=new HocBuTrungTamRepository(ketNoi);
        ChiTietHocVienTrungTamRepository chiTiet=
                new ChiTietHocVienTrungTamRepository(ketNoi::mo);
        var haiKhoa=chiTiet.tai(CO_SO,5).khoaHoc();
        assertEquals(2,haiKhoa.size());
        assertEquals(2,haiKhoa.stream().filter(k->k.dangKy().maDangKy()==1)
                .findFirst().orElseThrow().soBuoiHoanThanh());
        assertEquals(null,haiKhoa.stream().filter(k->k.dangKy().maDangKy()==7)
                .findFirst().orElseThrow().soBuoiHoanThanh());
        int lopChinhCu=lopChinh(ketNoi,2);
        int soDangKyCu=demBang(ketNoi,"DangKyKhoaHoc");
        int soPhieuThuCu=demBang(ketNoi,"PhieuThu");
        Fixture f=taoFixture(ketNoi);
        try {
            assertFalse(dd.layBuoi(CO_SO,LocalDate.now().minusDays(2)).isEmpty());
            dd.luuNoiDung(CO_SO,f.nguon(),"Bài kiểm thử GD6");
            dd.luuNoiDung(CO_SO,f.dich(),"Bài kiểm thử GD6");
            assertThrows(SQLException.class,()->dd.luuNoiDung(CO_SO,f.nguon(),"Bài khác"));
            dd.luuDiemDanh(CO_SO,f.nguon(),2,6,"VANG_CO_PHEP","SQL TEST",1,null);
            int source=timSapXep(ketNoi,f.nguon(),2);
            assertTrue(hb.layBuoiVang(CO_SO).stream().anyMatch(x->x.maSapXep()==source));
            assertTrue(hb.layBuoiDich(CO_SO,source).stream()
                    .anyMatch(x->x.maBuoi()==f.dich()));
            assertEquals(1,soBuoi(chiTiet));
            capNhatNoiDungTrucTiep(ketNoi,f.dich(),"Bài không khớp");
            try {
                assertThrows(SQLException.class,()->hb.xepHocBu(CO_SO,source,f.dich(),1));
                assertEquals(0,demHocBuHoatDong(ketNoi,source));
            } finally { capNhatNoiDungTrucTiep(ketNoi,f.dich(),"Bài kiểm thử GD6"); }
            capNhatSiSo(ketNoi,1);
            try {
                assertThrows(SQLException.class,()->hb.xepHocBu(CO_SO,source,f.dich(),1));
                assertEquals(0,demHocBuHoatDong(ketNoi,source));
            } finally { capNhatSiSo(ketNoi,20); }
            int buoiTrung;
            try (Connection c=ketNoi.mo()) {
                buoiTrung=themBuoi(c,2,4,LocalDate.now().plusDays(12));
            }
            try {
                assertThrows(SQLException.class,()->hb.xepHocBu(CO_SO,source,f.dich(),1));
                assertEquals(0,demHocBuHoatDong(ketNoi,source));
            } finally { xoaBuoi(ketNoi,buoiTrung); }

            // Hai nhan vien cung xep mot buoi vang: chi mot luot duoc tao.
            CountDownLatch batDau=new CountDownLatch(1);
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                Future<String> a=pool.submit(()->datHocBu(hb,source,f.dich(),batDau));
                Future<String> b=pool.submit(()->datHocBu(hb,source,f.dich(),batDau));
                batDau.countDown();
                String ra=a.get(20,TimeUnit.SECONDS),rb=b.get(20,TimeUnit.SECONDS);
                assertTrue("OK".equals(ra) ^ "OK".equals(rb),ra+" | "+rb);
            } finally { pool.shutdownNow(); }
            int maBu=timHocBu(ketNoi,source);
            assertEquals(1,demHocBuHoatDong(ketNoi,source));
            assertEquals(1,soBuoi(chiTiet));
            assertEquals(lopChinhCu,lopChinh(ketNoi,2));
            assertEquals(soDangKyCu,demBang(ketNoi,"DangKyKhoaHoc"));
            assertEquals(soPhieuThuCu,demBang(ketNoi,"PhieuThu"));
            assertThrows(SQLException.class,()->dd.luuDiemDanh(CO_SO,f.nguon(),2,6,
                    "CO_MAT","sửa",1,"kiểm thử"));
            assertEquals("VANG_CO_PHEP",trangThaiDiemDanh(ketNoi,source));
            hb.huyHocBu(CO_SO,maBu,1,"Đổi lịch kiểm thử");
            assertEquals(0,demHocBuHoatDong(ketNoi,source));
            assertTrue(hb.xepHocBu(CO_SO,source,f.dich(),1)>maBu);
            assertEquals(1,demHocBuHoatDong(ketNoi,source));

            // Chuyen ngay CHI tren TEST de gia lap buoi bu da dien ra.
            try (Connection c=ketNoi.mo();PreparedStatement ps=c.prepareStatement("""
                    UPDATE dbo.BuoiHoc SET NgayHoc=? WHERE MaBuoiHoc=?
                    """)) {
                ps.setDate(1,Date.valueOf(LocalDate.now().minusDays(1)));
                ps.setInt(2,f.dich());assertEquals(1,ps.executeUpdate());
            }
            dd.luuDiemDanh(CO_SO,f.dich(),2,6,"CO_MAT","Đã học bù",1,null);
            assertEquals("CO_MAT",trangThaiDiemDanh(ketNoi,timHocBu(ketNoi,source)));
            assertEquals(2,soBuoi(chiTiet));
            assertEquals("HOAN_THANH",trangThaiDangKy(ketNoi));
            assertEquals(lopChinhCu,lopChinh(ketNoi,2));
            assertEquals(soDangKyCu,demBang(ketNoi,"DangKyKhoaHoc"));
            assertEquals(soPhieuThuCu,demBang(ketNoi,"PhieuThu"));
            dd.luuDiemDanh(CO_SO,f.dich(),2,6,"VANG_CO_PHEP","Sửa kiểm thử",
                    1,"Điều chỉnh kết quả kiểm thử");
            assertEquals(1,soBuoi(chiTiet));
            assertEquals("DANG_HOC",trangThaiDangKy(ketNoi));
            assertEquals(1,demLichSuDiemDanh(ketNoi,timHocBu(ketNoi,source)));
            assertEquals(1,dd.layLichSuSua(CO_SO,timHocBu(ketNoi,source)).size());
            assertThrows(SQLException.class,()->hb.huyHocBu(CO_SO,timHocBu(ketNoi,source),
                    1,"Không được hủy buổi đã học"));
            kiemTraHaiKhoaDocLap(ketNoi,dd,chiTiet,f.nguon());
        } finally { donFixture(ketNoi,f); }
    }

    private static void kiemTraHaiKhoaDocLap(
            DiemDanhTrungTamRepository.KetNoi k,
            DiemDanhTrungTamRepository dd,
            ChiTietHocVienTrungTamRepository chiTiet,int maBuoi) throws SQLException {
        int tongCu;
        String trangThaiCu;
        Date ketThucCu;
        boolean tuDongCu;
        try (Connection c=k.mo();Statement s=c.createStatement();ResultSet rs=s.executeQuery("""
                SELECT TongSoBuoiDangKy,TrangThai,NgayKetThucThucTe,
                       HoanThanhTuDongGD6
                FROM dbo.DangKyKhoaHoc WHERE MaDangKy=1 AND MaCoSo=2
                """)) {
            if (!rs.next()) throw new SQLException("Thiếu đăng ký TEST số 1");
            tongCu=rs.getInt(1);trangThaiCu=rs.getString(2);
            ketThucCu=rs.getDate(3);tuDongCu=rs.getBoolean(4);
        }
        try {
            try (Connection c=k.mo();Statement s=c.createStatement()) {
                assertEquals(1,s.executeUpdate("""
                        UPDATE dbo.DangKyKhoaHoc SET TongSoBuoiDangKy=3
                        WHERE MaDangKy=1 AND MaCoSo=2
                        """));
            }
            dd.luuDiemDanh(CO_SO,maBuoi,1,5,"CO_MAT","GD6 SQL TEST",1,null);
            var khoa=chiTiet.tai(CO_SO,5).khoaHoc();
            assertEquals(3,khoa.stream().filter(x->x.dangKy().maDangKy()==1)
                    .findFirst().orElseThrow().soBuoiHoanThanh());
            assertEquals("HOAN_THANH",khoa.stream()
                    .filter(x->x.dangKy().maDangKy()==1)
                    .findFirst().orElseThrow().dangKy().trangThai());
            assertEquals(null,khoa.stream().filter(x->x.dangKy().maDangKy()==7)
                    .findFirst().orElseThrow().soBuoiHoanThanh());
            assertEquals("CHO_XEP_LOP",khoa.stream()
                    .filter(x->x.dangKy().maDangKy()==7)
                    .findFirst().orElseThrow().dangKy().trangThai());
        } finally {
            try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                    UPDATE dbo.DangKyKhoaHoc SET TongSoBuoiDangKy=?,TrangThai=?,
                           NgayKetThucThucTe=?,HoanThanhTuDongGD6=?
                    WHERE MaDangKy=1 AND MaCoSo=2
                    """)) {
                ps.setInt(1,tongCu);ps.setString(2,trangThaiCu);
                ps.setDate(3,ketThucCu);ps.setBoolean(4,tuDongCu);
                assertEquals(1,ps.executeUpdate());
            }
        }
    }

    private static String datHocBu(HocBuTrungTamRepository repo,int source,int target,
                                     CountDownLatch batDau) throws Exception {
        batDau.await();
        try { repo.xepHocBu(CO_SO,source,target,1);return "OK"; }
        catch (SQLException ex) { return ex.getMessage(); }
    }
    private static Fixture taoFixture(DiemDanhTrungTamRepository.KetNoi ketNoi)
            throws SQLException {
        int nguon=0,dich=0;
        try (Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);
            try {
                int tongCu;
                String trangThaiCu;
                Date ketThucCu;
                try (Statement s=c.createStatement();ResultSet rs=s.executeQuery("""
                        SELECT TongSoBuoiDangKy,TrangThai,NgayKetThucThucTe
                        FROM dbo.DangKyKhoaHoc WHERE MaDangKy=2 AND MaCoSo=2
                        """)) {
                    if (!rs.next()) throw new SQLException("Thieu dang ky TEST so 2");
                    tongCu=rs.getInt(1);trangThaiCu=rs.getString(2);
                    ketThucCu=rs.getDate(3);
                }
                try (Statement s=c.createStatement()) {
                    s.executeUpdate("UPDATE dbo.DangKyKhoaHoc SET TongSoBuoiDangKy=2 "
                            + "WHERE MaDangKy=2 AND MaCoSo=2");
                }
                nguon=themBuoi(c,2,4,LocalDate.now().minusDays(2));
                dich=themBuoi(c,1,1,LocalDate.now().plusDays(12));
                c.commit();return new Fixture(nguon,dich,tongCu,trangThaiCu,ketThucCu);
            } catch (SQLException ex) { c.rollback();throw ex; }
        }
    }
    private static int themBuoi(Connection c,int lop,int phong,LocalDate ngay)
            throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("""
                INSERT dbo.BuoiHoc(MaLop,NgayHoc,SoCa,TrangThai,MaPhong,GhiChu)
                VALUES (?,?,8,'DU_KIEN',?,N'[GD6 SQL TEST]')
                """,Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,lop);ps.setDate(2,Date.valueOf(ngay));ps.setInt(3,phong);
            ps.executeUpdate();
            try (ResultSet rs=ps.getGeneratedKeys()) { rs.next();return rs.getInt(1); }
        }
    }
    private static void capNhatNoiDungTrucTiep(DiemDanhTrungTamRepository.KetNoi k,
                                                int buoi,String noiDung) throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                UPDATE dbo.BuoiHoc SET NoiDungHoc=? WHERE MaBuoiHoc=?
                """)) {
            ps.setString(1,noiDung);ps.setInt(2,buoi);
            assertEquals(1,ps.executeUpdate());
        }
    }
    private static void capNhatSiSo(DiemDanhTrungTamRepository.KetNoi k,int siSo)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                UPDATE dbo.LopTrungTam SET SiSoToiDa=? WHERE MaLop=1 AND MaCoSo=2
                """)) {
            ps.setInt(1,siSo);assertEquals(1,ps.executeUpdate());
        }
    }
    private static void xoaBuoi(DiemDanhTrungTamRepository.KetNoi k,int buoi)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                DELETE FROM dbo.BuoiHoc WHERE MaBuoiHoc=?
                """)) {
            ps.setInt(1,buoi);assertEquals(1,ps.executeUpdate());
        }
    }
    private static void donFixture(DiemDanhTrungTamRepository.KetNoi ketNoi,Fixture f)
            throws SQLException {
        try (Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);
            try {
                for (String sql: List.of(
                        "DELETE ls FROM dbo.LichSuSuaDiemDanhTrungTam ls JOIN dbo.DiemDanhTrungTam dd ON dd.MaDiemDanh=ls.MaDiemDanh JOIN dbo.SapXepBuoiHoc s ON s.MaSapXep=dd.MaSapXep WHERE s.MaBuoiHoc IN (?,?)",
                        "DELETE dd FROM dbo.DiemDanhTrungTam dd JOIN dbo.SapXepBuoiHoc s ON s.MaSapXep=dd.MaSapXep WHERE s.MaBuoiHoc IN (?,?)")) {
                    try (PreparedStatement ps=c.prepareStatement(sql)) {
                        ps.setInt(1,f.nguon());ps.setInt(2,f.dich());ps.executeUpdate();
                    }
                }
                // FK tu than: xoa cac luot bu truoc, roi moi xoa nguon.
                try (PreparedStatement ps=c.prepareStatement(
                        "DELETE FROM dbo.SapXepBuoiHoc WHERE MaBuoiHoc=?")) {
                    ps.setInt(1,f.dich());ps.executeUpdate();
                    ps.setInt(1,f.nguon());ps.executeUpdate();
                }
                try (PreparedStatement ps=c.prepareStatement(
                        "DELETE FROM dbo.BuoiHoc WHERE MaBuoiHoc IN (?,?)")) {
                    ps.setInt(1,f.nguon());ps.setInt(2,f.dich());ps.executeUpdate();
                }
                try (PreparedStatement ps=c.prepareStatement("""
                        UPDATE dbo.DangKyKhoaHoc SET TongSoBuoiDangKy=?,TrangThai=?,
                            NgayKetThucThucTe=?,HoanThanhTuDongGD6=0
                        WHERE MaDangKy=2 AND MaCoSo=2
                        """)) {
                    ps.setInt(1,f.tongCu());ps.setString(2,f.trangThaiCu());
                    ps.setDate(3,f.ketThucCu());ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) { c.rollback();throw ex; }
        }
    }
    private static int timSapXep(DiemDanhTrungTamRepository.KetNoi k,int buoi,int dk)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                SELECT MaSapXep FROM dbo.SapXepBuoiHoc WHERE MaBuoiHoc=? AND MaDangKy=?
                """)) {
            ps.setInt(1,buoi);ps.setInt(2,dk);
            try (ResultSet rs=ps.executeQuery()) { rs.next();return rs.getInt(1); }
        }
    }
    private static int lopChinh(DiemDanhTrungTamRepository.KetNoi k,int maDangKy)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                SELECT MaLop FROM dbo.XepLopTrungTam
                WHERE MaDangKy=? AND TrangThai='HIEU_LUC' AND DenNgay IS NULL
                """)) {
            ps.setInt(1,maDangKy);
            try (ResultSet rs=ps.executeQuery()) { rs.next();return rs.getInt(1); }
        }
    }
    private static int demBang(DiemDanhTrungTamRepository.KetNoi k,String bang)
            throws SQLException {
        if (!List.of("DangKyKhoaHoc","PhieuThu").contains(bang))
            throw new IllegalArgumentException("Bang kiem thu khong hop le");
        try (Connection c=k.mo();Statement s=c.createStatement();
             ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM dbo."+bang)) {
            rs.next();return rs.getInt(1);
        }
    }
    private static Integer soBuoi(ChiTietHocVienTrungTamRepository repo)
            throws SQLException {
        return repo.tai(CO_SO,6).khoaHoc().stream()
                .filter(k->k.dangKy().maDangKy()==2).findFirst().orElseThrow()
                .soBuoiHoanThanh();
    }
    private static int timHocBu(DiemDanhTrungTamRepository.KetNoi k,int source)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                SELECT TOP (1) MaSapXep FROM dbo.SapXepBuoiHoc
                WHERE MaSapXepNguon=? AND TrangThai<>'HUY' ORDER BY MaSapXep DESC
                """)) {
            ps.setInt(1,source);
            try (ResultSet rs=ps.executeQuery()) { rs.next();return rs.getInt(1); }
        }
    }
    private static int demHocBuHoatDong(DiemDanhTrungTamRepository.KetNoi k,int source)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                SELECT COUNT(*) FROM dbo.SapXepBuoiHoc WHERE MaSapXepNguon=?
                AND TrangThai IN ('DA_XEP','HOAN_THANH')
                """)) {
            ps.setInt(1,source);
            try (ResultSet rs=ps.executeQuery()) { rs.next();return rs.getInt(1); }
        }
    }
    private static String trangThaiDiemDanh(DiemDanhTrungTamRepository.KetNoi k,int sapXep)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                SELECT TrangThai FROM dbo.DiemDanhTrungTam WHERE MaSapXep=?
                """)) {
            ps.setInt(1,sapXep);
            try (ResultSet rs=ps.executeQuery()) { rs.next();return rs.getString(1); }
        }
    }
    private static String trangThaiDangKy(DiemDanhTrungTamRepository.KetNoi k)
            throws SQLException {
        try (Connection c=k.mo();Statement s=c.createStatement();
             ResultSet rs=s.executeQuery("SELECT TrangThai FROM dbo.DangKyKhoaHoc WHERE MaDangKy=2")) {
            rs.next();return rs.getString(1);
        }
    }
    private static int demLichSuDiemDanh(DiemDanhTrungTamRepository.KetNoi k,int sapXep)
            throws SQLException {
        try (Connection c=k.mo();PreparedStatement ps=c.prepareStatement("""
                SELECT COUNT(*) FROM dbo.LichSuSuaDiemDanhTrungTam ls
                JOIN dbo.DiemDanhTrungTam dd ON dd.MaDiemDanh=ls.MaDiemDanh
                WHERE dd.MaSapXep=?
                """)) {
            ps.setInt(1,sapXep);
            try (ResultSet rs=ps.executeQuery()) { rs.next();return rs.getInt(1); }
        }
    }
    private static DiemDanhTrungTamRepository.KetNoi ketNoiTest() throws Exception {
        Properties p=new Properties();
        try (InputStream in=Files.newInputStream(Path.of("db.local.properties"))) {
            p.load(in);
        }
        String goc=p.getProperty("db.url","");
        if (!goc.matches("(?is).*databaseName=VINABACUS(?:;.*)?"))
            throw new IllegalStateException("Cau hinh goc khong tro VINABACUS");
        String url=goc.replaceFirst("(?i)(databaseName=)VINABACUS(?=;|$)","$1"+TEST_DB);
        return ()->{
            Connection c=DriverManager.getConnection(url,p.getProperty("db.user",""),
                    p.getProperty("db.password",""));
            if (!TEST_DB.equals(tenDb(c))) { c.close();
                throw new SQLException("Tu choi ghi ngoai CSDL TEST GD6"); }
            return c;
        };
    }
    private static String tenDb(Connection c) throws SQLException {
        try (Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT DB_NAME()")) {
            rs.next();return rs.getString(1);
        }
    }
}
