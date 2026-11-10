package vn.vinabacus.dieukhien.hocphi;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.hocphi.ThongTinHocPhi;
import vn.vinabacus.xuly.hocphi.XuLyHocPhi;

/** Controller module Học phí, giảm giá, công nợ – thanh vien 5. */
public class DieuKhienHocPhi {
    private final XuLyHocPhi xuLy;

    public DieuKhienHocPhi(XuLyHocPhi xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinHocPhi> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
