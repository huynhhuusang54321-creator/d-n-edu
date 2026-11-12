package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinTienAn;
import vn.vinabacus.repository.TienAnRepository;

/**
 * Service – quy tac nghiep vu module Tiền ăn Mầm non (thanh vien 5).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyTienAn {
    private final TienAnRepository repository;

    public XuLyTienAn(TienAnRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinTienAn> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
