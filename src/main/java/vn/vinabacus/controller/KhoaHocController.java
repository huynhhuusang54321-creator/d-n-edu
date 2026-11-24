package vn.vinabacus.controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.service.KhoaHocService;

public final class KhoaHocController {
    private final KhoaHocService service;

    public KhoaHocController(CoSo coSo) {
        service = new KhoaHocService(coSo);
    }

    public List<KhoaHoc> timKiem(String tuKhoa, Boolean trangThai) throws SQLException {
        return service.timKiem(tuKhoa, trangThai);
    }

    public List<MonHoc> layMonHoc() throws SQLException { return service.layMonHoc(); }
    public List<LopTrungTam> layLopHoc() throws SQLException { return service.layLopHoc(); }
    public List<DangKyKhoaHoc> dangKyCuaKhoa(int maKhoaHoc) throws SQLException {
        return service.dangKyCuaKhoa(maKhoaHoc);
    }

    public int them(int maMonHoc, String ten, int soBuoi, BigDecimal hocPhi,
                   boolean trangThai) throws SQLException {
        return service.them(maMonHoc, ten, soBuoi, hocPhi, trangThai);
    }

    public void sua(int maKhoaHoc, int maMonHoc, String ten, int soBuoi,
                   BigDecimal hocPhi, boolean trangThai) throws SQLException {
        service.sua(maKhoaHoc, maMonHoc, ten, soBuoi, hocPhi, trangThai);
    }
}
