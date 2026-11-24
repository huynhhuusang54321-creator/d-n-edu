package vn.vinabacus.repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.*;
import vn.vinabacus.service.HocPhiMamNonTheoNgay;

/** SQL Server repository TV4; mọi truy vấn đều giới hạn MaCoSo thuộc Mầm non. */
public final class MamNonRepository {
    @FunctionalInterface public interface KetNoi { Connection mo() throws SQLException; }
    private final KetNoi ketNoi;
    public MamNonRepository(){this(Database::moKetNoi);}
    public MamNonRepository(KetNoi ketNoi){this.ketNoi=Objects.requireNonNull(ketNoi);}
    public record LuaChon(int id, String ten) { @Override public String toString(){return ten;} }
    public record ThayDoiDiemDanh(int maHocVien, int maXepLop, String trangThai, String ghiChu) {}
    public record LichSuDiemDanh(int maHocVien, LocalDate ngayHoc, String hoTen, String tenLop,
        String trangThai, String ghiChu, String nguoiGhi, String ngayCapNhat) {}
    public record NhatKySua(String ngaySua, String truoc, String sau, String lyDo, String taiKhoan) {}
    public record BangTinhThang(int id,int maHocVien,String hocSinh,String lop,YearMonth ky,
        BigDecimal hocPhi,BigDecimal tienAn,String trangThai,int soNgayChuan,int soNgayCanDiemDanh,int soNgayDaDiemDanh) {}
    public record NgayHoatDong(LocalDate ngay,Boolean coHoc,Boolean coAn) {}
    /** id=0 và thangBatDau=null: đang dùng quy tắc mặc định đã duyệt, chưa lưu SQL. */
    public record CauHinhTienAn(int id,LocalDate thangBatDau,boolean coMat,
        boolean vangCoPhep,boolean vangKhongPhep,String ghiChu) {}

    private static void date(PreparedStatement p, int col, LocalDate value) throws SQLException {
        if(value==null) p.setNull(col, Types.DATE); else p.setDate(col, java.sql.Date.valueOf(value));
    }
    private static LocalDate day(ResultSet rs,String col) throws SQLException {
        java.sql.Date d=rs.getDate(col);return d==null?null:d.toLocalDate();
    }
    private static SQLException khongTimThay(){return new SQLException("Bản ghi không tồn tại tại cơ sở Mầm non hoặc đã được thay đổi.");}
    private static boolean coCauTrucLop(Connection c)throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery(
            "SELECT CASE WHEN COL_LENGTH('dbo.LopMamNon','Khoi') IS NOT NULL AND COL_LENGTH('dbo.LopMamNon','SiSoToiDa') IS NOT NULL THEN 1 ELSE 0 END")) {
            r.next();return r.getInt(1)==1;
        }
    }
    public boolean coCauTrucLop()throws SQLException {
        try(Connection c=ketNoi.mo()){return coCauTrucLop(c);}
    }

    public List<NamHocMamNon> namHoc(int coSo) throws SQLException {
        List<NamHocMamNon> out=new ArrayList<>();
        try(Connection c=ketNoi.mo(); PreparedStatement p=c.prepareStatement(
            "SELECT MaNamHoc,MaCoSo,TenNamHoc,TuNgay,DenNgay FROM dbo.NamHoc WHERE MaCoSo=? ORDER BY TuNgay DESC,MaNamHoc DESC")) {
            p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new NamHocMamNon(r.getInt(1),r.getInt(2),r.getString(3),day(r,"TuNgay"),day(r,"DenNgay")));}
        } return out;
    }
    public int luuNamHoc(int coSo,int id,String ten,LocalDate tu,LocalDate den) throws SQLException {
        String sql=id==0?"INSERT dbo.NamHoc(MaCoSo,TenNamHoc,TuNgay,DenNgay) VALUES(?,?,?,?)":
            "UPDATE dbo.NamHoc SET TenNamHoc=?,TuNgay=?,DenNgay=? WHERE MaNamHoc=? AND MaCoSo=?";
        try(Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM dbo.NamHoc WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND MaNamHoc<>? AND TuNgay<=? AND DenNgay>=?")) {
                    check.setInt(1,coSo);check.setInt(2,id);date(check,3,den);date(check,4,tu);
                    try(ResultSet r=check.executeQuery()){if(r.next())throw new SQLException("Khoảng ngày trùng với năm học khác của cơ sở Mầm non.");}
                }
                if(id!=0) {
                    try(PreparedStatement check=c.prepareStatement(
                        "SELECT 1 FROM dbo.XepLopMamNon x JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo " +
                        "WHERE l.MaNamHoc=? AND l.MaCoSo=? AND (x.TuNgay<? OR (x.DenNgay IS NOT NULL AND x.DenNgay>?))")) {
                        check.setInt(1,id);check.setInt(2,coSo);date(check,3,tu);date(check,4,den);
                        try(ResultSet r=check.executeQuery()){if(r.next())throw new SQLException("Không thể thu hẹp năm học vì lịch sử xếp lớp nằm ngoài khoảng ngày mới.");}
                    }
                    try(PreparedStatement check=c.prepareStatement(
                        "SELECT 1 FROM dbo.DiemDanhMamNon d JOIN dbo.XepLopMamNon x ON x.MaXepLop=d.MaXepLop AND x.MaCoSo=d.MaCoSo " +
                        "JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo WHERE l.MaNamHoc=? AND l.MaCoSo=? AND (d.NgayHoc<? OR d.NgayHoc>?)")) {
                        check.setInt(1,id);check.setInt(2,coSo);date(check,3,tu);date(check,4,den);
                        try(ResultSet r=check.executeQuery()){if(r.next())throw new SQLException("Không thể thu hẹp năm học vì có điểm danh ngoài khoảng ngày mới.");}
                    }
                }
                int result;
                try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
                    if(id==0){p.setInt(1,coSo);p.setString(2,ten);date(p,3,tu);date(p,4,den);}
                    else {p.setString(1,ten);date(p,2,tu);date(p,3,den);p.setInt(4,id);p.setInt(5,coSo);}
                    if(p.executeUpdate()!=1)throw khongTimThay();
                    result=id;
                    if(id==0){try(ResultSet k=p.getGeneratedKeys()){if(k.next())result=k.getInt(1);else throw new SQLException("Không lấy được mã năm học");}}
                }
                c.commit();return result;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public List<LopMamNon> lop(int coSo,Integer nam,String tuKhoa) throws SQLException {
        List<LopMamNon> out=new ArrayList<>();
        try(Connection c=ketNoi.mo()) {
            boolean cauTruc=coCauTrucLop(c);
            String sql="SELECT l.MaLop,l.MaNamHoc,l.MaCoSo,l.TenLop,l.TrangThai,n.TenNamHoc,"+
                (cauTruc?"l.Khoi,l.SiSoToiDa":"CAST(NULL AS VARCHAR(5)) AS Khoi,CAST(NULL AS INT) AS SiSoToiDa")+
                ",(SELECT COUNT(*) FROM dbo.XepLopMamNon x WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo AND x.TrangThai='HIEU_LUC' AND x.DenNgay IS NULL) AS SiSo " +
                "FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo WHERE l.MaCoSo=?"+
                (nam==null?"":" AND l.MaNamHoc=?")+" AND l.TenLop LIKE ? ORDER BY n.TuNgay DESC,l.TenLop";
            try(PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,coSo);int i=2;if(nam!=null)p.setInt(i++,nam);p.setString(i,"%"+(tuKhoa==null?"":tuKhoa.trim())+"%");
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LopMamNon(r.getInt(1),r.getInt(2),r.getInt(3),r.getString(4),r.getBoolean(5),r.getString(6),r.getString(7),(Integer)r.getObject(8),r.getInt(9)));}
            }
        }return out;
    }
    public int luuLop(int coSo,int id,int nam,String ten,boolean hoatDong,String khoi,Integer siSoToiDa) throws SQLException {
        String sql=id==0?"INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop,TrangThai,Khoi,SiSoToiDa) SELECT MaNamHoc,MaCoSo,?,?,?,? FROM dbo.NamHoc WHERE MaNamHoc=? AND MaCoSo=?":
            "UPDATE l SET MaNamHoc=?,TenLop=?,TrangThai=?,Khoi=?,SiSoToiDa=? FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=? AND n.MaCoSo=l.MaCoSo WHERE l.MaLop=? AND l.MaCoSo=?";
        try(Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if(!coCauTrucLop(c))throw new SQLException("CSDL chưa có Khoi và SiSoToiDa; cần migration Giai đoạn 8 trước khi thêm/sửa lớp.");
                if(siSoToiDa!=null&&siSoToiDa<=0)throw new SQLException("Sĩ số tối đa phải lớn hơn 0.");
                if(id!=0){
                    String oldGrade;
                    try(PreparedStatement lock=c.prepareStatement("SELECT Khoi FROM dbo.LopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaLop=? AND MaCoSo=?")){
                        lock.setInt(1,id);lock.setInt(2,coSo);
                        try(ResultSet r=lock.executeQuery()){if(!r.next())throw khongTimThay();oldGrade=r.getString(1);}
                    }
                    boolean hasHistory=exists(c,"SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaLop=? AND MaCoSo=?",id,coSo);
                    if(hasHistory&&oldGrade!=null&&!oldGrade.equals(khoi))
                        throw new SQLException("Lớp đã có lịch sử học sinh; không đổi hoặc xóa khối đã xác nhận.");
                    if(hasHistory&&exists(c,"SELECT 1 FROM dbo.LopMamNon WHERE MaLop=? AND MaCoSo=? AND MaNamHoc<>?",id,coSo,nam))
                        throw new SQLException("Không được đổi năm học khi lớp đã có lịch sử xếp học sinh.");
                    if(siSoToiDa!=null&&exists(c,"SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaLop=? AND MaCoSo=? AND TrangThai='HIEU_LUC' AND DenNgay IS NULL GROUP BY MaLop HAVING COUNT(*)>?",id,coSo,siSoToiDa))
                        throw new SQLException("Sĩ số tối đa nhỏ hơn số học sinh đang học.");
                }
                int result=id;
                try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
                    if(id==0){p.setString(1,ten);p.setBoolean(2,hoatDong);p.setString(3,khoi);if(siSoToiDa==null)p.setNull(4,Types.INTEGER);else p.setInt(4,siSoToiDa);p.setInt(5,nam);p.setInt(6,coSo);}
                    else {p.setInt(1,nam);p.setString(2,ten);p.setBoolean(3,hoatDong);p.setString(4,khoi);if(siSoToiDa==null)p.setNull(5,Types.INTEGER);else p.setInt(5,siSoToiDa);p.setInt(6,nam);p.setInt(7,id);p.setInt(8,coSo);}
                    if(p.executeUpdate()!=1)throw khongTimThay();
                    if(id==0){try(ResultSet k=p.getGeneratedKeys()){if(k.next())result=k.getInt(1);else throw new SQLException("Không lấy được mã lớp");}}
                }
                c.commit();return result;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public List<LuaChon> hocSinh(int coSo,String tim) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(
            "SELECT MaHocVien,HoTen FROM dbo.HocVien WHERE MaCoSo=? AND TrangThaiHoSo='HOAT_DONG' AND (HoTen LIKE ? OR CAST(MaHocVien AS VARCHAR(20)) LIKE ?) ORDER BY HoTen")) {
            p.setInt(1,coSo);p.setString(2,"%"+tim+"%");p.setString(3,"%"+tim+"%");
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LuaChon(r.getInt(1),"#"+r.getInt(1)+" · "+r.getString(2)));}
        }return out;
    }
    public List<LuaChon> giaoVien(int coSo) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(
            "SELECT n.MaNhanVien,n.HoTen FROM dbo.NhanVien n JOIN dbo.NhanVienCoSo cs ON cs.MaNhanVien=n.MaNhanVien WHERE cs.MaCoSo=? AND cs.TrangThai=1 AND n.TrangThai=1 ORDER BY n.HoTen")) {
            p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LuaChon(r.getInt(1),r.getString(2)));}
        }return out;
    }
    public List<LuaChon> giaoVienCuaLop(int coSo,int maLop) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(
            "SELECT pc.MaPhanCong,n.HoTen+N' ('+CONVERT(NVARCHAR(10),pc.TuNgay,103)+N' - '+ISNULL(CONVERT(NVARCHAR(10),pc.DenNgay,103),N'đang dạy')+N')' FROM dbo.PhanCongGiaoVienMamNon pc JOIN dbo.NhanVien n ON n.MaNhanVien=pc.MaNhanVien WHERE pc.MaCoSo=? AND pc.MaLop=? ORDER BY pc.TuNgay DESC")) {
            p.setInt(1,coSo);p.setInt(2,maLop);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LuaChon(r.getInt(1),r.getString(2)));}
        }return out;
    }
    public void phanCongGiaoVien(int coSo,int lop,int giaoVien,LocalDate tu,LocalDate den) throws SQLException {
        try(Connection c=ketNoi.mo()){
            c.setAutoCommit(false);
            try{
                if(!exists(c,"SELECT 1 FROM dbo.LopMamNon WHERE MaLop=? AND MaCoSo=? AND TrangThai=1",lop,coSo) ||
                   !exists(c,"SELECT 1 FROM dbo.NhanVienCoSo WHERE MaNhanVien=? AND MaCoSo=? AND TrangThai=1",giaoVien,coSo))
                   throw new SQLException("Lớp hoặc giáo viên không hợp lệ tại cơ sở Mầm non.");
                try(PreparedStatement p=c.prepareStatement("INSERT dbo.PhanCongGiaoVienMamNon(MaLop,MaCoSo,MaNhanVien,TuNgay,DenNgay) VALUES(?,?,?,?,?)")){
                    p.setInt(1,lop);p.setInt(2,coSo);p.setInt(3,giaoVien);date(p,4,tu);date(p,5,den);p.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public List<XepLopMamNon> xepLop(int coSo,Integer lop) throws SQLException {
        String sql="SELECT x.MaXepLop,x.MaHocVien,x.MaCoSo,x.MaLop,h.HoTen,l.TenLop,x.TuNgay,x.DenNgay,x.TrangThai,x.LyDo "+
            "FROM dbo.XepLopMamNon x JOIN dbo.HocVien h ON h.MaHocVien=x.MaHocVien AND h.MaCoSo=x.MaCoSo JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo "+
            "WHERE x.MaCoSo=?"+(lop==null?"":" AND x.MaLop=?")+" ORDER BY x.TuNgay DESC,x.MaXepLop DESC";
        List<XepLopMamNon> out=new ArrayList<>();
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,coSo);if(lop!=null)p.setInt(2,lop);
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new XepLopMamNon(r.getInt(1),r.getInt(2),r.getInt(3),r.getInt(4),r.getString(5),r.getString(6),day(r,"TuNgay"),day(r,"DenNgay"),r.getString(9),r.getString(10)));}
        }return out;
    }
    private boolean exists(Connection c,String sql,int... args)throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setInt(i+1,args[i]);try(ResultSet r=p.executeQuery()){return r.next();}}
    }
    private void kiemTraChoTrong(Connection c,int coSo,int lop,LocalDate ngay)throws SQLException {
        if(!coCauTrucLop(c))throw new SQLException("CSDL chưa có sĩ số tối đa lớp Mầm non; cần duyệt migration Giai đoạn 8 trước khi xếp thêm học sinh.");
        try(PreparedStatement p=c.prepareStatement("SELECT SiSoToiDa FROM dbo.LopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaLop=? AND MaCoSo=? AND TrangThai=1")) {
            p.setInt(1,lop);p.setInt(2,coSo);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new SQLException("Lớp đích đã ngừng hoạt động hoặc không thuộc cơ sở Mầm non.");
                int limit=r.getInt(1);
                if(r.wasNull()||limit<=0)throw new SQLException("Lớp chưa có sĩ số tối đa hợp lệ, không thể nhận thêm học sinh.");
                try(PreparedStatement count=c.prepareStatement("SELECT COUNT(*) FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaLop=? AND MaCoSo=? AND TrangThai='HIEU_LUC' AND TuNgay<=? AND (DenNgay IS NULL OR DenNgay>=?)")) {
                    count.setInt(1,lop);count.setInt(2,coSo);date(count,3,ngay);date(count,4,ngay);
                    try(ResultSet n=count.executeQuery()){n.next();if(n.getInt(1)>=limit)throw new SQLException("Lớp đích đã đạt sĩ số tối đa "+limit+" học sinh.");}
                }
            }
        }
    }
    public void xepHocSinh(int coSo,int lop,int hocSinh,LocalDate tu) throws SQLException {
        try(Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                // Khóa theo học sinh để tránh hai thao tác xếp lớp đồng thời.
                if(!exists(c,"SELECT 1 FROM dbo.HocVien WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=? AND TrangThaiHoSo='HOAT_DONG'",hocSinh,coSo))throw new SQLException("Không tìm thấy học sinh còn hoạt động ở cơ sở Mầm non.");
                kiemTraChuaTinhPhiTu(c,coSo,hocSinh,tu);
                try(PreparedStatement p=c.prepareStatement("SELECT n.TuNgay,n.DenNgay FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo WHERE l.MaLop=? AND l.MaCoSo=? AND l.TrangThai=1")){
                    p.setInt(1,lop);p.setInt(2,coSo);try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new SQLException("Lớp không tồn tại hoặc đã ngừng hoạt động.");
                        if(tu.isBefore(day(r,"TuNgay"))||tu.isAfter(day(r,"DenNgay")))throw new SQLException("Ngày xếp lớp phải thuộc năm học của lớp.");
                    }
                }
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND MaHocVien=? AND TrangThai<>'HUY' AND (DenNgay IS NULL OR DenNgay>=?)")){
                    p.setInt(1,coSo);p.setInt(2,hocSinh);date(p,3,tu);try(ResultSet r=p.executeQuery()){if(r.next())throw new SQLException("Học sinh đã có lớp hiệu lực; hãy kết thúc/chuyển lớp cũ trước.");}
                }
                kiemTraChoTrong(c,coSo,lop,tu);
                try(PreparedStatement p=c.prepareStatement("INSERT dbo.XepLopMamNon(MaHocVien,MaCoSo,MaLop,TuNgay,TrangThai) VALUES(?,?,?,?,'HIEU_LUC')")){
                    p.setInt(1,hocSinh);p.setInt(2,coSo);p.setInt(3,lop);date(p,4,tu);p.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public void ketThucXepLop(int coSo,int id,LocalDate den,String trangThai,String lyDo) throws SQLException {
        if(!Set.of("CHUYEN_LOP","NGHI_HAN","HUY").contains(trangThai))throw new SQLException("Trạng thái xếp lớp không hợp lệ.");
        try(Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                int hocSinh;
                try(PreparedStatement lookup=c.prepareStatement("SELECT MaHocVien FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaXepLop=? AND MaCoSo=?")){
                    lookup.setInt(1,id);lookup.setInt(2,coSo);
                    try(ResultSet r=lookup.executeQuery()){if(!r.next())throw khongTimThay();hocSinh=r.getInt(1);}
                }
                kiemTraChuaTinhPhiTu(c,coSo,hocSinh,den);
                try(PreparedStatement p=c.prepareStatement(
                "UPDATE x SET DenNgay=?,TrangThai=?,LyDo=? FROM dbo.XepLopMamNon x WHERE x.MaXepLop=? AND x.MaCoSo=? AND x.TrangThai='HIEU_LUC' AND x.TuNgay<=? " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.DiemDanhMamNon d WITH (UPDLOCK,HOLDLOCK) WHERE d.MaXepLop=x.MaXepLop AND d.MaCoSo=x.MaCoSo AND (d.NgayHoc>? OR ?='HUY'))")) {
                date(p,1,den);p.setString(2,trangThai);p.setString(3,lyDo);p.setInt(4,id);p.setInt(5,coSo);date(p,6,den);
                date(p,7,den);p.setString(8,trangThai);
                if(p.executeUpdate()!=1)throw new SQLException("Không thể kết thúc xếp lớp; kiểm tra trạng thái, ngày kết thúc và điểm danh đã ghi.");
                }
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    /** Kết quả tạo lớp từ năm học cũ. Chỉ tạo lớp, không chuyển học sinh. */
    public record KetQuaSaoChep(int daTao, int boQua) {}
    /** Kết quả chuyển học sinh; bỏ qua hồ sơ nghỉ/ngừng và hồ sơ đã được xếp ở nơi khác. */
    public record KetQuaChuyen(int daChuyen, int boQuaNghi, int boQuaTrung) {}

    private record LopNam(int nam, boolean active, LocalDate tu, LocalDate den, String khoi) {}
    private LopNam docLopNam(Connection c,int coSo,int lop,boolean lock)throws SQLException {
        if(!coCauTrucLop(c))throw new SQLException("CSDL chưa có khối và sĩ số tối đa; cần duyệt migration Giai đoạn 8 trước khi chuyển lớp.");
        String sql="SELECT l.MaNamHoc,l.TrangThai,n.TuNgay,n.DenNgay,l.Khoi FROM dbo.LopMamNon l " +
            "JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo " +
            "WHERE l.MaCoSo=? AND l.MaLop=?";
        if(lock)sql=sql.replace("dbo.LopMamNon l", "dbo.LopMamNon l WITH (UPDLOCK,HOLDLOCK)");
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,coSo);ps.setInt(2,lop);
            try(ResultSet rs=ps.executeQuery()){
                if(!rs.next())throw new SQLException("Lớp không tồn tại trong cơ sở Mầm non.");
                return new LopNam(rs.getInt(1),rs.getBoolean(2),day(rs,"TuNgay"),day(rs,"DenNgay"),rs.getString("Khoi"));
            }
        }
    }
    private void khoaHocSinh(Connection c,int coSo,int hocSinh)throws SQLException {
        if(!exists(c,"SELECT 1 FROM dbo.HocVien WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=?",hocSinh,coSo))
            throw new SQLException("Không tìm thấy học sinh của cơ sở Mầm non.");
    }
    private void kiemTraChuaTinhPhiTu(Connection c,int coSo,int hocSinh,LocalDate ngay)throws SQLException {
        String sql="SELECT 1 FROM dbo.BangTinhPhiMamNon b WITH (UPDLOCK,HOLDLOCK) " +
            "JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo " +
            "WHERE b.MaCoSo=? AND b.MaHocVien=? AND (t.Nam>? OR (t.Nam=? AND t.Thang>=?))";
        if(exists(c,sql,coSo,hocSinh,ngay.getYear(),ngay.getYear(),ngay.getMonthValue()))
            throw new SQLException("Đã có bảng tính phí từ tháng này; cần quy trình điều chỉnh có lịch sử trước khi thay đổi thời gian xếp lớp.");
    }
    private boolean coTrungLich(Connection c,int coSo,int hs,int oldXep,LocalDate tu,LocalDate den)throws SQLException {
        String sql="SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) " +
            "WHERE MaCoSo=? AND MaHocVien=? AND MaXepLop<>? AND TrangThai<>'HUY' " +
            "AND TuNgay<=? AND (DenNgay IS NULL OR DenNgay>=?)";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,coSo);ps.setInt(2,hs);ps.setInt(3,oldXep);
            date(ps,4,den);date(ps,5,tu);
            try(ResultSet rs=ps.executeQuery()){return rs.next();}
        }
    }
    private void kiemTraTrungLich(Connection c,int coSo,int hs,int oldXep,LocalDate tu,LocalDate den)throws SQLException {
        if(coTrungLich(c,coSo,hs,oldXep,tu,den))throw new SQLException("Học sinh đã có lịch xếp lớp chồng thời gian.");
    }
    private void kiemTraLoiTrinh(LopNam source,LopNam target)throws SQLException {
        if(source.nam()==target.nam()){
            if(source.khoi()==null||target.khoi()==null||!source.khoi().equals(target.khoi()))
                throw new SQLException("Chuyển lớp trong cùng năm học chỉ áp dụng trong cùng khối đã xác nhận.");
            return;
        }
        String expected=switch(source.khoi()==null?"":source.khoi()){
            case "MAM"->"CHOI";case "CHOI"->"LA";
            case "LA"->throw new SQLException("Học sinh khối Lá cần quy trình hoàn thành Mầm non; không tự chuyển sang khối khác.");
            default->throw new SQLException("Lớp nguồn chưa xác định khối; không thể tự suy đoán lộ trình chuyển năm.");
        };
        if(!expected.equals(target.khoi()))throw new SQLException("Lộ trình chuyển năm phải đúng Mầm → Chồi → Lá; lớp đích chưa đúng khối.");
    }
    /** Thao tác thủ công, một giao dịch để không tạo một phần danh sách lớp. */
    public KetQuaSaoChep saoChepLop(int coSo,int namNguon,int namDich,List<Integer> dsMaLop)throws SQLException {
        if(namNguon==namDich||dsMaLop==null||dsMaLop.isEmpty())throw new SQLException("Hãy chọn năm học mới và ít nhất một lớp.");
        try(Connection c=ketNoi.mo()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                LocalDate nguon=null,dich=null;
                try(PreparedStatement ps=c.prepareStatement("SELECT MaNamHoc,TuNgay FROM dbo.NamHoc WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND MaNamHoc IN (?,?)")){
                    ps.setInt(1,coSo);ps.setInt(2,namNguon);ps.setInt(3,namDich);
                    try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                        if(rs.getInt(1)==namNguon)nguon=day(rs,"TuNgay");
                        if(rs.getInt(1)==namDich)dich=day(rs,"TuNgay");
                    }}
                }
                if(nguon==null||dich==null||!dich.isAfter(nguon))throw new SQLException("Năm học đích phải mới hơn năm học nguồn.");
                int created=0,skipped=0;
                boolean cauTruc=coCauTrucLop(c);
                for(int id:new LinkedHashSet<>(dsMaLop)){
                    String ten,khoi=null;Integer limit=null;
                    try(PreparedStatement ps=c.prepareStatement("SELECT TenLop,"+(cauTruc?"Khoi,SiSoToiDa":"CAST(NULL AS VARCHAR(5)),CAST(NULL AS INT)")+" FROM dbo.LopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaLop=? AND MaCoSo=? AND MaNamHoc=?")){
                        ps.setInt(1,id);ps.setInt(2,coSo);ps.setInt(3,namNguon);
                        try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Lớp nguồn không còn hợp lệ: #"+id);ten=rs.getString(1);khoi=rs.getString(2);limit=(Integer)rs.getObject(3);}
                    }
                    if(existsName(c,coSo,namDich,ten)){skipped++;continue;}
                    String insert=cauTruc?"INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop,TrangThai,Khoi,SiSoToiDa) VALUES(?,?,?,1,?,?)":
                        "INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop,TrangThai) VALUES(?,?,?,1)";
                    try(PreparedStatement ps=c.prepareStatement(insert)){
                        ps.setInt(1,namDich);ps.setInt(2,coSo);ps.setString(3,ten);
                        if(cauTruc){ps.setString(4,khoi);if(limit==null)ps.setNull(5,Types.INTEGER);else ps.setInt(5,limit);}
                        created+=ps.executeUpdate();
                    }
                }
                c.commit();return new KetQuaSaoChep(created,skipped);
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    private boolean existsName(Connection c,int coSo,int nam,String ten)throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM dbo.LopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND MaNamHoc=? AND TenLop=?")){
            ps.setInt(1,coSo);ps.setInt(2,nam);ps.setString(3,ten);
            try(ResultSet rs=ps.executeQuery()){return rs.next();}
        }
    }
    private void chuyenMot(Connection c,int coSo,int xepId,int hs,int lopDich,LocalDate ngayBatDau,LocalDate ngayCuoi,String reason)throws SQLException {
        try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM dbo.DiemDanhMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaXepLop=? AND MaCoSo=? AND NgayHoc>?")) {
            check.setInt(1,xepId);check.setInt(2,coSo);date(check,3,ngayCuoi);
            try(ResultSet r=check.executeQuery()){if(r.next())throw new SQLException("Ngày chuyển sớm hơn điểm danh đã lưu của lớp cũ.");}
        }
        try(PreparedStatement ps=c.prepareStatement("UPDATE dbo.XepLopMamNon SET DenNgay=?,TrangThai='CHUYEN_LOP',LyDo=? WHERE MaXepLop=? AND MaHocVien=? AND MaCoSo=? AND TrangThai='HIEU_LUC' AND DenNgay IS NULL AND TuNgay<=?")){
            date(ps,1,ngayCuoi);ps.setString(2,reason);ps.setInt(3,xepId);ps.setInt(4,hs);ps.setInt(5,coSo);date(ps,6,ngayCuoi);
            if(ps.executeUpdate()!=1)throw new SQLException("Xếp lớp cũ đã thay đổi hoặc không còn hiệu lực.");
        }
        try(PreparedStatement ps=c.prepareStatement("INSERT dbo.XepLopMamNon(MaHocVien,MaCoSo,MaLop,TuNgay,TrangThai,LyDo) VALUES(?,?,?,?,'HIEU_LUC',?)")){
            ps.setInt(1,hs);ps.setInt(2,coSo);ps.setInt(3,lopDich);date(ps,4,ngayBatDau);ps.setString(5,reason);ps.executeUpdate();
        }
    }
    public record DuKienChuyen(int maXepLop,int maHocVien,String tenHocVien,String ketQua) {}
    /** Danh sách xem trước: bộ xử lý vẫn kiểm tra lại khi người dùng bấm xác nhận. */
    public List<DuKienChuyen> duKienChuyen(int coSo,int lopNguon,int lopDich,LocalDate ngayDich)throws SQLException {
        List<DuKienChuyen> out=new ArrayList<>();
        String sql="SELECT x.MaXepLop,x.MaHocVien,h.HoTen,x.TrangThai,h.TrangThaiHoSo, " +
            "CASE WHEN EXISTS(SELECT 1 FROM dbo.XepLopMamNon z WHERE z.MaCoSo=x.MaCoSo " +
            "AND z.MaHocVien=x.MaHocVien AND z.MaLop=? AND z.TrangThai<>'HUY') THEN 1 ELSE 0 END AS DaXep, x.DenNgay,x.TuNgay " +
            "FROM dbo.XepLopMamNon x JOIN dbo.HocVien h ON h.MaHocVien=x.MaHocVien AND h.MaCoSo=x.MaCoSo " +
            "WHERE x.MaCoSo=? AND x.MaLop=? ORDER BY x.MaHocVien,x.MaXepLop";
        try(Connection c=ketNoi.mo();PreparedStatement ps=c.prepareStatement(sql)){
            LopNam source=docLopNam(c,coSo,lopNguon,false),target=docLopNam(c,coSo,lopDich,false);
            kiemTraLoiTrinh(source,target);
            if(!target.active()||!target.tu().isAfter(source.tu())||ngayDich.isBefore(target.tu())||ngayDich.isAfter(target.den()))
                throw new SQLException("Lớp đích, năm học hoặc ngày chuyển không hợp lệ.");
            int available;
            try(PreparedStatement cap=c.prepareStatement("SELECT l.SiSoToiDa-(SELECT COUNT(*) FROM dbo.XepLopMamNon x WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo AND x.TrangThai='HIEU_LUC' AND x.TuNgay<=? AND (x.DenNgay IS NULL OR x.DenNgay>=?)) FROM dbo.LopMamNon l WHERE l.MaLop=? AND l.MaCoSo=? AND l.SiSoToiDa IS NOT NULL")){
                date(cap,1,ngayDich);date(cap,2,ngayDich);cap.setInt(3,lopDich);cap.setInt(4,coSo);
                try(ResultSet r=cap.executeQuery()){if(!r.next())throw new SQLException("Lớp đích chưa có sĩ số tối đa hợp lệ.");available=r.getInt(1);}
            }
            ps.setInt(1,lopDich);ps.setInt(2,coSo);ps.setInt(3,lopNguon);
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                String state=rs.getString(4),status;
                if(!"HOAT_DONG".equals(rs.getString(5))||!"HIEU_LUC".equals(state)||day(rs,"DenNgay")!=null)status="Bỏ qua (nghỉ/kết thúc)";
                else if(rs.getInt(6)==1)status="Bỏ qua (đã xếp lớp đích)";
                else if(!ngayDich.isAfter(day(rs,"TuNgay")))status="Bỏ qua (ngày chuyển chưa sau ngày vào lớp cũ)";
                else status="Dự kiến chuyển";
                out.add(new DuKienChuyen(rs.getInt(1),rs.getInt(2),rs.getString(3),status));
            }}
            long ready=out.stream().filter(x->"Dự kiến chuyển".equals(x.ketQua())).count();
            if(ready>available){
                List<DuKienChuyen> blocked=new ArrayList<>();
                for(DuKienChuyen x:out)blocked.add("Dự kiến chuyển".equals(x.ketQua())?
                    new DuKienChuyen(x.maXepLop(),x.maHocVien(),x.tenHocVien(),"Không đủ chỗ cho cả đợt (cần "+ready+", còn "+available+")"):x);
                return blocked;
            }
        }return out;
    }
    /** Chuyển một học sinh sang lớp đích (cùng năm hoặc năm sau) và giữ lại lớp cũ. */
    public void chuyenLop(int coSo,int maXep,int lopDich,LocalDate ngayDich,String reason)throws SQLException {
        try(Connection c=ketNoi.mo()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                int hs,lopNguon;LocalDate batDau;
                try(PreparedStatement ps=c.prepareStatement("SELECT MaHocVien,MaLop,TuNgay FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaXepLop=? AND MaCoSo=? AND TrangThai='HIEU_LUC' AND DenNgay IS NULL")){
                    ps.setInt(1,maXep);ps.setInt(2,coSo);
                    try(ResultSet rs=ps.executeQuery()){
                        if(!rs.next())throw new SQLException("Chỉ được chuyển hồ sơ xếp lớp đang hiệu lực.");
                        hs=rs.getInt(1);lopNguon=rs.getInt(2);batDau=day(rs,"TuNgay");
                    }
                }
                khoaHocSinh(c,coSo,hs);
                kiemTraChuaTinhPhiTu(c,coSo,hs,ngayDich);
                if(!exists(c,"SELECT 1 FROM dbo.HocVien WHERE MaHocVien=? AND MaCoSo=? AND TrangThaiHoSo='HOAT_DONG'",hs,coSo))
                    throw new SQLException("Học sinh đã ngừng sử dụng hồ sơ, không thể chuyển.");
                LopNam source=docLopNam(c,coSo,lopNguon,true), target=docLopNam(c,coSo,lopDich,true);
                if(!target.active()||lopNguon==lopDich)throw new SQLException("Lớp đích phải khác lớp cũ và đang hoạt động.");
                if(target.tu().isBefore(source.tu()))throw new SQLException("Không được chuyển về năm học cũ hơn.");
                kiemTraLoiTrinh(source,target);
                if(ngayDich.isBefore(target.tu())||ngayDich.isAfter(target.den())||!ngayDich.isAfter(batDau))
                    throw new SQLException("Ngày bắt đầu lớp mới phải thuộc năm học đích và sau ngày vào lớp cũ.");
                LocalDate last=ngayDich.minusDays(1).isBefore(source.den())?ngayDich.minusDays(1):source.den();
                if(last.isBefore(batDau))throw new SQLException("Ngày chuyển không hợp lệ với thời gian lớp cũ.");
                kiemTraTrungLich(c,coSo,hs,maXep,ngayDich,target.den());
                kiemTraChoTrong(c,coSo,lopDich,ngayDich);
                chuyenMot(c,coSo,maXep,hs,lopDich,ngayDich,last,reason);
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    /** Chuyển hàng loạt từ một lớp năm cũ: hồ sơ Nghỉ không được chuyển. */
    public KetQuaChuyen chuyenNamHoc(int coSo,int lopNguon,int lopDich,LocalDate ngayDich,String reason)throws SQLException {
        try(Connection c=ketNoi.mo()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                LopNam source=docLopNam(c,coSo,lopNguon,true),target=docLopNam(c,coSo,lopDich,true);
                if(!target.active()||!target.tu().isAfter(source.tu()))throw new SQLException("Lớp đích phải thuộc năm học mới hơn và đang hoạt động.");
                kiemTraLoiTrinh(source,target);
                if(ngayDich.isBefore(target.tu())||ngayDich.isAfter(target.den()))throw new SQLException("Ngày chuyển phải thuộc năm học mới.");
                List<int[]> rows=new ArrayList<>();int skipNghi=0,skipDuplicate=0,success=0;
                try(PreparedStatement ps=c.prepareStatement("SELECT x.MaXepLop,x.MaHocVien,x.TuNgay,x.TrangThai,x.DenNgay,h.TrangThaiHoSo " +
                        "FROM dbo.XepLopMamNon x WITH (UPDLOCK,HOLDLOCK) JOIN dbo.HocVien h ON h.MaHocVien=x.MaHocVien AND h.MaCoSo=x.MaCoSo " +
                        "WHERE x.MaCoSo=? AND x.MaLop=? ORDER BY x.MaHocVien,x.MaXepLop")){
                    ps.setInt(1,coSo);ps.setInt(2,lopNguon);
                    try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                        String status=rs.getString(4),hocVienStatus=rs.getString(6);
                        LocalDate oldStart=day(rs,"TuNgay"),oldEnd=day(rs,"DenNgay");
                        if(!"HIEU_LUC".equals(status)||oldEnd!=null||!"HOAT_DONG".equals(hocVienStatus)){skipNghi++;continue;}
                        if(!ngayDich.isAfter(oldStart)){skipDuplicate++;continue;}
                        rows.add(new int[]{rs.getInt(1),rs.getInt(2)});
                    }}
                }
                for(int[] x:rows){
                    int xep=x[0],hs=x[1];khoaHocSinh(c,coSo,hs);
                    kiemTraChuaTinhPhiTu(c,coSo,hs,ngayDich);
                    if(exists(c,"SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND MaHocVien=? AND MaLop=? AND TrangThai<>'HUY'",coSo,hs,lopDich)){
                        skipDuplicate++;continue;
                    }
                    if(coTrungLich(c,coSo,hs,xep,ngayDich,target.den())){skipDuplicate++;continue;}
                    LocalDate last=ngayDich.minusDays(1).isBefore(source.den())?ngayDich.minusDays(1):source.den();
                    kiemTraChoTrong(c,coSo,lopDich,ngayDich);
                    chuyenMot(c,coSo,xep,hs,lopDich,ngayDich,last,reason);success++;
                }
                c.commit();return new KetQuaChuyen(success,skipNghi,skipDuplicate);
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }

    public List<LuaChon> taiKhoanDiemDanh(int coSo) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        String sql="SELECT t.MaTaiKhoan,t.TenDangNhap FROM dbo.TaiKhoan t JOIN dbo.TaiKhoanCoSo cs ON cs.MaTaiKhoan=t.MaTaiKhoan WHERE cs.MaCoSo=? AND cs.TrangThai=1 AND t.TrangThai=1 ORDER BY t.TenDangNhap";
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LuaChon(r.getInt(1),r.getString(2)));}
        }return out;
    }
    public List<DiemDanhMamNon> bangDiemDanh(int coSo,int lop,LocalDate ngay) throws SQLException {
        List<DiemDanhMamNon> out=new ArrayList<>();
        String sql="SELECT h.MaHocVien,h.HoTen,x.MaXepLop,ISNULL(d.MaDiemDanh,0) AS MaDiemDanh,ISNULL(d.TrangThai,'CHUA_DIEM_DANH') AS TrangThai,d.GhiChu " +
            "FROM dbo.XepLopMamNon x JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo " +
            "JOIN dbo.HocVien h ON h.MaHocVien=x.MaHocVien AND h.MaCoSo=x.MaCoSo " +
            "LEFT JOIN dbo.DiemDanhMamNon d ON d.MaHocVien=x.MaHocVien AND d.MaCoSo=x.MaCoSo AND d.NgayHoc=? " +
            "WHERE x.MaCoSo=? AND x.MaLop=? AND x.TrangThai<>'HUY' AND x.TuNgay<=? AND (x.DenNgay IS NULL OR x.DenNgay>=?) ORDER BY h.HoTen";
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(sql)){
            date(p,1,ngay);p.setInt(2,coSo);p.setInt(3,lop);date(p,4,ngay);date(p,5,ngay);
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new DiemDanhMamNon(r.getInt("MaDiemDanh"),r.getInt("MaHocVien"),r.getInt("MaXepLop"),r.getString("HoTen"),ngay,r.getString("TrangThai"),r.getString("GhiChu")));}
        }return out;
    }
    public int luuDiemDanh(int coSo,int lop,LocalDate ngay,int taiKhoan,String lyDo,List<ThayDoiDiemDanh> rows) throws SQLException {
        Set<String> trangThaiHopLe=Set.of("CHUA_DIEM_DANH","CO_MAT","VANG_CO_PHEP","VANG_KHONG_PHEP");
        if(rows.isEmpty())return 0;
        try(Connection c=ketNoi.mo()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if(!exists(c,"SELECT 1 FROM dbo.TaiKhoanCoSo cs JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=cs.MaTaiKhoan WHERE cs.MaTaiKhoan=? AND cs.MaCoSo=? AND cs.TrangThai=1 AND t.TrangThai=1",taiKhoan,coSo))throw new SQLException("Tài khoản ghi điểm danh chưa hợp lệ cho cơ sở Mầm non.");
                if(exists(c,"SELECT 1 FROM dbo.NgayNghiChung WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND NgayNghi=DATEFROMPARTS(?,?,?)",coSo,ngay.getYear(),ngay.getMonthValue(),ngay.getDayOfMonth()))
                    throw new SQLException("Ngày này đã được khai báo là ngày nghỉ chung; không thể ghi điểm danh.");
                try(Statement check=c.createStatement();ResultSet r=check.executeQuery("SELECT OBJECT_ID('dbo.NgayHoatDongMamNon','U')")){
                    r.next();if(r.getObject(1)!=null && !exists(c,"SELECT 1 FROM dbo.NgayHoatDongMamNon WHERE MaCoSo=? AND Ngay=DATEFROMPARTS(?,?,?) AND CoHoc=1",coSo,ngay.getYear(),ngay.getMonthValue(),ngay.getDayOfMonth()))
                        throw new SQLException("Ngày này chưa được cấu hình là ngày có học; không thể điểm danh.");
                }
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo WHERE l.MaLop=? AND l.MaCoSo=? AND ? BETWEEN n.TuNgay AND n.DenNgay")) {
                    p.setInt(1,lop);p.setInt(2,coSo);date(p,3,ngay);
                    try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Ngày điểm danh nằm ngoài năm học của lớp.");}
                }
                if(exists(c,"SELECT 1 FROM dbo.ThangMamNon WHERE MaCoSo=? AND TrangThai='CHOT' AND Nam=? AND Thang=?",coSo,ngay.getYear(),ngay.getMonthValue()))throw new SQLException("Tháng đã chốt. Không được chỉnh sửa điểm danh.");
                Set<Integer> daXuLy=new HashSet<>(),canTinhLai=new HashSet<>();
                int thayDoi=0;
                for(ThayDoiDiemDanh x:rows){
                    if(!daXuLy.add(x.maHocVien()))throw new SQLException("Danh sách điểm danh trùng học sinh #"+x.maHocVien());
                    if(!trangThaiHopLe.contains(x.trangThai()))throw new SQLException("Trạng thái điểm danh không hợp lệ.");
                    // Đảm bảo buổi được ghi cho đúng học sinh, lớp, cơ sở và ngày hiệu lực.
                    String kt="SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaXepLop=? AND MaHocVien=? AND MaCoSo=? AND MaLop=? AND TrangThai<>'HUY' AND TuNgay<=? AND (DenNgay IS NULL OR DenNgay>=?)";
                    try(PreparedStatement p=c.prepareStatement(kt)){
                        p.setInt(1,x.maXepLop());p.setInt(2,x.maHocVien());p.setInt(3,coSo);p.setInt(4,lop);date(p,5,ngay);date(p,6,ngay);
                        try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Xếp lớp không còn hợp lệ cho ngày học: #"+x.maHocVien());}
                    }
                    int id=0;String old=null, oldGhiChu=null;
                    try(PreparedStatement p=c.prepareStatement("SELECT MaDiemDanh,TrangThai,GhiChu FROM dbo.DiemDanhMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=? AND NgayHoc=?")){
                        p.setInt(1,x.maHocVien());p.setInt(2,coSo);date(p,3,ngay);
                        try(ResultSet r=p.executeQuery()){if(r.next()){id=r.getInt(1);old=r.getString(2);oldGhiChu=r.getString(3);}}
                    }
                    String ghi=x.ghiChu()==null?"":x.ghiChu().trim();
                    if(id==0){
                        if(x.trangThai().equals("CHUA_DIEM_DANH")&&ghi.isBlank())continue;
                        try(PreparedStatement p=c.prepareStatement("INSERT dbo.DiemDanhMamNon(MaHocVien,MaCoSo,MaXepLop,NgayHoc,TrangThai,GhiChu,MaTaiKhoanDiemDanh) VALUES(?,?,?,?,?,?,?)")){
                            p.setInt(1,x.maHocVien());p.setInt(2,coSo);p.setInt(3,x.maXepLop());date(p,4,ngay);p.setString(5,x.trangThai());p.setString(6,ghi);p.setInt(7,taiKhoan);p.executeUpdate();thayDoi++;canTinhLai.add(x.maHocVien());}
                    }else if(!Objects.equals(old,x.trangThai())||!Objects.equals(oldGhiChu==null?"":oldGhiChu,ghi)){
                        if(lyDo==null||lyDo.trim().isBlank())throw new SQLException("Phải nhập lý do khi sửa điểm danh đã lưu (lưu lịch sử thay đổi).");
                        try(PreparedStatement p=c.prepareStatement("UPDATE dbo.DiemDanhMamNon SET TrangThai=?,GhiChu=?,MaTaiKhoanDiemDanh=?,ThoiDiemCapNhat=SYSUTCDATETIME() WHERE MaDiemDanh=?")){
                            p.setString(1,x.trangThai());p.setString(2,ghi);p.setInt(3,taiKhoan);p.setInt(4,id);p.executeUpdate();}
                        try(PreparedStatement p=c.prepareStatement("INSERT dbo.LichSuSuaDiemDanhMamNon(MaDiemDanh,TrangThaiTruoc,TrangThaiSau,GhiChuTruoc,GhiChuSau,LyDo,MaTaiKhoan) VALUES(?,?,?,?,?,?,?)")){
                            p.setInt(1,id);p.setString(2,old);p.setString(3,x.trangThai());p.setString(4,oldGhiChu);p.setString(5,ghi);p.setString(6,lyDo.trim());p.setInt(7,taiKhoan);p.executeUpdate();thayDoi++;canTinhLai.add(x.maHocVien());}
                    }
                }
                for(int hocSinh:canTinhLai)tinhLaiTienAnNhap(c,coSo,hocSinh,ngay);
                c.commit();return thayDoi;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    private record ChinhSachTienAn(boolean coMat,boolean vangCoPhep,boolean vangKhongPhep) {}
    public CauHinhTienAn cauHinhTienAn(int coSo,YearMonth ky)throws SQLException {
        try(Connection c=ketNoi.mo()){
            if(!coBangChinhSachTienAn(c))throw new SQLException("CSDL chưa có bảng chính sách tiền ăn Giai đoạn 8.");
            String sql="SELECT TOP (1) MaChinhSach,ThangBatDau,TinhCoMat,TinhVangCoPhep,TinhVangKhongPhep,GhiChu " +
                "FROM dbo.ChinhSachTienAnMamNon WHERE MaCoSo=? AND ThangBatDau<=? ORDER BY ThangBatDau DESC";
            try(PreparedStatement p=c.prepareStatement(sql)){
                p.setInt(1,coSo);date(p,2,ky.atDay(1));
                try(ResultSet r=p.executeQuery()){
                    return r.next()?new CauHinhTienAn(r.getInt(1),day(r,"ThangBatDau"),r.getBoolean(3),
                        r.getBoolean(4),r.getBoolean(5),r.getString(6)):
                        new CauHinhTienAn(0,null,true,false,true,null);
                }
            }
        }
    }
    /** Chính sách có hiệu lực từ đầu tháng; không sửa ngầm bảng phí đã lập từ kỳ này trở đi. */
    public void luuCauHinhTienAn(int coSo,YearMonth ky,boolean coMat,boolean vangCoPhep,
        boolean vangKhongPhep,String ghiChu)throws SQLException {
        if(ghiChu!=null&&ghiChu.trim().length()>500)throw new SQLException("Ghi chú chính sách không quá 500 ký tự.");
        LocalDate start=ky.atDay(1);
        try(Connection c=ketNoi.mo()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                if(!coBangChinhSachTienAn(c))throw new SQLException("CSDL chưa có bảng chính sách tiền ăn Giai đoạn 8.");
                if(!exists(c,"SELECT 1 FROM dbo.CoSo WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND LoaiCoSo='MAM_NON'",coSo))
                    throw new SQLException("Không tìm thấy cơ sở Mầm non.");
                String billed="SELECT 1 FROM dbo.BangTinhPhiMamNon b WITH (UPDLOCK,HOLDLOCK) " +
                    "JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo " +
                    "WHERE b.MaCoSo=? AND (t.Nam>? OR (t.Nam=? AND t.Thang>=?))";
                if(exists(c,billed,coSo,ky.getYear(),ky.getYear(),ky.getMonthValue()))
                    throw new SQLException("Đã có bảng tính phí từ tháng hiệu lực trở đi; cần quy trình điều chỉnh có lịch sử, không đổi chính sách ngầm.");
                int id=0;
                try(PreparedStatement p=c.prepareStatement("SELECT MaChinhSach FROM dbo.ChinhSachTienAnMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND ThangBatDau=?")){
                    p.setInt(1,coSo);date(p,2,start);
                    try(ResultSet r=p.executeQuery()){if(r.next())id=r.getInt(1);}
                }
                String sql=id==0?
                    "INSERT dbo.ChinhSachTienAnMamNon(MaCoSo,ThangBatDau,TinhCoMat,TinhVangCoPhep,TinhVangKhongPhep,GhiChu) VALUES(?,?,?,?,?,?)":
                    "UPDATE dbo.ChinhSachTienAnMamNon SET TinhCoMat=?,TinhVangCoPhep=?,TinhVangKhongPhep=?,GhiChu=? WHERE MaChinhSach=? AND MaCoSo=?";
                try(PreparedStatement p=c.prepareStatement(sql)){
                    int i=1;if(id==0){p.setInt(i++,coSo);date(p,i++,start);}
                    p.setBoolean(i++,coMat);p.setBoolean(i++,vangCoPhep);p.setBoolean(i++,vangKhongPhep);
                    p.setString(i++,ghiChu==null||ghiChu.isBlank()?null:ghiChu.trim());
                    if(id!=0){p.setInt(i++,id);p.setInt(i,coSo);}
                    if(p.executeUpdate()!=1)throw khongTimThay();
                }
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    private boolean coBangChinhSachTienAn(Connection c)throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT OBJECT_ID('dbo.ChinhSachTienAnMamNon','U')")){
            r.next();return r.getObject(1)!=null;
        }
    }
    private ChinhSachTienAn chinhSachTienAn(Connection c,int coSo,LocalDate ngay)throws SQLException {
        // Quy tắc tạm thời đã duyệt; bảng chính sách có hiệu lực theo tháng sau khi migration được phê duyệt.
        ChinhSachTienAn approved=new ChinhSachTienAn(true,false,true);
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT OBJECT_ID('dbo.ChinhSachTienAnMamNon','U')")) {
            r.next();if(r.getObject(1)==null)return approved;
        }
        try(PreparedStatement p=c.prepareStatement("SELECT TOP (1) TinhCoMat,TinhVangCoPhep,TinhVangKhongPhep FROM dbo.ChinhSachTienAnMamNon WHERE MaCoSo=? AND ThangBatDau<=? ORDER BY ThangBatDau DESC")) {
            p.setInt(1,coSo);date(p,2,ngay.withDayOfMonth(1));
            try(ResultSet r=p.executeQuery()){return r.next()?new ChinhSachTienAn(r.getBoolean(1),r.getBoolean(2),r.getBoolean(3)):approved;}
        }
    }
    private int demNgayAn(Connection c,int coSo,int hocSinh,int xep,LocalDate tu,LocalDate den,ChinhSachTienAn policy)throws SQLException {
        yeuCauBangLich(c);
        String sql="SELECT COUNT(*) FROM dbo.DiemDanhMamNon dd WHERE dd.MaHocVien=? AND dd.MaCoSo=? AND dd.MaXepLop=? " +
            "AND dd.NgayHoc BETWEEN ? AND ? AND ((dd.TrangThai='CO_MAT' AND ?=1) OR (dd.TrangThai='VANG_CO_PHEP' AND ?=1) OR (dd.TrangThai='VANG_KHONG_PHEP' AND ?=1)) " +
            "AND EXISTS(SELECT 1 FROM dbo.NgayHoatDongMamNon a WHERE a.MaCoSo=dd.MaCoSo AND a.Ngay=dd.NgayHoc AND a.CoHoc=1 AND a.CoAn=1) " +
            "AND NOT EXISTS(SELECT 1 FROM dbo.NgayNghiChung n WHERE n.MaCoSo=dd.MaCoSo AND n.NgayNghi=dd.NgayHoc)";
        try(PreparedStatement q=c.prepareStatement(sql)) {
            q.setInt(1,hocSinh);q.setInt(2,coSo);q.setInt(3,xep);date(q,4,tu);date(q,5,den);
            q.setInt(6,policy.coMat()?1:0);q.setInt(7,policy.vangCoPhep()?1:0);q.setInt(8,policy.vangKhongPhep()?1:0);
            try(ResultSet r=q.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    /** Chỉ điều chỉnh bảng tính NHAP chưa phát sinh công nợ; giữ nguyên mọi bảng CHOT và phiếu thu. */
    private void tinhLaiTienAnNhap(Connection c,int coSo,int hocSinh,LocalDate ngay)throws SQLException {
        ChinhSachTienAn policy=chinhSachTienAn(c,coSo,ngay);
        try(PreparedStatement lock=c.prepareStatement("SELECT b.TrangThai, CASE WHEN EXISTS(SELECT 1 FROM dbo.KhoanPhaiThu k WHERE k.MaBangTinh=b.MaBangTinh AND k.MaCoSo=b.MaCoSo) THEN 1 ELSE 0 END " +
            "FROM dbo.BangTinhPhiMamNon b WITH (UPDLOCK,HOLDLOCK) JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo " +
            "WHERE b.MaHocVien=? AND b.MaCoSo=? AND t.Nam=? AND t.Thang=?")) {
            lock.setInt(1,hocSinh);lock.setInt(2,coSo);lock.setInt(3,ngay.getYear());lock.setInt(4,ngay.getMonthValue());
            try(ResultSet r=lock.executeQuery()) {if(r.next()&&("CHOT".equals(r.getString(1))||r.getInt(2)==1))
                throw new SQLException("Điểm danh đã liên quan bảng tính chốt hoặc khoản phải thu; cần quy trình điều chỉnh tài chính có lịch sử.");}
        }
        String select="SELECT b.MaBangTinh FROM dbo.BangTinhPhiMamNon b WITH (UPDLOCK,HOLDLOCK) " +
            "JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo " +
            "WHERE b.MaHocVien=? AND b.MaCoSo=? AND t.Nam=? AND t.Thang=? AND b.TrangThai='NHAP' AND t.TrangThai='MO'";
        try(PreparedStatement p=c.prepareStatement(select)) {
            p.setInt(1,hocSinh);p.setInt(2,coSo);p.setInt(3,ngay.getYear());p.setInt(4,ngay.getMonthValue());
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())return;
                int bang=r.getInt(1);
                if(exists(c,"SELECT 1 FROM dbo.KhoanPhaiThu WHERE MaBangTinh=? AND MaCoSo=?",bang,coSo))
                    throw new SQLException("Bảng tính đã phát sinh khoản thu; cần nghiệp vụ điều chỉnh, không tự sửa tiền ăn.");
                BigDecimal tong=BigDecimal.ZERO;
                Set<Integer> daCo=new HashSet<>();
                String details="SELECT MaChiTietTienAn,MaXepLop,TuNgay,DenNgay,DonGiaApDung FROM dbo.ChiTietTienAnMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaBangTinh=? AND MaHocVien=? AND MaCoSo=?";
                try(PreparedStatement p2=c.prepareStatement(details)) {
                    p2.setInt(1,bang);p2.setInt(2,hocSinh);p2.setInt(3,coSo);
                    try(ResultSet d=p2.executeQuery()) {while(d.next()) {
                        int chiTiet=d.getInt(1),xep=d.getInt(2);
                        if(!daCo.add(xep))throw new SQLException("Bảng tính có nhiều dòng tiền ăn cho cùng một lần xếp lớp; cần kiểm tra thủ công.");
                        LocalDate tu=day(d,"TuNgay"),den=day(d,"DenNgay");
                        int soNgay=demNgayAn(c,coSo,hocSinh,xep,tu,den,policy);
                        BigDecimal thanhTien=d.getBigDecimal(5).multiply(BigDecimal.valueOf(soNgay));
                        try(PreparedStatement u=c.prepareStatement("UPDATE dbo.ChiTietTienAnMamNon SET SoNgayTinhTienAn=?,ThanhTien=? WHERE MaChiTietTienAn=?")) {
                            u.setInt(1,soNgay);u.setBigDecimal(2,thanhTien);u.setInt(3,chiTiet);u.executeUpdate();
                        }
                        tong=tong.add(thanhTien);
                    }}
                }
                if(!daCo.isEmpty())try(PreparedStatement u=c.prepareStatement("UPDATE dbo.BangTinhPhiMamNon SET TongTienAn=?,NgayTinh=SYSUTCDATETIME() WHERE MaBangTinh=? AND TrangThai='NHAP'")) {
                    u.setBigDecimal(1,tong);u.setInt(2,bang);u.executeUpdate();
                }
            }
        }
    }
    private record MucPhi(int id,BigDecimal soTien) {}
    private static boolean coBangLich(Connection c)throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT OBJECT_ID('dbo.NgayHoatDongMamNon','U')")){
            r.next();return r.getObject(1)!=null;
        }
    }
    private static void yeuCauBangLich(Connection c)throws SQLException {
        if(!coBangLich(c))throw new SQLException("CSDL chưa có lịch hoạt động Mầm non; cần duyệt migration Giai đoạn 8.");
    }
    public List<NgayHoatDong> lichHoatDong(int coSo,int namHoc,YearMonth ky)throws SQLException {
        try(Connection c=ketNoi.mo()){
            yeuCauBangLich(c);
            Map<LocalDate,NgayHoatDong> saved=new HashMap<>();
            try(PreparedStatement p=c.prepareStatement("SELECT Ngay,CoHoc,CoAn FROM dbo.NgayHoatDongMamNon WHERE MaCoSo=? AND MaNamHoc=? AND Ngay>=? AND Ngay<?")){
                p.setInt(1,coSo);p.setInt(2,namHoc);date(p,3,ky.atDay(1));date(p,4,ky.plusMonths(1).atDay(1));
                try(ResultSet r=p.executeQuery()){while(r.next()){
                    LocalDate day=day(r,"Ngay");saved.put(day,new NgayHoatDong(day,r.getBoolean(2),r.getBoolean(3)));
                }}
            }
            List<NgayHoatDong> out=new ArrayList<>();
            for(int d=1;d<=ky.lengthOfMonth();d++){
                LocalDate day=ky.atDay(d);out.add(saved.getOrDefault(day,new NgayHoatDong(day,null,null)));
            }
            return out;
        }
    }
    /** Lưu toàn bộ lịch tháng trong một transaction; không thay lịch đã có bảng tính. */
    public void luuLichHoatDong(int coSo,int namHoc,YearMonth ky,List<NgayHoatDong> ngay)throws SQLException {
        if(ngay==null||ngay.size()!=ky.lengthOfMonth())throw new SQLException("Phải xác nhận đủ từng ngày trong tháng.");
        Set<LocalDate> unique=new HashSet<>();
        for(NgayHoatDong item:ngay){
            if(item==null||item.ngay()==null||!YearMonth.from(item.ngay()).equals(ky)||item.coHoc()==null||item.coAn()==null||!unique.add(item.ngay()))
                throw new SQLException("Lịch tháng thiếu ngày, trùng ngày hoặc chưa chọn trạng thái học/ăn.");
            if(item.coAn()&&!item.coHoc())throw new SQLException("Ngày không học không thể tổ chức ăn.");
        }
        try(Connection c=ketNoi.mo()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                yeuCauBangLich(c);
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM dbo.NamHoc WITH (UPDLOCK,HOLDLOCK) WHERE MaNamHoc=? AND MaCoSo=? AND TuNgay<=? AND DenNgay>=?")){
                    p.setInt(1,namHoc);p.setInt(2,coSo);date(p,3,ky.atDay(1));date(p,4,ky.atEndOfMonth());
                    try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Tháng phải nằm trọn trong năm học đã chọn.");}
                }
                if(exists(c,"SELECT 1 FROM dbo.BangTinhPhiMamNon b WITH (UPDLOCK,HOLDLOCK) JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo WHERE t.MaCoSo=? AND t.Nam=? AND t.Thang=?",coSo,ky.getYear(),ky.getMonthValue()))
                    throw new SQLException("Tháng đã có bảng tính phí; không đổi lịch hoạt động âm thầm.");
                if(exists(c,"SELECT 1 FROM dbo.ThangMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND Nam=? AND Thang=? AND TrangThai<>'MO'",coSo,ky.getYear(),ky.getMonthValue()))
                    throw new SQLException("Tháng đã chốt; không đổi lịch hoạt động.");
                for(NgayHoatDong item:ngay){
                    try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM dbo.NgayNghiChung WHERE MaCoSo=? AND NgayNghi=?")){
                        check.setInt(1,coSo);date(check,2,item.ngay());
                        try(ResultSet r=check.executeQuery()){if(r.next()&&item.coHoc())throw new SQLException("Ngày "+item.ngay()+" đã khai báo nghỉ chung; không thể đánh dấu có học.");}
                    }
                    if(!item.coHoc())try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM dbo.DiemDanhMamNon WHERE MaCoSo=? AND NgayHoc=?")){
                        check.setInt(1,coSo);date(check,2,item.ngay());
                        try(ResultSet r=check.executeQuery()){if(r.next())throw new SQLException("Ngày "+item.ngay()+" đã có điểm danh; không thể đánh dấu nghỉ.");}
                    }
                    try(PreparedStatement p=c.prepareStatement("UPDATE dbo.NgayHoatDongMamNon SET CoHoc=?,CoAn=? WHERE MaCoSo=? AND MaNamHoc=? AND Ngay=?")){
                        p.setBoolean(1,item.coHoc());p.setBoolean(2,item.coAn());p.setInt(3,coSo);p.setInt(4,namHoc);date(p,5,item.ngay());
                        if(p.executeUpdate()==0)try(PreparedStatement insert=c.prepareStatement("INSERT dbo.NgayHoatDongMamNon(MaNamHoc,MaCoSo,Ngay,CoHoc,CoAn) VALUES(?,?,?,?,?)")){
                            insert.setInt(1,namHoc);insert.setInt(2,coSo);date(insert,3,item.ngay());insert.setBoolean(4,item.coHoc());insert.setBoolean(5,item.coAn());insert.executeUpdate();
                        }
                    }
                }
                int standard=soNgayHocChuan(c,coSo,ky);
                try(PreparedStatement p=c.prepareStatement("UPDATE dbo.ThangMamNon SET SoNgayChuan=? WHERE MaCoSo=? AND Nam=? AND Thang=? AND TrangThai='MO'")){
                    p.setInt(1,standard);p.setInt(2,coSo);p.setInt(3,ky.getYear());p.setInt(4,ky.getMonthValue());p.executeUpdate();
                }
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    private int soNgayHocChuan(Connection c,int coSo,YearMonth ky)throws SQLException {
        yeuCauBangLich(c);
        String sql="SELECT COUNT(*),SUM(CASE WHEN a.CoHoc=1 AND n.MaNgayNghi IS NULL THEN 1 ELSE 0 END) " +
            "FROM dbo.NgayHoatDongMamNon a LEFT JOIN dbo.NgayNghiChung n ON n.MaCoSo=a.MaCoSo AND n.NgayNghi=a.Ngay " +
            "WHERE a.MaCoSo=? AND a.Ngay>=? AND a.Ngay<?";
        try(PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,coSo);date(p,2,ky.atDay(1));date(p,3,ky.plusMonths(1).atDay(1));
            try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)!=ky.lengthOfMonth())
                throw new SQLException("Lịch hoạt động tháng "+ky+" chưa cấu hình đủ "+ky.lengthOfMonth()+" ngày.");
                int result=r.getInt(2);if(result<=0)throw new SQLException("Tháng không có ngày học chuẩn hợp lệ.");return result;
            }
        }
    }
    public int soNgayHocChuan(int coSo,YearMonth ky)throws SQLException {
        try(Connection c=ketNoi.mo()){return soNgayHocChuan(c,coSo,ky);}
    }
    private int demNgayHocChuan(Connection c,int coSo,LocalDate tu,LocalDate den)throws SQLException {
        String sql="SELECT COUNT(*) FROM dbo.NgayHoatDongMamNon a WHERE a.MaCoSo=? AND a.Ngay BETWEEN ? AND ? AND a.CoHoc=1 " +
            "AND NOT EXISTS(SELECT 1 FROM dbo.NgayNghiChung n WHERE n.MaCoSo=a.MaCoSo AND n.NgayNghi=a.Ngay)";
        try(PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,coSo);date(p,2,tu);date(p,3,den);
            try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    private MucPhi mucPhi(Connection c,String bang,String idCot,String tienCot,int lop,LocalDate thang)throws SQLException {
        String sql="SELECT TOP (1) "+idCot+","+tienCot+" FROM dbo."+bang+
            " WHERE MaLop=? AND ThangBatDau<=? ORDER BY ThangBatDau DESC";
        try(PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,lop);date(p,2,thang);
            try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("Lớp chưa được thiết lập "+(bang.equals("DonGiaTienAn")?"đơn giá tiền ăn":"học phí tháng")+" cho kỳ này.");
                return new MucPhi(r.getInt(1),r.getBigDecimal(2));
            }
        }
    }
    /** Chỉ thêm kỳ mới; số ngày chuẩn phải do nhà trường xác nhận, không suy đoán lịch học. */
    public int taoThang(int coSo,YearMonth ky,int soNgayChuan)throws SQLException {
        if(soNgayChuan<1||soNgayChuan>ky.lengthOfMonth())throw new SQLException("Số ngày chuẩn phải từ 1 đến số ngày trong tháng.");
        try(Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                try(PreparedStatement year=c.prepareStatement("SELECT 1 FROM dbo.NamHoc WHERE MaCoSo=? AND TuNgay<=? AND DenNgay>=?")){
                    year.setInt(1,coSo);date(year,2,ky.atDay(1));date(year,3,ky.atEndOfMonth());
                    try(ResultSet r=year.executeQuery()){if(!r.next())throw new SQLException("Tháng phải nằm trọn trong một năm học Mầm non.");}
                }
                int fromCalendar=soNgayHocChuan(c,coSo,ky);
                if(soNgayChuan!=fromCalendar)throw new SQLException("Số ngày chuẩn phải khớp lịch hoạt động đã cấu hình: "+fromCalendar+" ngày.");
                if(exists(c,"SELECT 1 FROM dbo.ThangMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND Nam=? AND Thang=?",coSo,ky.getYear(),ky.getMonthValue()))
                    throw new SQLException("Kỳ này đã được thiết lập; không tạo trùng.");
                try(PreparedStatement p=c.prepareStatement("INSERT dbo.ThangMamNon(MaCoSo,Nam,Thang,SoNgayChuan) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1,coSo);p.setInt(2,ky.getYear());p.setInt(3,ky.getMonthValue());p.setInt(4,soNgayChuan);
                    p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Không lấy được mã tháng.");int id=r.getInt(1);c.commit();return id;}
                }
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    private record DoanXep(int maXep,int maLop,LocalDate tu,LocalDate den,int soNgayHoc,
        BigDecimal hocPhiThang,MucPhi tienAn,int soNgayAn) {}
    /** Lập bảng NHAP theo các khoảng xếp lớp; học phí tháng làm tròn một lần sau khi cộng mọi khoảng. */
    public int lapBangTinhNhap(int coSo,int hocSinh,YearMonth ky)throws SQLException {
        LocalDate first=ky.atDay(1),last=ky.atEndOfMonth();
        try(Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if(!exists(c,"SELECT 1 FROM dbo.HocVien WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=? AND TrangThaiHoSo='HOAT_DONG'",hocSinh,coSo))
                    throw new SQLException("Học sinh đã ngừng hoạt động hoặc không thuộc cơ sở Mầm non.");
                int thang,standard;
                try(PreparedStatement p=c.prepareStatement("SELECT MaThang,SoNgayChuan,TrangThai FROM dbo.ThangMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND Nam=? AND Thang=?")) {
                    p.setInt(1,coSo);p.setInt(2,ky.getYear());p.setInt(3,ky.getMonthValue());
                    try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new SQLException("Chưa thiết lập số ngày chuẩn của tháng; không tự suy đoán.");
                        if(!"MO".equals(r.getString(3)))throw new SQLException("Tháng đã chốt.");
                        thang=r.getInt(1);standard=r.getInt(2);
                    }
                }
                int configured=soNgayHocChuan(c,coSo,ky);
                if(standard!=configured)throw new SQLException("Số ngày chuẩn của tháng không khớp lịch hoạt động ("+standard+"/"+configured+").");
                if(exists(c,"SELECT 1 FROM dbo.BangTinhPhiMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaThang=?",hocSinh,thang))
                    throw new SQLException("Học sinh đã có bảng tính tháng này; không tạo trùng.");
                List<DoanXep> segments=new ArrayList<>();LocalDate previousEnd=first.minusDays(1);
                String placement="SELECT x.MaXepLop,x.MaLop,x.TuNgay,x.DenNgay,n.TuNgay AS NamTu,n.DenNgay AS NamDen FROM dbo.XepLopMamNon x WITH (UPDLOCK,HOLDLOCK) " +
                    "JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo " +
                    "JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo " +
                    "WHERE x.MaHocVien=? AND x.MaCoSo=? AND x.TrangThai<>'HUY' AND x.TuNgay<=? AND (x.DenNgay IS NULL OR x.DenNgay>=?) " +
                    "ORDER BY x.TuNgay,x.MaXepLop";
                List<int[]> placements=new ArrayList<>();List<LocalDate[]> dates=new ArrayList<>();
                try(PreparedStatement p=c.prepareStatement(placement)) {
                    p.setInt(1,hocSinh);p.setInt(2,coSo);date(p,3,last);date(p,4,first);
                    try(ResultSet r=p.executeQuery()){while(r.next()){
                        LocalDate enrolled=day(r,"TuNgay"),finished=day(r,"DenNgay");
                        LocalDate tu=enrolled.isAfter(first)?enrolled:first;
                        LocalDate den=finished==null||finished.isAfter(last)?last:finished;
                        if(tu.isBefore(day(r,"NamTu"))||den.isAfter(day(r,"NamDen")))
                            throw new SQLException("Khoảng xếp lớp nằm ngoài năm học; cần đối chiếu lịch sử trước khi tính phí.");
                        if(!tu.isAfter(previousEnd))throw new SQLException("Các lần xếp lớp chồng ngày trong tháng; không tính trùng học phí.");
                        previousEnd=den;placements.add(new int[]{r.getInt(1),r.getInt(2)});dates.add(new LocalDate[]{tu,den});
                    }}
                }
                if(placements.isEmpty())throw new SQLException("Học sinh không có khoảng xếp lớp hợp lệ trong tháng.");
                ChinhSachTienAn policy=chinhSachTienAn(c,coSo,first);
                List<HocPhiMamNonTheoNgay.Doan> feeParts=new ArrayList<>();int counted=0;
                BigDecimal tongAn=BigDecimal.ZERO;
                for(int i=0;i<placements.size();i++){
                    int xep=placements.get(i)[0],lop=placements.get(i)[1];
                    LocalDate tu=dates.get(i)[0],den=dates.get(i)[1];
                    MucPhi hocPhi=mucPhi(c,"MucHocPhiLop","MaMucHocPhi","HocPhiThang",lop,first);
                    MucPhi meal=mucPhi(c,"DonGiaTienAn","MaDonGiaTienAn","DonGiaNgay",lop,first);
                    BigDecimal amount=hocPhi.soTien();
                    try(PreparedStatement p=c.prepareStatement("SELECT TOP (1) HocPhiRieng FROM dbo.HocPhiRiengMamNon WHERE MaHocVien=? AND MaLop=? AND ThangBatDau<=? ORDER BY ThangBatDau DESC")) {
                        p.setInt(1,hocSinh);p.setInt(2,lop);date(p,3,first);
                        try(ResultSet r=p.executeQuery()){if(r.next())amount=r.getBigDecimal(1);}
                    }
                    int days=demNgayHocChuan(c,coSo,tu,den),mealDays=demNgayAn(c,coSo,hocSinh,xep,tu,den,policy);
                    counted+=days;feeParts.add(new HocPhiMamNonTheoNgay.Doan(amount,days));
                    segments.add(new DoanXep(xep,lop,tu,den,days,amount,meal,mealDays));
                    tongAn=tongAn.add(meal.soTien().multiply(BigDecimal.valueOf(mealDays)));
                }
                if(counted==0)throw new SQLException("Học sinh không có ngày học chuẩn trong thời gian đăng ký của tháng.");
                HocPhiMamNonTheoNgay.KetQua result=HocPhiMamNonTheoNgay.tinh(standard,feeParts);
                int bang;
                try(PreparedStatement p=c.prepareStatement("INSERT dbo.BangTinhPhiMamNon(MaHocVien,MaCoSo,MaThang,NgayNhapHocTrongThang,NgayNghiHanTrongThang,TongHocPhi,TongTienAn) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1,hocSinh);p.setInt(2,coSo);p.setInt(3,thang);
                    date(p,4,segments.get(0).tu().isAfter(first)?segments.get(0).tu():null);
                    date(p,5,segments.get(segments.size()-1).den().isBefore(last)?segments.get(segments.size()-1).den():null);
                    p.setBigDecimal(6,result.tong());p.setBigDecimal(7,tongAn);p.executeUpdate();
                    try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Không lấy được mã bảng tính.");bang=r.getInt(1);}
                }
                for(int i=0;i<segments.size();i++){
                    DoanXep d=segments.get(i);
                    try(PreparedStatement p=c.prepareStatement("INSERT dbo.ChiTietHocPhiMamNon(MaBangTinh,MaXepLop,MaHocVien,MaCoSo,NgayBatDauPhanBo,NgayKetThucPhanBo,SoNgayTinhPhi,SoNgayChuan,MucHocPhiThang,ThanhTien) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                        p.setInt(1,bang);p.setInt(2,d.maXep());p.setInt(3,hocSinh);p.setInt(4,coSo);date(p,5,d.tu());date(p,6,d.den());
                        p.setInt(7,d.soNgayHoc());p.setInt(8,standard);p.setBigDecimal(9,d.hocPhiThang());p.setBigDecimal(10,result.theoDoan().get(i));p.executeUpdate();
                    }
                    try(PreparedStatement p=c.prepareStatement("INSERT dbo.ChiTietTienAnMamNon(MaBangTinh,MaXepLop,MaHocVien,MaCoSo,MaLop,MaDonGiaTienAn,TuNgay,DenNgay,SoNgayTinhTienAn,DonGiaApDung,ThanhTien) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                        p.setInt(1,bang);p.setInt(2,d.maXep());p.setInt(3,hocSinh);p.setInt(4,coSo);p.setInt(5,d.maLop());p.setInt(6,d.tienAn().id());date(p,7,d.tu());date(p,8,d.den());
                        p.setInt(9,d.soNgayAn());p.setBigDecimal(10,d.tienAn().soTien());p.setBigDecimal(11,d.tienAn().soTien().multiply(BigDecimal.valueOf(d.soNgayAn())));p.executeUpdate();
                    }
                }
                c.commit();return bang;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public List<BangTinhThang> bangTinhThang(int coSo,int namHoc,YearMonth ky)throws SQLException {
        List<BangTinhThang> out=new ArrayList<>();
        String calendarFilter="AND EXISTS(SELECT 1 FROM dbo.NgayHoatDongMamNon a WHERE a.MaCoSo=d.MaCoSo AND a.Ngay=d.NgayHoc AND a.CoHoc=1) ";
        String sql="SELECT b.MaBangTinh,b.MaHocVien,h.HoTen,names.TenLop,b.TongHocPhi,b.TongTienAn,b.TrangThai,t.SoNgayChuan," +
            "(SELECT COALESCE(SUM(SoNgayTinhPhi),0) FROM dbo.ChiTietHocPhiMamNon ct WHERE ct.MaBangTinh=b.MaBangTinh) AS CanDiemDanh," +
            "(SELECT COUNT(*) FROM dbo.DiemDanhMamNon d WHERE d.MaHocVien=b.MaHocVien AND d.MaCoSo=b.MaCoSo " +
            "AND d.NgayHoc>=? AND d.NgayHoc<? AND d.TrangThai<>'CHUA_DIEM_DANH' " +
            calendarFilter+
            "AND EXISTS(SELECT 1 FROM dbo.ChiTietHocPhiMamNon ct WHERE ct.MaBangTinh=b.MaBangTinh AND ct.MaXepLop=d.MaXepLop AND d.NgayHoc BETWEEN ct.NgayBatDauPhanBo AND ct.NgayKetThucPhanBo) " +
            "AND NOT EXISTS(SELECT 1 FROM dbo.NgayNghiChung n WHERE n.MaCoSo=d.MaCoSo AND n.NgayNghi=d.NgayHoc)) AS DaDiemDanh " +
            "FROM dbo.BangTinhPhiMamNon b JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo " +
            "JOIN dbo.HocVien h ON h.MaHocVien=b.MaHocVien AND h.MaCoSo=b.MaCoSo " +
            "OUTER APPLY (SELECT STRING_AGG(q.TenLop,N', ') AS TenLop FROM (SELECT DISTINCT l.TenLop FROM dbo.ChiTietHocPhiMamNon ct " +
            "JOIN dbo.XepLopMamNon x ON x.MaXepLop=ct.MaXepLop AND x.MaCoSo=ct.MaCoSo " +
            "JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo WHERE ct.MaBangTinh=b.MaBangTinh) q) names " +
            "WHERE b.MaCoSo=? AND t.Nam=? AND t.Thang=? AND EXISTS(SELECT 1 FROM dbo.ChiTietHocPhiMamNon ct " +
            "JOIN dbo.XepLopMamNon x ON x.MaXepLop=ct.MaXepLop AND x.MaCoSo=ct.MaCoSo " +
            "JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo WHERE ct.MaBangTinh=b.MaBangTinh AND l.MaNamHoc=?) ORDER BY h.HoTen";
        try(Connection c=ketNoi.mo()) {
            if(!coBangLich(c))sql=sql.replace(calendarFilter,"");
            try(PreparedStatement p=c.prepareStatement(sql)) {
            date(p,1,ky.atDay(1));date(p,2,ky.plusMonths(1).atDay(1));
            p.setInt(3,coSo);p.setInt(4,ky.getYear());p.setInt(5,ky.getMonthValue());p.setInt(6,namHoc);
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new BangTinhThang(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),ky,r.getBigDecimal(5),r.getBigDecimal(6),r.getString(7),r.getInt(8),r.getInt(9),r.getInt(10)));}
            }
        }return out;
    }
    /** Chốt từng học sinh sau khi đủ số ngày điểm danh do nhà trường cấu hình. */
    public void chotBangTinh(int coSo,int bang)throws SQLException {
        try(Connection c=ketNoi.mo()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                int hocSinh,standard;YearMonth ky;
                String sql="SELECT b.MaHocVien,b.TrangThai,t.Nam,t.Thang,t.SoNgayChuan,t.TrangThai AS TrangThaiThang " +
                    "FROM dbo.BangTinhPhiMamNon b WITH (UPDLOCK,HOLDLOCK) JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.MaCoSo=b.MaCoSo " +
                    "WHERE b.MaBangTinh=? AND b.MaCoSo=?";
                try(PreparedStatement p=c.prepareStatement(sql)) {
                    p.setInt(1,bang);p.setInt(2,coSo);
                    try(ResultSet r=p.executeQuery()){
                        if(!r.next()||!"NHAP".equals(r.getString(2))||!"MO".equals(r.getString(6)))throw new SQLException("Bảng tính không còn ở trạng thái nháp của tháng mở.");
                        hocSinh=r.getInt(1);ky=YearMonth.of(r.getInt(3),r.getInt(4));standard=r.getInt(5);
                    }
                }
                if(exists(c,"SELECT 1 FROM dbo.KhoanPhaiThu WHERE MaBangTinh=? AND MaCoSo=?",bang,coSo))
                    throw new SQLException("Bảng tính đã phát sinh công nợ; không chốt lại.");
                int configured=soNgayHocChuan(c,coSo,ky);
                if(standard!=configured)throw new SQLException("Số ngày chuẩn không khớp lịch hoạt động đã xác nhận.");
                int expected;
                try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(SoNgayTinhPhi),0) FROM dbo.ChiTietHocPhiMamNon WHERE MaBangTinh=? AND MaHocVien=? AND MaCoSo=?")){
                    p.setInt(1,bang);p.setInt(2,hocSinh);p.setInt(3,coSo);
                    try(ResultSet r=p.executeQuery()){r.next();expected=r.getInt(1);}
                }
                int done;
                try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM dbo.DiemDanhMamNon d WHERE d.MaHocVien=? AND d.MaCoSo=? AND d.NgayHoc>=? AND d.NgayHoc<? " +
                    "AND d.TrangThai<>'CHUA_DIEM_DANH' AND EXISTS(SELECT 1 FROM dbo.NgayHoatDongMamNon a WHERE a.MaCoSo=d.MaCoSo AND a.Ngay=d.NgayHoc AND a.CoHoc=1) " +
                    "AND EXISTS(SELECT 1 FROM dbo.ChiTietHocPhiMamNon ct WHERE ct.MaBangTinh=? AND ct.MaXepLop=d.MaXepLop AND d.NgayHoc BETWEEN ct.NgayBatDauPhanBo AND ct.NgayKetThucPhanBo) " +
                    "AND NOT EXISTS(SELECT 1 FROM dbo.NgayNghiChung n WHERE n.MaCoSo=d.MaCoSo AND n.NgayNghi=d.NgayHoc)")) {
                    p.setInt(1,hocSinh);p.setInt(2,coSo);date(p,3,ky.atDay(1));date(p,4,ky.plusMonths(1).atDay(1));
                    p.setInt(5,bang);try(ResultSet r=p.executeQuery()){r.next();done=r.getInt(1);}
                }
                if(expected<=0||expected>standard||done!=expected)throw new SQLException("Còn ngày chưa điểm danh trong thời gian đăng ký hoặc số ngày không khớp lịch ("+done+"/"+expected+").");
                try(PreparedStatement p=c.prepareStatement("UPDATE dbo.BangTinhPhiMamNon SET TrangThai='CHOT',NgayChot=SYSUTCDATETIME() WHERE MaBangTinh=? AND MaCoSo=? AND TrangThai='NHAP'")) {
                    p.setInt(1,bang);p.setInt(2,coSo);if(p.executeUpdate()!=1)throw khongTimThay();
                }
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public List<LichSuDiemDanh> lichSuDiemDanh(int coSo,Integer lop,Integer hocSinh,LocalDate tu,LocalDate den) throws SQLException {
        List<LichSuDiemDanh> out=new ArrayList<>();
        String sql="SELECT d.MaHocVien,d.NgayHoc,h.HoTen,l.TenLop,d.TrangThai,d.GhiChu,t.TenDangNhap,CONVERT(VARCHAR(19),d.ThoiDiemCapNhat,120) AS ThoiDiemCapNhat FROM dbo.DiemDanhMamNon d " +
            "JOIN dbo.HocVien h ON h.MaHocVien=d.MaHocVien AND h.MaCoSo=d.MaCoSo " +
            "JOIN dbo.XepLopMamNon x ON x.MaXepLop=d.MaXepLop AND x.MaCoSo=d.MaCoSo " +
            "JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop AND l.MaCoSo=x.MaCoSo " +
            "JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=d.MaTaiKhoanDiemDanh " +
            "WHERE d.MaCoSo=? AND d.NgayHoc>=? AND d.NgayHoc<=? "+
            (lop==null?"":" AND l.MaLop=?")+(hocSinh==null?"":" AND d.MaHocVien=?")+" ORDER BY d.NgayHoc DESC,h.HoTen";
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(sql)){
            int i=1;p.setInt(i++,coSo);date(p,i++,tu);date(p,i++,den);if(lop!=null)p.setInt(i++,lop);if(hocSinh!=null)p.setInt(i++,hocSinh);
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LichSuDiemDanh(r.getInt(1),day(r,"NgayHoc"),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8)));}
        }return out;
    }
    public List<NhatKySua> nhatKySua(int coSo,int maHocVien,LocalDate ngay) throws SQLException {
        List<NhatKySua> out=new ArrayList<>();
        String sql="SELECT CONVERT(VARCHAR(19),ls.NgaySua,120),ls.TrangThaiTruoc,ls.TrangThaiSau,ls.LyDo,t.TenDangNhap FROM dbo.LichSuSuaDiemDanhMamNon ls JOIN dbo.DiemDanhMamNon d ON d.MaDiemDanh=ls.MaDiemDanh JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=ls.MaTaiKhoan WHERE d.MaCoSo=? AND d.MaHocVien=? AND d.NgayHoc=? ORDER BY ls.NgaySua DESC";
        try(Connection c=ketNoi.mo();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,coSo);p.setInt(2,maHocVien);date(p,3,ngay);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new NhatKySua(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));}
        }return out;
    }
}
