package vn.vinabacus.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.*;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.MamNonRepository;
import vn.vinabacus.repository.MamNonRepository.*;

/** Nghiệp vụ của thành viên TV4: Năm học, Lớp, Xếp lớp, Điểm danh. */
public final class MamNonService {
    private final MamNonRepository repo=new MamNonRepository();
    private final CoSoRepository coSoRepo=new CoSoRepository();
    private int coSo()throws SQLException{return coSoRepo.timMaCoSo(CoSo.MAM_NON);}
    private static String ten(String t,int max,String nhan)throws SQLException {
        if(t==null||t.trim().isBlank()||t.trim().length()>max)throw new SQLException(nhan+" không được trống và tối đa "+max+" ký tự.");
        return t.trim();
    }
    private static void khoang(LocalDate tu,LocalDate den)throws SQLException{
        if(tu==null||den==null||den.isBefore(tu))throw new SQLException("Khoảng ngày không hợp lệ.");
    }
    public List<NamHocMamNon> namHoc()throws SQLException{return repo.namHoc(coSo());}
    public void luuNamHoc(int id,String ten,LocalDate tu,LocalDate den)throws SQLException{
        khoang(tu,den);repo.luuNamHoc(coSo(),id,ten(ten,50,"Tên năm học"),tu,den);
    }
    public List<LopMamNon> lop(Integer nam,String tim)throws SQLException{return repo.lop(coSo(),nam,tim);}
    public void luuLop(int id,int nam,String ten,boolean active)throws SQLException{
        if(nam<=0)throw new SQLException("Phải chọn năm học.");repo.luuLop(coSo(),id,nam,ten(ten,150,"Tên lớp"),active);
    }
    public List<LuaChon> hocSinh(String tuKhoa)throws SQLException{return repo.hocSinh(coSo(),tuKhoa==null?"":tuKhoa.trim());}
    public List<LuaChon> giaoVien()throws SQLException{return repo.giaoVien(coSo());}
    public List<LuaChon> giaoVienCuaLop(int lop)throws SQLException{return repo.giaoVienCuaLop(coSo(),lop);}
    public void phanCongGiaoVien(int lop,int gv,LocalDate tu,LocalDate den)throws SQLException{
        if(tu==null||(den!=null&&den.isBefore(tu)))throw new SQLException("Ngày phân công không hợp lệ.");
        repo.phanCongGiaoVien(coSo(),lop,gv,tu,den);
    }
    public List<XepLopMamNon> xepLop(Integer lop)throws SQLException{return repo.xepLop(coSo(),lop);}
    public void xepHocSinh(int lop,int hocSinh,LocalDate tu)throws SQLException{
        if(lop<=0||hocSinh<=0||tu==null)throw new SQLException("Thiếu lớp, học sinh hoặc ngày bắt đầu.");
        repo.xepHocSinh(coSo(),lop,hocSinh,tu);
    }
    public void ketThucXepLop(int id,LocalDate den,String trangThai,String lyDo)throws SQLException{
        if(den==null)throw new SQLException("Cần nhập ngày kết thúc.");
        repo.ketThucXepLop(coSo(),id,den,trangThai,ten(lyDo,500,"Lý do"));
    }
    public MamNonRepository.KetQuaSaoChep saoChepLop(int nguon,int dich,List<Integer> lop)throws SQLException{
        if(nguon<=0||dich<=0||lop==null||lop.isEmpty())throw new SQLException("Cần chọn năm học nguồn, đích và lớp.");
        return repo.saoChepLop(coSo(),nguon,dich,lop);
    }
    private String lyDoChuyen(String lyDo)throws SQLException{return ten(lyDo,500,"Lý do chuyển");}
    public List<MamNonRepository.DuKienChuyen> duKienChuyen(int lopCu,int lopMoi)throws SQLException{
        return repo.duKienChuyen(coSo(),lopCu,lopMoi);
    }
    public void chuyenLop(int xepId,int lopDich,LocalDate ngay,String lyDo)throws SQLException{
        if(xepId<=0||lopDich<=0||ngay==null)throw new SQLException("Thông tin chuyển lớp không hợp lệ.");
        repo.chuyenLop(coSo(),xepId,lopDich,ngay,lyDoChuyen(lyDo));
    }
    public MamNonRepository.KetQuaChuyen chuyenNamHoc(int lopCu,int lopMoi,LocalDate ngay,String lyDo)throws SQLException{
        if(lopCu<=0||lopMoi<=0||ngay==null)throw new SQLException("Thông tin chuyển năm học không hợp lệ.");
        return repo.chuyenNamHoc(coSo(),lopCu,lopMoi,ngay,lyDoChuyen(lyDo));
    }
    public List<LuaChon> taiKhoan()throws SQLException{return repo.taiKhoanDiemDanh(coSo());}
    public List<DiemDanhMamNon> bangDiemDanh(int lop,LocalDate ngay)throws SQLException{
        if(lop<=0||ngay==null)throw new SQLException("Phải chọn lớp và ngày học.");return repo.bangDiemDanh(coSo(),lop,ngay);
    }
    public int luuDiemDanh(int lop,LocalDate ngay,int taiKhoan,String lyDo,List<ThayDoiDiemDanh> ds)throws SQLException{
        if(lop<=0||ngay==null||taiKhoan<=0)throw new SQLException("Phải chọn lớp, ngày và tài khoản ghi nhận.");
        if(lyDo!=null&&lyDo.length()>500)throw new SQLException("Lý do sửa điểm danh không quá 500 ký tự.");
        for(ThayDoiDiemDanh x:ds){if(x.ghiChu()!=null&&x.ghiChu().length()>500)throw new SQLException("Ghi chú không quá 500 ký tự.");}
        return repo.luuDiemDanh(coSo(),lop,ngay,taiKhoan,lyDo,ds);
    }
    public List<LichSuDiemDanh> lichSu(Integer lop,Integer hocSinh,LocalDate tu,LocalDate den)throws SQLException{
        khoang(tu,den);return repo.lichSuDiemDanh(coSo(),lop,hocSinh,tu,den);
    }
    public List<NhatKySua> nhatKy(int hocSinh,LocalDate ngay)throws SQLException{return repo.nhatKySua(coSo(),hocSinh,ngay);}
}
