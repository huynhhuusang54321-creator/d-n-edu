package vn.vinabacus.test;

import java.awt.Container;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JCheckBox;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong;
import vn.vinabacus.service.CaHocTrungTam;
import vn.vinabacus.view.BangChonCaHocPanel;
import vn.vinabacus.view.ChonPhongHocPanel;

/** Kiem thu checkbox/rooms/dong bo trang thai khi khong co SQL Server. */
public final class KiemThuGiaoDienTV1 {
    private static int pass;
    static void kiem(boolean b, String msg) {
        if (!b) throw new AssertionError(msg);
        pass++;
    }
    static List<JCheckBox> checkboxes(Container p) {
        var result = new ArrayList<JCheckBox>();
        for (var c : p.getComponents()) {
            if (c instanceof JCheckBox ck) result.add(ck);
            if (c instanceof Container child) result.addAll(checkboxes(child));
        }
        return result;
    }
    // 8 ca x 7 cot, grid da them theo row-major, moi row co thu 2 -> CN.
    static JCheckBox cell(BangChonCaHocPanel p, int thu, int ca) {
        return checkboxes(p).get((ca - 1) * 7 + (thu - 2));
    }
    static LichHocLop lich(int id, int thu, String bd, String kt, Integer phong) {
        return new LichHocLop(id, 10, 11, 12, "Toán", "KID 1", "Lớp A",
                thu, LocalTime.parse(bd), LocalTime.parse(kt),
                LocalDate.parse("2026-11-20"), null, "HIEU_LUC", phong,
                phong == null ? null : "P" + phong);
    }
    public static void main(String[] args) {
        for (int c = 1; c <= 8; c++) {
            var ca = CaHocTrungTam.layTheoSoCa(c);
            kiem(java.time.Duration.between(ca.batDau(), ca.ketThuc()).toMinutes() == 90,
                    "Ca " + c + " can 90 phut");
            if (c > 1) kiem(CaHocTrungTam.layTheoSoCa(c - 1).ketThuc().equals(ca.batDau()),
                    "8 ca phai lien tuc");
        }
        kiem(CaHocTrungTam.layTheoSoCa(1).batDau().equals(LocalTime.of(7,0)), "tu 7h");
        kiem(CaHocTrungTam.layTheoSoCa(8).ketThuc().equals(LocalTime.of(19,0)), "den 19h");

        BangChonCaHocPanel them = new BangChonCaHocPanel(false);
        BangChonCaHocPanel sua = new BangChonCaHocPanel(true);
        kiem(checkboxes(them).size() == 56 && checkboxes(sua).size() == 56,
                "Them/Sua dung cung luoi 8x7");
        cell(them, 2, 1).doClick();
        cell(them, 2, 2).doClick();
        cell(them, 7, 8).doClick();
        kiem(them.layCacBuoi().size() == 3, "them nhieu ca, ca cung thu");
        kiem(them.layCacBuoi().get(1).batDau().equals(LocalTime.of(8,30)), "ca 2 08:30");
        kiem(them.layCacBuoi().get(2).thu() == 7, "thu 7");
        kiem(them.layODangChon().equals(new BangChonCaHocPanel.OCa(7,8)), "focus khi them");
        cell(them, 7, 8).doClick();
        kiem(them.layCacBuoi().size() == 2, "bo tick khi them");

        LichHocLop mon = lich(1, 2, "17:30", "19:00", 102);
        LichHocLop wed = lich(2, 4, "10:00", "11:30", 201);
        LichHocLop legacy = lich(3, 7, "20:00", "21:30", 101);
        AtomicReference<LichHocLop> current = new AtomicReference<>();
        sua.datKhiChon(o -> current.set(sua.layBanGhiDangSua()));
        sua.datBuoiDangSua(mon, List.of(mon, wed, legacy));
        kiem(cell(sua,2,8).isSelected(), "auto tick ca 8 thu 2");
        kiem(cell(sua,4,3).isSelected(), "auto tick ca 3 thu 4");
        kiem(!cell(sua,7,8).isSelected(), "khong tu cho lich legacy vao ca sai");
        kiem(current.get().maLichHoc() == 1, "chon buoi goc");
        // Click vao checkbox dang tick: khong duoc xoa buoi, phai chuyen focus.
        cell(sua,4,3).doClick();
        kiem(cell(sua,4,3).isSelected() && cell(sua,2,8).isSelected(), "khong bo buoi khi click");
        kiem(current.get().maLichHoc() == 2, "focus chuyen dung buoi");
        kiem(sua.layBuoiDangSua().batDau().equals(LocalTime.of(10,0)), "chon thu/ca de sua");
        cell(sua,4,2).doClick();
        kiem(sua.layBuoiDangSua().batDau().equals(LocalTime.of(8,30)), "doi ca o trong");
        kiem(sua.layBanGhiDangSua().maLichHoc() == 2, "doi ca khong doi ID");
        kiem(cell(sua,2,8).isSelected(), "buoi khac van tick");
        cell(sua,2,8).doClick();
        kiem(sua.layBanGhiDangSua().maLichHoc() == 1, "chuyen buoi bo doi ca tam");
        sua.chonBuoiGoc(legacy);
        try {
            sua.layBuoiDangSua();
            throw new AssertionError("Legacy phai chon ca moi truoc khi luu");
        } catch (IllegalArgumentException ok) { pass++; }
        cell(sua,7,7).doClick();
        kiem(sua.layBuoiDangSua().batDau().equals(LocalTime.of(16,0)), "legacy doi ca 7");

        List<PhongHoc> rooms = List.of(
                new PhongHoc(101,1,"P101",30,"HOAT_DONG"),
                new PhongHoc(102,1,"P102",18,"HOAT_DONG"),
                new PhongHoc(201,1,"P201",15,"HOAT_DONG"),
                new PhongHoc(202,1,"P202",20,"BAO_TRI"));
        ChonPhongHocPanel panel = new ChonPhongHocPanel(rooms);
        kiem(checkboxes(panel).size()==4,"4 phong");
        panel.chonPhong(102);
        kiem(panel.layPhong().maPhong() == 102, "phong dung buoi goc");
        panel.chonPhong(201);
        kiem(panel.layPhong().maPhong() == 201, "chuyen phong theo buoi");
        kiem(!checkboxes(panel).get(3).isEnabled(), "phong bao tri bi khoa");
        panel.datKhiChon(p -> { if (p != null) current.set(wed); });
        checkboxes(panel).get(0).doClick();
        kiem(panel.layPhong().maPhong()==101,"chon phong dang the");
        kiem(checkboxes(panel).stream().filter(JCheckBox::isSelected).count() == 1,
                "chi 1 phong mot buoi");
        BuoiCoPhong phanBo = new BuoiCoPhong(new BangChonCaHocPanel.OCa(4,3).buoi(),201);
        kiem(phanBo.maPhong() == 201 && phanBo.buoi().thu() == 4, "luu phong rieng moi buoi");
        System.out.println("PASS: " + pass + " kiem thu UI/ca/rooms TV1 (khong can SQL Server).");
    }
}
