package vn.vinabacus.tv3;

import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.MonHocPanel;
import vn.vinabacus.view.KhoaHocPanel;
import vn.vinabacus.view.LopTrungTamPanel;
import vn.vinabacus.view.LichHocLopPanel;
import vn.vinabacus.view.ChuongTrinhGiamGiaPanel;
import vn.vinabacus.view.DiemDanhTrungTamPanel;

/** Goi TV3Module.dangKy() mot lan truoc khi tao MainFrame. */
public final class TV3Module {
    private TV3Module() { }
    public static void dangKy() {
        DangKyManHinh.dangKy("tt-mon-hoc", MonHocPanel::new);
        DangKyManHinh.dangKy("tt-khoa-hoc", KhoaHocPanel::new);
        DangKyManHinh.dangKy("tt-giam-gia", ChuongTrinhGiamGiaPanel::new);
        DangKyManHinh.dangKy("tt-lop-hoc", LopTrungTamPanel::new);
        DangKyManHinh.dangKy("tt-lich-hoc", LichHocLopPanel::new);
        DangKyManHinh.dangKy("tt-diem-danh", DiemDanhTrungTamPanel::new);
    }
}
