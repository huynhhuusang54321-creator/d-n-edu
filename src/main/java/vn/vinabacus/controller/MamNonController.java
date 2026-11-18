package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.*;
import vn.vinabacus.repository.MamNonRepository.*;
import vn.vinabacus.service.MamNonService;

/** Controller TV4: giao diện không thao tác SQL trực tiếp. */
public final class MamNonController {
    private final MamNonService service=new MamNonService();
    public List<NamHocMamNon> namHoc()throws SQLException{return service.namHoc();}
    public void luuNamHoc(int id,String ten,LocalDate tu,LocalDate den)throws SQLException{service.luuNamHoc(id,ten,tu,den);}
    public List<LopMamNon> lop(Integer nam,String tim)throws SQLException{return service.lop(nam,tim);}
    public void luuLop(int id,int nam,String ten,boolean active)throws SQLException{service.luuLop(id,nam,ten,active);}
    public List<LuaChon> hocSinh(String tim)throws SQLException{return service.hocSinh(tim);}
    public List<LuaChon> giaoVien()throws SQLException{return service.giaoVien();}
    public List<LuaChon> giaoVienCuaLop(int lop)throws SQLException{return service.giaoVienCuaLop(lop);}
    public void phanCongGiaoVien(int lop,int gv,LocalDate tu,LocalDate den)throws SQLException{service.phanCongGiaoVien(lop,gv,tu,den);}
    public List<XepLopMamNon> xepLop(Integer lop)throws SQLException{return service.xepLop(lop);}
    public void xepHocSinh(int lop,int hocSinh,LocalDate tu)throws SQLException{service.xepHocSinh(lop,hocSinh,tu);}
    public void ketThucXepLop(int id,LocalDate den,String status,String lyDo)throws SQLException{service.ketThucXepLop(id,den,status,lyDo);}
    public KetQuaSaoChep saoChepLop(int nguon,int dich,List<Integer> lop)throws SQLException{return service.saoChepLop(nguon,dich,lop);}
    public List<DuKienChuyen> duKienChuyen(int lopCu,int lopMoi)throws SQLException{return service.duKienChuyen(lopCu,lopMoi);}
    public void chuyenLop(int xep,int lop,LocalDate ngay,String lyDo)throws SQLException{service.chuyenLop(xep,lop,ngay,lyDo);}
    public KetQuaChuyen chuyenNamHoc(int lopCu,int lopMoi,LocalDate ngay,String lyDo)throws SQLException{return service.chuyenNamHoc(lopCu,lopMoi,ngay,lyDo);}
    public List<LuaChon> taiKhoan()throws SQLException{return service.taiKhoan();}
    public List<DiemDanhMamNon> bangDiemDanh(int lop,LocalDate ngay)throws SQLException{return service.bangDiemDanh(lop,ngay);}
    public int luuDiemDanh(int lop,LocalDate ngay,int account,String reason,List<ThayDoiDiemDanh> items)throws SQLException{return service.luuDiemDanh(lop,ngay,account,reason,items);}
    public List<LichSuDiemDanh> lichSu(Integer lop,Integer hs,LocalDate tu,LocalDate den)throws SQLException{return service.lichSu(lop,hs,tu,den);}
    public List<NhatKySua> nhatKy(int hs,LocalDate ngay)throws SQLException{return service.nhatKy(hs,ngay);}
}
