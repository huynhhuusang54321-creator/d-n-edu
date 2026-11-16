
package vn.vinabacus.controller;

import java.sql.SQLException;
import java.util.List;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.NhanVien;
import vn.vinabacus.service.NhanVienService;

public class NhanVienController {

    private final NhanVienService service;

    public NhanVienController() {
        service = new NhanVienService();
    }

    public List<NhanVien> layDanhSach(CoSo coSo)
            throws SQLException {

        return service.layDanhSach(coSo);
    }
}
