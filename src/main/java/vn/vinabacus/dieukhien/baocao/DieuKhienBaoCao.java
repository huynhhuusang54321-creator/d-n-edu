package vn.vinabacus.dieukhien.baocao;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.baocao.ThongTinBaoCao;
import vn.vinabacus.xuly.baocao.XuLyBaoCao;

/** Controller module Báo cáo, thống kê – thanh vien 6. */
public class DieuKhienBaoCao {
    private final XuLyBaoCao xuLy;

    public DieuKhienBaoCao(XuLyBaoCao xuLy) {
        this.xuLy = Objects.requireNonNull(xuLy);
    }

    public List<ThongTinBaoCao> layDanhSach(int maCoSo) {
        return xuLy.layDanhSach(maCoSo);
    }
}
