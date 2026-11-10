package vn.vinabacus.xuly.baocao;

import java.util.List;
import java.util.Objects;
import vn.vinabacus.doituong.baocao.ThongTinBaoCao;
import vn.vinabacus.dulieu.baocao.DuLieuBaoCao;

/**
 * Service – quy tac nghiep vu module Báo cáo, thống kê (thanh vien 6).
 * Khong duoc dat SQL trong View hoac Controller.
 */
public class XuLyBaoCao {
    private final DuLieuBaoCao duLieu;

    public XuLyBaoCao(DuLieuBaoCao duLieu) {
        this.duLieu = Objects.requireNonNull(duLieu, "DuLieu khong duoc null");
    }

    public List<ThongTinBaoCao> layDanhSach(int maCoSo) {
        if (maCoSo <= 0) throw new IllegalArgumentException("Ma co so khong hop le");
        return duLieu.layDanhSach(maCoSo);
    }
}
