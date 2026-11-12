package vn.vinabacus.service;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinHeThong;
import vn.vinabacus.repository.HeThongRepository;

/**
 * Service – quy tac nghiep vu module Nhân viên, tài khoản, cơ sở (thanh vien 6).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHeThong {
    private final HeThongRepository repository;

    public XuLyHeThong(HeThongRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository không được null");
    }

    public List<ThongTinHeThong> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return repository.layDanhSach(maCoSo);
    }
}
