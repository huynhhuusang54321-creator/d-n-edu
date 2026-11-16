package vn.vinabacus.controller;

import java.sql.SQLException;
import java.util.List;
import vn.vinabacus.model.BaoCaoTonKho;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.service.BaoCaoTonKhoService;

public class BaoCaoTonKhoController {
    private final BaoCaoTonKhoService service = new BaoCaoTonKhoService();

    public List<BaoCaoTonKho> layBaoCao(CoSo coSo, int nam, int thang)
            throws SQLException {
        return service.layBaoCao(coSo, nam, thang);
    }
}
