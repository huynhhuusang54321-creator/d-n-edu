package vn.vinabacus.dieukhien.lichhoc;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.lichhoc.ThongTinLichHoc;
import vn.vinabacus.xuly.lichhoc.XuLyLichHoc;

/** Controller module Lịch lớp và buổi học – thanh vien 3. */
public class DieuKhienLichHoc {
    private final XuLyLichHoc xuLy;

    public DieuKhienLichHoc(XuLyLichHoc xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinLichHoc> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
