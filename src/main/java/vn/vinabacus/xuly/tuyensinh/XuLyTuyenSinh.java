package vn.vinabacus.xuly.tuyensinh;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.tuyensinh.ThongTinTuyenSinh;
import vn.vinabacus.dulieu.tuyensinh.DuLieuTuyenSinh;

/**
 * Service – quy tac nghiep vu module Tuyển sinh (thanh vien 2).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyTuyenSinh {
    private final DuLieuTuyenSinh duLieu;

    public XuLyTuyenSinh(DuLieuTuyenSinh duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinTuyenSinh> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
