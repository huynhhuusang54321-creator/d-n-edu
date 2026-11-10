package vn.vinabacus.xuly.hocvu;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.hocvu.ThongTinHocVu;
import vn.vinabacus.dulieu.hocvu.DuLieuHocVu;

/**
 * Service – quy tac nghiep vu module Học bù, đổi buổi, chuyển lớp (thanh vien 4).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHocVu {
    private final DuLieuHocVu duLieu;

    public XuLyHocVu(DuLieuHocVu duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinHocVu> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
