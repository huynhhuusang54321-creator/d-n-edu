package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinHocVu;
import vn.vinabacus.repository.HocVuRepository;

/**
 * Service – quy tac nghiep vu module Học bù, đổi buổi, chuyển lớp (thanh vien 4).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHocVu {
    private final HocVuRepository repository;

    public XuLyHocVu(HocVuRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinHocVu> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
