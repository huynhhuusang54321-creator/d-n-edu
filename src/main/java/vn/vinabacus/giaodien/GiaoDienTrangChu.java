package vn.vinabacus.giaodien;

import vn.vinabacus.giaodienchung.*;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DuLieuMau;
import vn.vinabacus.xuly.XuLyDuLieuMau;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Dashboard dùng chung bố cục, tách dữ liệu và lịch Mầm non / Trung tâm. */
public final class GiaoDienTrangChu extends JPanel {
    private static final DateTimeFormatter DMY=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final CoSo coSo;
    private final GiaoDienChinh cha;
    private final JPanel phanNoiDung = new JPanel();
    private LocalDate ngayDangXem=LocalDate.now();
    private LocalDate ngayChon=LocalDate.now();
    private boolean tuyChonNgay=false;

    public GiaoDienTrangChu(CoSo coSo, GiaoDienChinh cha) {
        this.coSo=coSo;this.cha=cha;
        setBackground(MauSacUngDung.NEN);setLayout(new BorderLayout());
        phanNoiDung.setOpaque(false);phanNoiDung.setLayout(new BoxLayout(phanNoiDung,BoxLayout.Y_AXIS));
        phanNoiDung.setBorder(new EmptyBorder(19,19,18,19));
        add(phanNoiDung,BorderLayout.CENTER);
        capNhatTrang();
    }
    private void capNhatTrang(){
        phanNoiDung.removeAll();
        phanNoiDung.add(taoBanner());gap(12);
        phanNoiDung.add(taoBoLoc());gap(12);
        phanNoiDung.add(taoTheThongKe());gap(12);
        phanNoiDung.add(taoPhanDuoi());gap(13);
        JLabel demo=ThanhPhanGiaoDien.nhan("DỮ LIỆU MẪU  •  Giao diện dùng để duyệt thiết kế, chưa có tài khoản đăng nhập và chưa kết nối SQL Server.",11,false,MauSacUngDung.PHU);
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);phanNoiDung.add(demo);
        phanNoiDung.revalidate();phanNoiDung.repaint();
    }
    private void gap(int px){phanNoiDung.add(Box.createVerticalStrut(px));}
    private void stretch(JComponent p,int cao){p.setMaximumSize(new Dimension(Integer.MAX_VALUE,cao));p.setPreferredSize(new Dimension(1000,cao));p.setAlignmentX(Component.LEFT_ALIGNMENT);}

    private JComponent taoBanner(){
        JPanel banner=new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics gr){
                Graphics2D g=(Graphics2D)gr.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0,0,new Color(0xF2FAF2),getWidth(),0,new Color(0xEAF8EB)));
                g.fillRoundRect(0,0,getWidth(),getHeight(),22,22);g.dispose();super.paintComponent(gr);
            }
        };
        banner.setOpaque(false);stretch(banner,165);
        JPanel left=new JPanel();left.setOpaque(false);left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(33,28,17,0));
        JLabel hi=ThanhPhanGiaoDien.nhan("Xin chào, Nguyễn Văn A!",28,true,new Color(0x00502D));
        hi.setAlignmentX(Component.LEFT_ALIGNMENT);left.add(hi);left.add(Box.createVerticalStrut(7));
        JLabel hello=ThanhPhanGiaoDien.nhan("Chào mừng bạn đến với VINABACUS",20,true,MauSacUngDung.NAVY);
        hello.setAlignmentX(Component.LEFT_ALIGNMENT);left.add(hello);left.add(Box.createVerticalStrut(14));
        JPanel gold=new JPanel();gold.setBackground(MauSacUngDung.VANG);
        gold.setPreferredSize(new Dimension(87,5));gold.setMaximumSize(new Dimension(87,5));gold.setAlignmentX(Component.LEFT_ALIGNMENT);left.add(gold);
        banner.add(left,BorderLayout.CENTER);
        String image=coSo==CoSo.MAM_NON?"/hinhanh/mamnon_truong.png":"/hinhanh/trungtam_truong.png";
        JPanel art=new JPanel(){
            private BufferedImage b=docAnh(image);
            @Override protected void paintComponent(Graphics gr){super.paintComponent(gr);
                if(b!=null){Graphics2D g=(Graphics2D)gr.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int h=getHeight();int w=Math.min(getWidth(),Math.max(430,(int)(h*b.getWidth()/(double)b.getHeight())));
                    g.drawImage(b,getWidth()-w,0,w,h,null);g.dispose();}
            }
        };
        art.setOpaque(false);art.setPreferredSize(new Dimension(440,165));banner.add(art,BorderLayout.EAST);
        return banner;
    }
    private BufferedImage docAnh(String path){try{return ImageIO.read(getClass().getResource(path));}catch(Exception ex){return null;}}

    private JComponent taoBoLoc(){
        ThanhPhanGiaoDien.TheTron card=new ThanhPhanGiaoDien.TheTron(Color.WHITE,MauSacUngDung.VIEN,20);
        card.setLayout(new BorderLayout(12,0));card.setBorder(new EmptyBorder(11,21,11,19));stretch(card,67);
        JPanel fields=new JPanel(new FlowLayout(FlowLayout.LEFT,16,3));fields.setOpaque(false);
        JLabel ic=new JLabel(new BieuTuong("lich",MauSacUngDung.XANH,27));fields.add(ic);
        fields.add(ThanhPhanGiaoDien.nhan("Lọc theo ngày:",14,true,MauSacUngDung.CHU));
        JRadioButton homNay=new JRadioButton("Hôm nay"),tuyChon=new JRadioButton("Tùy chọn ngày");
        ButtonGroup group=new ButtonGroup();group.add(homNay);group.add(tuyChon);
        for(JRadioButton r:List.of(homNay,tuyChon)){r.setOpaque(false);r.setFont(MauSacUngDung.font(14,r==homNay));
            r.setFocusPainted(false);r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
        homNay.setForeground(MauSacUngDung.XANH_DAM);tuyChon.setForeground(MauSacUngDung.NAVY);
        homNay.setSelected(!tuyChonNgay);tuyChon.setSelected(tuyChonNgay);
        fields.add(homNay);fields.add(tuyChon);
        BoChonNgay date=new BoChonNgay(ngayChon);date.kichHoat(tuyChonNgay);
        fields.add(date);
        homNay.addActionListener(e->{tuyChonNgay=false;ngayChon=LocalDate.now();date.datNgay(ngayChon);date.kichHoat(false);});
        tuyChon.addActionListener(e->{tuyChonNgay=true;date.kichHoat(true);});
        date.khiChonNgay(d->ngayChon=d);
        card.add(fields,BorderLayout.CENTER);
        ThanhPhanGiaoDien.NutTron nut=new ThanhPhanGiaoDien.NutTron("⚲  Lọc",new Color(0x008B23),Color.WHITE,18);
        nut.setPreferredSize(new Dimension(112,42));nut.addActionListener(e->{
            ngayDangXem=tuyChonNgay?date.layNgay():LocalDate.now();
            ngayChon=ngayDangXem;capNhatTrang();
        });card.add(nut,BorderLayout.EAST);return card;
    }

    private JComponent taoTheThongKe(){
        JPanel row=new JPanel(new GridLayout(1,3,14,0));row.setOpaque(false);stretch(row,110);
        DuLieuMau.SoLieu s=XuLyDuLieuMau.laySoLieu(coSo);
        boolean ngayCoMau=XuLyDuLieuMau.coDuLieuMauTrongNgay(ngayDangXem);
        row.add(taoThe("hocvien",s.hocVien(),coSo==CoSo.MAM_NON?"Học sinh hiện tại":"Học viên đang học",
                new Color(0xF2FBF3),new Color(0x008A20)));
        row.add(taoThe("lop",s.lop(),"Lớp học",new Color(0xFFF9ED),new Color(0xEFA800)));
        row.add(taoThe("lich",ngayCoMau?s.coMat():-1,
                ngayDangXem.equals(LocalDate.now())?"Có mặt hôm nay":"Có mặt ngày "+ngayDangXem.format(DMY),
                new Color(0xF0F6FF),MauSacUngDung.XANH_DUONG));return row;
    }
    private JComponent taoThe(String type,int value,String name,Color bg,Color color){
        ThanhPhanGiaoDien.TheTron card=new ThanhPhanGiaoDien.TheTron(bg,MauSacUngDung.VIEN,20);
        card.setLayout(new BorderLayout(17,0));card.setBorder(new EmptyBorder(16,25,16,12));
        ThanhPhanGiaoDien.TheTron logo=new ThanhPhanGiaoDien.TheTron(new Color(color.getRed(),color.getGreen(),color.getBlue(),20));
        logo.setLayout(new GridBagLayout());logo.setPreferredSize(new Dimension(74,70));
        logo.add(new JLabel(new BieuTuong(type,color,45)));card.add(logo,BorderLayout.WEST);
        JPanel numbers=new JPanel();numbers.setOpaque(false);numbers.setLayout(new BoxLayout(numbers,BoxLayout.Y_AXIS));
        JLabel n=ThanhPhanGiaoDien.nhan(value<0?"—":String.valueOf(value),33,true,Color.BLACK);
        n.setAlignmentX(Component.LEFT_ALIGNMENT);numbers.add(n);numbers.add(Box.createVerticalStrut(4));
        JLabel l=ThanhPhanGiaoDien.nhan(name,14,false,Color.BLACK);l.setAlignmentX(Component.LEFT_ALIGNMENT);
        numbers.add(l);card.add(numbers,BorderLayout.CENTER);return card;
    }

    private JComponent taoPhanDuoi(){
        JPanel row=new JPanel(new GridBagLayout());row.setOpaque(false);stretch(row,310);
        GridBagConstraints c=new GridBagConstraints();c.fill=GridBagConstraints.BOTH;c.gridy=0;c.weighty=1;
        c.gridx=0;c.weightx=0.48;c.insets=new Insets(0,0,0,7);
        row.add(taoBangLich(),c);
        c.gridx=1;c.weightx=0.52;c.insets=new Insets(0,7,0,0);
        row.add(taoBangDiemDanh(),c);
        return row;
    }
    private JPanel taoBangLich(){
        ThanhPhanGiaoDien.TheTron p=new ThanhPhanGiaoDien.TheTron(Color.WHITE,MauSacUngDung.VIEN,20);
        p.setLayout(new BorderLayout());p.setBorder(new EmptyBorder(14,17,12,17));
        boolean today=ngayDangXem.equals(LocalDate.now());
        String title=(coSo==CoSo.MAM_NON?"Lịch hoạt động":"Lịch học")+(today?" hôm nay":" ngày "+ngayDangXem.format(DMY));
        p.add(taoTieuDeBang("lich",title,"Sắp lịch học"),BorderLayout.NORTH);
        JPanel items=new JPanel();items.setOpaque(false);items.setLayout(new BoxLayout(items,BoxLayout.Y_AXIS));
        items.setBorder(new EmptyBorder(8,0,0,0));
        if (!XuLyDuLieuMau.coDuLieuMauTrongNgay(ngayDangXem)) {
            items.add(thongBaoRong("Không có dữ liệu lịch mẫu cho ngày đã chọn."));
        } else {
            for(DuLieuMau.Lich l:XuLyDuLieuMau.layLich(coSo)){
                JPanel rr=new JPanel(new BorderLayout(9,0));rr.setOpaque(false);
                int rowHeight=coSo==CoSo.TRUNG_TAM&&l.noiDung().size()>2?60:43;
                rr.setMaximumSize(new Dimension(Integer.MAX_VALUE,rowHeight));rr.setPreferredSize(new Dimension(400,rowHeight));
                JLabel gio=ThanhPhanGiaoDien.nhan(l.gio(),12,true,MauSacUngDung.CHU);
                gio.setHorizontalAlignment(SwingConstants.CENTER);gio.setOpaque(true);gio.setBackground(new Color(0xDFFBD8));
                gio.setBorder(new EmptyBorder(3,5,3,5));gio.setPreferredSize(new Dimension(125,30));
                JPanel pill=new JPanel(new BorderLayout());pill.setOpaque(false);pill.setBorder(new EmptyBorder(3,0,3,0));pill.add(gio,BorderLayout.CENTER);
                rr.add(pill,BorderLayout.WEST);
                JPanel content=new JPanel(new FlowLayout(FlowLayout.LEFT,6,6));content.setOpaque(false);
                for(String item:l.noiDung()){
                    if(coSo==CoSo.MAM_NON){content.add(ThanhPhanGiaoDien.nhan(item,13,false,MauSacUngDung.CHU));}
                    else {
                        JPanel chip=new JPanel(new FlowLayout(FlowLayout.LEFT,4,1));chip.setBackground(new Color(0xFAFCFB));
                        chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MauSacUngDung.VIEN,1,true),
                                new EmptyBorder(2,5,2,5)));
                        chip.add(new JLabel(new BieuTuong("khoa",MauSacUngDung.XANH,14)));
                        chip.add(ThanhPhanGiaoDien.nhan(item,11,false,MauSacUngDung.CHU));content.add(chip);
                    }
                }
                rr.add(content,BorderLayout.CENTER);items.add(rr);
            }
        }
        p.add(items,BorderLayout.CENTER);return p;
    }
    private JPanel taoBangDiemDanh(){
        ThanhPhanGiaoDien.TheTron p=new ThanhPhanGiaoDien.TheTron(Color.WHITE,MauSacUngDung.VIEN,20);
        p.setLayout(new BorderLayout());p.setBorder(new EmptyBorder(14,17,14,17));
        boolean today=ngayDangXem.equals(LocalDate.now());
        String title="Tình hình điểm danh"+(today?" hôm nay":" ngày "+ngayDangXem.format(DMY));
        p.add(taoTieuDeBang("baocao",title,"Điểm danh"),BorderLayout.NORTH);
        boolean has=XuLyDuLieuMau.coDuLieuMauTrongNgay(ngayDangXem);
        DuLieuMau.SoLieu s=XuLyDuLieuMau.laySoLieu(coSo);
        JPanel group=new JPanel(new GridBagLayout());group.setOpaque(false);
        GridBagConstraints c=new GridBagConstraints();c.gridy=0;c.fill=GridBagConstraints.BOTH;c.weighty=1;
        c.gridx=0;c.weightx=0.45;c.insets=new Insets(10,0,0,14);
        group.add(new VongTronDiemDanh(has?s:null,coSo),c);
        c.gridx=1;c.weightx=0.55;c.insets=new Insets(15,10,5,0);
        JPanel legend=new JPanel();legend.setOpaque(false);legend.setLayout(new BoxLayout(legend,BoxLayout.Y_AXIS));
        legend.add(Box.createVerticalGlue());
        String[] names={"Có mặt","Vắng có phép","Vắng không phép"};
        Color[] colors={new Color(0x10B922),new Color(0xFFBF00),MauSacUngDung.DO};
        int[] vals=has?new int[]{s.coMat(),s.vangCoPhep(),s.vangKhongPhep()}:new int[]{-1,-1,-1};
        for(int i=0;i<3;i++){
            ThanhPhanGiaoDien.TheTron legendRow=new ThanhPhanGiaoDien.TheTron(new Color(0xFCFDFC),MauSacUngDung.VIEN,13);
            legendRow.setLayout(new BorderLayout(10,0));legendRow.setBorder(new EmptyBorder(9,10,9,10));
            legendRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));legendRow.setPreferredSize(new Dimension(240,46));
            JPanel name=new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));name.setOpaque(false);
            JLabel swatch=new JLabel("  ");swatch.setOpaque(true);swatch.setBackground(colors[i]);swatch.setPreferredSize(new Dimension(15,16));
            name.add(swatch);name.add(ThanhPhanGiaoDien.nhan(names[i],12,false,MauSacUngDung.CHU));
            legendRow.add(name,BorderLayout.WEST);
            String val=has?String.format(Locale.forLanguageTag("vi-VN"),"%d  (%.1f%%)",vals[i],s.tiLe(vals[i])).replace('.',','):"—";
            legendRow.add(ThanhPhanGiaoDien.nhan(val,12,true,MauSacUngDung.CHU),BorderLayout.EAST);
            legend.add(legendRow);legend.add(Box.createVerticalStrut(7));
        }
        legend.add(Box.createVerticalGlue());group.add(legend,c);p.add(group,BorderLayout.CENTER);return p;
    }
    private JPanel taoTieuDeBang(String icon,String title,String link){
        JPanel h=new JPanel(new BorderLayout(6,0));h.setOpaque(false);h.setPreferredSize(new Dimension(300,39));
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,9,3));left.setOpaque(false);
        left.add(new JLabel(new BieuTuong(icon,MauSacUngDung.XANH,26)));
        left.add(ThanhPhanGiaoDien.nhan(title,16,true,MauSacUngDung.CHU));h.add(left,BorderLayout.WEST);
        JButton more=new JButton("Xem chi tiết  →");more.setFont(MauSacUngDung.font(12,true));more.setForeground(new Color(0x008B21));
        more.setContentAreaFilled(false);more.setFocusPainted(false);more.setBorder(new EmptyBorder(3,2,3,2));
        more.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));more.addActionListener(e->cha.moMuc(link));
        h.add(more,BorderLayout.EAST);return h;
    }
    private JComponent thongBaoRong(String str){
        JLabel l=ThanhPhanGiaoDien.nhan(str,13,false,MauSacUngDung.PHU);l.setBorder(new EmptyBorder(32,12,0,0));return l;
    }
    private static final class VongTronDiemDanh extends JPanel {
        private final DuLieuMau.SoLieu s;private final CoSo coSo;
        VongTronDiemDanh(DuLieuMau.SoLieu s,CoSo coSo){this.s=s;this.coSo=coSo;setOpaque(false);setPreferredSize(new Dimension(200,225));setMinimumSize(new Dimension(160,190));}
        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            Graphics2D g=(Graphics2D)gr.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int d=Math.min(Math.min(getWidth()-14,getHeight()-15),219),x=(getWidth()-d)/2,y=(getHeight()-d)/2;
            if(d<65){g.dispose();return;}
            if(s==null||s.daDiemDanh()==0){g.setColor(new Color(0xE1E7E1));g.fillOval(x,y,d,d);}
            else {
                int[] vals={s.coMat(),s.vangCoPhep(),s.vangKhongPhep()};
                Color[] cols={new Color(0x28C808),new Color(0xFFCF00),new Color(0xFF324B)};
                int start=90,total=s.daDiemDanh(),used=0;
                for(int i=0;i<3;i++){int degree=(i==2?360-used:(int)Math.round(vals[i]*360.0/total));
                    g.setColor(cols[i]);g.fillArc(x,y,d,d,start,-degree);start-=degree;used+=degree;}
            }
            g.setColor(Color.WHITE);int inner=(int)(d*0.68);g.fillOval(x+(d-inner)/2,y+(d-inner)/2,inner,inner);
            g.setColor(Color.BLACK);String number=s==null?"—":String.valueOf(s.coMat());
            g.setFont(MauSacUngDung.font(Math.max(20,(int)(d*.18)),true));
            FontMetrics fm=g.getFontMetrics();g.drawString(number,x+(d-fm.stringWidth(number))/2,y+d/2-7);
            g.setFont(MauSacUngDung.font(12,false));String l1=coSo==CoSo.MAM_NON?"Học sinh":"Lượt học";
            fm=g.getFontMetrics();g.drawString(l1,x+(d-fm.stringWidth(l1))/2,y+d/2+14);
            String l2="có mặt";g.drawString(l2,x+(d-fm.stringWidth(l2))/2,y+d/2+29);
            g.dispose();
        }
    }
}
