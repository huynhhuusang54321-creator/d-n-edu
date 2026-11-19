package vn.vinabacus.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.TaiChinhDTO.*;
import vn.vinabacus.service.TaiChinhService;

/** Lớp điều phối TV5, tách UI khỏi truy vấn và kiểm tra nghiệp vụ. */
public final class TaiChinhController {
    private final TaiChinhService service=new TaiChinhService();
    public List<KhoanThu> khoan(CoSo cs)throws Exception{return service.khoanThu(cs);}
    public List<PhieuThu> phieu(CoSo cs,YearMonth ky)throws Exception{return service.phieuThu(cs,ky);}
    public List<LuaChon> taiKhoan(CoSo cs)throws Exception{return service.taiKhoan(cs);}
    public List<LuaChon> lopMamNon()throws Exception{return service.lopMamNon();}
    public List<DangKyChuaThu> dangKy()throws Exception{return service.dangKyChuaThu();}
    public List<BangTinhChot> bangTinh()throws Exception{return service.bangTinhChot();}
    public List<HocPhiTrungTam> hocPhiTrungTam()throws Exception{return service.hocPhiTrungTam();}
    public void dieuChinhKhoan(CoSo cs,int maKhoan,BigDecimal thayDoi,String lyDo,int tk)throws Exception{
        service.dieuChinhKhoan(cs,maKhoan,thayDoi,lyDo,tk);
    }
    public List<MucPhi> mucPhi()throws Exception{return service.mucPhiMamNon();}
    public ThongKe thongKe(CoSo cs,YearMonth ky)throws Exception{return service.thongKe(cs,ky);}
    public void taoKhoanKhoa(int maDangKy,LocalDate han)throws Exception{service.lapKhoanHocPhiKhoa(maDangKy,han);}
    public int taoKhoanMamNon(int maBang,boolean hocPhi,boolean tienAn,LocalDate han)throws Exception{
        return service.lapKhoanMamNon(maBang,hocPhi,tienAn,han);
    }
    public void thietLapPhi(int maLop,boolean tienAn,LocalDate month,BigDecimal amount,int tk)throws Exception{
        service.taoMucPhi(maLop,tienAn,month,amount,tk);
    }
    public void thu(CoSo cs,int khoan,BigDecimal amount,String method,int tk,String note)throws Exception{
        service.lapPhieu(cs,khoan,amount,method,tk,note);
    }
}
