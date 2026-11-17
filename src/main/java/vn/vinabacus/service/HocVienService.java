package vn.vinabacus.service;
import vn.vinabacus.model.*;
import vn.vinabacus.repository.*;
import java.sql.SQLException;
import java.util.List;
public final class HocVienService {
    private final TV2CoSoRepository coSoRepository=new TV2CoSoRepository();
    private final HocVienRepository repository=new HocVienRepository();
    public int maCoSo(CoSo coSo)throws SQLException{return coSoRepository.layMaCoSo(coSo);}
    public List<HocVien> timKiem(CoSo coSo,String q,String state)throws SQLException {
        return repository.timKiem(maCoSo(coSo),q==null?"":q,state);
    }
    public int them(CoSo coSo,HocVien h)throws SQLException{
        int id=maCoSo(coSo);kiemTra(h);return repository.them(saoChep(h,0,id));
    }
    public void sua(CoSo coSo,HocVien h)throws SQLException{
        int id=maCoSo(coSo);kiemTra(h);
        if(!repository.sua(saoChep(h,h.maHocVien(),id)))throw new SQLException("Không sửa được học viên thuộc cơ sở này");
    }
    private void kiemTra(HocVien h){
        if(h.hoTen()==null||h.hoTen().isBlank())throw new IllegalArgumentException("Bạn chưa nhập họ tên học viên");
        if(!List.of("HOAT_DONG","NGUNG_SU_DUNG").contains(h.trangThaiHoSo()))
            throw new IllegalArgumentException("Trạng thái hồ sơ không hợp lệ");
    }
    private HocVien saoChep(HocVien h,int ma,int cs){return new HocVien(ma,cs,h.hoTen().trim(),h.ngaySinh(),h.gioiTinh(),
        h.tenNguoiLienHe(),h.soDienThoaiLienHe(),h.diaChi(),h.ghiChu(),h.trangThaiHoSo());}
}
