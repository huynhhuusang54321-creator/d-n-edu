package vn.vinabacus.test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import vn.vinabacus.service.CaHocTrungTam;
import vn.vinabacus.service.QuyTacTrungLich;

/** Chay bang javac/java, khong can JUnit hay SQL Server. */
public final class KiemThuTV1 {
    private static int daChay;
    private static void dung(boolean dieuKien, String ten) {
        daChay++;
        if (!dieuKien) throw new AssertionError("Lỗi kiểm thử: " + ten);
    }
    private static LocalTime gio(String s) { return LocalTime.parse(s); }
    private static LocalDate ngay(String s) { return LocalDate.parse(s); }
    public static void main(String[] args) {
        dung(CaHocTrungTam.DANH_SACH.size() == 8, "đủ 8 ca");
        for (int i = 1; i <= 8; i++) {
            var ca = CaHocTrungTam.layTheoSoCa(i);
            dung(ca.soCa() == i, "đúng số ca " + i);
            dung(Duration.between(ca.batDau(), ca.ketThuc()).toMinutes() == 90,
                    "đúng 90 phút ca " + i);
        }
        String[] mocGio = {"07:00", "08:30", "10:00", "11:30", "13:00", "14:30",
                "16:00", "17:30", "19:00"};
        for (int i = 0; i < 8; i++) {
            var ca = CaHocTrungTam.layTheoSoCa(i + 1);
            dung(ca.batDau().equals(gio(mocGio[i])), "đúng giờ bắt đầu ca " + (i + 1));
            dung(ca.ketThuc().equals(gio(mocGio[i + 1])), "đúng giờ kết thúc ca " + (i + 1));
            if (i > 0) {
                var truoc = CaHocTrungTam.layTheoSoCa(i);
                dung(truoc.ketThuc().equals(ca.batDau()), "không nghỉ giữa ca " + i + " / " + (i + 1));
                dung(!QuyTacTrungLich.giaoGio(truoc.batDau(), truoc.ketThuc(),
                        ca.batDau(), ca.ketThuc()), "ca kề nhau không bị coi là trùng " + i);
            }
        }
        dung(CaHocTrungTam.timTheoGio(gio("07:00"), gio("08:30")).isPresent(), "tìm ca 1");
        dung(CaHocTrungTam.timTheoGio(gio("07:05"), gio("08:35")).isEmpty(), "không nhận ca tùy ý");
        dung(QuyTacTrungLich.giaoGio(gio("07:00"), gio("08:30"), gio("08:00"), gio("09:30")), "giao giờ");
        dung(!QuyTacTrungLich.giaoGio(gio("07:00"), gio("08:30"), gio("08:30"), gio("10:00")), "chạm ranh giới không trùng");
        dung(QuyTacTrungLich.trungBuoiThucTe(ngay("2026-11-23"), gio("07:00"), gio("08:30"),
                ngay("2026-11-23"), gio("08:00"), gio("09:30")), "trùng buổi thật");
        dung(!QuyTacTrungLich.trungBuoiThucTe(ngay("2026-11-23"), gio("07:00"), gio("08:30"),
                ngay("2026-11-24"), gio("08:00"), gio("09:30")), "khác ngày");
        dung(QuyTacTrungLich.trungLichDinhKy(2, ngay("2026-11-20"), ngay("2026-12-20"), gio("07:00"), gio("08:30"),
                2, ngay("2026-11-23"), ngay("2026-12-10"), gio("08:00"), gio("09:30")), "trùng lịch thứ hai");
        dung(!QuyTacTrungLich.trungLichDinhKy(2, ngay("2026-11-17"), ngay("2026-11-20"), gio("07:00"), gio("08:30"),
                2, ngay("2026-11-19"), ngay("2026-11-21"), gio("08:00"), gio("09:30")), "giao ngày nhưng không có thứ hai");
        dung(QuyTacTrungLich.trungLichDinhKy(8, ngay("2026-11-19"), ngay("2026-12-01"), gio("07:00"), gio("08:30"),
                8, ngay("2026-11-21"), ngay("2026-11-25"), gio("08:00"), gio("09:30")), "chủ nhật SQL=8");
        QuyTacTrungLich.kiemTraPhong("HOAT_DONG", 20, 20);
        daChay++;
        try {
            QuyTacTrungLich.kiemTraPhong("BAO_TRI", 20, 10);
            throw new AssertionError("Không chặn phòng bảo trì");
        } catch (IllegalArgumentException ok) { daChay++; }
        try {
            QuyTacTrungLich.kiemTraPhong("HOAT_DONG", 20, 21);
            throw new AssertionError("Không chặn quá sức chứa");
        } catch (IllegalArgumentException ok) { daChay++; }
        System.out.println("THÀNH CÔNG: " + daChay + " kiểm thử TV1 (không cần SQL Server).");
    }
}
