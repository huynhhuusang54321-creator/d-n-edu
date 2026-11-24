package vn.vinabacus.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Quy tac nghiep vu DUNG CHUNG cho canh bao lich Trung tam.
 * Day la ham tinh thuan, KHONG truy van CSDL va KHONG thay the kiem tra
 * theo BUOI HOC THUC TE trong giao dich khi them/sua/doi phong/doi giao vien.
 */
public final class QuyTacTrungLich {
    private QuyTacTrungLich() { }

    /** Khoang [batDau, ketThuc): 08:30 va 08:30 KHONG bi coi la trung. */
    public static boolean giaoGio(LocalTime bd1, LocalTime kt1,
                                  LocalTime bd2, LocalTime kt2) {
        Objects.requireNonNull(bd1, "bd1");
        Objects.requireNonNull(kt1, "kt1");
        Objects.requireNonNull(bd2, "bd2");
        Objects.requireNonNull(kt2, "kt2");
        if (!kt1.isAfter(bd1) || !kt2.isAfter(bd2)) {
            throw new IllegalArgumentException("Khoảng giờ phải có giờ kết thúc sau giờ bắt đầu");
        }
        return bd1.isBefore(kt2) && bd2.isBefore(kt1);
    }

    /** Kiem tra hai buoi thuc te co cung ngay va giao gio khong. */
    public static boolean trungBuoiThucTe(LocalDate ngay1, LocalTime bd1, LocalTime kt1,
                                          LocalDate ngay2, LocalTime bd2, LocalTime kt2) {
        Objects.requireNonNull(ngay1, "ngay1");
        Objects.requireNonNull(ngay2, "ngay2");
        return ngay1.equals(ngay2) && giaoGio(bd1, kt1, bd2, kt2);
    }

    /**
     * Canh bao xung dot giua HAI LICH DINH KY cung thu, trong khoang ngay hieu luc.
     * ThuTrongTuan SQL: 2=Thu Hai, ... 7=Thu Bay, 8=Chu Nhat.
     * Khong ket luan trung neu giao nhau 1-2 ngay nhung KHONG chua ngay thu do.
     * denNgay = null nghia la chua co ngay ket thuc.
     */
    public static boolean trungLichDinhKy(
            int thu1, LocalDate tu1, LocalDate den1, LocalTime bd1, LocalTime kt1,
            int thu2, LocalDate tu2, LocalDate den2, LocalTime bd2, LocalTime kt2) {
        kiemTraThu(thu1);
        kiemTraThu(thu2);
        Objects.requireNonNull(tu1, "tu1");
        Objects.requireNonNull(tu2, "tu2");
        LocalDate cuoi1 = den1 == null ? LocalDate.of(9999, 12, 31) : den1;
        LocalDate cuoi2 = den2 == null ? LocalDate.of(9999, 12, 31) : den2;
        if (cuoi1.isBefore(tu1) || cuoi2.isBefore(tu2)) {
            throw new IllegalArgumentException("Ngày kết thúc phải >= ngày bắt đầu");
        }
        if (thu1 != thu2 || !giaoGio(bd1, kt1, bd2, kt2)) return false;
        LocalDate dauGiao = tu1.isAfter(tu2) ? tu1 : tu2;
        LocalDate cuoiGiao = cuoi1.isBefore(cuoi2) ? cuoi1 : cuoi2;
        if (dauGiao.isAfter(cuoiGiao)) return false;
        DayOfWeek thuJava = DayOfWeek.of(thu1 - 1); // 2 -> MONDAY, 8 -> SUNDAY
        LocalDate lanDau = dauGiao.with(TemporalAdjusters.nextOrSame(thuJava));
        return !lanDau.isAfter(cuoiGiao);
    }

    public static void kiemTraPhong(String trangThai, int sucChua, int siSo) {
        if (!"HOAT_DONG".equals(trangThai)) {
            throw new IllegalArgumentException("Phòng học không ở trạng thái hoạt động");
        }
        if (sucChua <= 0 || siSo < 0) {
            throw new IllegalArgumentException("Sức chứa/sĩ số không hợp lệ");
        }
        if (siSo > sucChua) {
            throw new IllegalArgumentException("Sĩ số vượt quá sức chứa phòng học");
        }
    }

    private static void kiemTraThu(int thu) {
        if (thu < 2 || thu > 8) {
            throw new IllegalArgumentException("Thứ học phải từ 2 (Thứ Hai) đến 8 (Chủ nhật)");
        }
    }
}
