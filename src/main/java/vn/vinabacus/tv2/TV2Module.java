package vn.vinabacus.tv2;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.HocVienPanel;
import vn.vinabacus.view.TuyenSinhPanel;
import vn.vinabacus.view.DangKyKhoaHocPanel;
/** TV1 goi mot lan truoc khi hien MainFrame. Khong sua MenuConfig. */
public final class TV2Module {
    private TV2Module(){}
    public static void dangKy(){
        DangKyManHinh.dangKy("tt-hoc-vien",()->new HocVienPanel(CoSo.TRUNG_TAM));
        DangKyManHinh.dangKy("mn-hoc-sinh",()->new HocVienPanel(CoSo.MAM_NON));
        DangKyManHinh.dangKy("tt-tuyen-sinh",()->new TuyenSinhPanel(CoSo.TRUNG_TAM));
        DangKyManHinh.dangKy("tt-dang-ky",DangKyKhoaHocPanel::new);
        DangKyManHinh.dangKy("mn-tuyen-sinh",()->new TuyenSinhPanel(CoSo.MAM_NON));
    }
}
