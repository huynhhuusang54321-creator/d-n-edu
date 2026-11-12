package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinHeThong;
import vn.vinabacus.service.XuLyHeThong;

/** Controller module Nhân viên, tài khoản, cơ sở – thanh vien 6. */
public class DieuKhienHeThong {
    private final XuLyHeThong xuLy;

    public DieuKhienHeThong(XuLyHeThong xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinHeThong> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
