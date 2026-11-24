package vn.vinabacus.controller;

import java.sql.SQLException;
import java.util.List;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.service.MonHocService;

public final class MonHocController {
    private final MonHocService service;

    public MonHocController(CoSo coSo) {
        this.service = new MonHocService(coSo);
    }

    public List<MonHoc> timKiem(String tuKhoa, Boolean trangThai) throws SQLException {
        return service.timKiem(tuKhoa, trangThai);
    }

    public int them(String ten, String ghiChu, boolean trangThai) throws SQLException {
        return service.them(ten, ghiChu, trangThai);
    }

    public void sua(int maMonHoc, String ten, String ghiChu, boolean trangThai)
            throws SQLException {
        service.sua(maMonHoc, ten, ghiChu, trangThai);
    }
}
