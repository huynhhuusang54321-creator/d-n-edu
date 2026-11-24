package vn.vinabacus.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import vn.vinabacus.model.PhongHoc;

/** Moi o ca hoc chi duoc gan mot phong; cac o khac co the dung phong khac. */
public final class ChonPhongHocPanel extends JPanel {
    private final List<PhongHoc> phong;
    private final List<JRadioButton> nut = new ArrayList<>();
    private final ButtonGroup nhom = new ButtonGroup();
    private Consumer<PhongHoc> khiChon = x -> { };

    public ChonPhongHocPanel(List<PhongHoc> phong) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.phong = List.copyOf(phong);
        setOpaque(false);
        for (PhongHoc p : phong) {
            String trangThai = "HOAT_DONG".equals(p.trangThai()) ? "Sẵn sàng"
                    : "BAO_TRI".equals(p.trangThai()) ? "Bảo trì" : "Ngừng sử dụng";
            String mauThai = "HOAT_DONG".equals(p.trangThai()) ? "#166534" : "#8A4734";
            JRadioButton chon = new JRadioButton("<html><body style='width: 275px'><b>"
                    + html(p.tenPhong()) + "</b><br>Sức chứa: " + p.sucChua()
                    + " chỗ · <font color='" + mauThai + "'>" + trangThai
                    + "</font></body></html>");
            chon.setToolTipText(p.tenPhong() + " · " + p.sucChua() + " chỗ · " + trangThai);
            chon.setOpaque(true);
            chon.setBackground(new Color(0xFAFCFB));
            chon.setForeground(new Color(0x17412C));
            chon.setFont(chon.getFont().deriveFont(Font.PLAIN, 12f));
            chon.setBorderPainted(true);
            chon.setFocusPainted(false);
            chon.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xD7E2DB), 1),
                    BorderFactory.createEmptyBorder(9, 9, 9, 9)));
            chon.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    Math.max(74, chon.getPreferredSize().height + 18)));
            chon.setEnabled("HOAT_DONG".equals(p.trangThai()));
            if (!chon.isEnabled()) chon.setBackground(new Color(0xEAEDED));
            nhom.add(chon);
            nut.add(chon);
            chon.addActionListener(e -> { capNhatMau(); khiChon.accept(layPhong()); });
            if (!nut.isEmpty() && nut.size() > 1) add(Box.createVerticalStrut(8));
            add(chon);
        }
        Dimension can = getPreferredSize();
        setPreferredSize(new Dimension(330, Math.max(240, can.height)));
        capNhatMau();
    }

    private void capNhatMau() {
        for (JRadioButton ck : nut) {
            ck.setBackground(!ck.isEnabled() ? new Color(0xEAEDED)
                    : ck.isSelected() ? new Color(0xE3F7E7) : Color.WHITE);
            ck.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ck.isSelected() ? new Color(0x0B8A43)
                            : new Color(0xD7E2DB), ck.isSelected() ? 2 : 1),
                    BorderFactory.createEmptyBorder(9, 9, 9, 4)));
        }
        repaint();
    }

    private static String html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    public void chonPhong(Integer maPhong) {
        nhom.clearSelection();
        if (maPhong == null) { capNhatMau(); return; }
        for (int i = 0; i < phong.size(); i++) {
            if (phong.get(i).maPhong() == maPhong) {
                nut.get(i).setSelected(true); // phong ngung hoat dong: chi xem, khi luu SQL kiem tra lai.
                break;
            }
        }
        capNhatMau();
    }
    public void datKhiChon(Consumer<PhongHoc> callback) {
        khiChon = callback == null ? x -> { } : callback;
    }
    public PhongHoc layPhong() {
        for (int i = 0; i < phong.size(); i++) if (nut.get(i).isSelected()) return phong.get(i);
        return null;
    }
}
