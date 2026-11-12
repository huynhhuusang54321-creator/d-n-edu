package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinHocVien;
import vn.vinabacus.repository.HocVienRepository;

/**
 * Service – quy tac nghiep vu module Học viên (thanh vien 2).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHocVien {
    private final HocVienRepository repository;

    public XuLyHocVien(HocVienRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinHocVien> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
