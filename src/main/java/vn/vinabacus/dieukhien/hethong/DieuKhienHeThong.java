package vn.vinabacus.dieukhien.hethong;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.hethong.ThongTinHeThong;
import vn.vinabacus.xuly.hethong.XuLyHeThong;

/** Controller module Nhân viên, tài khoản, cơ sở – thanh vien 6. */
public class DieuKhienHeThong {
    private final XuLyHeThong xuLy;

    public DieuKhienHeThong(XuLyHeThong xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinHeThong> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
