package vn.vinabacus.giaodien.phanhe;

import vn.vinabacus.giaodienchung.MauSacUngDung;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class ThanhPhanPhanHe {
    private ThanhPhanPhanHe(){}
    public static JPanel tam(){JPanel p=new JPanel();p.setOpaque(false);return p;}
    public static JLabel nhan(String s,int size, boolean dam,Color color){
        JLabel l=new JLabel(s);l.setForeground(color);l.setFont(MauSacUngDung.font(size,dam));return l;
    }
    public static JButton nut(String s,boolean primary){
        JButton b=new JButton(s);b.setFont(MauSacUngDung.font(13,true));b.setBackground(primary?new Color(0x04792D):new Color(0xE9F8E8));
        b.setForeground(primary?Color.WHITE:new Color(0x125F2B));b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10,15,10,15));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }
    public static JPanel card(LayoutManager lm,int pad){
        JPanel p=new JPanel(lm);p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xDEE8DE),1,true),new EmptyBorder(pad,pad,pad,pad)));
        return p;
    }
    public static JTextField nhap(){JTextField t=new JTextField();t.setFont(MauSacUngDung.font(13,false));
        t.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xC9D8CC),1,true),new EmptyBorder(8,10,8,10)));
        return t;}
    public static JComponent duoi(String s){JTextArea a=new JTextArea(s);a.setFont(MauSacUngDung.font(12,false));a.setLineWrap(true);a.setWrapStyleWord(true);
        a.setForeground(new Color(0x356548));a.setBackground(new Color(0xEDF8ED));a.setEditable(false);a.setMargin(new Insets(12,14,12,14));return a;}
}
