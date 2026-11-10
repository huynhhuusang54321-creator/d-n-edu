package vn.vinabacus.giaodien;

import vn.vinabacus.dieukhien.DieuKhienGiaoDienChinh;
import vn.vinabacus.giaodienchung.BieuTuong;
import vn.vinabacus.giaodienchung.MauSacUngDung;
import vn.vinabacus.giaodienchung.ThanhPhanGiaoDien;
import vn.vinabacus.model.CoSo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Khung desktop chung: thanh tiêu đề, menu, thanh cơ sở, tài khoản, vùng nội dung. */
public final class GiaoDienChinh extends JFrame {
    private final DieuKhienGiaoDienChinh dieuKhien = new DieuKhienGiaoDienChinh(this);
    private final JPanel phanChinh = new JPanel(new BorderLayout());
    private final JPanel khungPhai = new JPanel(new BorderLayout());
    private final JPanel khungNoiDung = new JPanel(new BorderLayout());
    private final Map<String, JComponent> cacMucMenu = new LinkedHashMap<>();
    private JPanel menuBenTrai, menuItems;
    private JLabel nhanTieuDe;
    private JButton nutCoSo;
    private boolean thuGon = false;
    private boolean phongTo = false;
    private Rectangle kichThuocCu = new Rectangle();
    private Point viTriChuot = null;

    public GiaoDienChinh() {
        setTitle("VINABACUS – Hệ thống quản lý giáo dục");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1490,Math.max(1050,screen.width-110)),Math.min(880,Math.max(720,screen.height-100)));
        setMinimumSize(new Dimension(1024,700));
        setLocationRelativeTo(null);
        try {setIconImage(taoIconCuaSo());}catch(Exception ignored){}
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(MauSacUngDung.NEN);
        getContentPane().add(taoThanhTieuDe(),BorderLayout.NORTH);
        phanChinh.setOpaque(false);
        getContentPane().add(phanChinh,BorderLayout.CENTER);
        khungPhai.setBackground(MauSacUngDung.NEN);
        khungPhai.add(khungNoiDung,BorderLayout.CENTER);
        lamMoiGiaoDien();
    }

    private Image taoIconCuaSo() {
        java.awt.image.BufferedImage out=new java.awt.image.BufferedImage(48,48,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(MauSacUngDung.VANG);g.fillRoundRect(4,8,40,35,9,9);
        g.setColor(MauSacUngDung.XANH_DAM);g.fillPolygon(new int[]{6,24,42,24},new int[]{16,5,16,25},4);
        g.dispose();return out;
    }

    private JComponent taoThanhTieuDe() {
        JPanel thanh = new JPanel(new BorderLayout());
        thanh.setBackground(new Color(0x00703C));thanh.setPreferredSize(new Dimension(0,42));
        JPanel t=new JPanel(new FlowLayout(FlowLayout.LEFT,12,8));t.setOpaque(false);
        JLabel icon=new JLabel("⬡");icon.setFont(MauSacUngDung.font(23,true));icon.setForeground(MauSacUngDung.VANG);
        t.add(icon);
        JLabel name=new JLabel("VINABACUS – Hệ thống quản lý giáo dục");
        name.setFont(MauSacUngDung.font(16,false));name.setForeground(Color.WHITE);t.add(name);
        thanh.add(t,BorderLayout.WEST);
        JPanel nut=new JPanel(new GridLayout(1,3));nut.setOpaque(false);
        JButton min=nhoKhung("—");min.addActionListener(e->setState(JFrame.ICONIFIED));
        JButton max=nhoKhung("□");max.addActionListener(e->phongToCuaSo());
        JButton dong=nhoKhung("×");dong.addActionListener(e->dongUngDung());
        dong.addMouseListener(new MouseAdapter(){@Override public void mouseEntered(MouseEvent e){dong.setForeground(new Color(0xFFADAD));}
            @Override public void mouseExited(MouseEvent e){dong.setForeground(Color.WHITE);}});
        nut.add(min);nut.add(max);nut.add(dong); thanh.add(nut,BorderLayout.EAST);
        MouseAdapter drag=new MouseAdapter(){
          @Override public void mousePressed(MouseEvent e){viTriChuot=e.getLocationOnScreen();}
          @Override public void mouseDragged(MouseEvent e){
            if(viTriChuot!=null&&!phongTo){Point now=e.getLocationOnScreen();setLocation(getX()+now.x-viTriChuot.x,getY()+now.y-viTriChuot.y);viTriChuot=now;}
          }
          @Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)phongToCuaSo();}
        };
        thanh.addMouseListener(drag);thanh.addMouseMotionListener(drag);
        t.addMouseListener(drag);t.addMouseMotionListener(drag);
        name.addMouseListener(drag);name.addMouseMotionListener(drag);
        return thanh;
    }
    private JButton nhoKhung(String s) {
        JButton b=new JButton(s);b.setPreferredSize(new Dimension(45,42));b.setBorderPainted(false);
        b.setFocusPainted(false);b.setContentAreaFilled(false);b.setFont(MauSacUngDung.font(22,false));
        b.setForeground(Color.WHITE);b.setMargin(new Insets(0,0,0,0));
        b.setBorder(new EmptyBorder(0,0,0,0));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }
    private void phongToCuaSo() {
        if(!phongTo) {
            kichThuocCu=getBounds();setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        }else {setBounds(kichThuocCu);}
        phongTo=!phongTo;
    }
    private void dongUngDung(){int ok=JOptionPane.showConfirmDialog(this,"Bạn muốn đóng VINABACUS?","Đóng ứng dụng",JOptionPane.YES_NO_OPTION);
        if(ok==JOptionPane.YES_OPTION)dispose();}

    public void lamMoiGiaoDien() {
        phanChinh.removeAll();khungPhai.removeAll();
        menuBenTrai=taoMenuBenTrai();
        phanChinh.add(menuBenTrai,BorderLayout.WEST);
        phanChinh.add(khungPhai,BorderLayout.CENTER);
        khungPhai.add(taoThanhDieuHuong(),BorderLayout.NORTH);
        khungPhai.add(khungNoiDung,BorderLayout.CENTER);
        khungNoiDung.removeAll();
        JComponent page = dieuKhien.layMuc().equals("Trang chủ")
                ? new GiaoDienTrangChu(dieuKhien.layCoSo(),this)
                : new vn.vinabacus.giaodien.phanhe.GiaoDienPhanHe(dieuKhien.layCoSo(),dieuKhien.layMuc(),this);
        JScrollPane scroll=new JScrollPane(page);
        scroll.setBorder(null);scroll.setBackground(MauSacUngDung.NEN);
        scroll.getViewport().setBackground(MauSacUngDung.NEN);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        khungNoiDung.add(scroll,BorderLayout.CENTER);
        phanChinh.revalidate(); phanChinh.repaint();
    }

    private JPanel taoMenuBenTrai() {
        JPanel sidebar=new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g=(Graphics2D)gr;g.setPaint(new GradientPaint(0,0,new Color(0x00643D),0,getHeight(),new Color(0x003921)));
                g.fillRect(0,0,getWidth(),getHeight());super.paintComponent(gr);
            }
        };
        sidebar.setOpaque(false);sidebar.setPreferredSize(new Dimension(thuGon?75:268,0));
        JPanel brand=new JPanel();brand.setOpaque(false);brand.setPreferredSize(new Dimension(0,114));
        brand.setLayout(new BoxLayout(brand,BoxLayout.Y_AXIS));
        if(!thuGon){
            JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,15,0));row.setOpaque(false);row.setMaximumSize(new Dimension(265,72));
            JLabel logo=new JLabel("⬡");logo.setForeground(MauSacUngDung.VANG);logo.setFont(MauSacUngDung.font(44,true));row.add(logo);
            JPanel text=new JPanel();text.setOpaque(false);text.setLayout(new BoxLayout(text,BoxLayout.Y_AXIS));
            JLabel l1=ThanhPhanGiaoDien.nhan("VINABACUS",22,true,Color.WHITE);
            JLabel l2=ThanhPhanGiaoDien.nhan("Hệ thống quản lý giáo dục",10,false,Color.WHITE);
            text.add(Box.createVerticalStrut(6));text.add(l1);text.add(Box.createVerticalStrut(4));text.add(l2);row.add(text);
            brand.add(Box.createVerticalStrut(22));brand.add(row);
            JPanel yellow=new JPanel();yellow.setBackground(MauSacUngDung.VANG);yellow.setMaximumSize(new Dimension(66,5));
            yellow.setPreferredSize(new Dimension(66,5));yellow.setAlignmentX(Component.CENTER_ALIGNMENT);brand.add(yellow);
        } else {
            JLabel logo=ThanhPhanGiaoDien.nhan("⬡",35,true,MauSacUngDung.VANG);
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);brand.add(Box.createVerticalStrut(20));brand.add(logo);
        }
        sidebar.add(brand,BorderLayout.NORTH);
        menuItems=new JPanel();menuItems.setOpaque(false);menuItems.setLayout(new BoxLayout(menuItems,BoxLayout.Y_AXIS));
        cacMucMenu.clear();
        for(String muc:dieuKhien.layCoSo().layMenu()) {
            JComponent b=taoMuc(muc,muc.equals(dieuKhien.layMuc()));
            menuItems.add(b);menuItems.add(Box.createVerticalStrut(4));cacMucMenu.put(muc,b);
        }
        JScrollPane sc=new JScrollPane(menuItems);sc.setOpaque(false);sc.getViewport().setOpaque(false);
        sc.setBorder(null);sc.getVerticalScrollBar().setUnitIncrement(14);
        sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebar.add(sc,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setOpaque(false);footer.setBorder(new EmptyBorder(8,17,12,0));
        JLabel version=ThanhPhanGiaoDien.nhan(thuGon?"v0.4":"Giao diện mẫu Java  •  v0.4",11,false,new Color(0xAACDBB));
        footer.add(version,BorderLayout.WEST);sidebar.add(footer,BorderLayout.SOUTH);
        return sidebar;
    }

    private JComponent taoMuc(String muc,boolean active) {
        JPanel wrap=new JPanel(new BorderLayout());wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(300,51));wrap.setPreferredSize(new Dimension(thuGon?68:260,51));
        wrap.setBorder(new EmptyBorder(0,thuGon?10:14,0,thuGon?9:9));
        JPanel p=new JPanel(new BorderLayout(12,0)) {
            @Override protected void paintComponent(Graphics gr) {
                if(active){Graphics2D g=(Graphics2D)gr.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setPaint(new GradientPaint(0,0,new Color(0xECFFDF),getWidth(),0,new Color(0xCEFFC3)));
                    g.fillRoundRect(0,0,getWidth(),getHeight(),18,18);g.dispose();}
                super.paintComponent(gr);
            }
        };
        p.setOpaque(false);p.setBorder(new EmptyBorder(0,thuGon?13:15,0,5));
        Color fg=active?MauSacUngDung.XANH_DAM:Color.WHITE;
        JLabel ic=new JLabel(new BieuTuong(BieuTuong.tuMenu(muc),fg,24));p.add(ic,BorderLayout.WEST);
        if(!thuGon){
            JLabel lb=ThanhPhanGiaoDien.nhan(muc,14,active,fg);p.add(lb,BorderLayout.CENTER);
            if(muc.equals("Hệ thống")){JLabel v=new JLabel(new BieuTuong("xuong",fg,17));p.add(v,BorderLayout.EAST);}
        }
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));p.setToolTipText(muc);
        p.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){dieuKhien.moMuc(muc);}});
        for(Component child:p.getComponents()) child.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){dieuKhien.moMuc(muc);}
        });
        wrap.add(p,BorderLayout.CENTER);return wrap;
    }

    private JComponent taoThanhDieuHuong() {
        JPanel nav=new JPanel(new BorderLayout());nav.setBackground(Color.WHITE);
        nav.setPreferredSize(new Dimension(0,80));
        nav.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(0xE0E7E2)),
                new EmptyBorder(0,21,0,24)));
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,16,16));left.setOpaque(false);
        JButton hamburger=new JButton(new BieuTuong("menu",new Color(0x162142),29));hamburger.setFocusPainted(false);
        hamburger.setContentAreaFilled(false);hamburger.setBorder(new EmptyBorder(6,0,6,8));
        hamburger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hamburger.addActionListener(e->{thuGon=!thuGon;lamMoiGiaoDien();});left.add(hamburger);
        nhanTieuDe=ThanhPhanGiaoDien.nhan(dieuKhien.layMuc(),24,true,new Color(0x121212));left.add(nhanTieuDe);
        nav.add(left,BorderLayout.WEST);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,18,13));right.setOpaque(false);
        nutCoSo=new JButton("Cơ sở: "+dieuKhien.layCoSo().layTen()+"    ⌄",new BieuTuong("truong",MauSacUngDung.XANH,25));
        nutCoSo.setHorizontalAlignment(SwingConstants.LEFT);nutCoSo.setIconTextGap(9);
        nutCoSo.setFont(MauSacUngDung.font(14,true));nutCoSo.setForeground(MauSacUngDung.CHU);
        nutCoSo.setPreferredSize(new Dimension(243,47));nutCoSo.setBackground(Color.WHITE);
        nutCoSo.setFocusPainted(false);nutCoSo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nutCoSo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MauSacUngDung.XANH,1,true),new EmptyBorder(4,14,4,10)));
        nutCoSo.addActionListener(e->moDanhSachCoSo(nutCoSo));right.add(nutCoSo);
        JPanel user=new JPanel(new BorderLayout(7,0));user.setOpaque(false);
        JLabel avatar=new JLabel("●");avatar.setFont(MauSacUngDung.font(35,true));avatar.setForeground(new Color(0x00A82D));user.add(avatar,BorderLayout.WEST);
        JPanel ten=new JPanel();ten.setOpaque(false);ten.setLayout(new BoxLayout(ten,BoxLayout.Y_AXIS));
        ten.add(ThanhPhanGiaoDien.nhan("Nguyễn Văn A",14,true,MauSacUngDung.CHU));
        ten.add(ThanhPhanGiaoDien.nhan("Quản trị viên (mẫu)",11,false,MauSacUngDung.PHU));user.add(ten,BorderLayout.CENTER);
        JLabel drop=new JLabel(new BieuTuong("xuong",MauSacUngDung.NAVY,18));user.add(drop,BorderLayout.EAST);
        user.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter click=new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){moMenuTaiKhoan(user);}};
        user.addMouseListener(click);for(Component x:user.getComponents()){x.addMouseListener(click);
          if(x instanceof JPanel group){for(Component sub:group.getComponents())sub.addMouseListener(click);}}
        right.add(user);nav.add(right,BorderLayout.EAST);
        return nav;
    }
    private void moDanhSachCoSo(Component parent) {
        JPopupMenu popup=new JPopupMenu();
        for(CoSo coSo:CoSo.values()){
            JMenuItem item=new JMenuItem((coSo==dieuKhien.layCoSo()?"✓  ":"     ")+coSo.layTen());
            item.setFont(MauSacUngDung.font(14,coSo==dieuKhien.layCoSo()));
            item.setBorder(new EmptyBorder(9,14,9,36));
            item.addActionListener(e->dieuKhien.chonCoSo(coSo));popup.add(item);
        }
        popup.show(parent,0,parent.getHeight());
    }
    private void moMenuTaiKhoan(Component parent) {
        JPopupMenu popup=new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(MauSacUngDung.VIEN));
        themPopup(popup,"Thông tin tài khoản",()->JOptionPane.showMessageDialog(this,
                "Tài khoản hiển thị đang là dữ liệu mẫu.\nĐăng nhập và quản lý tài khoản sẽ thực hiện ở giai đoạn sau.","Thông tin tài khoản",JOptionPane.INFORMATION_MESSAGE));
        themPopup(popup,"Đổi mật khẩu",()->hienChuaPhatTrien("Đổi mật khẩu"));
        themPopup(popup,"Cài đặt ứng dụng",()->dieuKhien.moMuc("Hệ thống"));
        popup.addSeparator();
        JMenuItem dangXuat=new JMenuItem("Đăng xuất (làm sau)");dangXuat.setForeground(MauSacUngDung.DO);
        dangXuat.setBorder(new EmptyBorder(10,14,10,32));dangXuat.addActionListener(e->hienChuaPhatTrien("Đăng xuất"));popup.add(dangXuat);
        popup.show(parent,0,parent.getHeight());
    }
    private void themPopup(JPopupMenu p,String ten,Runnable r){
        JMenuItem item=new JMenuItem(ten);item.setFont(MauSacUngDung.font(13,false));
        item.setBorder(new EmptyBorder(10,14,10,32));item.addActionListener(e->r.run());p.add(item);
    }
    public void hienChuaPhatTrien(String ten){JOptionPane.showMessageDialog(this,
            "Chức năng \""+ten+"\" sẽ được lập trình ở giai đoạn tiếp theo.\n"+
            "Bản hiện tại chỉ là giao diện với dữ liệu mẫu, chưa kết nối SQL Server.",
            "VINABACUS – Bản xem trước",JOptionPane.INFORMATION_MESSAGE);}
    public void moMuc(String muc){dieuKhien.moMuc(muc);}
    public CoSo layCoSo(){return dieuKhien.layCoSo();}
    public void chonCoSo(CoSo coSo){dieuKhien.chonCoSo(coSo);}
}
