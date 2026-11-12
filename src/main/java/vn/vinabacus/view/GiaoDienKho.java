package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import vn.vinabacus.view.MauSacUngDung;

/**
 * View khung module Kho, nhập/xuất, kiểm kê – thanh vien 6.
 * Lớp khung để phát triển giao diện riêng của module.
 * Menu đang dùng view.phanhe.GiaoDienPhanHe với dữ liệu mẫu trong RAM.
 * Khi nối module thật, truyền sự kiện qua controller và service, không gọi DAO tại view.
 */
public final class GiaoDienKho extends JPanel {
    public GiaoDienKho() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        JLabel nhan = new JLabel("Kho, nhập/xuất, kiểm kê - cho phat trien module", SwingConstants.CENTER);
        nhan.setFont(MauSacUngDung.font(18, true));
        nhan.setForeground(MauSacUngDung.XANH_DAM);
        add(nhan, BorderLayout.CENTER);
    }
}
