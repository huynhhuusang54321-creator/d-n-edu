
package vn.vinabacus.service;

import java.sql.SQLException;
import java.util.List;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.LichSuKho;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.LichSuKhoRepository;

public class LichSuKhoService {

    private final LichSuKhoRepository lichSuKhoRepository;
    private final CoSoRepository coSoRepository;

    public LichSuKhoService() {

        lichSuKhoRepository = new LichSuKhoRepository();
        coSoRepository = new CoSoRepository();
    }

    // Lay lich su kho theo co so
    public List<LichSuKho> layLichSu(CoSo coSo)
            throws SQLException {

        // Kiem tra co so
        if (coSo == null) {
            throw new IllegalArgumentException(
                "Vui lòng chọn cơ sở!"
            );
        }

        // Lay ma co so tu SQL Server
        int maCoSo = coSoRepository.timMaCoSo(coSo);

        // Goi Repository de lay lich su kho
        return lichSuKhoRepository.timTheoCoSo(maCoSo);
    }
}
