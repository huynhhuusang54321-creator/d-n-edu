package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinTuyenSinh;
import vn.vinabacus.service.XuLyTuyenSinh;

/** Controller module Tuyển sinh – thanh vien 2. */
public class DieuKhienTuyenSinh {
    private final XuLyTuyenSinh xuLy;

    public DieuKhienTuyenSinh(XuLyTuyenSinh xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinTuyenSinh> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
