package vn.vinabacus.dieukhien.lophoc;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.lophoc.ThongTinLopHoc;
import vn.vinabacus.xuly.lophoc.XuLyLopHoc;

/** Controller module Lớp học và phân công – thanh vien 3. */
public class DieuKhienLopHoc {
    private final XuLyLopHoc xuLy;

    public DieuKhienLopHoc(XuLyLopHoc xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinLopHoc> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
