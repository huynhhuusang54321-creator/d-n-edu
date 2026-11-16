package vn.vinabacus.view;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;
import vn.vinabacus.model.MucMenu;

/** Vung giao dien trong; khi dang ky module se hien JPanel cua thanh vien. */
public final class KhungNoiDungPanel extends JPanel {
    public KhungNoiDungPanel(MucMenu menu) {
        super(new BorderLayout());
        setBackground(GiaoDienMau.NEN);

        JComponent module = DangKyManHinh.lay(menu.ma());
        if (module != null) {
            add(module, BorderLayout.CENTER);
            return;
        }

        JPanel oGiua = new JPanel(new GridBagLayout());
        oGiua.setOpaque(false);
        JPanel noiDung = new JPanel();
        noiDung.setOpaque(false);
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(new LaptopCodeIcon(112));
        icon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel tieuDe = new JLabel("Khu vực phát triển chức năng");
        tieuDe.setFont(GiaoDienMau.font(Font.BOLD, 25));
        tieuDe.setForeground(GiaoDienMau.XANH_DAM);
        tieuDe.setAlignmentX(CENTER_ALIGNMENT);
        JLabel ghiChu = new JLabel("Thành viên sẽ bổ sung giao diện tại đây");
        ghiChu.setFont(GiaoDienMau.font(Font.PLAIN, 17));
        ghiChu.setForeground(new Color(67, 67, 67));
        ghiChu.setAlignmentX(CENTER_ALIGNMENT);
        JPanel gachVang = new JPanel() {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(GiaoDienMau.VANG);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int w = getWidth(), h = getHeight();
                g.drawArc(w/2-95, 8, 190, 25, 18, 145);
                g.dispose();
            }
        };
        gachVang.setOpaque(false);
        gachVang.setPreferredSize(new Dimension(210, 29));
        gachVang.setMaximumSize(new Dimension(210, 29));
        gachVang.setAlignmentX(CENTER_ALIGNMENT);

        noiDung.add(icon);
        noiDung.add(Box.createVerticalStrut(15));
        noiDung.add(tieuDe);
        noiDung.add(Box.createVerticalStrut(10));
        noiDung.add(ghiChu);
        noiDung.add(Box.createVerticalStrut(9));
        noiDung.add(gachVang);
        oGiua.add(noiDung);
        add(oGiua, BorderLayout.CENTER);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(242, 255, 236));
        Path2D.Double nho = new Path2D.Double();
        nho.moveTo(0, h - 18); nho.curveTo(w*.36, h-68, w*.68, h+18, w, h-46);
        nho.lineTo(w,h); nho.lineTo(0,h); nho.closePath(); g.fill(nho);
        g.setColor(GiaoDienMau.VANG);
        Path2D.Double vang = new Path2D.Double();
        vang.moveTo(w*.43,h); vang.curveTo(w*.65,h-19,w*.8,h-70,w,h-91);
        vang.lineTo(w,h); vang.closePath(); g.fill(vang);
        g.setColor(new Color(69, 196, 0));
        Path2D.Double xanh = new Path2D.Double();
        xanh.moveTo(0,h-11); xanh.curveTo(w*.3,h-54,w*.68,h+37,w,h-57);
        xanh.lineTo(w,h); xanh.lineTo(0,h); xanh.closePath(); g.fill(xanh);
        g.dispose();
    }

    private static final class LaptopCodeIcon implements Icon {
        private final int size;
        LaptopCodeIcon(int size) { this.size = size; }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
        @Override public void paintIcon(Component c, Graphics gg, int x, int y) {
            Graphics2D g = (Graphics2D) gg.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(132, 194, 119));
            g.drawRoundRect(13, 13, size-26, size-40, 7, 7);
            g.drawLine(9, size-24, size-9, size-24);
            g.drawLine(9, size-24, 3, size-15);
            g.drawLine(3, size-15, size-3, size-15);
            g.drawLine(size-3, size-15, size-9, size-24);
            int mid = size/2;
            g.drawPolyline(new int[] {mid-16,mid-28,mid-16}, new int[] {mid-6,mid+6,mid+18}, 3);
            g.drawPolyline(new int[] {mid+16,mid+28,mid+16}, new int[] {mid-6,mid+6,mid+18}, 3);
            g.drawLine(mid+5,mid-10,mid-5,mid+22);
            g.dispose();
        }
    }
}
