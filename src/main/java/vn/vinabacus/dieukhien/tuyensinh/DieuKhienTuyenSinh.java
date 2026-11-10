package vn.vinabacus.dieukhien.tuyensinh;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.tuyensinh.ThongTinTuyenSinh;
import vn.vinabacus.xuly.tuyensinh.XuLyTuyenSinh;

/** Controller module Tuyển sinh – thanh vien 2. */
public class DieuKhienTuyenSinh {
    private final XuLyTuyenSinh xuLy;

    public DieuKhienTuyenSinh(XuLyTuyenSinh xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinTuyenSinh> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
