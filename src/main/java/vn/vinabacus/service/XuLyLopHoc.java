package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinLopHoc;
import vn.vinabacus.repository.LopHocRepository;

/**
 * Service – quy tac nghiep vu module Lớp học và phân công (thanh vien 3).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyLopHoc {
    private final LopHocRepository repository;

    public XuLyLopHoc(LopHocRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinLopHoc> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
