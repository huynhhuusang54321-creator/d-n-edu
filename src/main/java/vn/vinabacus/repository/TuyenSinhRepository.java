package vn.vinabacus.repository;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.KhachTiemNang;

public final class TuyenSinhRepository {
    private static boolean coThongTinCaNhan(Connection c)throws SQLException{
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT CASE WHEN COL_LENGTH('dbo.KhachTiemNang','NgaySinh') IS NOT NULL AND COL_LENGTH('dbo.KhachTiemNang','GioiTinh') IS NOT NULL AND COL_LENGTH('dbo.KhachTiemNang','DiaChi') IS NOT NULL THEN 1 ELSE 0 END")){
            r.next();return r.getInt(1)==1;
        }
    }
    private static void canCoThongTinCaNhan(KhachTiemNang k,boolean ready)throws SQLException{
        if(!ready&&(k.ngaySinh()!=null||k.gioiTinh()!=null||k.diaChi()!=null))
            throw new SQLException("CSDL chưa có ba cột thông tin tuyển sinh; cần duyệt migration thông tin cá nhân trước khi lưu.");
    }
    private static void ngay(PreparedStatement p,int col,LocalDate value)throws SQLException{
        if(value==null)p.setNull(col,Types.DATE);else p.setDate(col,java.sql.Date.valueOf(value));
    }
    private static LocalDate ngay(ResultSet r,String col)throws SQLException{
        java.sql.Date value=r.getDate(col);return value==null?null:value.toLocalDate();
    }
    public List<KhachTiemNang> timKiem(int maCoSo,String tuKhoa,String trangThai) throws SQLException {
        try(Connection c=Database.moKetNoi()){
        boolean ready=coThongTinCaNhan(c);
        StringBuilder sql=new StringBuilder("SELECT MaKhach,MaCoSo,HoTen,SoDienThoai,TenNguoiLienHe,"+
          "NhuCau,TrangThai,DaHocThu,GhiChu,"+
          (ready?"NgaySinh,GioiTinh,DiaChi":"CAST(NULL AS DATE) AS NgaySinh,CAST(NULL AS VARCHAR(15)) AS GioiTinh,CAST(NULL AS NVARCHAR(400)) AS DiaChi")+
          " FROM dbo.KhachTiemNang WHERE MaCoSo=?");
        if(!"TAT_CA".equals(trangThai))sql.append(" AND TrangThai=?");
        if(!tuKhoa.isBlank())sql.append(" AND (CAST(MaKhach AS NVARCHAR(30)) LIKE ? "+
          "OR HoTen COLLATE Latin1_General_100_CI_AI LIKE ? OR SoDienThoai LIKE ?)");
        sql.append(" ORDER BY MaKhach DESC");
        List<KhachTiemNang> list=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement(sql.toString())){
            int p=1;ps.setInt(p++,maCoSo);
            if(!"TAT_CA".equals(trangThai))ps.setString(p++,trangThai);
            if(!tuKhoa.isBlank()){
                String q="%"+tuKhoa.trim()+"%";ps.setString(p++,q);ps.setString(p++,q);ps.setString(p++,q);
            }
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()) list.add(new KhachTiemNang(rs.getInt("MaKhach"),rs.getInt("MaCoSo"),
                  rs.getString("HoTen"),rs.getString("SoDienThoai"),rs.getString("TenNguoiLienHe"),
                  rs.getString("NhuCau"),rs.getString("TrangThai"),rs.getBoolean("DaHocThu"),rs.getString("GhiChu"),
                  ngay(rs,"NgaySinh"),rs.getString("GioiTinh"),rs.getString("DiaChi")));
            }
        }
        return list;
        }
    }
    public int them(KhachTiemNang k) throws SQLException {
        try(Connection c=Database.moKetNoi()){
        boolean ready=coThongTinCaNhan(c);canCoThongTinCaNhan(k,ready);
        String sql="INSERT dbo.KhachTiemNang(MaCoSo,HoTen,SoDienThoai,TenNguoiLienHe,NhuCau,TrangThai,DaHocThu,GhiChu"+
            (ready?",NgaySinh,GioiTinh,DiaChi":"")+") VALUES(?,?,?,?,?,?,?,?"+(ready?",?,?,?":"")+")";
        try(PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1,k.maCoSo());ps.setString(2,k.hoTen());ps.setString(3,k.soDienThoai());
            ps.setString(4,k.tenNguoiLienHe());ps.setString(5,k.nhuCau());ps.setString(6,k.trangThai());
            ps.setBoolean(7,k.daHocThu());ps.setString(8,k.ghiChu());
            if(ready){ngay(ps,9,k.ngaySinh());ps.setString(10,k.gioiTinh());ps.setString(11,k.diaChi());}
            ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())return rs.getInt(1);}
            throw new SQLException("Không lấy được mã tuyển sinh");
        }
        }
    }
    public boolean sua(KhachTiemNang k) throws SQLException {
        String sql="UPDATE dbo.KhachTiemNang SET HoTen=?,SoDienThoai=?,TenNguoiLienHe=?,NhuCau=?,"+
                   "TrangThai=?,DaHocThu=?,GhiChu=? WHERE MaKhach=? AND MaCoSo=? AND TrangThai<>'DA_CHUYEN_DOI'"+
                   " AND NOT EXISTS (SELECT 1 FROM dbo.YeuCauXepLop y"+
                   " WHERE y.MaKhach=? AND y.MaCoSo=?)";
        try(Connection c=Database.moKetNoi();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,k.hoTen());ps.setString(2,k.soDienThoai());ps.setString(3,k.tenNguoiLienHe());
            ps.setString(4,k.nhuCau());ps.setString(5,k.trangThai());ps.setBoolean(6,k.daHocThu());
            ps.setString(7,k.ghiChu());ps.setInt(8,k.maKhach());ps.setInt(9,k.maCoSo());
            ps.setInt(10,k.maKhach());ps.setInt(11,k.maCoSo());
            return ps.executeUpdate()==1;
        }
    }
    /** Sửa thông tin hồ sơ mà không ghi đè trạng thái do luồng đăng ký quản lý. */
    public void suaThongTin(KhachTiemNang k) throws SQLException {
        try (Connection c=Database.moKetNoi()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                boolean ready=coThongTinCaNhan(c);canCoThongTinCaNhan(k,ready);
                String trangThai;
                try (PreparedStatement ps=c.prepareStatement("SELECT TrangThai FROM dbo.KhachTiemNang WITH (UPDLOCK,HOLDLOCK) WHERE MaKhach=? AND MaCoSo=?")) {
                    ps.setInt(1,k.maKhach()); ps.setInt(2,k.maCoSo());
                    try (ResultSet rs=ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy hồ sơ tuyển sinh thuộc cơ sở này");
                        trangThai=rs.getString(1);
                    }
                }
                if ("DA_CHUYEN_DOI".equals(trangThai))
                    throw new SQLException("Hồ sơ đã chuyển đổi; hãy sửa thông tin tại Quản lý học viên");
                Integer maHocVien=null;
                try (PreparedStatement ps=c.prepareStatement("SELECT DISTINCT MaHocVien FROM dbo.YeuCauXepLop WITH (UPDLOCK,HOLDLOCK) WHERE MaKhach=? AND MaCoSo=?")) {
                    ps.setInt(1,k.maKhach()); ps.setInt(2,k.maCoSo());
                    try (ResultSet rs=ps.executeQuery()) {
                        while (rs.next()) {
                            int ma=rs.getInt(1);
                            if (maHocVien!=null && maHocVien!=ma)
                                throw new SQLException("Hồ sơ đang liên kết nhiều học viên; cần kiểm tra dữ liệu");
                            maHocVien=ma;
                        }
                    }
                }
                try (PreparedStatement ps=c.prepareStatement("UPDATE dbo.KhachTiemNang SET HoTen=?,SoDienThoai=?,TenNguoiLienHe=?,NhuCau=?,GhiChu=?"+
                    (ready?",NgaySinh=?,GioiTinh=?,DiaChi=?":"")+" WHERE MaKhach=? AND MaCoSo=? AND TrangThai<>'DA_CHUYEN_DOI'")) {
                    ps.setString(1,k.hoTen()); ps.setString(2,k.soDienThoai());
                    ps.setString(3,k.tenNguoiLienHe()); ps.setString(4,k.nhuCau());
                    ps.setString(5,k.ghiChu());int i=6;
                    if(ready){ngay(ps,i++,k.ngaySinh());ps.setString(i++,k.gioiTinh());ps.setString(i++,k.diaChi());}
                    ps.setInt(i++,k.maKhach()); ps.setInt(i,k.maCoSo());
                    if (ps.executeUpdate()!=1) throw new SQLException("Không lưu được thông tin tuyển sinh");
                }
                if (maHocVien!=null) {
                    // Học viên tạm có thể đã được chỉnh riêng; chỉ điền ô trống, chặn xung đột.
                    try(PreparedStatement ps=c.prepareStatement("SELECT HoTen,TenNguoiLienHe,SoDienThoaiLienHe,NgaySinh,GioiTinh,DiaChi FROM dbo.HocVien WITH (UPDLOCK,HOLDLOCK) WHERE MaHocVien=? AND MaCoSo=?")){
                        ps.setInt(1,maHocVien);ps.setInt(2,k.maCoSo());
                        try(ResultSet r=ps.executeQuery()){
                            if(!r.next())throw new SQLException("Không tìm thấy học viên liên kết.");
                            if((r.getString("HoTen")!=null&&!r.getString("HoTen").equals(k.hoTen()))
                                ||(r.getString("TenNguoiLienHe")!=null&&k.tenNguoiLienHe()!=null&&!r.getString("TenNguoiLienHe").equals(k.tenNguoiLienHe()))
                                ||(r.getString("SoDienThoaiLienHe")!=null&&k.soDienThoai()!=null&&!r.getString("SoDienThoaiLienHe").equals(k.soDienThoai()))
                                ||(ready&&ngay(r,"NgaySinh")!=null&&k.ngaySinh()!=null&&!ngay(r,"NgaySinh").equals(k.ngaySinh()))
                                ||(ready&&r.getString("GioiTinh")!=null&&k.gioiTinh()!=null&&!r.getString("GioiTinh").equals(k.gioiTinh()))
                                ||(ready&&r.getString("DiaChi")!=null&&k.diaChi()!=null&&!r.getString("DiaChi").equals(k.diaChi())))
                                throw new SQLException("Thông tin cá nhân khác học viên đã liên kết; hãy đối chiếu tại Chi tiết học viên trước khi lưu.");
                        }
                    }
                    try (PreparedStatement ps=c.prepareStatement("UPDATE dbo.HocVien SET HoTen=COALESCE(HoTen,?),TenNguoiLienHe=COALESCE(TenNguoiLienHe,?),SoDienThoaiLienHe=COALESCE(SoDienThoaiLienHe,?) WHERE MaHocVien=? AND MaCoSo=?")) {
                        ps.setString(1,k.hoTen()); ps.setString(2,k.tenNguoiLienHe());
                        ps.setString(3,k.soDienThoai()); ps.setInt(4,maHocVien); ps.setInt(5,k.maCoSo());
                        if (ps.executeUpdate()!=1) throw new SQLException("Không đồng bộ được học viên liên kết");
                    }
                    if(ready)try(PreparedStatement ps=c.prepareStatement("UPDATE dbo.HocVien SET NgaySinh=COALESCE(NgaySinh,?),GioiTinh=COALESCE(GioiTinh,?),DiaChi=COALESCE(DiaChi,?) WHERE MaHocVien=? AND MaCoSo=?")){
                        ngay(ps,1,k.ngaySinh());ps.setString(2,k.gioiTinh());ps.setString(3,k.diaChi());
                        ps.setInt(4,maHocVien);ps.setInt(5,k.maCoSo());ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }

    /** Chỉ cho các chuyển trạng thái tuyển sinh được khai báo bởi Service. */
    public void chuyenTrangThai(int maKhach, int maCoSo, String trangThai,
                               List<String> tuTrangThai) throws SQLException {
        StringBuilder sql=new StringBuilder("UPDATE dbo.KhachTiemNang SET DaHocThu=CASE WHEN TrangThai='HOC_THU' AND ?='CHO_XAC_NHAN' THEN 1 ELSE DaHocThu END,TrangThai=? WHERE MaKhach=? AND MaCoSo=? AND TrangThai IN (");
        for (int i=0;i<tuTrangThai.size();i++) {
            if (i>0) sql.append(',');
            sql.append('?');
        }
        sql.append(") AND NOT EXISTS (SELECT 1 FROM dbo.YeuCauXepLop WHERE MaKhach=? AND MaCoSo=?)");
        try (Connection c=Database.moKetNoi(); PreparedStatement ps=c.prepareStatement(sql.toString())) {
            int p=1;
            ps.setString(p++,trangThai); ps.setString(p++,trangThai);
            ps.setInt(p++,maKhach); ps.setInt(p++,maCoSo);
            for (String tu:tuTrangThai) ps.setString(p++,tu);
            ps.setInt(p++,maKhach); ps.setInt(p++,maCoSo);
            if (ps.executeUpdate()!=1)
                throw new SQLException("Không thể chuyển trạng thái: hồ sơ không đúng bước, đã đăng ký khóa hoặc không thuộc Trung tâm");
        }
    }
    /** Chuyen lead thanh HocVien va YeuCauXepLop trong 1 transaction. MaKhoaHoc NULL cho xep lop sau. */
    public int chuyenDoi(int maKhach,int maCoSo) throws SQLException {
        try(Connection c=Database.moKetNoi()){
            c.setAutoCommit(false);
            try {
                String hoTen,tenLienHe,sdt,nhuCau,trangThai;
                try(PreparedStatement ps=c.prepareStatement("SELECT HoTen,TenNguoiLienHe,SoDienThoai,NhuCau,TrangThai "+
                   "FROM dbo.KhachTiemNang WITH (UPDLOCK,ROWLOCK) WHERE MaKhach=? AND MaCoSo=?")){
                    ps.setInt(1,maKhach);ps.setInt(2,maCoSo);
                    try(ResultSet rs=ps.executeQuery()){
                        if(!rs.next())throw new SQLException("Không tìm thấy hồ sơ tuyển sinh thuộc cơ sở này");
                        hoTen=rs.getString(1);tenLienHe=rs.getString(2);sdt=rs.getString(3);
                        nhuCau=rs.getString(4);trangThai=rs.getString(5);
                    }
                }
                if("DA_CHUYEN_DOI".equals(trangThai))
                    throw new SQLException("Hồ sơ đã chuyển đổi thành học viên trước đó");
                if("KHONG_DANG_KY".equals(trangThai))
                    throw new SQLException("Hồ sơ Không đăng ký không thể chuyển đổi");
                int maHocVien;
                try(PreparedStatement ps=c.prepareStatement("INSERT dbo.HocVien(MaCoSo,HoTen,TenNguoiLienHe,"+
                    "SoDienThoaiLienHe,GhiChu) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    ps.setInt(1,maCoSo);ps.setString(2,hoTen);ps.setString(3,tenLienHe);
                    ps.setString(4,sdt);ps.setString(5,"Tuyển sinh từ khách #"+maKhach);
                    ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){
                        if(!rs.next())throw new SQLException("Không lấy được MaHocVien");
                        maHocVien=rs.getInt(1);
                    }
                }
                try(PreparedStatement ps=c.prepareStatement("INSERT dbo.YeuCauXepLop(MaKhach,MaHocVien,MaCoSo,NhuCau) VALUES(?,?,?,?)")){
                    ps.setInt(1,maKhach);ps.setInt(2,maHocVien);ps.setInt(3,maCoSo);ps.setString(4,nhuCau);ps.executeUpdate();
                }
                try(PreparedStatement ps=c.prepareStatement("UPDATE dbo.KhachTiemNang SET TrangThai='DA_CHUYEN_DOI' WHERE MaKhach=? AND MaCoSo=?")){
                    ps.setInt(1,maKhach);ps.setInt(2,maCoSo);ps.executeUpdate();
                }
                c.commit();return maHocVien;
            } catch(SQLException|RuntimeException ex) {
                try {c.rollback();}catch(SQLException roll){ex.addSuppressed(roll);}
                throw ex;
            } finally {c.setAutoCommit(true);}
        }
    }
}
