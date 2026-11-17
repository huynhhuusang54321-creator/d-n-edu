package vn.vinabacus.repository;
import vn.vinabacus.config.Database;
import vn.vinabacus.model.HocVien;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public final class HocVienRepository {
    public List<HocVien> timKiem(int maCoSo, String tuKhoa, String trangThai) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT MaHocVien,MaCoSo,HoTen,NgaySinh,GioiTinh,TenNguoiLienHe,"+
            "SoDienThoaiLienHe,DiaChi,GhiChu,TrangThaiHoSo FROM dbo.HocVien WHERE MaCoSo=?");
        if (!"TAT_CA".equals(trangThai)) sql.append(" AND TrangThaiHoSo=?");
        if (!tuKhoa.isBlank()) sql.append(" AND (CAST(MaHocVien AS NVARCHAR(30)) LIKE ? "+
                "OR HoTen COLLATE Latin1_General_100_CI_AI LIKE ? "+
                "OR SoDienThoaiLienHe LIKE ?)");
        sql.append(" ORDER BY MaHocVien DESC");
        List<HocVien> ketQua = new ArrayList<>();
        try(Connection con=Database.moKetNoi(); PreparedStatement ps=con.prepareStatement(sql.toString())) {
            int p=1; ps.setInt(p++,maCoSo);
            if (!"TAT_CA".equals(trangThai)) ps.setString(p++,trangThai);
            if (!tuKhoa.isBlank()) {
                String q="%"+tuKhoa.trim()+"%";
                ps.setString(p++,q); ps.setString(p++,q); ps.setString(p++,q);
            }
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) ketQua.add(doc(rs));
            }
        }
        return ketQua;
    }
    private HocVien doc(ResultSet rs) throws SQLException {
        Date d=rs.getDate("NgaySinh"); LocalDate ngay=d==null?null:d.toLocalDate();
        return new HocVien(rs.getInt("MaHocVien"),rs.getInt("MaCoSo"),rs.getString("HoTen"),ngay,
            rs.getString("GioiTinh"),rs.getString("TenNguoiLienHe"),
            rs.getString("SoDienThoaiLienHe"),rs.getString("DiaChi"),
            rs.getString("GhiChu"),rs.getString("TrangThaiHoSo"));
    }
    public int them(HocVien h) throws SQLException {
        String sql="INSERT dbo.HocVien(MaCoSo,HoTen,NgaySinh,GioiTinh,TenNguoiLienHe,"+
            "SoDienThoaiLienHe,DiaChi,GhiChu,TrangThaiHoSo) VALUES(?,?,?,?,?,?,?,?,?)";
        try(Connection con=Database.moKetNoi();
            PreparedStatement ps=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,h.maCoSo()); ps.setString(2,h.hoTen());
            setNgay(ps,3,h.ngaySinh()); ps.setString(4,h.gioiTinh());
            ps.setString(5,h.tenNguoiLienHe()); ps.setString(6,h.soDienThoaiLienHe());
            ps.setString(7,h.diaChi()); ps.setString(8,h.ghiChu()); ps.setString(9,h.trangThaiHoSo());
            ps.executeUpdate();
            try(ResultSet keys=ps.getGeneratedKeys()){if(keys.next())return keys.getInt(1);}
            throw new SQLException("Không lấy được mã học viên vừa thêm");
        }
    }
    public boolean sua(HocVien h) throws SQLException {
        String sql="UPDATE dbo.HocVien SET HoTen=?,NgaySinh=?,GioiTinh=?,TenNguoiLienHe=?,"+
           "SoDienThoaiLienHe=?,DiaChi=?,GhiChu=?,TrangThaiHoSo=? WHERE MaHocVien=? AND MaCoSo=?";
        try(Connection con=Database.moKetNoi();PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setString(1,h.hoTen());setNgay(ps,2,h.ngaySinh());ps.setString(3,h.gioiTinh());
            ps.setString(4,h.tenNguoiLienHe());ps.setString(5,h.soDienThoaiLienHe());
            ps.setString(6,h.diaChi());ps.setString(7,h.ghiChu());ps.setString(8,h.trangThaiHoSo());
            ps.setInt(9,h.maHocVien());ps.setInt(10,h.maCoSo());
            return ps.executeUpdate()==1;
        }
    }
    private static void setNgay(PreparedStatement ps,int idx,LocalDate date) throws SQLException {
        if(date==null) ps.setNull(idx,Types.DATE); else ps.setDate(idx,Date.valueOf(date));
    }
}
