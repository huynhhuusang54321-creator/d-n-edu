package vn.vinabacus.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DuLieuMau;
import vn.vinabacus.service.XuLyDuLieuMau;

/** Đầu vào của trang chủ và trang danh sách xem trước. */
public final class DieuKhienTrangChu {
    private final CoSo coSo;

    public DieuKhienTrangChu(CoSo coSo) {
        this.coSo = Objects.requireNonNull(coSo, "Cơ sở không được null");
    }

    public DuLieuMau.SoLieu laySoLieu() {
        return XuLyDuLieuMau.laySoLieu(coSo);
    }

    public List<DuLieuMau.Lich> layLich() {
        return XuLyDuLieuMau.layLich(coSo);
    }

    public boolean coDuLieuMauTrongNgay(LocalDate ngay) {
        return XuLyDuLieuMau.coDuLieuMauTrongNgay(ngay);
    }

    public DuLieuMau.Bang layBang(String muc) {
        return XuLyDuLieuMau.layBang(coSo, muc);
    }
}
