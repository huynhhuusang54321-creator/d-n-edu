package vn.vinabacus.test;

import java.time.LocalDate;
import java.time.LocalTime;
import vn.vinabacus.service.CaHocTrungTam;
import vn.vinabacus.service.LichHocLopService;
import vn.vinabacus.service.QuyTacTrungLich;

/** Kiem thu rule Service + danh muc ca, khong can JDBC. */
public final class KiemThuTichHopTV1 {
    private static int soTest;
    private static void dung(boolean ok, String ten) {
        soTest++;
        if (!ok) throw new AssertionError(ten);
    }
    private static void tuChoi(Runnable thaoTac, String ten) {
        try {
            thaoTac.run();
            throw new AssertionError("Không từ chối: " + ten);
        } catch (IllegalArgumentException ok) { soTest++; }
    }
    private static LocalTime gio(String s) { return LocalTime.parse(s); }
    private static LocalDate ngay(String s) { return LocalDate.parse(s); }

    public static void main(String[] args) {
        var batDau = ngay("2026-11-20");
        for (var ca : CaHocTrungTam.DANH_SACH) {
            LichHocLopService.kiemTra(2, ca.batDau(), ca.ketThuc(),
                    batDau, null, "HIEU_LUC");
            soTest++;
        }
        tuChoi(() -> LichHocLopService.kiemTra(2, gio("08:00"), gio("09:30"),
                batDau, null, "HIEU_LUC"), "Giờ thủ công không hợp lệ");
        tuChoi(() -> LichHocLopService.kiemTra(2, null, null,
                batDau, null, "HIEU_LUC"), "Không được bỏ trống ca");
        tuChoi(() -> LichHocLopService.kiemTra(9, gio("07:00"), gio("08:30"),
                batDau, null, "HIEU_LUC"), "Sai thứ");
        tuChoi(() -> LichHocLopService.kiemTra(2, gio("07:00"), gio("08:30"),
                batDau, batDau.minusDays(1), "HIEU_LUC"), "Sai khoảng ngày");
        tuChoi(() -> LichHocLopService.kiemTra(2, gio("07:00"), gio("08:30"),
                batDau, null, "KHAC"), "Sai trạng thái");
        // Huy du lieu legacy ngoai 8 ca van duoc phep, khong mat du lieu.
        LichHocLopService.kiemTra(2, gio("08:00"), gio("09:30"),
                batDau, null, "HUY");
        soTest++;
        LichHocLopService.kiemTra(2, null, null, batDau, null, "HUY");
        soTest++;
        // 2 lich cung thu + cung gio nhung khoang ngay khong co thu Hai -> khong trung.
        dung(!QuyTacTrungLich.trungLichDinhKy(2, ngay("2026-11-17"), ngay("2026-11-20"),
                gio("07:00"), gio("08:30"), 2, ngay("2026-11-19"), ngay("2026-11-21"),
                gio("07:00"), gio("08:30")), "Không trùng nếu không có ngày thứ 2");
        dung(QuyTacTrungLich.trungLichDinhKy(2, ngay("2026-11-17"), null,
                gio("07:00"), gio("08:30"), 2, ngay("2026-11-23"), ngay("2026-11-23"),
                gio("07:00"), gio("08:30")), "Trùng thứ hai khi không có ngày kết thúc");
        dung(!QuyTacTrungLich.trungLichDinhKy(2, ngay("2026-11-17"), null,
                gio("07:00"), gio("08:30"), 3, ngay("2026-11-23"), null,
                gio("07:00"), gio("08:30")), "Khác thứ không trùng");
        System.out.println("THÀNH CÔNG: " + soTest + " kiểm thử tích hợp TV1 (không cần SQL Server).");
    }
}
