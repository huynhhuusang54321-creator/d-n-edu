package vn.vinabacus.controller;
import java.sql.SQLException;
import java.util.List;
import vn.vinabacus.model.*;
import vn.vinabacus.service.TuyenSinhService;
public final class TuyenSinhController {
    private final TuyenSinhService service=new TuyenSinhService();
    private final CoSo coSo;
    public TuyenSinhController(CoSo coSo){this.coSo=coSo;}
    public List<KhachTiemNang> timKiem(String tuKhoa,String trangThai)throws SQLException{
        return service.timKiem(coSo,tuKhoa,trangThai);
    }
    public int them(KhachTiemNang k)throws SQLException{return service.them(coSo,k);}
    public void sua(KhachTiemNang k)throws SQLException{service.sua(coSo,k);}
    public int chuyenDoi(int maKhach)throws SQLException{return service.chuyenDoi(coSo,maKhach);}
}
