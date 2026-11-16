
package vn.vinabacus.service;

import java.sql.SQLException;
import java.util.List;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.NhanVien;
import vn.vinabacus.repository.CoSoRepository;
import vn.vinabacus.repository.NhanVienRepository;

public class NhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final CoSoRepository coSoRepository;

    public NhanVienService() {
        nhanVienRepository = new NhanVienRepository();
        coSoRepository = new CoSoRepository();
    }

    public List<NhanVien> layDanhSach(CoSo coSo)
            throws SQLException {

        int maCoSo = coSoRepository.timMaCoSo(coSo);

        return nhanVienRepository.timTheoCoSo(maCoSo);
    }
}
