package vn.vinabacus.dieukhien.diemdanh;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.diemdanh.ThongTinDiemDanh;
import vn.vinabacus.xuly.diemdanh.XuLyDiemDanh;

/** Controller module Điểm danh – thanh vien 4. */
public class DieuKhienDiemDanh {
    private final XuLyDiemDanh xuLy;

    public DieuKhienDiemDanh(XuLyDiemDanh xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinDiemDanh> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
