package vn.vinabacus.tv5;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.PhieuThuTaiChinhPanel;
import vn.vinabacus.view.TaiChinhPanel;

/** Đăng ký đúng các mã menu TÀI CHÍNH đã có sẵn trong MenuConfig. */
public final class TV5Module {
    private TV5Module() {}
    public static void dangKy() {
        DangKyManHinh.dangKy("tt-hoc-phi",()->new TaiChinhPanel(CoSo.TRUNG_TAM));
        DangKyManHinh.dangKy("tt-phieu-thu",()->new PhieuThuTaiChinhPanel(CoSo.TRUNG_TAM));
        DangKyManHinh.dangKy("mn-hoc-phi",()->new TaiChinhPanel(CoSo.MAM_NON));
        DangKyManHinh.dangKy("mn-phieu-thu",()->new PhieuThuTaiChinhPanel(CoSo.MAM_NON));
    }
}
