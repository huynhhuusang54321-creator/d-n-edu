package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinLichHoc;
import vn.vinabacus.repository.LichHocRepository;

/**
 * Service – quy tac nghiep vu module Lịch lớp và buổi học (thanh vien 3).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyLichHoc {
    private final LichHocRepository repository;

    public XuLyLichHoc(LichHocRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinLichHoc> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
