package vn.vinabacus.test;

import java.util.List;
import javax.swing.SwingUtilities;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.PhongHocRepository;
import vn.vinabacus.service.PhongHocService;
import vn.vinabacus.view.PhongHocPanel;

/** Kiem tra nghiep vu danh muc phong khong can SQL Server. */
public final class KiemThuPhongHocTV1 {
    private static int so;
    private static void dung(boolean condition, String msg) {
        so++;
        if (!condition) throw new AssertionError(msg);
    }
    private static void sai(Runnable action, String msg) {
        so++;
        try { action.run(); throw new AssertionError("KHONG TU CHOI: " + msg); }
        catch (IllegalArgumentException expected) { /* dat */ }
    }

    public static void main(String[] args) throws Exception {
        PhongHoc p1 = new PhongHoc(1, 5, "[MẪU] P101 - Phòng 101", 20,
                PhongHocService.HOAT_DONG, "Phòng Toán");
        PhongHoc p2 = new PhongHoc(2, 5, "P102 - Phòng 102", 18,
                PhongHocService.BAO_TRI, "Đang sửa chữa");
        PhongHoc p3 = new PhongHoc(3, 5, "P201 - Phòng 201", 30,
                PhongHocService.NGUNG_SU_DUNG, null);
        dung("P101".equals(PhongHocService.maHienThi(p1)), "ma phong mau");
        dung("Phòng 101".equals(PhongHocService.tenHienThi(p1)), "ten phong mau");
        dung("P102".equals(PhongHocService.maHienThi(p2)), "ma phong thuong");
        dung("P301 - Phòng 301".equals(PhongHocService.tenLuu("p301", " Phòng 301 ")),
                "chuan hoa ten luu");
        dung("Sẵn sàng".equals(PhongHocService.nhanTrangThai(p1.trangThai())), "tt san sang");
        dung("Bảo trì".equals(PhongHocService.nhanTrangThai(p2.trangThai())), "tt bao tri");
        dung("Ngừng sử dụng".equals(PhongHocService.nhanTrangThai(p3.trangThai())), "tt ngung");
        dung(PhongHocService.loc(List.of(p1,p2,p3), "p10", null).size() == 2, "loc ma");
        dung(PhongHocService.loc(List.of(p1,p2,p3), "Phong 101", null).size() == 1, "tim khong dau");
        dung(PhongHocService.loc(List.of(p1,p2,p3), "", PhongHocService.BAO_TRI).size() == 1, "loc trang thai");
        dung(PhongHocService.loc(List.of(p1,p2,p3), "dang sua", null).size() == 1, "loc ghi chu");
        List<PhongHocRepository.LopSuDung> suDung = List.of(
                new PhongHocRepository.LopSuDung(1, 10, "A1", "Tiếng Anh", "Anh 1",
                        "Giáo viên A", 12, 15, "DANG_MO", true),
                new PhongHocRepository.LopSuDung(1, 11, "A2", "Tiếng Anh", "Anh 1",
                        null, 0, 15, "DA_DONG", false),
                new PhongHocRepository.LopSuDung(2, 12, "B1", "Toán", "Kid 1",
                        null, 5, 10, "DANG_MO", true));
        dung(PhongHocService.soLopDangSuDung(suDung, 1) == 1,
                "chi dem lop dang su dung, khong tinh lich su");
        dung(PhongHocService.soLopDangSuDung(suDung, 2) == 1,
                "dem rieng tung phong");
        dung(PhongHocService.soLopDangSuDung(suDung, 3) == 0,
                "phong chua co lop");
        PhongHocService.kiemTra(null, "p301", "Phòng 301", 30, PhongHocService.HOAT_DONG, null);
        dung(true, "tao hop le");
        PhongHocService.kiemTra(p1, "P101", "Phòng học Toán", 20, PhongHocService.HOAT_DONG, "");
        dung(true, "sua hop le");
        sai(() -> PhongHocService.kiemTra(null, "", "Phòng", 30, "HOAT_DONG", ""), "ma rong");
        sai(() -> PhongHocService.kiemTra(null, "P 102", "Phòng", 30, "HOAT_DONG", ""), "ma khong hop le");
        sai(() -> PhongHocService.kiemTra(null, "P101", "", 30, "HOAT_DONG", ""), "ten rong");
        sai(() -> PhongHocService.kiemTra(null, "P101", "Phòng", 0, "HOAT_DONG", ""), "suc chua = 0");
        sai(() -> PhongHocService.kiemTra(null, "P101", "Phòng", -1, "HOAT_DONG", ""), "suc chua am");
        sai(() -> PhongHocService.kiemTra(null, "P101", "Phòng", 10, "SAI", ""), "trang thai sai");
        sai(() -> PhongHocService.kiemTra(p1, "P102", "Phòng", 20, "HOAT_DONG", ""), "doi ma");
        sai(() -> PhongHocService.kiemTra(null, "P101", "Phòng", 20, "HOAT_DONG", "x".repeat(501)), "ghi chu dai");
        sai(() -> PhongHocService.kiemTra(null, "P101", "x".repeat(99), 20, "HOAT_DONG", ""), "ten qua dai");
        SwingUtilities.invokeAndWait(() -> {
            PhongHocPanel panel = new PhongHocPanel();
            dung(panel.getComponentCount() == 3, "giao dien dung 3 vung");
        });
        System.out.println("THANH CONG: " + so + " kiem thu PhongHoc TV1 (khong can SQL Server).");
    }
}
