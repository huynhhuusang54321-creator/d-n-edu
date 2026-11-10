package vn.vinabacus.xuly.diemdanh;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.diemdanh.ThongTinDiemDanh;
import vn.vinabacus.dulieu.diemdanh.DuLieuDiemDanh;

/**
 * Service – quy tac nghiep vu module Điểm danh (thanh vien 4).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyDiemDanh {
    private final DuLieuDiemDanh duLieu;

    public XuLyDiemDanh(DuLieuDiemDanh duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinDiemDanh> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
