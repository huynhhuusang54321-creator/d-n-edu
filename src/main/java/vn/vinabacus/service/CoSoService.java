
package vn.vinabacus.service;

import java.sql.SQLException;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.repository.CoSoRepository;

public class CoSoService {

    private final CoSoRepository repository;

    public CoSoService() {
        repository = new CoSoRepository();
    }

    public int layMaCoSo(CoSo coSo) throws SQLException {
        return repository.timMaCoSo(coSo);
    }
}
