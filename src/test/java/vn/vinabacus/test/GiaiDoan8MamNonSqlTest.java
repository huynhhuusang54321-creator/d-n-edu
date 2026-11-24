package vn.vinabacus.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.vinabacus.repository.MamNonRepository;
import vn.vinabacus.repository.MamNonRepository.ThayDoiDiemDanh;
import vn.vinabacus.repository.MamNonRepository.NgayHoatDong;

/** Ghi dữ liệu cô lập trên TEST; kiểm tra DB_NAME ở mọi kết nối và dọn các dòng thử. */
class GiaiDoan8MamNonSqlTest {
    private static final String TEST_DB="VINABACUS_GD6_TEST_20261123";

    @Test
    @EnabledIfSystemProperty(named="vinabacus.gd8.sqltest",matches="true")
    void chuyenLopGiuaThangTinhHaiMucPhiMotLanLamTron()throws Exception {
        MamNonRepository.KetNoi k=ketNoiTest();MamNonRepository repo=new MamNonRepository(k);
        int coSo=1,nam=0,tk=0,lopCu=0,lopMoi=0,hs=0,thang=0,bang=0;
        int giaCu=0,giaMoi=0,anCu=0,anMoi=0;
        LocalDate start=null;
        String tag="[GD8_SPLIT_"+UUID.randomUUID().toString().substring(0,8)+"]";
        try(Connection c=k.mo()){
            try(PreparedStatement p=c.prepareStatement("SELECT TOP (1) MaNamHoc,TuNgay FROM dbo.NamHoc WHERE MaCoSo=? ORDER BY TuNgay DESC")){
                p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){assertTrue(r.next());nam=r.getInt(1);start=r.getDate(2).toLocalDate();}
            }
            try(PreparedStatement p=c.prepareStatement("SELECT TOP (1) cs.MaTaiKhoan FROM dbo.TaiKhoanCoSo cs JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=cs.MaTaiKhoan WHERE cs.MaCoSo=? AND cs.TrangThai=1 AND t.TrangThai=1")){
                p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){assertTrue(r.next());tk=r.getInt(1);}
            }
        }
        YearMonth ky=YearMonth.from(start).plusMonths(1);LocalDate first=ky.atDay(1),move=ky.atDay(16);
        try {
            lopCu=repo.luuLop(coSo,0,nam,tag+" A",true,"MAM",1);
            lopMoi=repo.luuLop(coSo,0,nam,tag+" B",true,"MAM",1);
            try(Connection c=k.mo()){hs=insert(c,"INSERT dbo.HocVien(MaCoSo,HoTen,TrangThaiHoSo) VALUES(?,?,'HOAT_DONG')",coSo,tag);}
            repo.xepHocSinh(coSo,lopCu,hs,first);
            int finalHs=hs;
            int xep=repo.xepLop(coSo,lopCu).stream().filter(x->x.maHocVien()==finalHs).findFirst().orElseThrow().maXepLop();
            repo.chuyenLop(coSo,xep,lopMoi,move,"Chuyển lớp trên TEST");
            List<NgayHoatDong> calendar=new java.util.ArrayList<>();
            for(int d=1;d<=ky.lengthOfMonth();d++){
                LocalDate day=ky.atDay(d);boolean lesson=d==3||d==10||d==20||d==25;
                calendar.add(new NgayHoatDong(day,lesson,lesson));
            }
            repo.luuLichHoatDong(coSo,nam,ky,calendar);
            thang=repo.taoThang(coSo,ky,4);
            try(Connection c=k.mo()){
                giaCu=insert(c,"INSERT dbo.MucHocPhiLop(MaLop,ThangBatDau,HocPhiThang,MaTaiKhoanThietLap) VALUES(?,?,?,?)",lopCu,first,new BigDecimal("1500001.00"),tk);
                giaMoi=insert(c,"INSERT dbo.MucHocPhiLop(MaLop,ThangBatDau,HocPhiThang,MaTaiKhoanThietLap) VALUES(?,?,?,?)",lopMoi,first,new BigDecimal("1800002.00"),tk);
                anCu=insert(c,"INSERT dbo.DonGiaTienAn(MaLop,ThangBatDau,DonGiaNgay,MaTaiKhoanThietLap) VALUES(?,?,?,?)",lopCu,first,new BigDecimal("35000.00"),tk);
                anMoi=insert(c,"INSERT dbo.DonGiaTienAn(MaLop,ThangBatDau,DonGiaNgay,MaTaiKhoanThietLap) VALUES(?,?,?,?)",lopMoi,first,new BigDecimal("40000.00"),tk);
            }
            bang=repo.lapBangTinhNhap(coSo,hs,ky);
            var rows=repo.bangTinhThang(coSo,nam,ky);
            assertEquals(1,rows.size(),"Một học sinh/tháng chỉ có một bảng tính dù chuyển lớp");
            assertEquals(new BigDecimal("1650002.00"),rows.get(0).hocPhi());
            assertTrue(rows.get(0).lop().contains(tag+" A")&&rows.get(0).lop().contains(tag+" B"));
            assertEquals(4,rows.get(0).soNgayCanDiemDanh());
            assertEquals(2,count(k,"SELECT COUNT(*) FROM dbo.ChiTietHocPhiMamNon WHERE MaBangTinh=?",bang));
            int xepMoi=repo.xepLop(coSo,lopMoi).stream().filter(x->x.maHocVien()==finalHs).findFirst().orElseThrow().maXepLop();
            assertThrows(SQLException.class,()->repo.ketThucXepLop(coSo,xepMoi,ky.atDay(26),"NGHI_HAN","Không được đổi khi đã tính phí"));
            int finalBang=bang;
            assertThrows(SQLException.class,()->repo.chotBangTinh(coSo,finalBang));
        }finally{
            try(Connection c=k.mo()){
                c.setAutoCommit(false);
                try{
                    if(bang>0){update(c,"DELETE dbo.ChiTietTienAnMamNon WHERE MaBangTinh=?",bang);update(c,"DELETE dbo.ChiTietHocPhiMamNon WHERE MaBangTinh=?",bang);update(c,"DELETE dbo.BangTinhPhiMamNon WHERE MaBangTinh=?",bang);}
                    for(int id:new int[]{giaCu,giaMoi})if(id>0)update(c,"DELETE dbo.MucHocPhiLop WHERE MaMucHocPhi=?",id);
                    for(int id:new int[]{anCu,anMoi})if(id>0)update(c,"DELETE dbo.DonGiaTienAn WHERE MaDonGiaTienAn=?",id);
                    if(thang>0)update(c,"DELETE dbo.ThangMamNon WHERE MaThang=?",thang);
                    update(c,"DELETE dbo.NgayHoatDongMamNon WHERE MaCoSo=? AND Ngay>=? AND Ngay<?",coSo,first,ky.plusMonths(1).atDay(1));
                    if(hs>0){update(c,"DELETE dbo.XepLopMamNon WHERE MaHocVien=?",hs);update(c,"DELETE dbo.HocVien WHERE MaHocVien=?",hs);}
                    if(lopCu>0)update(c,"DELETE dbo.LopMamNon WHERE MaLop=?",lopCu);
                    if(lopMoi>0)update(c,"DELETE dbo.LopMamNon WHERE MaLop=?",lopMoi);
                    c.commit();
                }catch(Exception ex){c.rollback();throw ex;}
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named="vinabacus.gd8.sqltest",matches="true")
    void chuyenNamHocGiuLichSuVaRollbackKhiHetCho() throws Exception {
        MamNonRepository.KetNoi ketNoi=ketNoiTest();MamNonRepository repo=new MamNonRepository(ketNoi);
        int coSo=1,sourceYear=0,targetYear=0,source=0,target=0,wrong=0,student1=0,student2=0;
        LocalDate oldStart=null,newStart=null;
        String tag="[GD8_MOVE_"+UUID.randomUUID().toString().substring(0,8)+"]";
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement("SELECT TOP (2) MaNamHoc,TuNgay FROM dbo.NamHoc WHERE MaCoSo=? ORDER BY TuNgay")) {
            p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){
                assertTrue(r.next());sourceYear=r.getInt(1);oldStart=r.getDate(2).toLocalDate();
                assertTrue(r.next());targetYear=r.getInt(1);newStart=r.getDate(2).toLocalDate();
            }
        }
        try {
            source=repo.luuLop(coSo,0,sourceYear,tag+" Mầm",true,"MAM",2);
            target=repo.luuLop(coSo,0,targetYear,tag+" Chồi",true,"CHOI",1);
            wrong=repo.luuLop(coSo,0,targetYear,tag+" Lá",true,"LA",2);
            try(Connection c=ketNoi.mo()){
                student1=insert(c,"INSERT dbo.HocVien(MaCoSo,HoTen,TrangThaiHoSo) VALUES(?,?,'HOAT_DONG')",coSo,tag+" A");
                student2=insert(c,"INSERT dbo.HocVien(MaCoSo,HoTen,TrangThaiHoSo) VALUES(?,?,'HOAT_DONG')",coSo,tag+" B");
            }
            repo.xepHocSinh(coSo,source,student1,oldStart);
            repo.xepHocSinh(coSo,source,student2,oldStart);
            int finalSource=source,finalTarget=target,finalWrong=wrong;LocalDate transfer=newStart;
            assertThrows(SQLException.class,()->repo.duKienChuyen(coSo,finalSource,finalWrong,transfer));
            assertTrue(repo.duKienChuyen(coSo,source,target,newStart).stream()
                .filter(x->x.ketQua().startsWith("Không đủ chỗ cho cả đợt")).count()>=2);
            assertThrows(SQLException.class,()->repo.chuyenNamHoc(coSo,finalSource,finalWrong,transfer,"Sai khối trên TEST"));
            assertThrows(SQLException.class,()->repo.chuyenNamHoc(coSo,finalSource,finalTarget,transfer,"Vượt sĩ số trên TEST"));
            assertEquals(0,count(ketNoi,"SELECT COUNT(*) FROM dbo.XepLopMamNon WHERE MaLop=?",target));
            int secondPlacement;
            try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement("SELECT MaXepLop FROM dbo.XepLopMamNon WHERE MaHocVien=? AND MaLop=?")) {
                p.setInt(1,student2);p.setInt(2,source);try(ResultSet r=p.executeQuery()){assertTrue(r.next());secondPlacement=r.getInt(1);}
            }
            repo.ketThucXepLop(coSo,secondPlacement,oldStart.plusDays(5),"NGHI_HAN","Nghỉ thử trên TEST");
            assertEquals(1,repo.duKienChuyen(coSo,source,target,newStart).stream()
                .filter(x->"Dự kiến chuyển".equals(x.ketQua())).count());
            var result=repo.chuyenNamHoc(coSo,source,target,newStart,"Chuyển năm học trên TEST");
            assertEquals(1,result.daChuyen());assertEquals(1,result.boQuaNghi());
            assertEquals(1,count(ketNoi,"SELECT COUNT(*) FROM dbo.XepLopMamNon WHERE MaLop=? AND TrangThai='HIEU_LUC'",target));
            assertEquals(1,count(ketNoi,"SELECT COUNT(*) FROM dbo.XepLopMamNon WHERE MaLop=? AND TrangThai='CHUYEN_LOP' AND DenNgay=?",source,newStart.minusDays(1)));
        } finally {
            try(Connection c=ketNoi.mo()){
                c.setAutoCommit(false);
                try{
                    if(student1>0)update(c,"DELETE dbo.XepLopMamNon WHERE MaHocVien=?",student1);
                    if(student2>0)update(c,"DELETE dbo.XepLopMamNon WHERE MaHocVien=?",student2);
                    if(student1>0)update(c,"DELETE dbo.HocVien WHERE MaHocVien=?",student1);
                    if(student2>0)update(c,"DELETE dbo.HocVien WHERE MaHocVien=?",student2);
                    if(source>0)update(c,"DELETE dbo.LopMamNon WHERE MaLop=?",source);
                    if(target>0)update(c,"DELETE dbo.LopMamNon WHERE MaLop=?",target);
                    if(wrong>0)update(c,"DELETE dbo.LopMamNon WHERE MaLop=?",wrong);
                    c.commit();
                }catch(Exception ex){c.rollback();throw ex;}
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named="vinabacus.gd8.sqltest",matches="true")
    void siSoDiemDanhTienAnVaRollback() throws Exception {
        MamNonRepository.KetNoi ketNoi=ketNoiTest();
        MamNonRepository repo=new MamNonRepository(ketNoi);
        assertTrue(repo.coCauTrucLop(),"Migration Giai đoạn 8 chưa có trên TEST");
        int coSo=1,nam=0,taiKhoan=0,lop=0,hs1=0,hs2=0,xep=0,thang=0,gia=0,hocPhiRate=0,bang=0;
        LocalDate tu=null;
        String tag="[GD8_TEST_"+UUID.randomUUID().toString().substring(0,8)+"]";
        try(Connection c=ketNoi.mo()) {
            try(PreparedStatement p=c.prepareStatement("SELECT TOP (1) MaNamHoc,TuNgay FROM dbo.NamHoc WHERE MaCoSo=? ORDER BY TuNgay DESC")) {
                p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){assertTrue(r.next());nam=r.getInt(1);tu=r.getDate(2).toLocalDate();}
            }
            try(PreparedStatement p=c.prepareStatement("SELECT TOP (1) cs.MaTaiKhoan FROM dbo.TaiKhoanCoSo cs JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=cs.MaTaiKhoan WHERE cs.MaCoSo=? AND cs.TrangThai=1 AND t.TrangThai=1")) {
                p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){assertTrue(r.next());taiKhoan=r.getInt(1);}
            }
        }
        LocalDate namStart=tu,ngay=tu.plusDays(10),dauThang=ngay.withDayOfMonth(1);
        try {
            int finalCoSo=coSo;
            assertThrows(SQLException.class,()->repo.luuNamHoc(finalCoSo,0,tag,namStart,namStart.plusMonths(1)));
            lop=repo.luuLop(coSo,0,nam,tag,true,"MAM",1);
            try(Connection c=ketNoi.mo()) {
                hs1=insert(c,"INSERT dbo.HocVien(MaCoSo,HoTen,TrangThaiHoSo) VALUES(?,?,'HOAT_DONG')",coSo,tag+" A");
                hs2=insert(c,"INSERT dbo.HocVien(MaCoSo,HoTen,TrangThaiHoSo) VALUES(?,?,'HOAT_DONG')",coSo,tag+" B");
            }
            repo.xepHocSinh(coSo,lop,hs1,ngay);
            try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement("SELECT MaXepLop FROM dbo.XepLopMamNon WHERE MaHocVien=? AND MaLop=?")) {
                p.setInt(1,hs1);p.setInt(2,lop);try(ResultSet r=p.executeQuery()){assertTrue(r.next());xep=r.getInt(1);}
            }
            int finalLop=lop,finalHs2=hs2;
            assertThrows(SQLException.class,()->repo.xepHocSinh(finalCoSo,finalLop,finalHs2,ngay));
            int finalNam=nam;
            assertThrows(SQLException.class,()->repo.luuLop(coSo,finalLop,finalNam,tag,true,"CHOI",1),
                "Không đổi khối lớp đã có lịch sử học sinh");
            repo.luuLop(coSo,lop,nam,tag,true,"MAM",2);
            assertEquals(2,repo.lop(coSo,nam,tag).get(0).siSoToiDa());
            repo.luuLop(coSo,lop,nam,tag,true,"MAM",1);
            List<NgayHoatDong> calendar=new java.util.ArrayList<>();
            for(int d=1;d<=YearMonth.from(ngay).lengthOfMonth();d++){
                LocalDate day=YearMonth.from(ngay).atDay(d);
                boolean studying=!day.isBefore(ngay.minusDays(1))&&!day.isAfter(ngay.plusDays(1));
                calendar.add(new NgayHoatDong(day,studying,studying));
            }
            repo.luuLichHoatDong(coSo,nam,YearMonth.from(ngay),calendar);
            assertEquals(calendar,repo.lichHoatDong(coSo,nam,YearMonth.from(ngay)),
                "Lịch lưu và tải lại phải giữ đúng từng ngày học/ăn/nghỉ");
            YearMonth policyMonth=YearMonth.from(ngay);
            assertEquals(0,repo.cauHinhTienAn(coSo,policyMonth).id());
            repo.luuCauHinhTienAn(coSo,policyMonth,true,false,true,"Chính sách TEST");
            assertTrue(repo.cauHinhTienAn(coSo,policyMonth).id()>0);
            assertEquals(policyMonth.atDay(1),repo.cauHinhTienAn(coSo,policyMonth).thangBatDau());
            assertEquals(3,repo.soNgayHocChuan(coSo,YearMonth.from(ngay)));
            thang=repo.taoThang(coSo,YearMonth.from(ngay),3);
            try(Connection c=ketNoi.mo()) {
                gia=insert(c,"INSERT dbo.DonGiaTienAn(MaLop,ThangBatDau,DonGiaNgay,MaTaiKhoanThietLap) VALUES(?,?,?,?)",lop,dauThang,new BigDecimal("35000.00"),taiKhoan);
                hocPhiRate=insert(c,"INSERT dbo.MucHocPhiLop(MaLop,ThangBatDau,HocPhiThang,MaTaiKhoanThietLap) VALUES(?,?,?,?)",lop,dauThang,new BigDecimal("1500000.00"),taiKhoan);
            }
            bang=repo.lapBangTinhNhap(coSo,hs1,YearMonth.from(ngay));
            assertEquals(new BigDecimal("1000000.00"),repo.bangTinhThang(coSo,nam,YearMonth.from(ngay)).get(0).hocPhi());
            assertThrows(SQLException.class,()->repo.luuCauHinhTienAn(coSo,policyMonth,false,true,false,"Không sửa sau bảng phí"));
            assertEquals(BigDecimal.ZERO.setScale(2),tienAn(ketNoi,bang));
            int finalHs1ForDuplicate=hs1;
            assertThrows(SQLException.class,()->repo.lapBangTinhNhap(finalCoSo,finalHs1ForDuplicate,YearMonth.from(ngay)));
            repo.luuDiemDanh(coSo,lop,ngay,taiKhoan,null,List.of(new ThayDoiDiemDanh(hs1,xep,"CO_MAT","")));
            assertEquals(new BigDecimal("35000.00"),tienAn(ketNoi,bang));
            int finalBangForEarlyClose=bang;
            assertThrows(SQLException.class,()->repo.chotBangTinh(finalCoSo,finalBangForEarlyClose));
            repo.luuDiemDanh(coSo,lop,ngay.plusDays(1),taiKhoan,null,List.of(new ThayDoiDiemDanh(hs1,xep,"VANG_KHONG_PHEP","")));
            assertEquals(new BigDecimal("70000.00"),tienAn(ketNoi,bang));
            repo.luuDiemDanh(coSo,lop,ngay.plusDays(1),taiKhoan,"Sửa trên TEST",List.of(new ThayDoiDiemDanh(hs1,xep,"VANG_CO_PHEP","")));
            assertEquals(new BigDecimal("35000.00"),tienAn(ketNoi,bang));
            repo.luuDiemDanh(coSo,lop,ngay.plusDays(1),taiKhoan,"Chưa xác nhận trên TEST",List.of(new ThayDoiDiemDanh(hs1,xep,"CHUA_DIEM_DANH","")));
            assertEquals(new BigDecimal("35000.00"),tienAn(ketNoi,bang));
            repo.luuDiemDanh(coSo,lop,ngay.plusDays(1),taiKhoan,"Xác nhận trên TEST",List.of(new ThayDoiDiemDanh(hs1,xep,"VANG_KHONG_PHEP","")));
            repo.chotBangTinh(coSo,bang);
            int finalHs1=hs1,finalXep=xep,finalTk=taiKhoan;
            assertThrows(SQLException.class,()->repo.luuDiemDanh(finalCoSo,finalLop,ngay,finalTk,"Không được sửa",List.of(new ThayDoiDiemDanh(finalHs1,finalXep,"VANG_CO_PHEP",""))));
            assertEquals("CO_MAT",trangThai(ketNoi,hs1,ngay));
        } finally {
            try(Connection c=ketNoi.mo()) {
                c.setAutoCommit(false);
                try {
                    if(hs1>0) {
                        update(c,"DELETE ls FROM dbo.LichSuSuaDiemDanhMamNon ls JOIN dbo.DiemDanhMamNon d ON d.MaDiemDanh=ls.MaDiemDanh WHERE d.MaHocVien=?",hs1);
                        update(c,"DELETE dbo.DiemDanhMamNon WHERE MaHocVien=?",hs1);
                    }
                    if(bang>0){update(c,"DELETE dbo.ChiTietTienAnMamNon WHERE MaBangTinh=?",bang);update(c,"DELETE dbo.ChiTietHocPhiMamNon WHERE MaBangTinh=?",bang);update(c,"DELETE dbo.BangTinhPhiMamNon WHERE MaBangTinh=?",bang);}
                    if(gia>0)update(c,"DELETE dbo.DonGiaTienAn WHERE MaDonGiaTienAn=?",gia);
                    if(hocPhiRate>0)update(c,"DELETE dbo.MucHocPhiLop WHERE MaMucHocPhi=?",hocPhiRate);
                    if(thang>0)update(c,"DELETE dbo.ThangMamNon WHERE MaThang=?",thang);
                    update(c,"DELETE dbo.NgayHoatDongMamNon WHERE MaCoSo=? AND Ngay>=? AND Ngay<?",coSo,dauThang,dauThang.plusMonths(1));
                    update(c,"DELETE dbo.ChinhSachTienAnMamNon WHERE MaCoSo=? AND ThangBatDau=?",coSo,dauThang);
                    if(xep>0)update(c,"DELETE dbo.XepLopMamNon WHERE MaXepLop=?",xep);
                    if(hs1>0)update(c,"DELETE dbo.HocVien WHERE MaHocVien=?",hs1);
                    if(hs2>0)update(c,"DELETE dbo.HocVien WHERE MaHocVien=?",hs2);
                    if(lop>0)update(c,"DELETE dbo.LopMamNon WHERE MaLop=?",lop);
                    c.commit();
                } catch(Exception ex){c.rollback();throw ex;}
            }
        }
    }
    private static int insert(Connection c,String sql,Object... args)throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            bind(p,args);assertEquals(1,p.executeUpdate());try(ResultSet r=p.getGeneratedKeys()){assertTrue(r.next());return r.getInt(1);}
        }
    }
    private static void update(Connection c,String sql,Object... args)throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)){bind(p,args);p.executeUpdate();}
    }
    private static void bind(PreparedStatement p,Object... args)throws SQLException {
        for(int i=0;i<args.length;i++){
            Object value=args[i];if(value instanceof LocalDate date)p.setDate(i+1,Date.valueOf(date));
            else p.setObject(i+1,value);
        }
    }
    private static BigDecimal tienAn(MamNonRepository.KetNoi k,int bang)throws SQLException {
        try(Connection c=k.mo();PreparedStatement p=c.prepareStatement("SELECT TongTienAn FROM dbo.BangTinhPhiMamNon WHERE MaBangTinh=?")) {
            p.setInt(1,bang);try(ResultSet r=p.executeQuery()){assertTrue(r.next());return r.getBigDecimal(1);}
        }
    }
    private static String trangThai(MamNonRepository.KetNoi k,int hs,LocalDate day)throws SQLException {
        try(Connection c=k.mo();PreparedStatement p=c.prepareStatement("SELECT TrangThai FROM dbo.DiemDanhMamNon WHERE MaHocVien=? AND NgayHoc=?")) {
            p.setInt(1,hs);p.setDate(2,Date.valueOf(day));try(ResultSet r=p.executeQuery()){assertTrue(r.next());return r.getString(1);}
        }
    }
    private static int count(MamNonRepository.KetNoi k,String sql,Object... args)throws SQLException {
        try(Connection c=k.mo();PreparedStatement p=c.prepareStatement(sql)){
            bind(p,args);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    private static MamNonRepository.KetNoi ketNoiTest()throws Exception {
        Properties p=new Properties();try(InputStream in=Files.newInputStream(Path.of("db.local.properties"))){p.load(in);}
        String source=p.getProperty("db.url","");
        if(!source.matches("(?is).*databaseName=VINABACUS(?:;.*)?"))throw new IllegalStateException("Cấu hình gốc phải trỏ VINABACUS");
        String url=source.replaceFirst("(?i)(databaseName=)VINABACUS(?=;|$)","$1"+TEST_DB);
        return ()->{
            Connection c=DriverManager.getConnection(url,p.getProperty("db.user",""),p.getProperty("db.password",""));
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT DB_NAME()")){
                r.next();if(!TEST_DB.equals(r.getString(1)))throw new SQLException("Từ chối ghi ngoài TEST");
            }catch(SQLException ex){c.close();throw ex;}
            return c;
        };
    }
}
