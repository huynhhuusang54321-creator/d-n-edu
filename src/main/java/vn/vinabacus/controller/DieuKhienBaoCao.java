package vn.vinabacus.controller;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.ThongTinBaoCao;
import vn.vinabacus.service.XuLyBaoCao;

/** Controller module Báo cáo, thống kê – thanh vien 6. */
public class DieuKhienBaoCao {
    private final XuLyBaoCao xuLy;

    public DieuKhienBaoCao(XuLyBaoCao xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinBaoCao> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
