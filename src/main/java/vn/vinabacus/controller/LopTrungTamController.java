package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.LopTrungTamRepository.GiaoVien;
import vn.vinabacus.repository.LopTrungTamRepository.HocVienTrongLop;
import vn.vinabacus.repository.LopTrungTamRepository.LichSuPhanCong;
import vn.vinabacus.service.LopTrungTamService;

public final class LopTrungTamController {
    private final LopTrungTamService service;
    public LopTrungTamController(CoSo coSo) {
        service = new LopTrungTamService(coSo);
    }
    public List<MonHoc> layMonHoc() throws SQLException { return service.layMonHoc(); }
    public List<KhoaHoc> layKhoaHoc() throws SQLException { return service.layKhoaHoc(); }
    public List<GiaoVien> layGiaoVien() throws SQLException { return service.layGiaoVien(); }
    public List<GiaoVien> layGiaoVienChuyenMon(int maMonHoc, LocalDate ngay)
            throws SQLException { return service.layGiaoVienChuyenMon(maMonHoc, ngay); }
    public void themChuyenMonGiaoVien(int maNhanVien, int maMonHoc,
                                     LocalDate ngay, String lyDo) throws SQLException {
        service.themChuyenMonGiaoVien(maNhanVien, maMonHoc, ngay, lyDo);
    }
    public void ketThucChuyenMonGiaoVien(int maNhanVien, int maMonHoc,
                                         LocalDate ngay, String lyDo) throws SQLException {
        service.ketThucChuyenMonGiaoVien(maNhanVien, maMonHoc, ngay, lyDo);
    }
    public List<HocVienTrongLop> hocVienLop(int maLop) throws SQLException {
        return service.hocVienLop(maLop);
    }
    public List<LichSuPhanCong> lichSuPhanCong(int maLop) throws SQLException {
        return service.lichSuPhanCong(maLop);
    }
    public List<LichHocLop> lichLop(int maLop) throws SQLException {
        return service.lichLop(maLop);
    }
    public List<LopTrungTam> timKiem(String tuKhoa, String trangThai)
            throws SQLException { return service.timKiem(tuKhoa, trangThai); }
    public int them(int maKhoaHoc, String ten, Integer maGiaoVien,
                    LocalDate ngay, String trangThai, int siSoToiDa) throws SQLException {
        return service.them(maKhoaHoc, ten, maGiaoVien, ngay, trangThai, siSoToiDa);
    }
    public int themVoiLich(int maKhoaHoc,String ten,Integer maGiaoVien,
            LocalDate ngay,String trangThai,int siSoToiDa,
            List<vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong> cacBuoi,
            LocalDate tuNgay) throws SQLException {
        return service.themVoiLich(maKhoaHoc,ten,maGiaoVien,ngay,trangThai,
                siSoToiDa,cacBuoi,tuNgay);
    }
    public void sua(int maLop, int maKhoaHoc, String ten, Integer maGiaoVien,
                    LocalDate ngay, String trangThai, Integer siSoToiDa) throws SQLException {
        service.sua(maLop, maKhoaHoc, ten, maGiaoVien, ngay, trangThai, siSoToiDa);
    }
    public void suaVoiLich(int maLop,int maKhoaHoc,String ten,Integer maGiaoVien,
            LocalDate ngay,String trangThai,Integer siSoToiDa,
            List<vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong> cacBuoi,
            LocalDate tuNgay) throws SQLException {
        service.suaVoiLich(maLop,maKhoaHoc,ten,maGiaoVien,ngay,trangThai,
                siSoToiDa,cacBuoi,tuNgay);
    }
    public void chuyenGiaoVien(int maLop, int maGiaoVienMoi,
                              LocalDate ngayChuyen, String lyDo) throws SQLException {
        service.chuyenGiaoVien(maLop, maGiaoVienMoi, ngayChuyen, lyDo);
    }
}
