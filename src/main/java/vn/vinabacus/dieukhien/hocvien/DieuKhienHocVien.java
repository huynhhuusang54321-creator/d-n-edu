package vn.vinabacus.dieukhien.hocvien;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.hocvien.ThongTinHocVien;
import vn.vinabacus.xuly.hocvien.XuLyHocVien;

/** Controller module Học viên – thanh vien 2. */
public class DieuKhienHocVien {
    private final XuLyHocVien xuLy;

    public DieuKhienHocVien(XuLyHocVien xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinHocVien> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
