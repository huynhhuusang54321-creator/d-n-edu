package vn.vinabacus.xuly.hocvien;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.hocvien.ThongTinHocVien;
import vn.vinabacus.dulieu.hocvien.DuLieuHocVien;

/**
 * Service – quy tac nghiep vu module Học viên (thanh vien 2).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHocVien {
    private final DuLieuHocVien duLieu;

    public XuLyHocVien(DuLieuHocVien duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinHocVien> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
