package vn.vinabacus.service;
import vn.vinabacus.model.*;
import vn.vinabacus.repository.*;
import java.sql.SQLException;
import java.util.List;
public final class TuyenSinhService {
    private final TV2CoSoRepository coSoRepository=new TV2CoSoRepository();
    private final TuyenSinhRepository repository=new TuyenSinhRepository();
    public List<KhachTiemNang> timKiem(CoSo coSo,String q,String state)throws SQLException{
        return repository.timKiem(coSoRepository.layMaCoSo(coSo),q==null?"":q,state);
    }
    public int them(CoSo coSo,KhachTiemNang k)throws SQLException{
        kiemTra(k);return repository.them(saoChep(k,0,coSoRepository.layMaCoSo(coSo)));
    }
    public void sua(CoSo coSo,KhachTiemNang k)throws SQLException{
        kiemTra(k);if(!repository.sua(saoChep(k,k.maKhach(),coSoRepository.layMaCoSo(coSo))))
            throw new SQLException("Không sửa được hồ sơ tuyển sinh thuộc cơ sở này");
    }
    public int chuyenDoi(CoSo coSo,int maKhach)throws SQLException{
        return repository.chuyenDoi(maKhach,coSoRepository.layMaCoSo(coSo));
    }
    private void kiemTra(KhachTiemNang k){
        if(k.hoTen()==null||k.hoTen().isBlank())throw new IllegalArgumentException("Bạn chưa nhập họ tên khách");
        if(!List.of("TIEP_NHAN","HOC_THU","CHO_XAC_NHAN","CHO_XEP_LOP","KHONG_DANG_KY").contains(k.trangThai()))
            throw new IllegalArgumentException("Trạng thái tuyển sinh không hợp lệ");
    }
    private KhachTiemNang saoChep(KhachTiemNang k,int ma,int cs){return new KhachTiemNang(ma,cs,k.hoTen().trim(),
        k.soDienThoai(),k.tenNguoiLienHe(),k.nhuCau(),k.trangThai(),k.daHocThu(),k.ghiChu());}
}
