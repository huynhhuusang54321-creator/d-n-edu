package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinKhoaHoc;
import vn.vinabacus.repository.KhoaHocRepository;

/**
 * Service – quy tac nghiep vu module Khóa học và gói năm (thanh vien 3).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyKhoaHoc {
    private final KhoaHocRepository repository;

    public XuLyKhoaHoc(KhoaHocRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinKhoaHoc> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
