package vn.vinabacus.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.repository.LopTrungTamRepository.GiaoVien;
import vn.vinabacus.service.LopTrungTamService;

public final class LopTrungTamController {
    private final LopTrungTamService service;
    public LopTrungTamController(CoSo coSo) {
        service = new LopTrungTamService(coSo);
    }
    public List<MonHoc> layMonHoc() throws SQLException { return service.layMonHoc(); }
    public List<KhoaHoc> layKhoaHoc() throws SQLException { return service.layKhoaHoc(); }
    public List<GiaoVien> layGiaoVien() throws SQLException { return service.layGiaoVien(); }
    public List<LopTrungTam> timKiem(String tuKhoa, String trangThai)
            throws SQLException { return service.timKiem(tuKhoa, trangThai); }
    public int them(int maKhoaHoc, String ten, Integer maGiaoVien,
                    LocalDate ngay, String trangThai) throws SQLException {
        return service.them(maKhoaHoc, ten, maGiaoVien, ngay, trangThai);
    }
    public void sua(int maLop, int maKhoaHoc, String ten, Integer maGiaoVien,
                    LocalDate ngay, String trangThai) throws SQLException {
        service.sua(maLop, maKhoaHoc, ten, maGiaoVien, ngay, trangThai);
    }
}
