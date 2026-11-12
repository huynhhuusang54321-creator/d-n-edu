package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinTuyenSinh;
import vn.vinabacus.repository.TuyenSinhRepository;

/**
 * Service – quy tac nghiep vu module Tuyển sinh (thanh vien 2).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyTuyenSinh {
    private final TuyenSinhRepository repository;

    public XuLyTuyenSinh(TuyenSinhRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinTuyenSinh> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
