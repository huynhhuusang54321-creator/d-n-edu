package vn.vinabacus.giaodien.hocvu;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import vn.vinabacus.giaodienchung.MauSacUngDung;

/**
 * View khung module Học bù, đổi buổi, chuyển lớp – thanh vien 4.
 * CHUA duoc gan vao menu; menu hien nay van dung giao dien mau v0.1.
 * Khi module san sang, truong nhom moi ket noi View vao GiaoDienChinh.
 */
public final class GiaoDienHocVu extends JPanel {
    public GiaoDienHocVu() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        JLabel nhan = new JLabel("Học bù, đổi buổi, chuyển lớp - cho phat trien module", SwingConstants.CENTER);
        nhan.setFont(MauSacUngDung.font(18, true));
        nhan.setForeground(MauSacUngDung.XANH_DAM);
        add(nhan, BorderLayout.CENTER);
    }
}
