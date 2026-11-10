package vn.vinabacus.dieukhien.kho;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.kho.ThongTinKho;
import vn.vinabacus.xuly.kho.XuLyKho;

/** Controller module Kho, nhập/xuất, kiểm kê – thanh vien 6. */
public class DieuKhienKho {
    private final XuLyKho xuLy;

    public DieuKhienKho(XuLyKho xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinKho> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
