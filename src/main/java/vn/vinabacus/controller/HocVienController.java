package vn.vinabacus.controller;
import java.sql.SQLException;
import java.util.List;
import vn.vinabacus.model.*;
import vn.vinabacus.service.HocVienService;
public final class HocVienController {
    private final HocVienService service=new HocVienService();
    private final CoSo coSo;
    public HocVienController(CoSo coSo){this.coSo=coSo;}
    public List<HocVien> timKiem(String tuKhoa,String trangThai)throws SQLException{
        return service.timKiem(coSo,tuKhoa,trangThai);
    }
    public int them(HocVien h)throws SQLException{return service.them(coSo,h);}
    public void sua(HocVien h)throws SQLException{service.sua(coSo,h);}
}
