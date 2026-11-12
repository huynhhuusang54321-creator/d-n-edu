package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinBaoCao;
import vn.vinabacus.repository.BaoCaoRepository;

/**
 * Service – quy tac nghiep vu module Báo cáo, thống kê (thanh vien 6).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyBaoCao {
    private final BaoCaoRepository repository;

    public XuLyBaoCao(BaoCaoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinBaoCao> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
