package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinKho;
import vn.vinabacus.repository.KhoRepository;

/**
 * Service – quy tac nghiep vu module Kho, nhập/xuất, kiểm kê (thanh vien 6).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyKho {
    private final KhoRepository repository;

    public XuLyKho(KhoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinKho> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
