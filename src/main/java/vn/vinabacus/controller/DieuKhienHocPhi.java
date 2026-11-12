package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinHocPhi;
import vn.vinabacus.service.XuLyHocPhi;

/** Controller module Học phí, giảm giá, công nợ – thanh vien 5. */
public class DieuKhienHocPhi {
    private final XuLyHocPhi xuLy;

    public DieuKhienHocPhi(XuLyHocPhi xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinHocPhi> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
