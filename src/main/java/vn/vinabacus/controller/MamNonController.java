package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
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
    public boolean coCauTrucLop()throws SQLException{return service.coCauTrucLop();}
    public void luuLop(int id,int nam,String ten,boolean active,String khoi,Integer siSoToiDa)throws SQLException{service.luuLop(id,nam,ten,active,khoi,siSoToiDa);}
    public List<LuaChon> hocSinh(String tim)throws SQLException{return service.hocSinh(tim);}
    public List<LuaChon> giaoVien()throws SQLException{return service.giaoVien();}
    public List<LuaChon> giaoVienCuaLop(int lop)throws SQLException{return service.giaoVienCuaLop(lop);}
    public void phanCongGiaoVien(int lop,int gv,LocalDate tu,LocalDate den)throws SQLException{service.phanCongGiaoVien(lop,gv,tu,den);}
    public List<XepLopMamNon> xepLop(Integer lop)throws SQLException{return service.xepLop(lop);}
    public void xepHocSinh(int lop,int hocSinh,LocalDate tu)throws SQLException{service.xepHocSinh(lop,hocSinh,tu);}
    public void ketThucXepLop(int id,LocalDate den,String status,String lyDo)throws SQLException{service.ketThucXepLop(id,den,status,lyDo);}
    public KetQuaSaoChep saoChepLop(int nguon,int dich,List<Integer> lop)throws SQLException{return service.saoChepLop(nguon,dich,lop);}
    public List<DuKienChuyen> duKienChuyen(int lopCu,int lopMoi,LocalDate ngay)throws SQLException{return service.duKienChuyen(lopCu,lopMoi,ngay);}
    public void chuyenLop(int xep,int lop,LocalDate ngay,String lyDo)throws SQLException{service.chuyenLop(xep,lop,ngay,lyDo);}
    public KetQuaChuyen chuyenNamHoc(int lopCu,int lopMoi,LocalDate ngay,String lyDo)throws SQLException{return service.chuyenNamHoc(lopCu,lopMoi,ngay,lyDo);}
    public List<LuaChon> taiKhoan()throws SQLException{return service.taiKhoan();}
    public List<DiemDanhMamNon> bangDiemDanh(int lop,LocalDate ngay)throws SQLException{return service.bangDiemDanh(lop,ngay);}
    public int luuDiemDanh(int lop,LocalDate ngay,int account,String reason,List<ThayDoiDiemDanh> items)throws SQLException{return service.luuDiemDanh(lop,ngay,account,reason,items);}
    public List<LichSuDiemDanh> lichSu(Integer lop,Integer hs,LocalDate tu,LocalDate den)throws SQLException{return service.lichSu(lop,hs,tu,den);}
    public List<NhatKySua> nhatKy(int hs,LocalDate ngay)throws SQLException{return service.nhatKy(hs,ngay);}
    public int taoThang(YearMonth ky,int soNgayChuan)throws SQLException{return service.taoThang(ky,soNgayChuan);}
    public int soNgayHocChuan(YearMonth ky)throws SQLException{return service.soNgayHocChuan(ky);}
    public List<NgayHoatDong> lichHoatDong(int nam,YearMonth ky)throws SQLException{return service.lichHoatDong(nam,ky);}
    public void luuLichHoatDong(int nam,YearMonth ky,List<NgayHoatDong> ngay)throws SQLException{service.luuLichHoatDong(nam,ky,ngay);}
    public CauHinhTienAn cauHinhTienAn(YearMonth ky)throws SQLException{return service.cauHinhTienAn(ky);}
    public void luuCauHinhTienAn(YearMonth ky,boolean coMat,boolean vangCoPhep,
        boolean vangKhongPhep,String ghiChu)throws SQLException{service.luuCauHinhTienAn(ky,coMat,vangCoPhep,vangKhongPhep,ghiChu);}
    public int lapBangTinhNhap(int hocSinh,YearMonth ky)throws SQLException{return service.lapBangTinhNhap(hocSinh,ky);}
    public List<BangTinhThang> bangTinhThang(int nam,YearMonth ky)throws SQLException{return service.bangTinhThang(nam,ky);}
    public void chotBangTinh(int bang)throws SQLException{service.chotBangTinh(bang);}
}
