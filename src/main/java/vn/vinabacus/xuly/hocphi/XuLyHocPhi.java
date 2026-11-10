package vn.vinabacus.xuly.hocphi;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.hocphi.ThongTinHocPhi;
import vn.vinabacus.dulieu.hocphi.DuLieuHocPhi;

/**
 * Service – quy tac nghiep vu module Học phí, giảm giá, công nợ (thanh vien 5).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyHocPhi {
    private final DuLieuHocPhi duLieu;

    public XuLyHocPhi(DuLieuHocPhi duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinHocPhi> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
