package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinDiemDanh;
import vn.vinabacus.repository.DiemDanhRepository;

/**
 * Service – quy tac nghiep vu module Điểm danh (thanh vien 4).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyDiemDanh {
    private final DiemDanhRepository repository;

    public XuLyDiemDanh(DiemDanhRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinDiemDanh> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
