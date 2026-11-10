package vn.vinabacus.xuly.tienan;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.tienan.ThongTinTienAn;
import vn.vinabacus.dulieu.tienan.DuLieuTienAn;

/**
 * Service – quy tac nghiep vu module Tiền ăn Mầm non (thanh vien 5).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyTienAn {
    private final DuLieuTienAn duLieu;

    public XuLyTienAn(DuLieuTienAn duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinTienAn> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
