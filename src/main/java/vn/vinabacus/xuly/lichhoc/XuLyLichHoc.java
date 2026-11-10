package vn.vinabacus.xuly.lichhoc;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.lichhoc.ThongTinLichHoc;
import vn.vinabacus.dulieu.lichhoc.DuLieuLichHoc;

/**
 * Service – quy tac nghiep vu module Lịch lớp và buổi học (thanh vien 3).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyLichHoc {
    private final DuLieuLichHoc duLieu;

    public XuLyLichHoc(DuLieuLichHoc duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinLichHoc> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
