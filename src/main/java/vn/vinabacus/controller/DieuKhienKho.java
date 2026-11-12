package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinKho;
import vn.vinabacus.service.XuLyKho;

/** Controller module Kho, nhập/xuất, kiểm kê – thanh vien 6. */
public class DieuKhienKho {
    private final XuLyKho xuLy;

    public DieuKhienKho(XuLyKho xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinKho> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
