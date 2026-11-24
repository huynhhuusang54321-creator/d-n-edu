package vn.vinabacus.service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Danh muc ca hoc CO DINH cho Trung tam VINABACUS.
 * Dung chung cho form tao/sua lop, lich hoc va thoi khoa bieu.
 * Java 17+, khong phu thuoc JDBC.
 */
public final class CaHocTrungTam {
    private CaHocTrungTam() { }

    public record Ca(int soCa, String buoi, LocalTime batDau, LocalTime ketThuc) {
        public Ca {
            if (soCa < 1 || soCa > 8) throw new IllegalArgumentException("Số ca không hợp lệ");
            Objects.requireNonNull(buoi, "buoi");
            Objects.requireNonNull(batDau, "batDau");
            Objects.requireNonNull(ketThuc, "ketThuc");
            if (Duration.between(batDau, ketThuc).toMinutes() != 90) {
                throw new IllegalArgumentException("Mỗi ca phải đúng 90 phút");
            }
        }
        @Override public String toString() {
            return "Ca " + soCa + " - " + batDau + "–" + ketThuc + " (" + buoi + ")";
        }
    }

    // Lich hoc lien tuc, moi ca 90 phut, bat dau 07:00. Nguon duy nhat cho UI va SQL.
    public static final List<Ca> DANH_SACH = List.of(
            ca(1, "Sáng", "07:00", "08:30"),
            ca(2, "Sáng", "08:30", "10:00"),
            ca(3, "Sáng", "10:00", "11:30"),
            ca(4, "Trưa", "11:30", "13:00"),
            ca(5, "Chiều", "13:00", "14:30"),
            ca(6, "Chiều", "14:30", "16:00"),
            ca(7, "Chiều", "16:00", "17:30"),
            ca(8, "Tối", "17:30", "19:00")
    );

    /**
     * Bao ve quy tac 8 ca lien tuc: 07:00-19:00, 90 phut/ca, KHONG co gio nghi.
     * Neu ai thay doi mot moc gio trong tuong lai, ung dung se bao loi thay vi
     * luu lich sai hoac hien thi sai gio giua form va thoi khoa bieu.
     */
    static {
        if (DANH_SACH.size() != 8
                || !DANH_SACH.get(0).batDau().equals(LocalTime.of(7, 0))
                || !DANH_SACH.get(7).ketThuc().equals(LocalTime.of(19, 0))) {
            throw new IllegalStateException("Trung tam phai co dung 8 ca, tu 07:00 den 19:00");
        }
        for (int i = 0; i < DANH_SACH.size(); i++) {
            Ca hienTai = DANH_SACH.get(i);
            if (hienTai.soCa() != i + 1) {
                throw new IllegalStateException("So ca hoc khong lien tuc: " + hienTai.soCa());
            }
            if (i > 0 && !DANH_SACH.get(i - 1).ketThuc().equals(hienTai.batDau())) {
                throw new IllegalStateException("Khong duoc co gio nghi giua ca " + i + " va " + (i + 1));
            }
        }
    }

    private static Ca ca(int soCa, String buoi, String bd, String kt) {
        return new Ca(soCa, buoi, LocalTime.parse(bd), LocalTime.parse(kt));
    }

    public static Ca layTheoSoCa(int soCa) {
        if (soCa < 1 || soCa > DANH_SACH.size()) {
            throw new IllegalArgumentException("Ca học phải từ 1 đến 8");
        }
        return DANH_SACH.get(soCa - 1);
    }

    public static Optional<Ca> timTheoGio(LocalTime batDau, LocalTime ketThuc) {
        return DANH_SACH.stream().filter(c -> c.batDau().equals(batDau)
                && c.ketThuc().equals(ketThuc)).findFirst();
    }

    /** Gọi tại Service khi người dùng chọn ca, không cho nhập giờ tùy ý. */
    public static Ca batBuocCaCoDinh(LocalTime batDau, LocalTime ketThuc) {
        return timTheoGio(batDau, ketThuc).orElseThrow(() ->
                new IllegalArgumentException("Chỉ được chọn 1 trong 8 ca học cố định"));
    }
}
