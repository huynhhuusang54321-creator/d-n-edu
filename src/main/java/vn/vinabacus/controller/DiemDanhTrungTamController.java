package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.Buoi;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.HocVienBuoi;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.LichSuSua;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.TaiKhoan;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.ThayDoi;
import vn.vinabacus.repository.HocBuTrungTamRepository.BuoiDich;
import vn.vinabacus.repository.HocBuTrungTamRepository.BuoiVang;
import vn.vinabacus.repository.HocBuTrungTamRepository.LichHocBu;
import vn.vinabacus.service.DiemDanhTrungTamService;

public final class DiemDanhTrungTamController {
    private final DiemDanhTrungTamService service = new DiemDanhTrungTamService();
    public List<TaiKhoan> taiKhoan() throws SQLException { return service.taiKhoan(); }
    public List<Buoi> buoi(LocalDate ngay) throws SQLException { return service.buoi(ngay); }
    public int lichChuaCoBuoi(LocalDate ngay) throws SQLException {
        return service.lichChuaCoBuoi(ngay);
    }
    public List<HocVienBuoi> hocVien(int maBuoi) throws SQLException { return service.hocVien(maBuoi); }
    public List<LichSuSua> lichSuSua(int maSapXep) throws SQLException {
        return service.lichSuSua(maSapXep);
    }
    public void luu(int maBuoi,HocVienBuoi hocVien,String trangThai,String ghiChu,
                    int taiKhoan,String lyDoSua) throws SQLException {
        service.luu(maBuoi,hocVien,trangThai,ghiChu,taiKhoan,lyDoSua);
    }
    public void luuNhieu(int maBuoi,List<ThayDoi> thayDoi,int taiKhoan)
            throws SQLException { service.luuNhieu(maBuoi,thayDoi,taiKhoan); }
    public void noiDung(int maBuoi,String noiDung) throws SQLException { service.noiDung(maBuoi,noiDung); }
    public List<BuoiVang> buoiVang() throws SQLException { return service.buoiVang(); }
    public List<BuoiDich> buoiDich(int nguon) throws SQLException { return service.buoiDich(nguon); }
    public List<LichHocBu> lichHocBu() throws SQLException { return service.lichHocBu(); }
    public int xepHocBu(int nguon,int dich,int taiKhoan) throws SQLException {
        return service.xepHocBu(nguon,dich,taiKhoan);
    }
    public void huyHocBu(int ma,int taiKhoan,String lyDo) throws SQLException {
        service.huyHocBu(ma,taiKhoan,lyDo);
    }
}
