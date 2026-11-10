package vn.vinabacus.giaodienchung;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class ThanhPhanGiaoDien {
    private ThanhPhanGiaoDien() {}

    public static final class TheTron extends JPanel {
        private Color mauNen, vien;
        private int boGoc = 20;
        public TheTron(Color mauNen) { this(mauNen, null, 20); }
        public TheTron(Color mauNen, Color vien, int boGoc) {
            this.mauNen = mauNen; this.vien = vien; this.boGoc = boGoc;
            setOpaque(false);
        }
        public void doiMau(Color mau) { this.mauNen = mau; repaint(); }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(mauNen); g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, boGoc, boGoc);
            if (vien != null) { g.setColor(vien); g.setStroke(new BasicStroke(1));
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, boGoc, boGoc); }
            g.dispose(); super.paintComponent(gr);
        }
    }

    public static final class NutTron extends JButton {
        private final Color nen, chu;
        private final int boGoc;
        public NutTron(String noiDung, Color nen, Color chu, int boGoc) {
            super(noiDung); this.nen=nen; this.chu=chu; this.boGoc=boGoc;
            setFont(MauSacUngDung.font(14, true)); setForeground(chu);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9,16,9,16)); setContentAreaFilled(false);
            setFocusPainted(false); setOpaque(false);
        }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g=(Graphics2D)gr.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isRollover()?nen.brighter():nen);
            g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,boGoc,boGoc);
            g.dispose(); super.paintComponent(gr);
        }
    }

    public static JLabel nhan(String text, int co, boolean dam, Color mau) {
        JLabel lb=new JLabel(text); lb.setFont(MauSacUngDung.font(co,dam)); lb.setForeground(mau); return lb;
    }
    public static JPanel panelDoc(int khoang) {
        JPanel p=new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); return p;
    }
    public static JPanel panelNgang() {
        JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));return p;
    }
    public static void khoangCach(Container container, int px) {
        container.add(Box.createRigidArea(new Dimension(0,px)));
    }
    public static void doTrong(JComponent comp, int tren, int trai, int duoi, int phai) {
        comp.setBorder(new EmptyBorder(tren,trai,duoi,phai));
    }
}
