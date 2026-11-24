package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.repository.TaiChinhDangKyRepository;
import vn.vinabacus.repository.TaiChinhRepository;

/** Chỉ ghi CSDL TEST đã kiểm tra DB_NAME mỗi lần mở kết nối. */
class GiaiDoan7TaiChinhSqlTest {
    private static final String TEST_DB="VINABACUS_GD6_TEST_20261123";
    private static final CoSo CS=CoSo.TRUNG_TAM;
    private static final int HOC_VIEN=5,TAI_KHOAN=1,KHOAN_MOT=1;

    @Test
    @EnabledIfSystemProperty(named="vinabacus.gd7.sqltest",matches="true")
    void thuNhieuKhoaHuyPhieuVaDongThoi() throws Exception {
        TaiChinhRepository.KetNoi ketNoi=ketNoiTest();
        TaiChinhRepository repo=new TaiChinhRepository(ketNoi);
        int receiptCount=count(ketNoi,"PhieuThu");
        int detailCount=count(ketNoi,"ChiTietPhieuThu");
        int debtCount=count(ketNoi,"KhoanPhaiThu");
        var reportBefore=repo.thongKe(CS,YearMonth.now());
        BigDecimal paidBefore=reportBefore.daThuToanBo();
        LocalDate firstDay=YearMonth.now().atDay(1),nextMonth=YearMonth.now().plusMonths(1).atDay(1);
        BigDecimal allocationsBefore=repo.baoCaoThu(CS,firstDay,nextMonth).stream()
            .map(x->x.phanBoRong()).reduce(BigDecimal.ZERO,BigDecimal::add);
        assertEquals(0,demTheoDangKy(ketNoi,7),"TEST đã có khoản thu đăng ký #7");
        UUID one=UUID.randomUUID(),bad=UUID.randomUUID(),raceA=UUID.randomUUID(),raceB=UUID.randomUUID();
        List<UUID> tokens=List.of(one,bad,raceA,raceB);
        int second=0;
        try {
            repo.lapKhoanHocPhiKhoa(7,null);
            second=maKhoanDangKy(ketNoi,7);
            final int secondId=second;
            var allocations=List.of(new TaiChinhDangKyRepository.PhanBo(KHOAN_MOT,new BigDecimal("100000.00")),
                new TaiChinhDangKyRepository.PhanBo(second,new BigDecimal("50000.00")));
            BigDecimal amount=new BigDecimal("150000.00");
            int receipt=repo.lapPhieuNhieuKhoan(CS,HOC_VIEN,amount,allocations,
                "TIEN_MAT",TAI_KHOAN,"GD7 SQL TEST",one);
            assertEquals(2,repo.phanBoPhieu(CS,receipt).size());
            assertEquals(0,repo.phanBoPhieu(CS,receipt).stream()
                .map(x->x.soTien()).reduce(BigDecimal.ZERO,BigDecimal::add)
                .compareTo(amount));
            assertEquals(2,repo.lichSuThuHocVien(CS,HOC_VIEN).stream()
                .filter(x->x.maPhieu()==receipt).count());
            assertEquals(receipt,repo.lapPhieuNhieuKhoan(CS,HOC_VIEN,amount,allocations,
                "TIEN_MAT",TAI_KHOAN,"GD7 SQL TEST",one));
            assertEquals(1,soPhieuToken(ketNoi,one));
            assertEquals(0,repo.khoanThu(CS).stream().filter(k->k.maKhoan()==secondId)
                .findFirst().orElseThrow().conNo().compareTo(new BigDecimal("200000.00")));
            assertEquals(0,repo.thongKe(CS,YearMonth.now()).daThuToanBo()
                .compareTo(paidBefore.add(amount)));
            assertEquals(reportBefore.soPhieuHopLe()+1,
                repo.thongKe(CS,YearMonth.now()).soPhieuHopLe());
            assertEquals(0,repo.baoCaoThu(CS,firstDay,nextMonth).stream()
                .map(x->x.phanBoRong()).reduce(BigDecimal.ZERO,BigDecimal::add)
                .compareTo(allocationsBefore.add(amount)));
            assertEquals(0,reportBefore.tongGiamGia()
                .compareTo(repo.thongKe(CS,YearMonth.now()).tongGiamGia()));
            assertThrows(SQLException.class,()->repo.lapPhieuNhieuKhoan(CS,HOC_VIEN,
                new BigDecimal("2000000.00"),List.of(
                    new TaiChinhDangKyRepository.PhanBo(KHOAN_MOT,new BigDecimal("2000000.00"))),
                "TIEN_MAT",TAI_KHOAN,"GD7 SQL TEST",bad));
            assertEquals(0,soPhieuToken(ketNoi,bad));
            assertThrows(IllegalArgumentException.class,()->
                TaiChinhDangKyRepository.kiemTraPhanBo(BigDecimal.ZERO,allocations));
            repo.huyPhieu(CS,receipt,TAI_KHOAN,"Hủy phiếu kiểm thử");
            assertEquals("HUY",trangThaiPhieu(ketNoi,receipt));
            assertEquals(0,repo.thongKe(CS,YearMonth.now()).daThuToanBo().compareTo(paidBefore));
            assertEquals(reportBefore.soPhieuHopLe(),
                repo.thongKe(CS,YearMonth.now()).soPhieuHopLe());
            assertEquals(0,repo.baoCaoThu(CS,firstDay,nextMonth).stream()
                .map(x->x.phanBoRong()).reduce(BigDecimal.ZERO,BigDecimal::add)
                .compareTo(allocationsBefore));
            assertEquals(2,repo.phanBoPhieu(CS,receipt).size(),"Lịch sử phân bổ phải còn");
            assertThrows(SQLException.class,()->repo.huyPhieu(CS,receipt,TAI_KHOAN,"Hủy lặp"));

            // Hai nhân viên cùng thu toàn bộ một khoản: chỉ một giao dịch thành công.
            CountDownLatch start=new CountDownLatch(1);
            var pool=Executors.newFixedThreadPool(2);
            try {
                var a=pool.submit(()->race(repo,start,raceA));
                var b=pool.submit(()->race(repo,start,raceB));
                start.countDown();
                boolean x=a.get(25,TimeUnit.SECONDS),y=b.get(25,TimeUnit.SECONDS);
                assertTrue(x^y,"Chỉ một nhân viên được thu đủ số dư");
            } finally {pool.shutdownNow();}
            int raceReceipt=maPhieuToken(ketNoi,soPhieuToken(ketNoi,raceA)==1?raceA:raceB);
            repo.huyPhieu(CS,raceReceipt,TAI_KHOAN,"Dọn giao dịch đồng thời trên TEST");
            assertEquals(0,repo.thongKe(CS,YearMonth.now()).daThuToanBo().compareTo(paidBefore));
        } finally {
            donFixture(ketNoi,tokens,second);
        }
        assertEquals(receiptCount,count(ketNoi,"PhieuThu"));
        assertEquals(detailCount,count(ketNoi,"ChiTietPhieuThu"));
        assertEquals(debtCount,count(ketNoi,"KhoanPhaiThu"));
    }

    private static boolean race(TaiChinhRepository repo,CountDownLatch start,UUID token)throws Exception {
        start.await();
        try {
            repo.lapPhieuNhieuKhoan(CS,HOC_VIEN,new BigDecimal("1400000.00"),
                List.of(new TaiChinhDangKyRepository.PhanBo(KHOAN_MOT,new BigDecimal("1400000.00"))),
                "TIEN_MAT",TAI_KHOAN,"GD7 SQL TEST",token);
            return true;
        } catch(SQLException ex) {return false;}
    }
    private static int count(TaiChinhRepository.KetNoi k,String table)throws SQLException {
        if(!List.of("PhieuThu","ChiTietPhieuThu","KhoanPhaiThu").contains(table))
            throw new IllegalArgumentException(table);
        try(Connection c=k.mo();Statement s=c.createStatement();ResultSet r=s.executeQuery(
                "SELECT COUNT(*) FROM dbo."+table)){r.next();return r.getInt(1);}
    }
    private static int demTheoDangKy(TaiChinhRepository.KetNoi k,int id)throws SQLException {
        try(Connection c=k.mo();PreparedStatement s=c.prepareStatement(
                "SELECT COUNT(*) FROM dbo.KhoanPhaiThu WHERE MaDangKy=?")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    private static int maKhoanDangKy(TaiChinhRepository.KetNoi k,int id)throws SQLException {
        try(Connection c=k.mo();PreparedStatement s=c.prepareStatement(
                "SELECT MaKhoanThu FROM dbo.KhoanPhaiThu WHERE MaDangKy=?")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){
                if(!r.next())throw new SQLException("Không tìm thấy khoản TEST");return r.getInt(1);
            }}
    }
    private static int soPhieuToken(TaiChinhRepository.KetNoi k,UUID token)throws SQLException {
        try(Connection c=k.mo();PreparedStatement s=c.prepareStatement(
                "SELECT COUNT(*) FROM dbo.PhieuThu WHERE MaGiaoDichNghiepVu=?")){
            s.setString(1,token.toString());try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    private static int maPhieuToken(TaiChinhRepository.KetNoi k,UUID token)throws SQLException {
        try(Connection c=k.mo();PreparedStatement s=c.prepareStatement(
                "SELECT MaPhieuThu FROM dbo.PhieuThu WHERE MaGiaoDichNghiepVu=?")){
            s.setString(1,token.toString());try(ResultSet r=s.executeQuery()){
                if(!r.next())throw new SQLException("Thiếu phiếu TEST");return r.getInt(1);
            }}
    }
    private static String trangThaiPhieu(TaiChinhRepository.KetNoi k,int id)throws SQLException {
        try(Connection c=k.mo();PreparedStatement s=c.prepareStatement(
                "SELECT TrangThai FROM dbo.PhieuThu WHERE MaPhieuThu=?")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){
                if(!r.next())throw new SQLException("Thiếu phiếu TEST");return r.getString(1);
            }}
    }
    private static void donFixture(TaiChinhRepository.KetNoi k,List<UUID> tokens,int debt)throws SQLException {
        try(Connection c=k.mo()) {
            c.setAutoCommit(false);
            try {
                for(UUID token:tokens)try(PreparedStatement s=c.prepareStatement("""
                    DELETE ct FROM dbo.ChiTietPhieuThu ct JOIN dbo.PhieuThu p
                    ON p.MaPhieuThu=ct.MaPhieuThu WHERE p.MaGiaoDichNghiepVu=?
                    """)){
                    s.setString(1,token.toString());s.executeUpdate();
                }
                for(UUID token:tokens)try(PreparedStatement s=c.prepareStatement(
                        "DELETE FROM dbo.PhieuThu WHERE MaGiaoDichNghiepVu=?")){
                    s.setString(1,token.toString());s.executeUpdate();
                }
                if(debt>0)try(PreparedStatement s=c.prepareStatement(
                        "DELETE FROM dbo.KhoanPhaiThu WHERE MaKhoanThu=? AND MaDangKy=7")){
                    s.setInt(1,debt);s.executeUpdate();
                }
                c.commit();
            } catch(SQLException ex){c.rollback();throw ex;}
        }
    }
    private static TaiChinhRepository.KetNoi ketNoiTest()throws Exception {
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(Path.of("db.local.properties"))){p.load(in);}
        String source=p.getProperty("db.url","");
        if(!source.matches("(?is).*databaseName=VINABACUS(?:;.*)?"))
            throw new IllegalStateException("Cấu hình gốc phải trỏ VINABACUS");
        String url=source.replaceFirst("(?i)(databaseName=)VINABACUS(?=;|$)","$1"+TEST_DB);
        return ()->{
            Connection c=DriverManager.getConnection(url,p.getProperty("db.user",""),
                p.getProperty("db.password",""));
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT DB_NAME()")){
                r.next();if(!TEST_DB.equals(r.getString(1)))throw new SQLException("Từ chối ghi ngoài TEST");
            } catch(SQLException ex){c.close();throw ex;}
            return c;
        };
    }
}
