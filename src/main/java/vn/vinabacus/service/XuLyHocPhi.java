package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinHocPhi;
import vn.vinabacus.repository.HocPhiRepository;

/**
 * Service – quy tac nghiep vu module Học phí, giảm giá, công nợ (thanh vien 5).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHocPhi {
    private final HocPhiRepository repository;

    public XuLyHocPhi(HocPhiRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinHocPhi> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
