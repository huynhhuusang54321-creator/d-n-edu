package vn.vinabacus.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.repository.LichHocLopRepository.BuoiTrongTuan;
import vn.vinabacus.service.CaHocTrungTam;

/**
 * Bang ca DUNG CHUNG cho Them va Sua: 8 ca theo hang doc, 7 thu theo cot ngang.
 * Them: danh dau nhieu o va chon o de gan phong rieng.
 * Sua: cac buoi da ton tai luon co tich; nhan o co lich de chon ban ghi,
 * nhan o trong de de xuat doi ca cho dung ban ghi dang sua (chua ghi SQL).
 */
public final class BangChonCaHocPanel extends JPanel {
    public record OCa(int thu, int soCa) {
        public BuoiTrongTuan buoi() {
            CaHocTrungTam.Ca ca = CaHocTrungTam.layTheoSoCa(soCa);
            return new BuoiTrongTuan(thu, ca.batDau(), ca.ketThuc());
        }
    }
    private static final Color NEN = new Color(0xE8F7EB);
    private static final Color VIEN = new Color(0xD6E4DA);
    private static final String[] THU = {
        "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"
    };
    private final JCheckBox[][] o = new JCheckBox[7][8];
    private final boolean dangSua;
    private final Map<OCa, LichHocLop> daCo = new HashMap<>();
    private LichHocLop buoiGoc;
    private OCa dangChon;
    private Consumer<OCa> khiChon = x -> { };

    public BangChonCaHocPanel(boolean dangSua) {
        super(new GridBagLayout());
        this.dangSua = dangSua;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(VIEN));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        themO(0, 0, nhan("Ca học", NEN, true), 120, g);
        for (int i = 0; i < 7; i++)
            themO(i + 1, 0, nhan(THU[i], NEN, true), 85, g);
        for (int ca = 0; ca < 8; ca++) {
            CaHocTrungTam.Ca item = CaHocTrungTam.layTheoSoCa(ca + 1);
            Color mau = ca < 3 ? new Color(0xFFF6DE) : ca < 6
                    ? new Color(0xE7F7DF) : new Color(0xF2E9FF);
            themO(0, ca + 1, nhan("<html><center><b>Ca " + (ca + 1) + "</b><br>"
                    + gio(item.batDau()) + "–" + gio(item.ketThuc()) + "</center></html>", mau, false), 120, g);
            for (int ngay = 0; ngay < 7; ngay++) {
                final OCa oCa = new OCa(ngay + 2, ca + 1);
                JCheckBox check = new JCheckBox();
                check.setHorizontalAlignment(SwingConstants.CENTER);
                check.setFocusPainted(false);
                check.setOpaque(true);
                check.setToolTipText(THU[ngay] + ", ca " + (ca + 1) + ": "
                        + gio(item.batDau()) + "–" + gio(item.ketThuc())
                        + " (90 phút, liên tục - không nghỉ giữa ca)");
                check.addActionListener(e -> click(oCa));
                o[ngay][ca] = check;
                themO(ngay + 1, ca + 1, check, 85, g);
            }
        }
        setPreferredSize(new Dimension(780, 400));
        toMau();
    }

    public void datKhiChon(Consumer<OCa> callback) {
        khiChon = callback == null ? x -> { } : callback;
    }

    private void click(OCa cell) {
        JCheckBox check = o[cell.thu() - 2][cell.soCa() - 1];
        if (dangSua) {
            LichHocLop banGhi = daCo.get(cell);
            if (banGhi != null) buoiGoc = banGhi;
            // O da co lich chi DUOC CHON de sua; khong bo lich bang checkbox.
            // Neu muon huy, dung nut Huy buoi hoc va xac nhan.
            if (buoiGoc == null) { check.setSelected(false); return; }
            dangChon = cell;
        } else if (check.isSelected()) {
            dangChon = cell;
        } else if (cell.equals(dangChon)) {
            dangChon = layCacO().stream().findFirst().orElse(null);
        }
        toMau();
        khiChon.accept(dangChon);
    }

    /** Danh dau TAT CA buoi co ca chuan; chon san buoi ban dau neu no hop le. */
    public void datBuoiDangSua(LichHocLop selected, List<LichHocLop> cacBuoi) {
        if (!dangSua) throw new IllegalStateException("Chi dung khi sua");
        daCo.clear();
        for (LichHocLop b : cacBuoi) {
            if (!"HIEU_LUC".equals(b.trangThai())) continue;
            timO(b).ifPresent(cell -> daCo.putIfAbsent(cell, b));
        }
        chonBuoiGoc(selected);
    }

    /** Can dung cho cac lich CU khong trung 8 ca: chon ban ghi, sau do chon ca moi. */
    public void chonBuoiGoc(LichHocLop selected) {
        if (!dangSua) throw new IllegalStateException("Chi dung khi sua");
        buoiGoc = selected;
        dangChon = selected == null ? null : timO(selected).orElse(null);
        toMau();
        khiChon.accept(dangChon);
    }

    public LichHocLop layBanGhiDangSua() { return buoiGoc; }
    public OCa layODangChon() { return dangChon; }

    public List<OCa> layCacO() {
        List<OCa> result = new ArrayList<>();
        if (dangSua) {
            if (dangChon != null) result.add(dangChon);
            return result;
        }
        for (int thu = 2; thu <= 8; thu++) for (int ca = 1; ca <= 8; ca++)
            if (o[thu - 2][ca - 1].isSelected()) result.add(new OCa(thu, ca));
        return result;
    }

    public List<BuoiTrongTuan> layCacBuoi() {
        return layCacO().stream().map(OCa::buoi).toList();
    }

    public BuoiTrongTuan layBuoiDangSua() {
        if (!dangSua) throw new IllegalStateException("Chi dung khi sua");
        if (buoiGoc == null || dangChon == null)
            throw new IllegalArgumentException("Hãy chọn một ca chuẩn cho buổi đang sửa");
        LichHocLop daChiem = daCo.get(dangChon);
        if (daChiem != null && daChiem.maLichHoc() != buoiGoc.maLichHoc())
            throw new IllegalArgumentException("Ca này đã có buổi khác của lớp. Hãy chọn ca trống.");
        return dangChon.buoi();
    }

    private void toMau() {
        for (int thu = 2; thu <= 8; thu++) for (int ca = 1; ca <= 8; ca++) {
            OCa cell = new OCa(thu, ca);
            JCheckBox ck = o[thu - 2][ca - 1];
            boolean coLich = dangSua && daCo.containsKey(cell);
            boolean active = cell.equals(dangChon);
            ck.setSelected(coLich || active || (!dangSua && ck.isSelected()));
            ck.setBackground(active ? new Color(0xB7ECC6)
                    : coLich ? new Color(0xE1F1E6)
                    : (ca % 2 == 0 ? Color.WHITE : new Color(0xF4FAF6)));
            ck.setBorder(BorderFactory.createLineBorder(active ? new Color(0x0B913E)
                    : new Color(0xDCE8E0), active ? 2 : 1));
        }
    }

    private static java.util.Optional<OCa> timO(LichHocLop lich) {
        if (lich == null) return java.util.Optional.empty();
        return CaHocTrungTam.timTheoGio(lich.gioBatDau(), lich.gioKetThuc())
                .filter(c -> lich.thuTrongTuan() >= 2 && lich.thuTrongTuan() <= 8)
                .map(c -> new OCa(lich.thuTrongTuan(), c.soCa()));
    }
    private static String gio(LocalTime time) { return String.format("%02d:%02d", time.getHour(), time.getMinute()); }
    private static JLabel nhan(String s, Color bg, boolean dam) {
        JLabel lbl = new JLabel(s, SwingConstants.CENTER);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setForeground(new Color(0x164C35));
        if (dam) lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        return lbl;
    }
    private void themO(int x, int y, javax.swing.JComponent cmp, int width, GridBagConstraints g) {
        g.gridx = x; g.gridy = y; g.weightx = x == 0 ? 0 : 1;
        cmp.setPreferredSize(new Dimension(width, y == 0 ? 44 : 45));
        cmp.setMinimumSize(new Dimension(width, y == 0 ? 44 : 45));
        cmp.setBorder(BorderFactory.createLineBorder(new Color(0xDCE8E0)));
        add(cmp, g);
    }
}
