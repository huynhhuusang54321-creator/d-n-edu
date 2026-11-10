package vn.vinabacus.xuly.hethong;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.hethong.ThongTinHeThong;
import vn.vinabacus.dulieu.hethong.DuLieuHeThong;

/**
 * Service – quy tac nghiep vu module Nhân viên, tài khoản, cơ sở (thanh vien 6).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHeThong {
    private final DuLieuHeThong duLieu;

    public XuLyHeThong(DuLieuHeThong duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinHeThong> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
