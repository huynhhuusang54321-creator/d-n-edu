package vn.vinabacus.dieukhien.khoahoc;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.khoahoc.ThongTinKhoaHoc;
import vn.vinabacus.xuly.khoahoc.XuLyKhoaHoc;

/** Controller module Khóa học và gói năm – thanh vien 3. */
public class DieuKhienKhoaHoc {
    private final XuLyKhoaHoc xuLy;

    public DieuKhienKhoaHoc(XuLyKhoaHoc xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinKhoaHoc> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
