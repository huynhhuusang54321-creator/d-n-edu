package vn.vinabacus.repository;

import java.util.List;
import vn.vinabacus.data.DuLieuMauTrangChu;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DuLieuMau;

/** Truy xuất dữ liệu trang chủ; nguồn hiện tại là dữ liệu mẫu trong bộ nhớ. */
public final class DuLieuTrangChuRepository {
    public DuLieuMau.SoLieu laySoLieu(CoSo coSo) {
        return DuLieuMauTrangChu.laySoLieu(coSo);
    }

    public List<DuLieuMau.Lich> layLich(CoSo coSo) {
        return DuLieuMauTrangChu.layLich(coSo);
    }

    public DuLieuMau.Bang layBang(CoSo coSo, String muc) {
        return DuLieuMauTrangChu.layBang(coSo, muc);
    }
}
