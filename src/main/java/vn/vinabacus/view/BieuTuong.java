package vn.vinabacus.view;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/** Biểu tượng vector vẽ bằng Java2D, không cần tải bộ icon ngoài. */
public final class BieuTuong implements Icon {
    private final String ten;
    private final Color mau;
    private final int co;
    public BieuTuong(String ten, Color mau, int co) { this.ten=ten; this.mau=mau;this.co=co; }
    @Override public int getIconWidth() { return co; }
    @Override public int getIconHeight() { return co; }
    @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
        Graphics2D g=(Graphics2D)gr.create();
        g.translate(x,y);g.scale(co/28.0,co/28.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.setColor(mau);
        switch (ten) {
            case "trangchu" -> { path(g,new int[]{3,14,25},new int[]{13,4,13});
                g.drawLine(6,12,6,25);g.drawLine(22,12,22,25);g.drawLine(6,25,22,25);
                g.drawRoundRect(11,17,6,8,1,1); }
            case "hocvien", "nguoidung" -> { g.drawOval(10,3,9,9);g.drawArc(5,13,18,13,0,180);
                if (ten.equals("hocvien")) {g.drawOval(1,6,6,6);g.drawArc(0,14,8,9,0,90);} }
            case "lop" -> { g.drawRoundRect(2,4,24,17,2,2);g.drawLine(8,24,20,24);g.drawLine(14,21,14,24);
                g.fillOval(11,11,6,6);}
            case "khoa" -> {g.drawRoundRect(4,4,20,21,2,2);g.drawLine(10,4,10,25);
                g.drawLine(7,9,9,9);g.drawLine(14,11,21,11);g.drawLine(14,16,20,16);}
            case "lich","lichhoc" -> {g.drawRoundRect(3,5,22,20,3,3);g.drawLine(3,11,25,11);
                g.drawLine(9,2,9,8);g.drawLine(19,2,19,8);g.fillOval(9,15,3,3);g.fillOval(16,15,3,3);
                g.fillOval(9,21,3,3);}
            case "diemdanh" -> {g.drawRoundRect(3,3,22,22,3,3);g.drawLine(7,14,12,19);g.drawLine(12,19,22,8);}
            case "hocphi" -> {g.drawOval(4,3,20,22);g.drawLine(14,6,14,22);g.drawArc(9,8,10,7,90,180);g.drawArc(9,14,10,7,-90,180);}
            case "hocbu" -> {g.drawArc(4,4,20,20,40,240);g.drawLine(3,7,6,16);g.drawLine(6,16,13,12);
                g.drawArc(4,4,20,20,220,220);g.drawLine(24,21,22,12);g.drawLine(22,12,15,16);}
            case "tuyensinh" -> {g.drawOval(8,3,10,10);g.drawArc(4,14,18,13,0,180);g.drawLine(24,13,24,24);g.drawLine(19,18,28,18);}
            case "kho" -> {g.drawRect(5,10,18,15);path(g,new int[]{2,14,26},new int[]{11,3,11});g.drawLine(11,15,17,15);}
            case "baocao" -> {g.drawLine(3,25,26,25);g.drawLine(3,3,3,25);g.drawRoundRect(7,14,4,10,1,1);
                g.drawRoundRect(14,8,4,16,1,1);g.drawRoundRect(21,4,4,20,1,1);}
            case "hethong", "caidat" -> {g.drawOval(5,5,18,18);g.drawOval(10,10,8,8);
                for(int a=0;a<8;a++){double r=a*Math.PI/4;int x1=(int)(14+11*Math.cos(r)),y1=(int)(14+11*Math.sin(r));
                  int x2=(int)(14+14*Math.cos(r)),y2=(int)(14+14*Math.sin(r));g.drawLine(x1,y1,x2,y2);} }
            case "menu" -> {g.setStroke(new BasicStroke(3));for(int a=0;a<3;a++)g.drawLine(3,6+a*8,25,6+a*8);}
            case "truong" -> {g.drawRect(4,11,20,14);g.drawLine(2,11,14,3);g.drawLine(14,3,26,11);
                for(int i=0;i<3;i++)g.drawRect(8+i*6,15,3,4);}
            case "loc" -> {path(g,new int[]{3,25,17,17,11,11,3},new int[]{4,4,13,24,19,13,4});}
            case "phai" -> {g.drawLine(4,14,24,14);g.drawLine(16,7,24,14);g.drawLine(16,21,24,14);}
            case "xuong" -> {g.drawLine(5,10,14,18);g.drawLine(14,18,23,10);}
            case "kiemtra" -> {g.drawOval(2,2,24,24);g.drawLine(8,14,12,19);g.drawLine(12,19,21,9);}
            case "dongho" -> {g.drawOval(3,3,22,22);g.drawLine(14,14,14,7);g.drawLine(14,14,20,17);}
            case "dangxuat" -> {g.drawRoundRect(3,4,15,20,2,2);g.drawLine(11,14,26,14);g.drawLine(21,9,26,14);g.drawLine(21,19,26,14);}
            case "chinhsua" -> {g.drawLine(5,22,22,5);g.drawLine(9,24,24,9);g.drawLine(4,24,9,24);}
            default -> {g.fillOval(10,10,8,8);}
        }
        g.dispose();
    }
    private void path(Graphics2D g,int[] xs,int[] ys) {
        Path2D p=new Path2D.Double();p.moveTo(xs[0],ys[0]);for(int i=1;i<xs.length;i++)p.lineTo(xs[i],ys[i]);g.draw(p);
    }
    public static String tuMenu(String menu) {
        if (menu.equals("Trang chủ")) return "trangchu";
        if (menu.equals("Quản lý học viên")) return "hocvien";
        if (menu.equals("Quản lý khóa học")) return "khoa";
        if (menu.equals("Quản lý lớp học")) return "lop";
        if (menu.equals("Sắp lịch học")) return "lichhoc";
        if (menu.equals("Điểm danh")) return "diemdanh";
        if (menu.startsWith("Học phí")) return "hocphi";
        if (menu.startsWith("Học bù")) return "hocbu";
        if (menu.equals("Tuyển sinh")) return "tuyensinh";
        if (menu.startsWith("Kho")) return "kho";
        if (menu.startsWith("Báo cáo")) return "baocao";
        return "hethong";
    }
}
