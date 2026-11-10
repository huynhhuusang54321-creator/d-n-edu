package vn.vinabacus.giaodienchung;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Bộ chọn một ngày có lịch popup, không chọn khoảng ngày. */
public final class BoChonNgay extends JPanel {
    private static final DateTimeFormatter DMY=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JButton nut;
    private LocalDate ngay;
    private Consumer<LocalDate> khiDoi;
    private final JPopupMenu lichPopup=new JPopupMenu();
    private YearMonth thangDangXem;

    public BoChonNgay(LocalDate ngayMacDinh) {
        this.ngay=ngayMacDinh; this.thangDangXem=YearMonth.from(ngay);
        setOpaque(false);setLayout(new BorderLayout());
        nut=new JButton(); nut.setFont(MauSacUngDung.font(14,false));
        nut.setForeground(new Color(0x555C77)); nut.setBackground(Color.WHITE);
        nut.setFocusPainted(false);nut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nut.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xC5CCD4),1,true),
                new EmptyBorder(8,14,8,14)));
        nut.setPreferredSize(new Dimension(190,39));
        nut.addActionListener(e->hienLich());
        add(nut);capNhatNut();
        lichPopup.setBorder(BorderFactory.createLineBorder(MauSacUngDung.VIEN));
    }
    public LocalDate layNgay() { return ngay; }
    public void kichHoat(boolean bat) { nut.setEnabled(bat); nut.setForeground(bat ? new Color(0x555C77) : new Color(0xAAB0B8)); }
    public void datNgay(LocalDate ngay) {
        if (ngay==null) return;
        this.ngay=ngay;this.thangDangXem=YearMonth.from(ngay);capNhatNut();
        if(khiDoi!=null)khiDoi.accept(ngay);
    }
    public void khiChonNgay(Consumer<LocalDate> callback){khiDoi=callback;}
    private void capNhatNut(){nut.setText("▦   "+ngay.format(DMY)+"    ⌄");}
    private void hienLich() {
        lichPopup.removeAll();
        JPanel p=new JPanel(new BorderLayout(2,8));p.setBackground(Color.WHITE);p.setBorder(new EmptyBorder(12,12,12,12));
        JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);
        JButton truoc=nho("‹"),sau=nho("›");
        JLabel lb=ThanhPhanGiaoDien.nhan("Tháng "+thangDangXem.getMonthValue()+" / "+thangDangXem.getYear(),14,true,MauSacUngDung.CHU);
        lb.setHorizontalAlignment(SwingConstants.CENTER);
        truoc.addActionListener(e->{thangDangXem=thangDangXem.minusMonths(1);hienLich();});
        sau.addActionListener(e->{thangDangXem=thangDangXem.plusMonths(1);hienLich();});
        title.add(truoc,BorderLayout.WEST);title.add(lb,BorderLayout.CENTER);title.add(sau,BorderLayout.EAST);
        p.add(title,BorderLayout.NORTH);
        JPanel grid=new JPanel(new GridLayout(0,7,3,3));grid.setOpaque(false);
        for(String str:new String[]{"T2","T3","T4","T5","T6","T7","CN"}) {
            JLabel l=ThanhPhanGiaoDien.nhan(str,11,true,MauSacUngDung.PHU);
            l.setHorizontalAlignment(SwingConstants.CENTER);grid.add(l);
        }
        int offset=thangDangXem.atDay(1).getDayOfWeek().getValue()-1;
        for(int i=0;i<offset;i++){grid.add(new JLabel());}
        for(int i=1;i<=thangDangXem.lengthOfMonth();i++){
            LocalDate day=thangDangXem.atDay(i);
            JButton b=nho(String.valueOf(i)); b.setPreferredSize(new Dimension(32,32));
            if (day.equals(ngay)) {b.setBackground(MauSacUngDung.XANH);b.setForeground(Color.WHITE);b.setOpaque(true);}
            else if (day.equals(LocalDate.now())) {b.setForeground(MauSacUngDung.XANH);}
            b.addActionListener(e->{datNgay(day);lichPopup.setVisible(false);});grid.add(b);
        }
        p.add(grid,BorderLayout.CENTER);
        JButton homNay=nho("Chọn hôm nay");homNay.setForeground(MauSacUngDung.XANH);
        homNay.addActionListener(e->{datNgay(LocalDate.now());lichPopup.setVisible(false);});p.add(homNay,BorderLayout.SOUTH);
        lichPopup.add(p);
        lichPopup.show(nut,0,nut.getHeight());
    }
    private static JButton nho(String text){
        JButton b=new JButton(text);b.setFont(MauSacUngDung.font(12,true));b.setFocusPainted(false);
        b.setBackground(Color.WHITE);b.setBorder(new EmptyBorder(5,7,5,7));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }
}
