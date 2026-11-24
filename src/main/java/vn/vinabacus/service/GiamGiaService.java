package vn.vinabacus.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.KhoaHoc;

/** Tính ưu đãi trên từng khóa, không truy cập hay thay đổi CSDL. */
public final class GiamGiaService {
    private GiamGiaService() { }

    public record Dong(int maKhoaHoc, BigDecimal hocPhiGoc,
            Map<Integer, BigDecimal> giamTheoChuongTrinh) {
        public Dong {
            giamTheoChuongTrinh = Map.copyOf(giamTheoChuongTrinh);
        }
        public BigDecimal tongGiam() {
            return giamTheoChuongTrinh.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        public BigDecimal phaiThu() { return hocPhiGoc.subtract(tongGiam()); }
    }
    public record BangGia(List<Dong> dong) {
        public BangGia { dong = List.copyOf(dong); }
        public BigDecimal hocPhiGoc() {
            return dong.stream().map(Dong::hocPhiGoc).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        public BigDecimal tongGiam() {
            return dong.stream().map(Dong::tongGiam).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        public BigDecimal phaiThu() { return hocPhiGoc().subtract(tongGiam()); }
        public Dong choKhoa(int maKhoa) {
            return dong.stream().filter(d -> d.maKhoaHoc() == maKhoa).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không có trong bảng giá"));
        }
    }

    /** Phần trăm tính trên giá gốc; tiền cố định chia theo dư nợ của các khóa đủ điều kiện. */
    public static BangGia tinh(List<KhoaHoc> cacKhoa,
            List<ChuongTrinhGiamGia> cacChuongTrinh, LocalDate ngay) {
        if (cacKhoa == null || cacKhoa.isEmpty() || ngay == null)
            throw new IllegalArgumentException("Cần chọn khóa và ngày đăng ký");
        if (cacChuongTrinh == null) cacChuongTrinh = List.of();
        List<KhoaHoc> khoa = new ArrayList<>(cacKhoa);
        khoa.sort(Comparator.comparingInt(KhoaHoc::maKhoaHoc));
        Set<Integer> maKhoa = new HashSet<>();
        for (KhoaHoc k : khoa) {
            if (k == null || k.maKhoaHoc() <= 0 || k.hocPhiMacDinh() == null
                    || k.hocPhiMacDinh().signum() < 0 || !maKhoa.add(k.maKhoaHoc()))
                throw new IllegalArgumentException("Khóa học hoặc học phí không hợp lệ");
        }
        Set<Integer> maChuongTrinh = new HashSet<>();
        for (ChuongTrinhGiamGia ct : cacChuongTrinh) {
            if (ct == null || ct.maChuongTrinh() <= 0
                    || !maChuongTrinh.add(ct.maChuongTrinh()))
                throw new IllegalArgumentException("Chương trình giảm giá bị chọn trùng");
            kiemTraCauHinh(ct);
            if (!ct.hieuLuc(ngay))
                throw new IllegalArgumentException("Chương trình " + ct.ten() + " chưa có hiệu lực");
            if (khoa.stream().noneMatch(ct::apDung))
                throw new IllegalArgumentException("Chương trình " + ct.ten()
                        + " không áp dụng cho khóa đã chọn");
        }
        for (int i = 0; i < cacChuongTrinh.size(); i++) {
            ChuongTrinhGiamGia a = cacChuongTrinh.get(i);
            for (int j = i + 1; j < cacChuongTrinh.size(); j++) {
                ChuongTrinhGiamGia b = cacChuongTrinh.get(j);
                if (a.choPhepKetHop() && b.choPhepKetHop()) continue;
                if (khoa.stream().anyMatch(k -> a.apDung(k) && b.apDung(k)))
                    throw new IllegalArgumentException("Chương trình " + a.ten()
                            + " và " + b.ten() + " không được kết hợp trên cùng khóa");
            }
        }
        Map<Integer, BigDecimal> conLai = new LinkedHashMap<>();
        Map<Integer, Map<Integer, BigDecimal>> giam = new HashMap<>();
        for (KhoaHoc k : khoa) {
            conLai.put(k.maKhoaHoc(), k.hocPhiMacDinh().setScale(2, RoundingMode.HALF_UP));
            giam.put(k.maKhoaHoc(), new LinkedHashMap<>());
        }
        List<ChuongTrinhGiamGia> thuTu = new ArrayList<>(cacChuongTrinh);
        thuTu.sort(Comparator.comparingInt((ChuongTrinhGiamGia c) ->
                "PHAN_TRAM".equals(c.loaiGiam()) ? 0 : 1)
                .thenComparingInt(ChuongTrinhGiamGia::maChuongTrinh));
        for (ChuongTrinhGiamGia ct : thuTu) {
            List<KhoaHoc> duDieuKien = khoa.stream().filter(ct::apDung).toList();
            if ("PHAN_TRAM".equals(ct.loaiGiam())) {
                for (KhoaHoc k : duDieuKien) {
                    BigDecimal tien = k.hocPhiMacDinh().multiply(ct.giaTri())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    ghi(k.maKhoaHoc(), ct.maChuongTrinh(), tien, conLai, giam);
                }
            } else {
                BigDecimal tongCon = duDieuKien.stream().map(k -> conLai.get(k.maKhoaHoc()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal phanBo = ct.giaTri().min(tongCon).setScale(2, RoundingMode.HALF_UP);
                Map<Integer, BigDecimal> soChia = new LinkedHashMap<>();
                BigDecimal daChia = BigDecimal.ZERO;
                for (KhoaHoc k : duDieuKien) {
                    int id = k.maKhoaHoc();
                    BigDecimal tien = tongCon.signum() == 0 ? BigDecimal.ZERO
                            : phanBo.multiply(conLai.get(id)).divide(tongCon, 2, RoundingMode.DOWN);
                    soChia.put(id, tien);
                    daChia = daChia.add(tien);
                }
                BigDecimal le = phanBo.subtract(daChia);
                while (le.signum() > 0) {
                    boolean daBoSung = false;
                    for (KhoaHoc k : duDieuKien) {
                        int id = k.maKhoaHoc();
                        if (soChia.get(id).add(new BigDecimal("0.01"))
                                .compareTo(conLai.get(id)) <= 0) {
                            soChia.put(id, soChia.get(id).add(new BigDecimal("0.01")));
                            le = le.subtract(new BigDecimal("0.01"));
                            daBoSung = true;
                            if (le.signum() == 0) break;
                        }
                    }
                    if (!daBoSung) throw new IllegalStateException("Không phân bổ hết ưu đãi cố định");
                }
                for (KhoaHoc k : duDieuKien) ghi(k.maKhoaHoc(), ct.maChuongTrinh(),
                        soChia.get(k.maKhoaHoc()), conLai, giam);
            }
        }
        List<Dong> dong = new ArrayList<>();
        for (KhoaHoc k : khoa) dong.add(new Dong(k.maKhoaHoc(),
                k.hocPhiMacDinh().setScale(2, RoundingMode.HALF_UP), giam.get(k.maKhoaHoc())));
        return new BangGia(dong);
    }

    private static void ghi(int maKhoa, int maChuongTrinh, BigDecimal tien,
            Map<Integer, BigDecimal> conLai, Map<Integer, Map<Integer, BigDecimal>> giam) {
        BigDecimal thucTe = tien.min(conLai.get(maKhoa));
        giam.get(maKhoa).put(maChuongTrinh, thucTe);
        conLai.put(maKhoa, conLai.get(maKhoa).subtract(thucTe));
    }

    public static void kiemTraCauHinh(ChuongTrinhGiamGia ct) {
        if (ct.ten() == null || ct.ten().isBlank() || ct.ten().length() > 180
                || ct.tuNgay() == null || ct.denNgay() == null
                || ct.denNgay().isBefore(ct.tuNgay()) || ct.giaTri() == null
                || ct.giaTri().signum() < 0 || ct.giaTri().scale() > 2
                || ct.giaTri().precision() > 18)
            throw new IllegalArgumentException("Tên, ngày hoặc giá trị giảm không hợp lệ");
        if ("PHAN_TRAM".equals(ct.loaiGiam())) {
            if (ct.giaTri().compareTo(BigDecimal.valueOf(100)) > 0)
                throw new IllegalArgumentException("Phần trăm giảm phải từ 0 đến 100");
        } else if (!"SO_TIEN".equals(ct.loaiGiam()))
            throw new IllegalArgumentException("Loại giảm giá không hợp lệ");
        if (!ct.apDungTatCaMon() && ct.maMonHoc().isEmpty())
            throw new IllegalArgumentException("Hãy chọn ít nhất một môn áp dụng");
    }
}
