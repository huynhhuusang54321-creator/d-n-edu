package vn.vinabacus.controller;

import java.sql.SQLException;
import java.util.List;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.PhongHocRepository;
import vn.vinabacus.service.PhongHocService;

/** Dieu phoi cac thao tac phong hoc Trung tam qua service. */
public final class PhongHocController {
    private final PhongHocService service;

    public PhongHocController() { this(new PhongHocService()); }
    public PhongHocController(PhongHocService service) { this.service = service; }

    public List<PhongHoc> layDanhSach() throws SQLException { return service.layDanhSach(); }
    public List<PhongHocRepository.LopSuDung> layLopSuDung() throws SQLException {
        return service.layLopSuDung();
    }
    public List<LichHocLop> layLichSuDung() throws SQLException {
        return service.layLichSuDung();
    }
    public int them(String ma, String ten, int sucChua, String trangThai, String ghiChu)
            throws SQLException { return service.them(ma, ten, sucChua, trangThai, ghiChu); }
    public void sua(PhongHoc cu, String ma, String ten, int sucChua, String trangThai, String ghiChu)
            throws SQLException { service.sua(cu, ma, ten, sucChua, trangThai, ghiChu); }
    public void doiTrangThai(PhongHoc p, String trangThai) throws SQLException {
        service.doiTrangThai(p, trangThai);
    }
}
