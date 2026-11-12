package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinLopHoc;
import vn.vinabacus.service.XuLyLopHoc;

/** Controller module Lớp học và phân công – thanh vien 3. */
public class DieuKhienLopHoc {
    private final XuLyLopHoc xuLy;

    public DieuKhienLopHoc(XuLyLopHoc xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinLopHoc> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
