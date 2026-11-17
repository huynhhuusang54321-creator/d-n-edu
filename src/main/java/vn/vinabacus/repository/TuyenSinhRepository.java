package vn.vinabacus.repository;
import java.sql.*;
import java.util.*;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.KhachTiemNang;

public final class TuyenSinhRepository {
    public List<KhachTiemNang> timKiem(int maCoSo,String tuKhoa,String trangThai) throws SQLException {
        StringBuilder sql=new StringBuilder("SELECT MaKhach,MaCoSo,HoTen,SoDienThoai,TenNguoiLienHe,"+
          "NhuCau,TrangThai,DaHocThu,GhiChu FROM dbo.KhachTiemNang WHERE MaCoSo=?");
        if(!"TAT_CA".equals(trangThai))sql.append(" AND TrangThai=?");
        if(!tuKhoa.isBlank())sql.append(" AND (CAST(MaKhach AS NVARCHAR(30)) LIKE ? "+
          "OR HoTen COLLATE Latin1_General_100_CI_AI LIKE ? OR SoDienThoai LIKE ?)");
        sql.append(" ORDER BY MaKhach DESC");
        List<KhachTiemNang> list=new ArrayList<>();
        try(Connection c=Database.moKetNoi();PreparedStatement ps=c.prepareStatement(sql.toString())){
            int p=1;ps.setInt(p++,maCoSo);
            if(!"TAT_CA".equals(trangThai))ps.setString(p++,trangThai);
            if(!tuKhoa.isBlank()){
                String q="%"+tuKhoa.trim()+"%";ps.setString(p++,q);ps.setString(p++,q);ps.setString(p++,q);
            }
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()) list.add(new KhachTiemNang(rs.getInt("MaKhach"),rs.getInt("MaCoSo"),
                  rs.getString("HoTen"),rs.getString("SoDienThoai"),rs.getString("TenNguoiLienHe"),
                  rs.getString("NhuCau"),rs.getString("TrangThai"),rs.getBoolean("DaHocThu"),rs.getString("GhiChu")));
            }
        }
        return list;
    }
    public int them(KhachTiemNang k) throws SQLException {
        String sql="INSERT dbo.KhachTiemNang(MaCoSo,HoTen,SoDienThoai,TenNguoiLienHe,NhuCau,TrangThai,DaHocThu,GhiChu) "+
                   "VALUES(?,?,?,?,?,?,?,?)";
        try(Connection c=Database.moKetNoi();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1,k.maCoSo());ps.setString(2,k.hoTen());ps.setString(3,k.soDienThoai());
            ps.setString(4,k.tenNguoiLienHe());ps.setString(5,k.nhuCau());ps.setString(6,k.trangThai());
            ps.setBoolean(7,k.daHocThu());ps.setString(8,k.ghiChu());
            ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())return rs.getInt(1);}
            throw new SQLException("Không lấy được mã tuyển sinh");
        }
    }
    public boolean sua(KhachTiemNang k) throws SQLException {
        String sql="UPDATE dbo.KhachTiemNang SET HoTen=?,SoDienThoai=?,TenNguoiLienHe=?,NhuCau=?,"+
                   "TrangThai=?,DaHocThu=?,GhiChu=? WHERE MaKhach=? AND MaCoSo=? AND TrangThai<>'DA_CHUYEN_DOI'";
        try(Connection c=Database.moKetNoi();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,k.hoTen());ps.setString(2,k.soDienThoai());ps.setString(3,k.tenNguoiLienHe());
            ps.setString(4,k.nhuCau());ps.setString(5,k.trangThai());ps.setBoolean(6,k.daHocThu());
            ps.setString(7,k.ghiChu());ps.setInt(8,k.maKhach());ps.setInt(9,k.maCoSo());
            return ps.executeUpdate()==1;
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
