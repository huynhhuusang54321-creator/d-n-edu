package vn.vinabacus.xuly.lophoc;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.lophoc.ThongTinLopHoc;
import vn.vinabacus.dulieu.lophoc.DuLieuLopHoc;

/**
 * Service – quy tac nghiep vu module Lớp học và phân công (thanh vien 3).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyLopHoc {
    private final DuLieuLopHoc duLieu;

    public XuLyLopHoc(DuLieuLopHoc duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinLopHoc> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
