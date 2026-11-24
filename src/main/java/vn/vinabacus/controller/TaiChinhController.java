package vn.vinabacus.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import vn.vinabacus.repository.TaiChinhDangKyRepository;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.TaiChinhDTO.*;
import vn.vinabacus.service.TaiChinhService;

/** Lớp điều phối TV5, tách UI khỏi truy vấn và kiểm tra nghiệp vụ. */
public final class TaiChinhController {
    private final TaiChinhService service=new TaiChinhService();
    public List<KhoanThu> khoan(CoSo cs)throws Exception{return service.khoanThu(cs);}
    public List<PhieuThu> phieu(CoSo cs,YearMonth ky)throws Exception{return service.phieuThu(cs,ky);}
    public List<PhieuThu> phieu(CoSo cs,LocalDate tuNgay,LocalDate denTruoc)throws Exception{
        return service.phieuThu(cs,tuNgay,denTruoc);
    }
    public List<vn.vinabacus.model.TaiChinhDTO.PhanBoPhieu> phanBoPhieu(CoSo cs,int maPhieu)throws Exception{
        return service.phanBoPhieu(cs,maPhieu);
    }
    public List<LichSuThu> lichSuThuHocVien(CoSo cs,int maHocVien)throws Exception {
        return service.lichSuThuHocVien(cs,maHocVien);
    }
    public List<UuDaiDangKy> uuDaiHocVien(int maHocVien)throws Exception {
        return service.uuDaiHocVien(maHocVien);
    }
    public int thuNhieuKhoa(CoSo cs,int maHocVien,BigDecimal soTien,
            List<TaiChinhDangKyRepository.PhanBo> phanBo,String hinhThuc,
            int taiKhoan,String ghiChu,UUID token)throws Exception {
        return service.lapPhieuNhieuKhoan(cs,maHocVien,soTien,phanBo,hinhThuc,taiKhoan,ghiChu,token);
    }
    public void huyPhieu(CoSo cs,int maPhieu,int taiKhoan,String lyDo)throws Exception{
        service.huyPhieu(cs,maPhieu,taiKhoan,lyDo);
    }
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
    public ThongKe thongKe(CoSo cs,LocalDate tu,LocalDate den)throws Exception{
        return service.thongKe(cs,tu,den);
    }
    public List<DongThuBaoCao> baoCaoThu(CoSo cs,LocalDate tu,LocalDate den)throws Exception{
        return service.baoCaoThu(cs,tu,den);
    }
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
