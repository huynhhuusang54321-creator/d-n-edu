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
        kiemTra(k);
        if (coSo==CoSo.TRUNG_TAM && !"TIEP_NHAN".equals(k.trangThai()))
            throw new IllegalArgumentException("Hồ sơ Trung tâm mới phải bắt đầu ở trạng thái Tiếp nhận");
        return repository.them(saoChep(k,0,coSoRepository.layMaCoSo(coSo)));
    }
    public void sua(CoSo coSo,KhachTiemNang k)throws SQLException{
        if (coSo==CoSo.TRUNG_TAM)
            throw new IllegalArgumentException("Hãy sửa thông tin hoặc chuyển trạng thái qua thao tác riêng của Trung tâm");
        kiemTra(k);
        if(!repository.sua(saoChep(k,k.maKhach(),coSoRepository.layMaCoSo(coSo))))
            throw new SQLException("Không sửa được hồ sơ: hồ sơ không thuộc cơ sở này hoặc đã liên kết đăng ký/xếp lớp");
    }
    public void suaThongTin(CoSo coSo,KhachTiemNang k)throws SQLException{
        if (coSo!=CoSo.TRUNG_TAM)
            throw new IllegalArgumentException("Thao tác này chỉ dành cho Tuyển sinh Trung tâm");
        if (k==null || k.maKhach()<=0 || k.hoTen()==null || k.hoTen().isBlank())
            throw new IllegalArgumentException("Hãy chọn hồ sơ và nhập họ tên học sinh");
        kiemTra(k);
        repository.suaThongTin(saoChep(k,k.maKhach(),coSoRepository.layMaCoSo(coSo)));
    }
    public void chuyenTrangThai(CoSo coSo,int maKhach,String trangThai)throws SQLException{
        if (coSo!=CoSo.TRUNG_TAM)
            throw new IllegalArgumentException("Thao tác này chỉ dành cho Tuyển sinh Trung tâm");
        if (maKhach<=0) throw new IllegalArgumentException("Hãy chọn hồ sơ tuyển sinh");
        List<String> nguon=nguonTrangThaiHopLe(trangThai);
        repository.chuyenTrangThai(maKhach,coSoRepository.layMaCoSo(coSo),trangThai,nguon);
    }
    /** CHO_XEP_LOP và DA_CHUYEN_DOI chỉ do giao dịch đăng ký/chuyển đổi cập nhật. */
    public static List<String> nguonTrangThaiHopLe(String dich){
        if (dich==null) throw new IllegalArgumentException("Trạng thái không hợp lệ");
        return switch (dich) {
            case "HOC_THU" -> List.of("TIEP_NHAN");
            case "CHO_XAC_NHAN" -> List.of("TIEP_NHAN","HOC_THU");
            case "KHONG_DANG_KY" -> List.of("TIEP_NHAN","HOC_THU","CHO_XAC_NHAN");
            case "TIEP_NHAN" -> List.of("KHONG_DANG_KY");
            default -> throw new IllegalArgumentException("Không thể chuyển trực tiếp sang trạng thái này");
        };
    }
    public int chuyenDoi(CoSo coSo,int maKhach)throws SQLException{
        if (coSo == CoSo.TRUNG_TAM)
            throw new IllegalArgumentException("Trung tâm cần đăng ký khóa học trước khi xếp lớp");
        return repository.chuyenDoi(maKhach,coSoRepository.layMaCoSo(coSo));
    }
    private void kiemTra(KhachTiemNang k){
        if(k.hoTen()==null||k.hoTen().isBlank())throw new IllegalArgumentException("Bạn chưa nhập họ tên khách");
        if(k.gioiTinh()!=null&&!List.of("NAM","NU","KHAC").contains(k.gioiTinh()))
            throw new IllegalArgumentException("Giới tính không hợp lệ");
        if(k.diaChi()!=null&&k.diaChi().length()>400)throw new IllegalArgumentException("Địa chỉ tối đa 400 ký tự");
        if(!List.of("TIEP_NHAN","HOC_THU","CHO_XAC_NHAN","CHO_XEP_LOP","KHONG_DANG_KY").contains(k.trangThai()))
            throw new IllegalArgumentException("Trạng thái tuyển sinh không hợp lệ");
    }
    private KhachTiemNang saoChep(KhachTiemNang k,int ma,int cs){return new KhachTiemNang(ma,cs,k.hoTen().trim(),
        k.soDienThoai(),k.tenNguoiLienHe(),k.nhuCau(),k.trangThai(),k.daHocThu(),k.ghiChu(),
        k.ngaySinh(),k.gioiTinh(),k.diaChi());}
}
