package vn.vinabacus.xuly.khoahoc;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.khoahoc.ThongTinKhoaHoc;
import vn.vinabacus.dulieu.khoahoc.DuLieuKhoaHoc;

/**
 * Service – quy tac nghiep vu module Khóa học và gói năm (thanh vien 3).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyKhoaHoc {
    private final DuLieuKhoaHoc duLieu;

    public XuLyKhoaHoc(DuLieuKhoaHoc duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinKhoaHoc> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
