
package vn.vinabacus.controller;

import java.sql.SQLException;
import java.util.List;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.LichSuKho;
import vn.vinabacus.service.LichSuKhoService;

public class LichSuKhoController {

    private final LichSuKhoService service;

    public LichSuKhoController() {
        service = new LichSuKhoService();
    }

    // Lay lich su nhap xuat kho theo co so
    public List<LichSuKho> layLichSu(CoSo coSo)
            throws SQLException {

        return service.layLichSu(coSo);
    }
}
