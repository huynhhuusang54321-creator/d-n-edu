package vn.vinabacus.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.service.PhongHocService;

/** Mau du lieu DUNG CHUNG cho form Them va Sua phong. */
public final class PhongHocFormPanel extends JPanel {
    public record DuLieu(String ma, String ten, int sucChua, String trangThai, String ghiChu) { }

    private static final Color CHU = GiaoDienMau.CHU;
    private static final Color VIEN = new Color(0xAEBDB5);
    private final JTextField ma = new JTextField();
    private final JTextField ten = new JTextField();
    private final JSpinner sucChua;
    private final JTextArea ghiChu = new JTextArea(4, 28);
    private final JRadioButton sanSang = taoRadio("Sẵn sàng");
    private final JRadioButton baoTri = taoRadio("Bảo trì");
    private final JRadioButton ngungSuDung = taoRadio("Ngừng sử dụng");

    public PhongHocFormPanel(PhongHoc phongCu) {
        super(new GridBagLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
        sucChua = new JSpinner(new SpinnerNumberModel(phongCu == null ? 20 : phongCu.sucChua(), 1, 100000, 1));
        ma.setText(phongCu == null ? "" : PhongHocService.maHienThi(phongCu));
        ten.setText(phongCu == null ? "" : PhongHocService.tenHienThi(phongCu));
        ghiChu.setText(phongCu == null || phongCu.ghiChu() == null ? "" : phongCu.ghiChu());
        ma.setEditable(phongCu == null);
        if (phongCu != null) {
            ma.setBackground(new Color(0xEDF1EF));
            ma.setForeground(CHU);
        }
        ghiChu.setLineWrap(true);
        ghiChu.setWrapStyleWord(true);
        ghiChu.setFont(GiaoDienMau.font(Font.PLAIN, 15));
        ghiChu.setForeground(CHU);
        ghiChu.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        for (javax.swing.JComponent c : new javax.swing.JComponent[]{ma, ten, sucChua}) {
            c.setPreferredSize(new Dimension(450, 41));
            c.setFont(GiaoDienMau.font(Font.PLAIN, 16));
            c.setForeground(CHU);
            c.setBorder(BorderFactory.createLineBorder(VIEN));
        }
        ma.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(VIEN),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        ten.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(VIEN),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));

        ButtonGroup group = new ButtonGroup();
        for (JRadioButton r : new JRadioButton[]{sanSang, baoTri, ngungSuDung}) {
            group.add(r);
            r.addActionListener(e -> capNhatMauRadio());
        }
        JPanel radioRow = new JPanel(new GridLayout(1, 3, 8, 0));
        radioRow.setBackground(new Color(0xF2F9F3));
        radioRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC1DEC9)),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)));
        for (JRadioButton r : new JRadioButton[]{sanSang, baoTri, ngungSuDung}) radioRow.add(r);
        radioRow.setPreferredSize(new Dimension(440, 56));
        chonTrangThai(phongCu == null ? PhongHocService.HOAT_DONG : phongCu.trangThai());

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 7, 8, 12);
        String[] nhan = {"Mã phòng (*)", "Tên phòng (*)", "Sức chứa (*)", "Trạng thái", "Ghi chú"};
        javax.swing.JComponent[] truong = {ma, ten, sucChua, radioRow,
                new JScrollPane(ghiChu, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)};
        for (int i = 0; i < nhan.length; i++) {
            JLabel label = new JLabel(nhan[i]);
            label.setFont(GiaoDienMau.font(Font.BOLD, 16));
            label.setForeground(CHU);
            g.gridx = 0; g.gridy = i; g.weightx = 0; g.anchor = GridBagConstraints.NORTHWEST;
            g.insets = new Insets(i == 4 ? 13 : 8, 4, 8, 16);
            add(label, g);
            g.gridx = 1; g.weightx = 1;
            g.insets = new Insets(6, 0, 6, 2);
            if (i == 4) truong[i].setPreferredSize(new Dimension(440, 94));
            add(truong[i], g);
        }
    }

    private static JRadioButton taoRadio(String text) {
        JRadioButton r = new JRadioButton(text);
        r.setFont(GiaoDienMau.font(Font.PLAIN, 15));
        r.setForeground(CHU);
        r.setFocusPainted(false);
        r.setOpaque(true);
        r.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
        r.setBorderPainted(false);
        return r;
    }

    private void capNhatMauRadio() {
        for (JRadioButton r : new JRadioButton[]{sanSang, baoTri, ngungSuDung}) {
            r.setBackground(r.isSelected() ? new Color(0xCEEED5) : new Color(0xF2F9F3));
            r.setForeground(CHU);
            r.setFont(GiaoDienMau.font(r.isSelected() ? Font.BOLD : Font.PLAIN, 15));
        }
    }

    public void chonTrangThai(String trangThai) {
        switch (trangThai) {
            case PhongHocService.BAO_TRI -> baoTri.setSelected(true);
            case PhongHocService.NGUNG_SU_DUNG -> ngungSuDung.setSelected(true);
            default -> sanSang.setSelected(true);
        }
        capNhatMauRadio();
    }

    public String layTrangThai() {
        if (baoTri.isSelected()) return PhongHocService.BAO_TRI;
        if (ngungSuDung.isSelected()) return PhongHocService.NGUNG_SU_DUNG;
        return PhongHocService.HOAT_DONG;
    }

    public int soRadioDuocChon() {
        int dem = 0;
        for (JRadioButton r : new JRadioButton[]{sanSang, baoTri, ngungSuDung})
            if (r.isSelected()) dem++;
        return dem;
    }

    public DuLieu layDuLieu() {
        return new DuLieu(ma.getText().trim(), ten.getText().trim(),
                ((Number) sucChua.getValue()).intValue(), layTrangThai(), ghiChu.getText());
    }
}
