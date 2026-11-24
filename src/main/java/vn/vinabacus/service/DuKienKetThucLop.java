package vn.vinabacus.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Set;
import vn.vinabacus.model.LichHocLop;

/** Tinh buoi thu N tu lich dinh ky hieu luc, khong ghi vao CSDL. */
public final class DuKienKetThucLop {
    private DuKienKetThucLop() { }

    public record Lich(int maLich, int thu, LocalTime batDau, LocalTime ketThuc,
                       LocalDate tuNgay, LocalDate denNgay, String trangThai) { }
    public record BuoiDieuChinh(int maBuoiHoc, int maLop, Integer maLichHoc,
                                LocalDate ngayHoc, int soCa, String trangThai,
                                Integer maPhong, String tenPhong) {
        public BuoiDieuChinh(int maBuoiHoc, int maLop, Integer maLichHoc,
                             LocalDate ngayHoc, int soCa, String trangThai) {
            this(maBuoiHoc, maLop, maLichHoc, ngayHoc, soCa, trangThai, null, null);
        }
    }
    private record LanHoc(Lich lich, LocalDate ngay, boolean motLan) {
        LanHoc(Lich lich, LocalDate ngay) { this(lich, ngay, false); }
    }

    /**
     * Moi o thu/ca la mot buoi, ke ca khi hai ca dien ra cung ngay.
     * Ngay bat dau duoc tinh trong so buoi; lich HUY va lich ngoai 8 ca
     * khong duoc dung de du kien cho khoa hoc moi.
     */
    public static Optional<LocalDate> tinh(LocalDate ngayBatDau, int tongSoBuoi,
                                           List<LichHocLop> cacLich) {
        return tinh(ngayBatDau, tongSoBuoi, cacLich, List.of(), Set.of());
    }

    public static Optional<LocalDate> tinh(LocalDate ngayBatDau, int tongSoBuoi,
                                           List<LichHocLop> cacLich,
                                           List<BuoiDieuChinh> dieuChinh,
                                           Set<LocalDate> ngayNghi) {
        Objects.requireNonNull(cacLich, "cacLich");
        return tinhTuLich(ngayBatDau, tongSoBuoi, cacLich.stream()
                .map(x -> new Lich(x.maLichHoc(), x.thuTrongTuan(), x.gioBatDau(),
                        x.gioKetThuc(), x.tuNgay(), x.denNgay(), x.trangThai()))
                .toList(), dieuChinh, ngayNghi);
    }

    public static Optional<LocalDate> tinhTuLich(LocalDate ngayBatDau, int tongSoBuoi,
                                                  List<Lich> cacLich) {
        return tinhTuLich(ngayBatDau, tongSoBuoi, cacLich, List.of(), Set.of());
    }

    /** Buoi HUY loai mot lan lap; DU_KIEN them buoi bu/doi va khong dem doi. */
    public static Optional<LocalDate> tinhTuLich(LocalDate ngayBatDau, int tongSoBuoi,
                                                  List<Lich> cacLich,
                                                  List<BuoiDieuChinh> dieuChinh,
                                                  Set<LocalDate> ngayNghi) {
        Objects.requireNonNull(ngayBatDau, "ngayBatDau");
        Objects.requireNonNull(cacLich, "cacLich");
        Objects.requireNonNull(dieuChinh, "dieuChinh");
        Objects.requireNonNull(ngayNghi, "ngayNghi");
        if (tongSoBuoi <= 0) throw new IllegalArgumentException("Tổng số buổi phải lớn hơn 0");
        PriorityQueue<LanHoc> hangDoi = new PriorityQueue<>(Comparator
                .comparing(LanHoc::ngay)
                .thenComparing(x -> x.lich().batDau())
                .thenComparingInt(x -> x.lich().maLich()));
        for (Lich lich : cacLich) {
            if (!"HIEU_LUC".equals(lich.trangThai()) || lich.tuNgay() == null
                    || lich.thu() < 2 || lich.thu() > 8
                    || lich.batDau() == null || lich.ketThuc() == null
                    || CaHocTrungTam.timTheoGio(lich.batDau(), lich.ketThuc()).isEmpty())
                continue;
            LocalDate tu = ngayBatDau.isAfter(lich.tuNgay()) ? ngayBatDau : lich.tuNgay();
            LocalDate ngay = tu.with(TemporalAdjusters.nextOrSame(
                    DayOfWeek.of(lich.thu() - 1)));
            if (lich.denNgay() == null || !ngay.isAfter(lich.denNgay()))
                hangDoi.add(new LanHoc(lich, ngay));
        }
        Set<String> huyLich = new HashSet<>();
        Set<String> huyCa = new HashSet<>();
        for (BuoiDieuChinh buoi : dieuChinh) {
            if (buoi.ngayHoc() == null || buoi.ngayHoc().isBefore(ngayBatDau)) continue;
            if ("HUY".equals(buoi.trangThai())) {
                if (buoi.maLichHoc() != null)
                    huyLich.add(buoi.maLichHoc() + ":" + buoi.ngayHoc());
                else huyCa.add(buoi.ngayHoc() + ":" + buoi.soCa());
            } else if ("DU_KIEN".equals(buoi.trangThai())) {
                if (buoi.soCa() < 1 || buoi.soCa() > 8) return Optional.empty();
                CaHocTrungTam.Ca ca = CaHocTrungTam.layTheoSoCa(buoi.soCa());
                Lich motLan = new Lich(-buoi.maBuoiHoc(),
                        buoi.ngayHoc().getDayOfWeek().getValue() + 1,
                        ca.batDau(), ca.ketThuc(), buoi.ngayHoc(),
                        buoi.ngayHoc(), "HIEU_LUC");
                hangDoi.add(new LanHoc(motLan, buoi.ngayHoc(), true));
            }
        }
        Set<String> daDem = new HashSet<>();
        int soBuoi = 0;
        while (!hangDoi.isEmpty()) {
            LanHoc lan = hangDoi.remove();
            int soCa = CaHocTrungTam.timTheoGio(lan.lich().batDau(),
                    lan.lich().ketThuc()).orElseThrow().soCa();
            String ngayCa = lan.ngay() + ":" + soCa;
            boolean huy = !lan.motLan()
                    && (huyLich.contains(lan.lich().maLich() + ":" + lan.ngay())
                    || huyCa.contains(ngayCa));
            if (!huy && !ngayNghi.contains(lan.ngay()) && daDem.add(ngayCa)
                    && ++soBuoi == tongSoBuoi) return Optional.of(lan.ngay());
            if (lan.motLan()) continue;
            if (lan.ngay().getYear() >= 9999) continue;
            LocalDate ngayTiep = lan.ngay().plusWeeks(1);
            if (ngayTiep.getYear() <= 9999
                    && (lan.lich().denNgay() == null
                    || !ngayTiep.isAfter(lan.lich().denNgay())))
                hangDoi.add(new LanHoc(lan.lich(), ngayTiep));
        }
        return Optional.empty();
    }
}
