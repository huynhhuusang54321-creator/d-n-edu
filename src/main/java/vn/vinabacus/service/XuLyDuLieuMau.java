package vn.vinabacus.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DuLieuMau;
import vn.vinabacus.repository.DuLieuTrangChuRepository;

/** Cung cấp số liệu trang chủ qua repository và kiểm tra phạm vi dữ liệu mẫu. */
public final class XuLyDuLieuMau {
    private static final DuLieuTrangChuRepository REPOSITORY = new DuLieuTrangChuRepository();

    private XuLyDuLieuMau() {}

    public static DuLieuMau.SoLieu laySoLieu(CoSo coSo) {
        return REPOSITORY.laySoLieu(Objects.requireNonNull(coSo, "Cơ sở không được null"));
    }

    public static List<DuLieuMau.Lich> layLich(CoSo coSo) {
        return REPOSITORY.layLich(Objects.requireNonNull(coSo, "Cơ sở không được null"));
    }

    /** Chỉ ngày hiện tại có mẫu; không hiển thị lịch sử giả cho ngày khác. */
    public static boolean coDuLieuMauTrongNgay(LocalDate ngay) {
        return LocalDate.now().equals(ngay);
    }

    public static DuLieuMau.Bang layBang(CoSo coSo, String muc) {
        Objects.requireNonNull(coSo, "Cơ sở không được null");
        Objects.requireNonNull(muc, "Mục không được null");
        return REPOSITORY.layBang(coSo, muc);
    }
}
