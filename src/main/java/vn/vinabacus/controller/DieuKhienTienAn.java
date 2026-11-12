package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinTienAn;
import vn.vinabacus.service.XuLyTienAn;

/** Controller module Tiền ăn Mầm non – thanh vien 5. */
public class DieuKhienTienAn {
    private final XuLyTienAn xuLy;

    public DieuKhienTienAn(XuLyTienAn xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinTienAn> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
