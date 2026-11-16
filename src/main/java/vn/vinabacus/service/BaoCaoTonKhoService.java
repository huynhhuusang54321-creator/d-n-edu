package vn.vinabacus.service;

import java.sql.SQLException;
import java.util.List;
import vn.vinabacus.model.BaoCaoTonKho;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.repository.BaoCaoTonKhoRepository;
import vn.vinabacus.repository.CoSoRepository;

public class BaoCaoTonKhoService {
    private final BaoCaoTonKhoRepository repository = new BaoCaoTonKhoRepository();
    private final CoSoRepository coSoRepository = new CoSoRepository();

    public List<BaoCaoTonKho> layBaoCao(CoSo coSo, int nam, int thang)
            throws SQLException {
        if (coSo == null) {
            throw new IllegalArgumentException("Vui lòng chọn cơ sở!");
        }
        if (nam < 2000 || nam > 2100 || thang < 1 || thang > 12) {
            throw new IllegalArgumentException("Tháng hoặc năm không hợp lệ!");
        }
        int maCoSo = coSoRepository.timMaCoSo(coSo);
        return repository.layBaoCao(maCoSo, nam, thang);
    }
}
