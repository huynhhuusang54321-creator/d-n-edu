package vn.vinabacus.tv4;

import vn.vinabacus.view.*;

/** TV4 – bốn module Mầm non tách biệt, không ghi đè TV3. */
public final class TV4Module {
    private TV4Module(){}
    public static void dangKy(){
        DangKyManHinh.dangKy("mn-nam-hoc",NamHocMamNonPanel::new);
        DangKyManHinh.dangKy("mn-lop-hoc",LopMamNonPanel::new);
        DangKyManHinh.dangKy("mn-xep-lop",XepLopMamNonPanel::new);
        DangKyManHinh.dangKy("mn-diem-danh",DiemDanhMamNonPanel::new);
    }
}
