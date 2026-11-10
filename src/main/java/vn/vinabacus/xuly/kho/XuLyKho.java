package vn.vinabacus.xuly.kho;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.kho.ThongTinKho;
import vn.vinabacus.dulieu.kho.DuLieuKho;

/**
 * Service – quy tac nghiep vu module Kho, nhập/xuất, kiểm kê (thanh vien 6).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyKho {
    private final DuLieuKho duLieu;

    public XuLyKho(DuLieuKho duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinKho> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
