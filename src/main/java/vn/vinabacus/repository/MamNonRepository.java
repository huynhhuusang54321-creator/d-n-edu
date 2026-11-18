package vn.vinabacus.repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.*;

/** SQL Server repository TV4; mọi truy vấn đều giới hạn MaCoSo thuộc Mầm non. */
public final class MamNonRepository {
    public record LuaChon(int id, String ten) { @Override public String toString(){return ten;} }
    public record ThayDoiDiemDanh(int maHocVien, int maXepLop, String trangThai, String ghiChu) {}
    public record LichSuDiemDanh(int maHocVien, LocalDate ngayHoc, String hoTen, String tenLop,
        String trangThai, String ghiChu, String nguoiGhi, String ngayCapNhat) {}
    public record NhatKySua(String ngaySua, String truoc, String sau, String lyDo, String taiKhoan) {}

    private static void date(PreparedStatement p, int col, LocalDate value) throws SQLException {
        if(value==null) p.setNull(col, Types.DATE); else p.setDate(col, java.sql.Date.valueOf(value));
    }
    private static LocalDate day(ResultSet rs,String col) throws SQLException {
        java.sql.Date d=rs.getDate(col);return d==null?null:d.toLocalDate();
    }
    private static SQLException khongTimThay(){return new SQLException("Bản ghi không tồn tại tại cơ sở Mầm non hoặc đã được thay đổi.");}

    public List<NamHocMamNon> namHoc(int coSo) throws SQLException {
        List<NamHocMamNon> out=new ArrayList<>();
        try(Connection c=Database.moKetNoi(); PreparedStatement p=c.prepareStatement(
            "SELECT MaNamHoc,MaCoSo,TenNamHoc,TuNgay,DenNgay FROM dbo.NamHoc WHERE MaCoSo=? ORDER BY TuNgay DESC,MaNamHoc DESC")) {
            p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new NamHocMamNon(r.getInt(1),r.getInt(2),r.getString(3),day(r,"TuNgay"),day(r,"DenNgay")));}
        } return out;
    }
    public int luuNamHoc(int coSo,int id,String ten,LocalDate tu,LocalDate den) throws SQLException {
        String sql=id==0?"INSERT dbo.NamHoc(MaCoSo,TenNamHoc,TuNgay,DenNgay) VALUES(?,?,?,?)":
            "UPDATE dbo.NamHoc SET TenNamHoc=?,TuNgay=?,DenNgay=? WHERE MaNamHoc=? AND MaCoSo=?";
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            if(id==0){p.setInt(1,coSo);p.setString(2,ten);date(p,3,tu);date(p,4,den);}
            else {p.setString(1,ten);date(p,2,tu);date(p,3,den);p.setInt(4,id);p.setInt(5,coSo);}
            if(p.executeUpdate()!=1)throw khongTimThay();
            if(id==0){try(ResultSet k=p.getGeneratedKeys()){if(k.next())return k.getInt(1);}throw new SQLException("Không lấy được mã năm học");}
            return id;
        }
    }
    public List<LopMamNon> lop(int coSo,Integer nam,String tuKhoa) throws SQLException {
        String sql="SELECT l.MaLop,l.MaNamHoc,l.MaCoSo,l.TenLop,l.TrangThai,n.TenNamHoc FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo WHERE l.MaCoSo=?"+
            (nam==null?"":" AND l.MaNamHoc=?")+" AND l.TenLop LIKE ? ORDER BY n.TuNgay DESC,l.TenLop";
        List<LopMamNon> out=new ArrayList<>();
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,coSo);int i=2;if(nam!=null)p.setInt(i++,nam);p.setString(i,"%"+(tuKhoa==null?"":tuKhoa.trim())+"%");
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LopMamNon(r.getInt(1),r.getInt(2),r.getInt(3),r.getString(4),r.getBoolean(5),r.getString(6)));}
        }return out;
    }
    public int luuLop(int coSo,int id,int nam,String ten,boolean hoatDong) throws SQLException {
        String sql=id==0?"INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop,TrangThai) SELECT MaNamHoc,MaCoSo,?,? FROM dbo.NamHoc WHERE MaNamHoc=? AND MaCoSo=?":
            "UPDATE l SET MaNamHoc=?,TenLop=?,TrangThai=? FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=? AND n.MaCoSo=l.MaCoSo WHERE l.MaLop=? AND l.MaCoSo=?";
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            if(id!=0 && exists(c,"SELECT 1 FROM dbo.XepLopMamNon x JOIN dbo.LopMamNon l ON l.MaLop=x.MaLop WHERE l.MaLop=? AND l.MaCoSo=? AND l.MaNamHoc<>?",id,coSo,nam))
                throw new SQLException("Không được đổi năm học khi lớp đã có lịch sử xếp học sinh.");
            if(id==0){p.setString(1,ten);p.setBoolean(2,hoatDong);p.setInt(3,nam);p.setInt(4,coSo);}
            else {p.setInt(1,nam);p.setString(2,ten);p.setBoolean(3,hoatDong);p.setInt(4,nam);p.setInt(5,id);p.setInt(6,coSo);}
            if(p.executeUpdate()!=1)throw khongTimThay();
            if(id==0){try(ResultSet k=p.getGeneratedKeys()){if(k.next())return k.getInt(1);}throw new SQLException("Không lấy được mã lớp");}return id;
        }
    }
    public List<LuaChon> hocSinh(int coSo,String tim) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(
            "SELECT MaHocVien,HoTen FROM dbo.HocVien WHERE MaCoSo=? AND TrangThaiHoSo='HOAT_DONG' AND (HoTen LIKE ? OR CAST(MaHocVien AS VARCHAR(20)) LIKE ?) ORDER BY HoTen")) {
            p.setInt(1,coSo);p.setString(2,"%"+tim+"%");p.setString(3,"%"+tim+"%");
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LuaChon(r.getInt(1),"#"+r.getInt(1)+" · "+r.getString(2)));}
        }return out;
    }
    public List<LuaChon> giaoVien(int coSo) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(
            "SELECT n.MaNhanVien,n.HoTen FROM dbo.NhanVien n JOIN dbo.NhanVienCoSo cs ON cs.MaNhanVien=n.MaNhanVien WHERE cs.MaCoSo=? AND cs.TrangThai=1 AND n.TrangThai=1 ORDER BY n.HoTen")) {
            p.setInt(1,coSo);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LuaChon(r.getInt(1),r.getString(2)));}
        }return out;
    }
    public List<LuaChon> giaoVienCuaLop(int coSo,int maLop) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(
            "SELECT pc.MaPhanCong,n.HoTen+N' ('+CONVERT(NVARCHAR(10),pc.TuNgay,103)+N' - '+ISNULL(CONVERT(NVARCHAR(10),pc.DenNgay,103),N'đang dạy')+N')' FROM dbo.PhanCongGiaoVienMamNon pc JOIN dbo.NhanVien n ON n.MaNhanVien=pc.MaNhanVien WHERE pc.MaCoSo=? AND pc.MaLop=? ORDER BY pc.TuNgay DESC")) {
            p.setInt(1,coSo);p.setInt(2,maLop);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LuaChon(r.getInt(1),r.getString(2)));}
        }return out;
    }
    public void phanCongGiaoVien(int coSo,int lop,int giaoVien,LocalDate tu,LocalDate den) throws SQLException {
        try(Connection c=Database.moKetNoi()){
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
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,coSo);if(lop!=null)p.setInt(2,lop);
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new XepLopMamNon(r.getInt(1),r.getInt(2),r.getInt(3),r.getInt(4),r.getString(5),r.getString(6),day(r,"TuNgay"),day(r,"DenNgay"),r.getString(9),r.getString(10)));}
        }return out;
    }
    private boolean exists(Connection c,String sql,int... args)throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setInt(i+1,args[i]);try(ResultSet r=p.executeQuery()){return r.next();}}
    }
    public void xepHocSinh(int coSo,int lop,int hocSinh,LocalDate tu) throws SQLException {
        try(Connection c=Database.moKetNoi()) {
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                // Khóa theo học sinh để tránh hai thao tác xếp lớp đồng thời.
                if(!exists(c,"SELECT 1 FROM dbo.HocVien WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=? AND TrangThaiHoSo='HOAT_DONG'",hocSinh,coSo))throw new SQLException("Không tìm thấy học sinh còn hoạt động ở cơ sở Mầm non.");
                try(PreparedStatement p=c.prepareStatement("SELECT n.TuNgay,n.DenNgay FROM dbo.LopMamNon l JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo WHERE l.MaLop=? AND l.MaCoSo=? AND l.TrangThai=1")){
                    p.setInt(1,lop);p.setInt(2,coSo);try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new SQLException("Lớp không tồn tại hoặc đã ngừng hoạt động.");
                        if(tu.isBefore(day(r,"TuNgay"))||tu.isAfter(day(r,"DenNgay")))throw new SQLException("Ngày xếp lớp phải thuộc năm học của lớp.");
                    }
                }
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND MaHocVien=? AND TrangThai<>'HUY' AND (DenNgay IS NULL OR DenNgay>=?)")){
                    p.setInt(1,coSo);p.setInt(2,hocSinh);date(p,3,tu);try(ResultSet r=p.executeQuery()){if(r.next())throw new SQLException("Học sinh đã có lớp hiệu lực; hãy kết thúc/chuyển lớp cũ trước.");}
                }
                try(PreparedStatement p=c.prepareStatement("INSERT dbo.XepLopMamNon(MaHocVien,MaCoSo,MaLop,TuNgay,TrangThai) VALUES(?,?,?,?,'HIEU_LUC')")){
                    p.setInt(1,hocSinh);p.setInt(2,coSo);p.setInt(3,lop);date(p,4,tu);p.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public void ketThucXepLop(int coSo,int id,LocalDate den,String trangThai,String lyDo) throws SQLException {
        if(!Set.of("CHUYEN_LOP","NGHI_HAN","HUY").contains(trangThai))throw new SQLException("Trạng thái xếp lớp không hợp lệ.");
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(
            "UPDATE dbo.XepLopMamNon SET DenNgay=?,TrangThai=?,LyDo=? WHERE MaXepLop=? AND MaCoSo=? AND TrangThai='HIEU_LUC' AND TuNgay<=?")) {
            date(p,1,den);p.setString(2,trangThai);p.setString(3,lyDo);p.setInt(4,id);p.setInt(5,coSo);date(p,6,den);
            if(p.executeUpdate()!=1)throw new SQLException("Không thể kết thúc xếp lớp; kiểm tra trạng thái và ngày kết thúc.");
        }
    }
    /** Kết quả tạo lớp từ năm học cũ. Chỉ tạo lớp, không chuyển học sinh. */
    public record KetQuaSaoChep(int daTao, int boQua) {}
    /** Kết quả chuyển học sinh; bỏ qua hồ sơ nghỉ/ngừng và hồ sơ đã được xếp ở nơi khác. */
    public record KetQuaChuyen(int daChuyen, int boQuaNghi, int boQuaTrung) {}

    private record LopNam(int nam, boolean active, LocalDate tu, LocalDate den) {}
    private LopNam docLopNam(Connection c,int coSo,int lop,boolean lock)throws SQLException {
        String sql="SELECT l.MaNamHoc,l.TrangThai,n.TuNgay,n.DenNgay FROM dbo.LopMamNon l " +
            "JOIN dbo.NamHoc n ON n.MaNamHoc=l.MaNamHoc AND n.MaCoSo=l.MaCoSo " +
            "WHERE l.MaCoSo=? AND l.MaLop=?";
        if(lock)sql=sql.replace("dbo.LopMamNon l", "dbo.LopMamNon l WITH (UPDLOCK,HOLDLOCK)");
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,coSo);ps.setInt(2,lop);
            try(ResultSet rs=ps.executeQuery()){
                if(!rs.next())throw new SQLException("Lớp không tồn tại trong cơ sở Mầm non.");
                return new LopNam(rs.getInt(1),rs.getBoolean(2),day(rs,"TuNgay"),day(rs,"DenNgay"));
            }
        }
    }
    private void khoaHocSinh(Connection c,int coSo,int hocSinh)throws SQLException {
        if(!exists(c,"SELECT 1 FROM dbo.HocVien WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=?",hocSinh,coSo))
            throw new SQLException("Không tìm thấy học sinh của cơ sở Mầm non.");
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
    /** Thao tác thủ công, một giao dịch để không tạo một phần danh sách lớp. */
    public KetQuaSaoChep saoChepLop(int coSo,int namNguon,int namDich,List<Integer> dsMaLop)throws SQLException {
        if(namNguon==namDich||dsMaLop==null||dsMaLop.isEmpty())throw new SQLException("Hãy chọn năm học mới và ít nhất một lớp.");
        try(Connection c=Database.moKetNoi()){
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
                for(int id:new LinkedHashSet<>(dsMaLop)){
                    String ten;
                    try(PreparedStatement ps=c.prepareStatement("SELECT TenLop FROM dbo.LopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaLop=? AND MaCoSo=? AND MaNamHoc=?")){
                        ps.setInt(1,id);ps.setInt(2,coSo);ps.setInt(3,namNguon);
                        try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Lớp nguồn không còn hợp lệ: #"+id);ten=rs.getString(1);}
                    }
                    if(existsName(c,coSo,namDich,ten)){skipped++;continue;}
                    try(PreparedStatement ps=c.prepareStatement("INSERT dbo.LopMamNon(MaNamHoc,MaCoSo,TenLop,TrangThai) VALUES(?,?,?,1)")){
                        ps.setInt(1,namDich);ps.setInt(2,coSo);ps.setString(3,ten);created+=ps.executeUpdate();
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
    public List<DuKienChuyen> duKienChuyen(int coSo,int lopNguon,int lopDich)throws SQLException {
        List<DuKienChuyen> out=new ArrayList<>();
        String sql="SELECT x.MaXepLop,x.MaHocVien,h.HoTen,x.TrangThai,h.TrangThaiHoSo, " +
            "CASE WHEN EXISTS(SELECT 1 FROM dbo.XepLopMamNon z WHERE z.MaCoSo=x.MaCoSo " +
            "AND z.MaHocVien=x.MaHocVien AND z.MaLop=? AND z.TrangThai<>'HUY') THEN 1 ELSE 0 END AS DaXep, x.DenNgay " +
            "FROM dbo.XepLopMamNon x JOIN dbo.HocVien h ON h.MaHocVien=x.MaHocVien AND h.MaCoSo=x.MaCoSo " +
            "WHERE x.MaCoSo=? AND x.MaLop=? ORDER BY h.HoTen";
        try(Connection c=Database.moKetNoi();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,lopDich);ps.setInt(2,coSo);ps.setInt(3,lopNguon);
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                String state=rs.getString(4),status;
                if(!"HOAT_DONG".equals(rs.getString(5))||!"HIEU_LUC".equals(state)||day(rs,"DenNgay")!=null)status="Bỏ qua (nghỉ/kết thúc)";
                else if(rs.getInt(6)==1)status="Bỏ qua (đã xếp lớp đích)";
                else status="Dự kiến chuyển";
                out.add(new DuKienChuyen(rs.getInt(1),rs.getInt(2),rs.getString(3),status));
            }}
        }return out;
    }
    /** Chuyển một học sinh sang lớp đích (cùng năm hoặc năm sau) và giữ lại lớp cũ. */
    public void chuyenLop(int coSo,int maXep,int lopDich,LocalDate ngayDich,String reason)throws SQLException {
        try(Connection c=Database.moKetNoi()){
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
                if(!exists(c,"SELECT 1 FROM dbo.HocVien WHERE MaHocVien=? AND MaCoSo=? AND TrangThaiHoSo='HOAT_DONG'",hs,coSo))
                    throw new SQLException("Học sinh đã ngừng sử dụng hồ sơ, không thể chuyển.");
                LopNam source=docLopNam(c,coSo,lopNguon,true), target=docLopNam(c,coSo,lopDich,true);
                if(!target.active()||lopNguon==lopDich)throw new SQLException("Lớp đích phải khác lớp cũ và đang hoạt động.");
                if(target.tu().isBefore(source.tu()))throw new SQLException("Không được chuyển về năm học cũ hơn.");
                if(ngayDich.isBefore(target.tu())||ngayDich.isAfter(target.den())||!ngayDich.isAfter(batDau))
                    throw new SQLException("Ngày bắt đầu lớp mới phải thuộc năm học đích và sau ngày vào lớp cũ.");
                LocalDate last=ngayDich.minusDays(1).isBefore(source.den())?ngayDich.minusDays(1):source.den();
                if(last.isBefore(batDau))throw new SQLException("Ngày chuyển không hợp lệ với thời gian lớp cũ.");
                kiemTraTrungLich(c,coSo,hs,maXep,ngayDich,target.den());
                chuyenMot(c,coSo,maXep,hs,lopDich,ngayDich,last,reason);
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    /** Chuyển hàng loạt từ một lớp năm cũ: hồ sơ Nghỉ không được chuyển. */
    public KetQuaChuyen chuyenNamHoc(int coSo,int lopNguon,int lopDich,LocalDate ngayDich,String reason)throws SQLException {
        try(Connection c=Database.moKetNoi()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                LopNam source=docLopNam(c,coSo,lopNguon,true),target=docLopNam(c,coSo,lopDich,true);
                if(!target.active()||!target.tu().isAfter(source.tu()))throw new SQLException("Lớp đích phải thuộc năm học mới hơn và đang hoạt động.");
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
                    if(exists(c,"SELECT 1 FROM dbo.XepLopMamNon WITH (UPDLOCK,HOLDLOCK) WHERE MaCoSo=? AND MaHocVien=? AND MaLop=? AND TrangThai<>'HUY'",coSo,hs,lopDich)){
                        skipDuplicate++;continue;
                    }
                    if(coTrungLich(c,coSo,hs,xep,ngayDich,target.den())){skipDuplicate++;continue;}
                    LocalDate last=ngayDich.minusDays(1).isBefore(source.den())?ngayDich.minusDays(1):source.den();
                    chuyenMot(c,coSo,xep,hs,lopDich,ngayDich,last,reason);success++;
                }
                c.commit();return new KetQuaChuyen(success,skipNghi,skipDuplicate);
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }

    public List<LuaChon> taiKhoanDiemDanh(int coSo) throws SQLException {
        List<LuaChon> out=new ArrayList<>();
        String sql="SELECT t.MaTaiKhoan,t.TenDangNhap FROM dbo.TaiKhoan t JOIN dbo.TaiKhoanCoSo cs ON cs.MaTaiKhoan=t.MaTaiKhoan WHERE cs.MaCoSo=? AND cs.TrangThai=1 AND t.TrangThai=1 ORDER BY t.TenDangNhap";
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql)){
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
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql)){
            date(p,1,ngay);p.setInt(2,coSo);p.setInt(3,lop);date(p,4,ngay);date(p,5,ngay);
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new DiemDanhMamNon(r.getInt("MaDiemDanh"),r.getInt("MaHocVien"),r.getInt("MaXepLop"),r.getString("HoTen"),ngay,r.getString("TrangThai"),r.getString("GhiChu")));}
        }return out;
    }
    public int luuDiemDanh(int coSo,int lop,LocalDate ngay,int taiKhoan,String lyDo,List<ThayDoiDiemDanh> rows) throws SQLException {
        Set<String> trangThaiHopLe=Set.of("CHUA_DIEM_DANH","CO_MAT","VANG_CO_PHEP","VANG_KHONG_PHEP");
        if(rows.isEmpty())return 0;
        try(Connection c=Database.moKetNoi()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if(!exists(c,"SELECT 1 FROM dbo.TaiKhoanCoSo cs JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=cs.MaTaiKhoan WHERE cs.MaTaiKhoan=? AND cs.MaCoSo=? AND cs.TrangThai=1 AND t.TrangThai=1",taiKhoan,coSo))throw new SQLException("Tài khoản ghi điểm danh chưa hợp lệ cho cơ sở Mầm non.");
                if(exists(c,"SELECT 1 FROM dbo.ThangMamNon WHERE MaCoSo=? AND TrangThai='CHOT' AND Nam=? AND Thang=?",coSo,ngay.getYear(),ngay.getMonthValue()))throw new SQLException("Tháng đã chốt. Không được chỉnh sửa điểm danh.");
                int thayDoi=0;
                for(ThayDoiDiemDanh x:rows){
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
                            p.setInt(1,x.maHocVien());p.setInt(2,coSo);p.setInt(3,x.maXepLop());date(p,4,ngay);p.setString(5,x.trangThai());p.setString(6,ghi);p.setInt(7,taiKhoan);p.executeUpdate();thayDoi++;}
                    }else if(!Objects.equals(old,x.trangThai())||!Objects.equals(oldGhiChu==null?"":oldGhiChu,ghi)){
                        if(lyDo==null||lyDo.trim().isBlank())throw new SQLException("Phải nhập lý do khi sửa điểm danh đã lưu (lưu lịch sử thay đổi).");
                        try(PreparedStatement p=c.prepareStatement("UPDATE dbo.DiemDanhMamNon SET TrangThai=?,GhiChu=?,MaTaiKhoanDiemDanh=?,ThoiDiemCapNhat=SYSUTCDATETIME() WHERE MaDiemDanh=?")){
                            p.setString(1,x.trangThai());p.setString(2,ghi);p.setInt(3,taiKhoan);p.setInt(4,id);p.executeUpdate();}
                        try(PreparedStatement p=c.prepareStatement("INSERT dbo.LichSuSuaDiemDanhMamNon(MaDiemDanh,TrangThaiTruoc,TrangThaiSau,GhiChuTruoc,GhiChuSau,LyDo,MaTaiKhoan) VALUES(?,?,?,?,?,?,?)")){
                            p.setInt(1,id);p.setString(2,old);p.setString(3,x.trangThai());p.setString(4,oldGhiChu);p.setString(5,ghi);p.setString(6,lyDo.trim());p.setInt(7,taiKhoan);p.executeUpdate();thayDoi++;}
                    }
                }
                c.commit();return thayDoi;
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
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql)){
            int i=1;p.setInt(i++,coSo);date(p,i++,tu);date(p,i++,den);if(lop!=null)p.setInt(i++,lop);if(hocSinh!=null)p.setInt(i++,hocSinh);
            try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LichSuDiemDanh(r.getInt(1),day(r,"NgayHoc"),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8)));}
        }return out;
    }
    public List<NhatKySua> nhatKySua(int coSo,int maHocVien,LocalDate ngay) throws SQLException {
        List<NhatKySua> out=new ArrayList<>();
        String sql="SELECT CONVERT(VARCHAR(19),ls.NgaySua,120),ls.TrangThaiTruoc,ls.TrangThaiSau,ls.LyDo,t.TenDangNhap FROM dbo.LichSuSuaDiemDanhMamNon ls JOIN dbo.DiemDanhMamNon d ON d.MaDiemDanh=ls.MaDiemDanh JOIN dbo.TaiKhoan t ON t.MaTaiKhoan=ls.MaTaiKhoan WHERE d.MaCoSo=? AND d.MaHocVien=? AND d.NgayHoc=? ORDER BY ls.NgaySua DESC";
        try(Connection c=Database.moKetNoi();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,coSo);p.setInt(2,maHocVien);date(p,3,ngay);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new NhatKySua(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));}
        }return out;
    }
}
