package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinHocVu;
import vn.vinabacus.service.XuLyHocVu;

/** Controller module Học bù, đổi buổi, chuyển lớp – thanh vien 4. */
public class DieuKhienHocVu {
    private final XuLyHocVu xuLy;

    public DieuKhienHocVu(XuLyHocVu xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinHocVu> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
