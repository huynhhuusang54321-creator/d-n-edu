package vn.vinabacus.view;

import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableCellRenderer;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import vn.vinabacus.controller.TaiChinhController;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.TaiChinhDTO;
import vn.vinabacus.model.TongHopThuPhi;
import vn.vinabacus.model.TongHopThuPhi.Dong;
import vn.vinabacus.service.TaiChinhService;

/**
 * TV5 – Một màn hình Quản lý thu phí thay cho 3 tab Học phí/Khoản phải thu/Công nợ.
 * Phiếu thu, Tiền ăn Mầm non và Báo cáo vẫn là các tab độc lập.
 * Không thay đổi cơ chế ghi sổ hoặc cơ sở dữ liệu của dự án.
 */
public final class TaiChinhPanel extends JPanel {
    private static final DateTimeFormatter NGAY_GIO=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final CoSo coSo;
    private final TaiChinhController ctrl=new TaiChinhController();
    private final MamNonController mamNonCtrl=new MamNonController();
    private final JTextField thang=new JTextField(8),search=new JTextField(16);
    private final JLabel status=new JLabel("Đang chờ dữ liệu...");
    private final DefaultTableModel mThuPhi=TaiChinhUi.model("Mã HV / Nguồn","Học viên","Nội dung","Phải thu","Đã đóng","Còn nợ","Hạn","Trạng thái","Thao tác");
    private final JTable tThuPhi=TaiChinhUi.bang(mThuPhi);
    private final DefaultTableModel mTienAn=TaiChinhUi.model("Nguồn","Học viên / Lớp",
        "Kỳ / Áp dụng","Số tiền","Loại");
    private final JTable tTienAn=TaiChinhUi.bang(mTienAn);
    private final JComboBox<String> cbTrangThai=new JComboBox<>(new String[]{"Tất cả trạng thái",
        TongHopThuPhi.CHUA_LAP,TongHopThuPhi.CHUA_DONG,TongHopThuPhi.MOT_PHAN,
        TongHopThuPhi.DA_DONG,TongHopThuPhi.THU_VUOT,TongHopThuPhi.DA_HUY,TongHopThuPhi.MIEN_PHI});
    private final JComboBox<String> cbLoai=new JComboBox<>(new String[]{"Tất cả loại","Học phí khóa","Học phí tháng","Tiền ăn"});
    private final JComboBox<String> cbMon=new JComboBox<>(new String[]{"Tất cả môn"});
    private final JComboBox<String> cbKhoa=new JComboBox<>(new String[]{"Tất cả khóa"});
    private final JComboBox<String> cbLop=new JComboBox<>(new String[]{"Tất cả lớp"});
    private final JLabel[] thongKe=new JLabel[9],tongQuan=new JLabel[4];
    private final JTextField reportFrom=new JTextField(9),reportTo=new JTextField(9);
    private final JComboBox<String> reportGroup=new JComboBox<>(new String[]{
        "Theo ngày","Theo tháng","Theo khóa / khoản","Theo nhân viên thu"});
    private final DefaultTableModel mBaoCao=TaiChinhUi.model("Nhóm","Phân bổ ròng");
    private final JTable tBaoCao=TaiChinhUi.bang(mBaoCao);
    private final JLabel reportTotal=new JLabel("Chưa có dữ liệu phân bổ.");
    private final JLabel visibleCount=new JLabel("0 khoản thu"), pageText=new JLabel("Trang 1/1");
    private final JLabel summaryText=new JLabel("Chọn một khoản thu để xem chi tiết hoặc lập phiếu.");
    private JButton btnCreate,btnAdjust,btnReceipt,btnDetail,btnPrev,btnNext;
    private int currentPage=0;
    private static final int PAGE_SIZE=15;
    private List<TaiChinhDTO.KhoanThu> khoan=List.of();
    private List<TaiChinhDTO.DongThuBaoCao> reportRows=List.of();
    private List<TaiChinhDTO.HocPhiTrungTam> hocPhi=List.of();
    private List<TaiChinhDTO.MucPhi> phi=List.of();
    private List<TaiChinhDTO.DangKyChuaThu> dangKy=List.of();
    private List<TaiChinhDTO.BangTinhChot> bangTinh=List.of();
    private List<TaiChinhDTO.LuaChon> classes=List.of(),accounts=List.of();
    private List<Dong> tongHop=List.of(),dangHienThi=List.of();
    private boolean updatingFilters,boQuaTaiPhieu;
    private final JTabbedPane tabs=new JTabbedPane();
    private final List<JButton> navButtons=new ArrayList<>();
    private final JPanel thuPhiTab;
    private final PhieuThuTaiChinhPanel phieu;

    public TaiChinhPanel(CoSo coSo) {
        super(new BorderLayout(8, 10));
        this.coSo=Objects.requireNonNull(coSo);
        setBackground(TaiChinhUi.NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 15, 10, 15));
        thang.setText(YearMonth.now().toString());
        TaiChinhUi.field(search);
        search.setToolTipText("Tìm theo mã khoản, tên học viên, môn, khóa, lớp...");
        JButton reload=TaiChinhUi.nut("Làm mới",true);
        JPanel titleBar=new JPanel(new BorderLayout(8,0)); titleBar.setOpaque(false);
        JLabel heading=TaiChinhUi.title("QUẢN LÝ THU PHÍ – "+coSo.ten().toUpperCase());
        titleBar.add(heading,BorderLayout.WEST);
        JPanel period=TaiChinhUi.hang(new JLabel("Kỳ báo cáo:"),TaiChinhUi.oThang(thang,this),reload);
        titleBar.add(period,BorderLayout.EAST);
        JPanel topArea=new JPanel(new BorderLayout(0,8));topArea.setOpaque(false);
        topArea.add(titleBar,BorderLayout.NORTH);
        JPanel navigation=TaiChinhUi.hang();
        String[] tabNames=coSo==CoSo.MAM_NON
            ?new String[]{"Danh sách thu phí","Phiếu thu","Tiền ăn","Tính phí tháng","Báo cáo"}
            :new String[]{"Danh sách thu phí","Phiếu thu","Báo cáo"};
        String[] tabIcons=coSo==CoSo.MAM_NON
            ?new String[]{"money","check","calendar","calendar","chart"}
            :new String[]{"money","check","chart"};
        for(int i=0;i<tabNames.length;i++){
            final int index=i;
            JButton nav=TaiChinhUi.nut(tabNames[i],false);
            nav.setIcon(new BieuTuong(tabIcons[i],17));
            nav.setIconTextGap(7);
            int navWidth=nav.getFontMetrics(nav.getFont()).stringWidth(tabNames[i])+58;
            nav.setPreferredSize(new Dimension(Math.max(145,navWidth),42));
            nav.addActionListener(e->tabs.setSelectedIndex(index));
            navButtons.add(nav);navigation.add(nav);
        }
        topArea.add(navigation,BorderLayout.SOUTH);
        add(topArea,BorderLayout.NORTH);

        thuPhiTab=tabThuPhi();
        phieu=new PhieuThuTaiChinhPanel(coSo);
        tabs.setUI(new BasicTabbedPaneUI(){
            @Override protected int calculateTabAreaHeight(int placement,int runs,int maxHeight){return 0;}
            @Override protected void paintTabArea(Graphics g,int placement,int selected){}
            @Override protected void paintContentBorder(Graphics g,int placement,int selected){}
        });
        tabs.setBorder(null);
        tabs.setBackground(Color.WHITE);
        tabs.addTab("Quản lý thu phí",thuPhiTab);
        tabs.addTab("Phiếu thu",phieu);
        if(coSo==CoSo.MAM_NON)tabs.addTab("Tiền ăn",tabTienAn());
        if(coSo==CoSo.MAM_NON)tabs.addTab("Tính phí tháng",new TinhPhiMamNonPanel());
        tabs.addTab("Báo cáo",tabBaoCao());
        tabs.addChangeListener(e->{
            capNhatNav();
            if(tabs.getSelectedComponent()==phieu) {
                if(boQuaTaiPhieu)boQuaTaiPhieu=false;
                else phieu.load();
            }else if(tabs.getSelectedComponent()==thuPhiTab
                    ||tabs.getSelectedIndex()==(coSo==CoSo.MAM_NON?4:2))load();
        });
        add(tabs,BorderLayout.CENTER);
        capNhatNav();
        status.setForeground(TaiChinhUi.CHU_MO);
        status.setFont(TaiChinhUi.font(Font.PLAIN,11));
        add(status,BorderLayout.SOUTH);
        reload.addActionListener(e->load()); thang.addActionListener(e->load());
        search.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){currentPage=0;hienThiTatCa();}
            public void removeUpdate(DocumentEvent e){currentPage=0;hienThiTatCa();}
            public void changedUpdate(DocumentEvent e){currentPage=0;hienThiTatCa();}
        });
        for(JComboBox<String> cb:List.of(cbTrangThai,cbLoai,cbMon,cbKhoa,cbLop))
            cb.addActionListener(e->{currentPage=0;hienThiThuPhi();});
        TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(mThuPhi);
        for(int col:new int[]{3,4,5})sorter.setComparator(col,(a,b)->
            soTienHienThi(a).compareTo(soTienHienThi(b)));
        tThuPhi.setRowSorter(sorter);
        tThuPhi.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel columns=tThuPhi.getColumnModel();
        int[] widths={145,220,350,120,120,120,112,145,95};
        for(int i=0;i<widths.length;i++){
            columns.getColumn(i).setPreferredWidth(widths[i]);
            columns.getColumn(i).setMinWidth(i==2?130:i==8?72:65);
        }
        DefaultTableCellRenderer textCell=new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,
                    boolean selected,boolean focus,int row,int col){
                super.getTableCellRendererComponent(table,value,selected,focus,row,col);
                setToolTipText(value==null?null:value.toString());
                return this;
            }
        };
        for(int col:new int[]{0,1,2,6})columns.getColumn(col).setCellRenderer(textCell);
        for(int i=3;i<=5;i++)columns.getColumn(i).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,
                    boolean selected,boolean focus,int row,int col){
                super.getTableCellRendererComponent(table,value,selected,focus,row,col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(BorderFactory.createEmptyBorder(0,3,0,8));
                setBackground(selected?new Color(224,245,231):(row%2==0?Color.WHITE:new Color(249,251,250)));
                setForeground(col==5?new Color(172,49,44):TaiChinhUi.CHU);
                setFont(TaiChinhUi.font(Font.BOLD,12));
                return this;
            }
        });
        columns.getColumn(7).setCellRenderer(new StatusBadge());
        columns.getColumn(8).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,
                    boolean selected,boolean focus,int row,int col){
                super.getTableCellRendererComponent(table,value,selected,focus,row,col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setForeground(TaiChinhUi.XANH);
                setFont(TaiChinhUi.font(Font.BOLD,12));
                setBackground(selected?new Color(224,245,231):Color.WHITE);
                return this;
            }
        });
        tThuPhi.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())capNhatNut();});
        tThuPhi.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                if(SwingUtilities.isLeftMouseButton(e)){
                    int col=tThuPhi.columnAtPoint(e.getPoint());
                    if(col==8 || e.getClickCount()==2)xemChiTiet();
                }
            }
            @Override public void mousePressed(MouseEvent e){popup(e);}
            @Override public void mouseReleased(MouseEvent e){popup(e);}
            private void popup(MouseEvent e){
                if(!e.isPopupTrigger())return;
                int r=tThuPhi.rowAtPoint(e.getPoint());if(r<0)return;
                tThuPhi.setRowSelectionInterval(r,r);
                Dong d=dongDuocChon();if(d==null)return;
                JPopupMenu menu=new JPopupMenu();
                JMenuItem view=new JMenuItem("Xem chi tiết");view.addActionListener(a->xemChiTiet());menu.add(view);
                JMenuItem receive=new JMenuItem("Lập phiếu thu");receive.setEnabled(d.duocThu());
                receive.addActionListener(a->lapPhieuTuDong());menu.add(receive);
                JMenuItem adjust=new JMenuItem("Điều chỉnh khoản thu");adjust.setEnabled(d.duocDieuChinh());
                adjust.addActionListener(a->dieuChinhKhoan());menu.add(adjust);
                if(d.chuaLap()){
                    JMenuItem create=new JMenuItem("Tạo khoản thu");
                    create.addActionListener(a->taoKhoan(d));menu.add(create);
                }
                menu.show(tThuPhi,e.getX(),e.getY());
            }
        });
        capNhatNut();
        SwingUtilities.invokeLater(this::load);
    }

    private static final class StatusBadge extends DefaultTableCellRenderer {
        private Color bg=Color.WHITE;
        StatusBadge(){setHorizontalAlignment(SwingConstants.CENTER);setOpaque(false);}
        @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,
                boolean focus,int row,int col){
            super.getTableCellRendererComponent(t,value,selected,focus,row,col);
            String state=String.valueOf(value);
            bg=switch(state){
                case TongHopThuPhi.DA_DONG -> new Color(218,246,223);
                case TongHopThuPhi.CHUA_DONG -> new Color(255,230,229);
                case TongHopThuPhi.MOT_PHAN -> new Color(255,244,205);
                case TongHopThuPhi.CHUA_LAP -> new Color(236,231,255);
                case TongHopThuPhi.THU_VUOT -> new Color(255,217,217);
                default -> new Color(235,239,237);
            };
            setForeground(switch(state){
                case TongHopThuPhi.DA_DONG -> new Color(0,111,49);
                case TongHopThuPhi.CHUA_DONG -> new Color(183,44,41);
                case TongHopThuPhi.MOT_PHAN -> new Color(163,99,0);
                case TongHopThuPhi.CHUA_LAP -> new Color(95,56,180);
                case TongHopThuPhi.THU_VUOT -> new Color(170,30,30);
                default -> new Color(78,90,83);
            });
            setFont(TaiChinhUi.font(Font.BOLD,11));
            setBorder(BorderFactory.createEmptyBorder(0,3,0,3));
            return this;
        }
        @Override protected void paintComponent(Graphics gg){
            Graphics2D g=(Graphics2D)gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(bg);g.fillRoundRect(4,7,getWidth()-8,getHeight()-14,16,16);g.dispose();
            super.paintComponent(gg);
        }
    }
    private void capNhatNav(){
        for(int i=0;i<navButtons.size();i++){
            JButton button=navButtons.get(i);
            boolean active=i==tabs.getSelectedIndex();
            button.setSelected(active);
            button.setForeground(active?Color.WHITE:TaiChinhUi.XANH_DAM);
            button.repaint();
        }
    }
    private void capNhatNut(){
        Dong d=dongDuocChon();
        if(btnDetail!=null)btnDetail.setEnabled(d!=null);
        if(btnAdjust!=null)btnAdjust.setEnabled(d!=null&&d.duocDieuChinh());
        if(btnReceipt!=null)btnReceipt.setEnabled(d!=null&&d.duocThu());
    }
    private static BigDecimal soTienHienThi(Object value) {
        if(value==null||"—".equals(value.toString()))return BigDecimal.ZERO;
        String s=value.toString().replace("đ","").replace(".","").replace(",",".").trim();
        try{return new BigDecimal(s);}catch(NumberFormatException ex){return BigDecimal.ZERO;}
    }
    private static String tien(BigDecimal n) {
        DecimalFormat f=new DecimalFormat("#,##0.##",DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
        return f.format(n)+"đ";
    }
    private JPanel tabThuPhi() {
        JPanel body=new JPanel(new BorderLayout(0,9));
        body.setOpaque(true);body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(6,4,6,4));
        JPanel header=new JPanel();header.setOpaque(false);
        header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
        JPanel cards=new JPanel(new GridLayout(1,4,10,0));cards.setOpaque(false);
        String[] labels={"Tổng phải thu","Đã đóng","Còn nợ","Chưa lập thu"};
        Color[] backgrounds={new Color(230,250,234),new Color(233,245,255),
            new Color(255,239,236),new Color(242,236,255)};
        Color[] accent={new Color(0,102,47),new Color(20,89,170),
            new Color(191,45,39),new Color(111,61,190)};
        String[] icons={"₫","✓","!","…"};
        for(int i=0;i<4;i++){
            JPanel card=TaiChinhUi.card(backgrounds[i],new Color(219,231,225),18);
            card.setLayout(new BorderLayout(8,2));
            card.setBorder(BorderFactory.createEmptyBorder(11,14,11,10));
            JLabel icon=new JLabel(icons[i],SwingConstants.CENTER);
            icon.setFont(TaiChinhUi.font(Font.BOLD,22));icon.setForeground(accent[i]);
            icon.setPreferredSize(new Dimension(37,45));
            JPanel content=new JPanel(new GridLayout(2,1,0,4));content.setOpaque(false);
            JLabel label=new JLabel(labels[i]);label.setFont(TaiChinhUi.font(Font.BOLD,12));
            label.setForeground(TaiChinhUi.CHU);
            JLabel value=new JLabel("—");value.setFont(TaiChinhUi.font(Font.BOLD,20));
            value.setForeground(accent[i]);tongQuan[i]=value;
            content.add(label);content.add(value);
            card.add(icon,BorderLayout.WEST);card.add(content,BorderLayout.CENTER);
            cards.add(card);
        }
        cards.setPreferredSize(new Dimension(0,78));
        header.add(cards);
        header.add(Box.createVerticalStrut(6));
        JPanel searchRow=new JPanel(new BorderLayout(12,0));searchRow.setOpaque(false);
        JPanel searchBox=new JPanel(new BorderLayout(6,0));searchBox.setOpaque(false);
        JLabel searchIcon=new JLabel("Tìm kiếm:");searchIcon.setFont(TaiChinhUi.font(Font.BOLD,12));
        searchBox.add(searchIcon,BorderLayout.WEST);searchBox.add(search,BorderLayout.CENTER);
        searchRow.add(searchBox,BorderLayout.CENTER);
        JLabel hint=new JLabel("Nhấp đúp vào dòng để xem chi tiết");
        hint.setForeground(TaiChinhUi.CHU_MO);hint.setFont(TaiChinhUi.font(Font.PLAIN,11));
        searchRow.add(hint,BorderLayout.EAST);
        header.add(searchRow);
        header.add(Box.createVerticalStrut(6));
        JPanel filters=new JPanel(new GridLayout(1,coSo==CoSo.TRUNG_TAM?5:2,8,0));filters.setOpaque(false);
        filters.add(filterBox("Trạng thái",cbTrangThai));filters.add(filterBox("Loại thu",cbLoai));
        if(coSo==CoSo.TRUNG_TAM){
            filters.add(filterBox("Môn học",cbMon));filters.add(filterBox("Khóa học",cbKhoa));
            filters.add(filterBox("Lớp",cbLop));
        }
        header.add(filters);
        header.add(Box.createVerticalStrut(6));
        btnCreate=TaiChinhUi.nut("+ Tạo khoản thu",true);
        btnAdjust=TaiChinhUi.nut("Điều chỉnh",false);
        btnReceipt=TaiChinhUi.nut("Lập phiếu thu",false);
        btnDetail=TaiChinhUi.nut("Xem chi tiết",false);
        btnAdjust.setToolTipText("Chọn một khoản thu đã lập để điều chỉnh.");
        btnReceipt.setToolTipText("Chọn một khoản còn nợ để lập phiếu thu.");
        btnDetail.setToolTipText("Chọn một dòng để xem đầy đủ thông tin.");
        JButton export=TaiChinhUi.nut("Xuất CSV",false);
        btnCreate.addActionListener(e->{Dong d=dongDuocChon();taoKhoan(d!=null&&d.chuaLap()?d:null);});
        btnAdjust.addActionListener(e->dieuChinhKhoan());
        btnReceipt.addActionListener(e->lapPhieuTuDong());
        btnDetail.addActionListener(e->xemChiTiet());
        export.addActionListener(e->xuatCsv());
        JPanel actions=new JPanel(new GridLayout(1,5,9,0));actions.setOpaque(false);
        for(JButton b:new JButton[]{btnCreate,btnAdjust,btnReceipt,btnDetail,export}){
            b.setPreferredSize(new Dimension(b.getPreferredSize().width,38));
            actions.add(b);
        }
        header.add(actions);
        if(coSo==CoSo.MAM_NON){
            JPanel more=TaiChinhUi.hang();
            JButton setup=TaiChinhUi.nut("Thiết lập học phí lớp",false);
            JButton prices=TaiChinhUi.nut("Xem mức học phí",false);
            setup.addActionListener(e->thietLapPhi(false));
            prices.addActionListener(e->xemMucHocPhi());
            more.add(setup);more.add(prices);header.add(more);
        }
        header.add(Box.createVerticalStrut(6));
        body.add(header,BorderLayout.NORTH);
        body.add(TaiChinhUi.scroll(tThuPhi),BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(10,0));footer.setOpaque(false);
        visibleCount.setForeground(TaiChinhUi.CHU);visibleCount.setFont(TaiChinhUi.font(Font.BOLD,12));
        summaryText.setForeground(TaiChinhUi.CHU_MO);summaryText.setFont(TaiChinhUi.font(Font.PLAIN,11));
        JPanel left=new JPanel(new GridLayout(2,1,0,2));left.setOpaque(false);
        left.add(visibleCount);left.add(summaryText);footer.add(left,BorderLayout.WEST);
        btnPrev=TaiChinhUi.nut("‹",false);btnNext=TaiChinhUi.nut("›",false);
        btnPrev.setPreferredSize(new Dimension(42,34));btnNext.setPreferredSize(new Dimension(42,34));
        btnPrev.addActionListener(e->{currentPage--;hienThiThuPhi();});
        btnNext.addActionListener(e->{currentPage++;hienThiThuPhi();});
        JPanel pager=TaiChinhUi.hang(btnPrev,pageText,btnNext);
        footer.add(pager,BorderLayout.EAST);
        footer.setPreferredSize(new Dimension(0,45));
        body.add(footer,BorderLayout.SOUTH);
        return body;
    }
    private JPanel filterBox(String label,JComboBox<String> cb){
        JPanel p=new JPanel(new BorderLayout(0,3));p.setOpaque(false);
        JLabel l=new JLabel(label);l.setFont(TaiChinhUi.font(Font.BOLD,12));l.setForeground(TaiChinhUi.CHU);
        TaiChinhUi.field(cb);p.add(l,BorderLayout.NORTH);p.add(cb,BorderLayout.CENTER);
        return p;
    }
    private void xuatCsv(){
        if(dangHienThi.isEmpty()){
            JOptionPane.showMessageDialog(this,"Danh sách đang hiển thị không có dữ liệu để xuất.");return;
        }
        JFileChooser chooser=new JFileChooser();
        chooser.setSelectedFile(new java.io.File("VINABACUS_ThuPhi_"+coSo.name()+"_"+thang.getText()+".csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath();
        if(!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
            path=Path.of(path.toString()+".csv");
        if(Files.exists(path)&&!TaiChinhUi.xacNhan(this,"Tệp đã tồn tại. Bạn muốn ghi đè?"))return;
        try{
            StringBuilder sb=new StringBuilder("\uFEFFMã khoản;Học viên;Nội dung;Phải thu;Đã đóng;Còn nợ;Hạn;Trạng thái\r\n");
            for(Dong d:dangHienThi){
                String[] row={d.maHienThi(),d.hocVien(),d.noiDung(),tien(d.phaiThu()),
                    d.chuaLap()?"":tien(d.daThu()),d.chuaLap()?"":tien(d.conNo()),
                    d.han()==null?"":d.han().toString(),d.trangThai()};
                for(int i=0;i<row.length;i++){
                    if(i>0)sb.append(';');
                    String safe=row[i]==null?"":row[i];
                    if(!safe.isEmpty()&&"=+-@".indexOf(safe.charAt(0))>=0)safe="'"+safe;
                    sb.append('"').append(safe.replace("\"","\"\"")).append('"');
                }
                sb.append("\r\n");
            }
            Files.writeString(path,sb.toString(),StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this,"Đã xuất " + dangHienThi.size() + " dòng ra:\n" + path);
        }catch(Exception ex){TaiChinhUi.loi(this,ex);}
    }
    private JPanel tabTienAn() {
        JButton add=TaiChinhUi.nut("+ Thiết lập đơn giá tiền ăn",true);
        JButton policy=TaiChinhUi.nut("Chính sách tiền ăn",false);
        add.addActionListener(e->thietLapPhi(true));
        policy.addActionListener(e->cauHinhTienAn());
        JPanel p=new JPanel(new BorderLayout(7,8));
        p.add(TaiChinhUi.hang(add,policy,new JLabel("Đơn giá theo lớp; quy tắc tính theo cơ sở và tháng hiệu lực")),BorderLayout.NORTH);
        p.add(TaiChinhUi.scroll(tTienAn),BorderLayout.CENTER);
        return p;
    }
    private void cauHinhTienAn(){
        YearMonth initial;
        try{initial=YearMonth.parse(thang.getText().trim());}catch(Exception ex){initial=YearMonth.now();}
        YearMonth ky=TaiChinhUi.chonThang(this,initial);if(ky==null)return;
        TaiChinhUi.tai(this,()->mamNonCtrl.cauHinhTienAn(ky),current->{
            JCheckBox present=new JCheckBox("Có mặt: tính 1 ngày",current.coMat());
            JCheckBox excused=new JCheckBox("Vắng có phép: tính 1 ngày",current.vangCoPhep());
            JCheckBox unexcused=new JCheckBox("Vắng không phép: tính 1 ngày",current.vangKhongPhep());
            JPanel rules=new JPanel(new GridLayout(3,1,3,4));rules.setOpaque(false);
            rules.add(present);rules.add(excused);rules.add(unexcused);
            JTextField note=new JTextField(current.ghiChu()==null?"":current.ghiChu(),28);
            String source=current.id()==0?"Quy tắc đã duyệt, chưa lưu trong SQL":
                "Đang kế thừa chính sách từ "+current.thangBatDau();
            JPanel form=TaiChinhUi.doc(
                TaiChinhUi.chuThich("Đơn giá/ngày được thiết lập riêng cho từng lớp bằng nút bên cạnh. " +
                    "Chưa điểm danh không tính tiền ăn và không được chốt tháng; ngày nghỉ không tính.",new Color(240,249,243)),
                TaiChinhUi.nhom("Quy tắc áp dụng từ "+ky,TaiChinhUi.form(
                    "Chính sách hiện có",new JLabel(source),"Trạng thái điểm danh",rules,"Ghi chú",note)),
                TaiChinhUi.chuThich("Không thể lưu thay đổi từ kỳ đã có bảng tính phí trở đi; " +
                    "các khoản đã lập cần quy trình điều chỉnh có lịch sử.",new Color(255,248,232)));
            if(!TaiChinhUi.hopThoaiForm(this,"Chính sách tiền ăn Mầm non",
                "Chọn quy tắc theo tháng hiệu lực, không sửa khoản thu cũ",form,"Lưu chính sách",()->{
                    if(note.getText().trim().length()>500)throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
                }))return;
            TaiChinhUi.ghi(this,()->{
                mamNonCtrl.luuCauHinhTienAn(ky,present.isSelected(),excused.isSelected(),
                    unexcused.isSelected(),note.getText());return null;
            },this::load);
        });
    }
    private JPanel tabBaoCao() {
        JPanel body=new JPanel(new GridLayout(3,3,12,12));
        body.setBorder(BorderFactory.createEmptyBorder(18,10,18,10));
        body.setPreferredSize(new Dimension(0,265));
        String[] titles={"Thực thu kỳ đã chọn","Thực thu toàn bộ","Phải thu đang hiệu lực",
            "Công nợ hiện tại","Số khoản còn nợ","Số khoản đã đóng đủ",
            "Giảm giá đăng ký Trung tâm","Số học viên còn nợ","Số phiếu thu hợp lệ"};
        for(int i=0;i<thongKe.length;i++) {
            JPanel card=new JPanel(new BorderLayout(7,7));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(195,225,205)),
                BorderFactory.createEmptyBorder(12,14,12,14)));
            JLabel label=new JLabel(titles[i]);label.setForeground(new Color(50,90,64));
            JLabel number=new JLabel("—");number.setFont(number.getFont().deriveFont(Font.BOLD,22f));
            number.setForeground(TaiChinhUi.XANH);thongKe[i]=number;
            card.add(label,BorderLayout.NORTH);card.add(number,BorderLayout.CENTER);body.add(card);
        }
        JPanel p=new JPanel(new BorderLayout(8,8));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(body,BorderLayout.CENTER);
        reportFrom.setToolTipText("yyyy-MM-dd; để trống hai ô để dùng kỳ báo cáo ở trên");
        reportTo.setToolTipText("yyyy-MM-dd; ngày kết thúc được tính trong báo cáo");
        JPanel controls=TaiChinhUi.hang(new JLabel("Từ ngày:"),reportFrom,
            new JLabel("Đến ngày:"),reportTo,new JLabel("Gộp:"),reportGroup);
        top.add(controls,BorderLayout.SOUTH);p.add(top,BorderLayout.NORTH);
        p.add(TaiChinhUi.scroll(tBaoCao),BorderLayout.CENTER);
        reportGroup.addActionListener(e->hienThiBaoCao());
        reportFrom.addActionListener(e->load());reportTo.addActionListener(e->load());
        JLabel note=new JLabel("Tiền thực thu theo kỳ báo cáo; phải thu và công nợ là số dư hiện tại. Chưa đối trừ tín dụng/bù trừ chuyển lớp.");
        note.setForeground(new Color(133,83,29));
        JPanel foot=TaiChinhUi.doc(reportTotal,note);p.add(foot,BorderLayout.SOUTH);
        return p;
    }
    private void hienThiBaoCao(){
        mBaoCao.setRowCount(0);
        Map<String,BigDecimal> grouped=new LinkedHashMap<>();
        for(TaiChinhDTO.DongThuBaoCao row:reportRows){
            String key=switch(reportGroup.getSelectedIndex()){
                case 1->YearMonth.from(row.ngayThu()).toString();
                case 2->row.khoaHoacKhoan();
                case 3->row.nhanVien();
                default->row.ngayThu().toString();
            };
            grouped.merge(key,row.phanBoRong(),BigDecimal::add);
        }
        BigDecimal sum=BigDecimal.ZERO;
        for(var entry:grouped.entrySet()){
            mBaoCao.addRow(new Object[]{entry.getKey(),tien(entry.getValue())});
            sum=sum.add(entry.getValue());
        }
        reportTotal.setText(grouped.isEmpty()?"Không có phân bổ trong kỳ đã chọn."
            :"Tổng phân bổ ròng theo nhóm: "+tien(sum));
    }
    private YearMonth ky() {
        try{return YearMonth.parse(thang.getText().trim());}
        catch(Exception ex){throw new IllegalArgumentException("Kỳ báo cáo phải theo yyyy-MM, ví dụ 2026-11.");}
    }
    private record Data(List<TaiChinhDTO.KhoanThu> khoan,
        List<TaiChinhDTO.HocPhiTrungTam> hocPhi,List<TaiChinhDTO.MucPhi> phi,
        List<TaiChinhDTO.DangKyChuaThu> dangKy,List<TaiChinhDTO.BangTinhChot> bangTinh,
        List<TaiChinhDTO.LuaChon> lop,List<TaiChinhDTO.LuaChon> taiKhoan,
        TaiChinhDTO.ThongKe report,List<TaiChinhDTO.DongThuBaoCao> reportRows) {}
    private record ChiTietTaiChinh(List<TaiChinhDTO.LichSuThu> history,
                                    List<TaiChinhDTO.UuDaiDangKy> uuDai) {}

    private void load() {
        YearMonth month;
        try{month=ky();}catch(Exception ex){TaiChinhUi.loi(this,ex);return;}
        final LocalDate from,to;
        try{
            if((reportFrom.getText().isBlank()&&reportTo.getText().isBlank())
                    ||tabs.getSelectedIndex()!=(coSo==CoSo.MAM_NON?4:2)
                    &&(reportFrom.getText().isBlank()||reportTo.getText().isBlank())){
                from=month.atDay(1);to=month.plusMonths(1).atDay(1);
            }else{
                if(reportFrom.getText().isBlank()||reportTo.getText().isBlank())
                    throw new IllegalArgumentException("Nhập đủ từ ngày và đến ngày báo cáo.");
                from=LocalDate.parse(reportFrom.getText().trim());
                to=LocalDate.parse(reportTo.getText().trim()).plusDays(1);
                if(!from.isBefore(to))throw new IllegalArgumentException("Khoảng báo cáo không hợp lệ.");
            }
        }catch(Exception ex){TaiChinhUi.loi(this,ex);return;}
        status.setText("Đang tải dữ liệu tài chính cho "+coSo.ten()+"...");
        TaiChinhUi.tai(this,()->new Data(ctrl.khoan(coSo),
            coSo==CoSo.TRUNG_TAM?ctrl.hocPhiTrungTam():List.of(),
            coSo==CoSo.MAM_NON?ctrl.mucPhi():List.of(),
            coSo==CoSo.TRUNG_TAM?ctrl.dangKy():List.of(),
            coSo==CoSo.MAM_NON?ctrl.bangTinh():List.of(),
            coSo==CoSo.MAM_NON?ctrl.lopMamNon():List.of(),
            ctrl.taiKhoan(coSo),ctrl.thongKe(coSo,from,to),
            ctrl.baoCaoThu(coSo,from,to)), result->{
                khoan=result.khoan();hocPhi=result.hocPhi();phi=result.phi();
                reportRows=result.reportRows();hienThiBaoCao();
                dangKy=result.dangKy();bangTinh=result.bangTinh();
                classes=result.lop();accounts=result.taiKhoan();
                tongHop=coSo==CoSo.TRUNG_TAM
                    ?TongHopThuPhi.trungTam(khoan,hocPhi)
                    :TongHopThuPhi.mamNon(khoan,bangTinh);
                capNhatBoLoc();hienThiTatCa();
                TongHopThuPhi.TongQuan total=TongHopThuPhi.tongQuan(tongHop);
                tongQuan[0].setText(tien(total.phaiThu()));
                tongQuan[1].setText(tien(total.daThu()));
                tongQuan[2].setText(tien(total.conNo()));
                tongQuan[3].setText(Integer.toString(total.chuaLap())+" khoản");
                TaiChinhDTO.ThongKe d=result.report();
                String[] values={tien(d.doanhThuThang()),tien(d.daThuToanBo()),
                    tien(d.tongPhaiThuHieuLuc()),tien(d.congNoHienTai()),
                    Long.toString(d.soKhoanConNo()),Long.toString(d.soKhoanDaTatToan()),
                    tien(d.tongGiamGia()),Long.toString(d.soHocVienConNo()),
                    Long.toString(d.soPhieuHopLe())};
                for(int i=0;i<thongKe.length;i++)thongKe[i].setText(values[i]);
                status.setText("Đã tải "+tongHop.size()+" dòng thu phí ("+khoan.size()
                    +" khoản đã lập) – "+coSo.ten());
            });
    }
    private void hienThiTatCa(){hienThiThuPhi();hienThiTienAn();}
    private static String timKiem(String s) {
        if(s==null)return "";
        return Normalizer.normalize(s,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ','d').replace('Đ','D')
            .toLowerCase(Locale.ROOT).trim();
    }
    private static String tenChon(JComboBox<String> combo) {return String.valueOf(combo.getSelectedItem());}
    private void capNhatBoLoc() {
        if(coSo!=CoSo.TRUNG_TAM)return;
        updatingFilters=true;
        try {
            TreeSet<String> mon=new TreeSet<>(),khoa=new TreeSet<>(),lop=new TreeSet<>();
            for(TaiChinhDTO.HocPhiTrungTam h:hocPhi) {
                mon.add(h.monHoc());khoa.add(h.khoaHoc());lop.add(h.lopHoc());
            }
            datLuaChon(cbMon,"Tất cả môn",mon);
            datLuaChon(cbKhoa,"Tất cả khóa",khoa);
            datLuaChon(cbLop,"Tất cả lớp",lop);
        }finally{updatingFilters=false;}
    }
    private static void datLuaChon(JComboBox<String> cb,String all,TreeSet<String> values) {
        Object selected=cb.getSelectedItem();cb.removeAllItems();cb.addItem(all);
        for(String value:values)cb.addItem(value);
        if(selected!=null&&values.contains(selected.toString()))cb.setSelectedItem(selected);
    }
    private void hienThiThuPhi() {
        if(updatingFilters)return;
        mThuPhi.setRowCount(0);
        List<Dong> visible=new ArrayList<>();
        String keyword=timKiem(search.getText());
        for(Dong d:tongHop) {
            String content=d.hocVien()+" "+d.noiDung()+" "+d.maHienThi()+" "+d.tenLoai()
                +" "+d.maHocVien()+" "+d.soDienThoai();
            if(!keyword.isEmpty()&&!timKiem(content).contains(keyword))continue;
            if(!"Tất cả trạng thái".equals(tenChon(cbTrangThai))
                &&!d.trangThai().equals(tenChon(cbTrangThai)))continue;
            if(!"Tất cả loại".equals(tenChon(cbLoai))&&!d.tenLoai().equals(tenChon(cbLoai)))continue;
            if(coSo==CoSo.TRUNG_TAM) {
                if(!"Tất cả môn".equals(tenChon(cbMon))&&!d.mon().equals(tenChon(cbMon)))continue;
                if(!"Tất cả khóa".equals(tenChon(cbKhoa))&&!d.khoa().equals(tenChon(cbKhoa)))continue;
                if(!"Tất cả lớp".equals(tenChon(cbLop))&&!d.lop().equals(tenChon(cbLop)))continue;
            }
            visible.add(d);
        }
        int totalPages=Math.max(1,(visible.size()+PAGE_SIZE-1)/PAGE_SIZE);
        currentPage=Math.max(0,Math.min(currentPage,totalPages-1));
        int start=currentPage*PAGE_SIZE,end=Math.min(visible.size(),start+PAGE_SIZE);
        dangHienThi=new ArrayList<>(visible.subList(start,end));
        for(Dong d:dangHienThi)
            mThuPhi.addRow(new Object[]{d.maHocVien()>0?"HV #"+d.maHocVien()+" / "+d.maHienThi():d.maHienThi(),
                d.hocVien(),d.noiDung(),
                tien(d.phaiThu()),d.chuaLap()?"—":tien(d.daThu()),
                d.chuaLap()?"—":tien(d.conNo()),
                d.han()==null?"—":d.han().toString(),d.trangThai(),
                "Chi tiết"});
        visibleCount.setText("Hiển thị "+visible.size()+" / "+tongHop.size()+" khoản thu");
        pageText.setText("Trang "+(currentPage+1)+"/"+totalPages);
        btnPrev.setEnabled(currentPage>0);btnNext.setEnabled(currentPage<totalPages-1);
        BigDecimal debt=BigDecimal.ZERO;
        for(Dong d:visible)if(!d.chuaLap()&&!TongHopThuPhi.DA_HUY.equals(d.trangThai()))debt=debt.add(d.conNo());
        summaryText.setText("Công nợ theo bộ lọc: "+tien(debt)+"  •  Nhấp đúp để xem chi tiết");
        capNhatNut();
    }
    private void hienThiTienAn() {
        if(coSo!=CoSo.MAM_NON)return;
        mTienAn.setRowCount(0);
        String keyword=timKiem(search.getText());
        for(TaiChinhDTO.MucPhi p:phi) {
            if(!p.loai().contains("Tiền ăn"))continue;
            if(!keyword.isEmpty()&&!timKiem(p.lop()).contains(keyword))continue;
            mTienAn.addRow(new Object[]{"Đơn giá #"+p.id(),p.lop(),p.apDung(),
                tien(p.donGia()),"Đơn giá/ngày"});
        }
        for(TaiChinhDTO.BangTinhChot b:bangTinh) {
            if(!keyword.isEmpty()&&!timKiem(b.hocVien()).contains(keyword))continue;
            mTienAn.addRow(new Object[]{"Bảng chốt #"+b.id(),b.hocVien(),b.ky(),
                tien(b.tienAn()),"Tổng tiền ăn"});
        }
    }
    private Dong dongDuocChon() {
        int view=tThuPhi.getSelectedRow();if(view<0)return null;
        int model=tThuPhi.convertRowIndexToModel(view);
        return model>=0&&model<dangHienThi.size()?dangHienThi.get(model):null;
    }
    private Dong batBuocChon() {
        Dong d=dongDuocChon();
        if(d==null)JOptionPane.showMessageDialog(this,"Hãy chọn một dòng trong bảng Quản lý thu phí.");
        return d;
    }
    private void xemChiTiet() {
        Dong d=batBuocChon();if(d==null)return;
        TaiChinhUi.tai(this,()->new ChiTietTaiChinh(
            d.maHocVien()>0?ctrl.lichSuThuHocVien(coSo,d.maHocVien()):List.of(),
            coSo==CoSo.TRUNG_TAM&&d.maHocVien()>0
                ?ctrl.uuDaiHocVien(d.maHocVien()):List.of()),data->{
        List<TaiChinhDTO.LichSuThu> history=data.history();
        JPanel money=TaiChinhUi.theTien(
            new String[]{"Phải thu","Đã đóng","Còn nợ"},
            new String[]{tien(d.phaiThu()),d.chuaLap()?"Chưa phát sinh":tien(d.daThu()),
                d.chuaLap()?"Chưa phát sinh":tien(d.conNo())},
            new Color[]{TaiChinhUi.XANH_DAM,new Color(27,91,168),new Color(184,48,39)});
        JPanel detail=TaiChinhUi.thongTinHaiCot(
            "Mã khoản / nguồn",d.maHienThi(),"Học viên",d.hocVien(),
            "Mã học viên",d.maHocVien()>0?String.valueOf(d.maHocVien()):"—",
            "Số điện thoại",d.soDienThoai()==null?"—":d.soDienThoai(),
            "Loại khoản",d.tenLoai(),"Trạng thái",d.trangThai(),
            "Học phí gốc",tien(d.goc()),"Giảm giá",tien(d.giam()),
            "Điều chỉnh",tien(d.dieuChinh()),
            "Hạn thanh toán",d.han()==null?"Chưa thiết lập":d.han().toString(),
            "Nội dung",d.noiDung());
        List<JComponent> sections=new ArrayList<>();
        sections.add(money);
        sections.add(TaiChinhUi.nhom("Thông tin khoản thu",detail));
        if(coSo==CoSo.TRUNG_TAM&&d.maHocVien()>0){
            List<Dong> courses=tongHop.stream().filter(item->item.maHocVien()==d.maHocVien()
                &&"HOC_PHI_KHOA".equals(item.loai())
                &&!TongHopThuPhi.DA_HUY.equals(item.trangThai())).toList();
            DefaultTableModel model=TaiChinhUi.model("Đăng ký","Khóa học","Ưu đãi đã chốt",
                "Gốc","Giảm","Phải thu","Đã đóng","Còn nợ","Trạng thái");
            BigDecimal goc=BigDecimal.ZERO,giam=BigDecimal.ZERO,due=BigDecimal.ZERO,
                paid=BigDecimal.ZERO,debt=BigDecimal.ZERO;
            for(Dong course:courses){
                String tenUuDai=data.uuDai().stream()
                    .filter(item->item.maDangKy()==course.maNguon())
                    .map(TaiChinhDTO.UuDaiDangKy::tenSnapshot)
                    .collect(java.util.stream.Collectors.joining(", "));
                if(tenUuDai.isBlank())tenUuDai=course.giam().signum()>0
                    ?"Chưa có snapshot chi tiết":"—";
                goc=goc.add(course.goc());giam=giam.add(course.giam());
                due=due.add(course.phaiThu());paid=paid.add(course.daThu());
                if(!course.chuaLap())debt=debt.add(course.conNo());
                model.addRow(new Object[]{"#"+course.maNguon(),course.khoa(),tenUuDai,
                    tien(course.goc()),tien(course.giam()),tien(course.phaiThu()),
                    course.chuaLap()?"Chưa phát sinh":tien(course.daThu()),
                    course.chuaLap()?"Chưa phát sinh":tien(course.conNo()),course.trangThai()});
            }
            JTable table=TaiChinhUi.bang(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int[] widths={85,170,190,110,100,110,110,110,130};
            for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i)
                .setPreferredWidth(widths[i]);
            JScrollPane scroll=TaiChinhUi.scroll(table);
            scroll.setPreferredSize(new Dimension(620,Math.min(210,75+courses.size()*28)));
            JPanel sum=TaiChinhUi.thongTinHaiCot("Tổng gốc",tien(goc),"Tổng giảm",tien(giam),
                "Tổng phải thu",tien(due),"Đã đóng",tien(paid),"Còn nợ đã lập",tien(debt));
            sections.add(TaiChinhUi.nhom("Các khóa của học viên",TaiChinhUi.doc(sum,scroll)));
        }
        if(coSo==CoSo.TRUNG_TAM&&"HOC_PHI_KHOA".equals(d.loai())){
            TaiChinhDTO.HocPhiTrungTam h=hocPhi.stream()
                .filter(item->item.maDangKy()==d.maNguon()).findFirst().orElse(null);
            String ngay=h==null||h.ngayThuGanNhat()==null?"Chưa có lần thu":h.ngayThuGanNhat().format(NGAY_GIO);
            String soTien=h==null||h.soTienLanThu()==null?"—":tien(h.soTienLanThu());
            String soPhieu=h==null||h.soPhieuGanNhat()==null?"—":h.soPhieuGanNhat();
            sections.add(TaiChinhUi.nhom("Lần thu gần nhất",TaiChinhUi.thongTinHaiCot(
                "Ngày thu",ngay,"Số tiền phân bổ",soTien,"Số phiếu",soPhieu)));
        }
        if(d.maHocVien()>0){
            DefaultTableModel payments=TaiChinhUi.model("Phiếu","Ngày thu","Khóa / khoản",
                "Số phân bổ","Trạng thái");
            for(TaiChinhDTO.LichSuThu item:history)payments.addRow(new Object[]{
                item.soPhieu(),item.ngayThu().format(NGAY_GIO),item.khoaHoc(),
                tien(item.soTien()),item.trangThai()});
            JTable table=TaiChinhUi.bang(payments);
            JScrollPane scroll=TaiChinhUi.scroll(table);
            scroll.setPreferredSize(new Dimension(620,Math.min(210,75+history.size()*27)));
            sections.add(TaiChinhUi.nhom("Lịch sử phân bổ và phiếu thu",scroll));
        }
        sections.add(TaiChinhUi.chuThich(d.chuaLap()
                ?"Khoản này là học phí dự kiến. Chưa phát sinh công nợ hoặc phiếu thu."
                :"Số tiền đã đóng được tổng hợp từ các phiếu thu hợp lệ; không chỉnh trực tiếp tại đây.",
                new Color(240,249,243)));
        JPanel content=TaiChinhUi.doc(sections.toArray(new JComponent[0]));
        TaiChinhUi.xemThongTin(this,"Chi tiết thu phí – "+d.hocVien(),
            "Xem đầy đủ thông tin học phí và trạng thái thanh toán",content);
        });
    }
    private void lapPhieuTuDong() {
        Dong d=batBuocChon();if(d==null)return;
        if(!d.duocThu()) {
            JOptionPane.showMessageDialog(this,d.chuaLap()
                ?"Khoản này chưa lập thu. Hãy chọn Tạo khoản thu trước khi lập phiếu."
                :"Dòng được chọn đã đóng đủ hoặc đã hủy, không còn số dư để thu.");return;
        }
        boQuaTaiPhieu=true;
        tabs.setSelectedComponent(phieu);
        phieu.loadVaLapPhieu(d.maKhoan());
    }
    private LocalDate hanTra(JTextField due) {
        String str=due.getText().trim();if(str.isEmpty())return null;
        try{return LocalDate.parse(str);}
        catch(Exception ex){throw new IllegalArgumentException("Hạn thanh toán phải là yyyy-MM-dd.");}
    }
    private void taoKhoan(Dong selected) {
        if(selected!=null&&!selected.chuaLap()) {
            JOptionPane.showMessageDialog(this,"Dòng này đã lập khoản thu hoặc không phát sinh học phí.");return;
        }
        if(coSo==CoSo.TRUNG_TAM&&dangKy.isEmpty()) {
            JOptionPane.showMessageDialog(this,"Không có đăng ký khóa học chưa lập khoản phải thu.");return;
        }
        if(coSo==CoSo.MAM_NON&&bangTinh.isEmpty()) {
            JOptionPane.showMessageDialog(this,"Chưa có bảng tính phí Mầm non đã CHỐT.");return;
        }
        JTextField due=new JTextField(LocalDate.now().plusDays(10).toString(),12);
        if(coSo==CoSo.TRUNG_TAM) {
            JComboBox<TaiChinhDTO.DangKyChuaThu> sources=new JComboBox<>(
                dangKy.toArray(new TaiChinhDTO.DangKyChuaThu[0]));
            if(selected!=null)for(TaiChinhDTO.DangKyChuaThu d:dangKy)
                if(d.id()==selected.maNguon()){sources.setSelectedItem(d);break;}
            JLabel preview=new JLabel("Chọn đăng ký để xem học phí");
            preview.setForeground(TaiChinhUi.XANH_DAM);
            preview.setFont(TaiChinhUi.font(Font.BOLD,13));
            Runnable refresh=()->{
                TaiChinhDTO.DangKyChuaThu item=(TaiChinhDTO.DangKyChuaThu)sources.getSelectedItem();
                if(item!=null)preview.setText("Học phí sau giảm: "+tien(item.sauGiam()));
            };
            sources.addActionListener(e->refresh.run());refresh.run();
            JPanel form=TaiChinhUi.doc(
                TaiChinhUi.chuThich("Chỉ chọn các đăng ký chưa có khoản phải thu. Hệ thống không tạo trùng khoản thu.",new Color(240,249,243)),
                TaiChinhUi.nhom("Thông tin đăng ký",TaiChinhUi.form(
                    "Đăng ký khóa học (*)",sources,"Học phí cần thu",preview)),
                TaiChinhUi.nhom("Thông tin khoản thu",TaiChinhUi.form(
                    "Hạn thanh toán",TaiChinhUi.oNgay(due,this))));
            if(!TaiChinhUi.hopThoaiForm(this,"Tạo khoản học phí khóa",
                "Chọn đăng ký chưa lập thu; dữ liệu sẽ ghi vào SQL Server",form,"Tạo khoản thu",()->{
                    if(sources.getSelectedItem()==null)throw new IllegalArgumentException("Chưa chọn đăng ký.");
                    hanTra(due);
                }))return;
            try {
                TaiChinhDTO.DangKyChuaThu d=(TaiChinhDTO.DangKyChuaThu)sources.getSelectedItem();
                if(d==null)return;
                LocalDate date=hanTra(due);
                TaiChinhUi.ghi(this,()->{ctrl.taoKhoanKhoa(d.id(),date);return null;},this::load);
            }catch(Exception ex){TaiChinhUi.loi(this,ex);}
        }else{
            JComboBox<TaiChinhDTO.BangTinhChot> sources=new JComboBox<>(
                bangTinh.toArray(new TaiChinhDTO.BangTinhChot[0]));
            JCheckBox fee=new JCheckBox("Học phí tháng",selected==null||"HOC_PHI_THANG".equals(selected.loai()));
            JCheckBox meal=new JCheckBox("Tiền ăn",selected==null||"TIEN_AN".equals(selected.loai()));
            if(selected!=null)for(TaiChinhDTO.BangTinhChot b:bangTinh)
                if(b.id()==selected.maNguon()){sources.setSelectedItem(b);break;}
            JPanel check=new JPanel(new GridLayout(1,2,8,0));check.setOpaque(false);
            check.add(fee);check.add(meal);
            JPanel form=TaiChinhUi.doc(
                TaiChinhUi.chuThich("Chỉ lập khoản thu từ bảng tính đã CHỐT. Chọn học phí, tiền ăn hoặc cả hai.",new Color(240,249,243)),
                TaiChinhUi.nhom("Bảng tính Mầm non",TaiChinhUi.form("Bảng tính đã CHỐT (*)",sources)),
                TaiChinhUi.nhom("Khoản cần lập",TaiChinhUi.form(
                    "Chọn loại khoản thu (*)",check,"Hạn thanh toán",TaiChinhUi.oNgay(due,this))));
            if(!TaiChinhUi.hopThoaiForm(this,"Tạo khoản thu Mầm non",
                "Chỉ tạo từ bảng tính phí đã chốt",form,"Tạo khoản thu",()->{
                    if(sources.getSelectedItem()==null||(!fee.isSelected()&&!meal.isSelected()))
                        throw new IllegalArgumentException("Chọn bảng tính và ít nhất một khoản thu.");
                    hanTra(due);
                }))return;
            try {
                TaiChinhDTO.BangTinhChot b=(TaiChinhDTO.BangTinhChot)sources.getSelectedItem();
                if(b==null||(!fee.isSelected()&&!meal.isSelected()))
                    throw new IllegalArgumentException("Phải chọn ít nhất một khoản.");
                LocalDate date=hanTra(due);
                TaiChinhUi.ghi(this,()->ctrl.taoKhoanMamNon(b.id(),
                    fee.isSelected(),meal.isSelected(),date),this::load);
            }catch(Exception ex){TaiChinhUi.loi(this,ex);}
        }
    }
    private void dieuChinhKhoan() {
        Dong row=batBuocChon();if(row==null)return;
        if(!row.duocDieuChinh()) {
            JOptionPane.showMessageDialog(this,"Chỉ điều chỉnh được khoản phải thu đang hiệu lực.");return;
        }
        TaiChinhDTO.KhoanThu selected=khoan.stream()
            .filter(k->k.maKhoan()==row.maKhoan()).findFirst().orElse(null);
        if(selected==null||!"HIEU_LUC".equals(selected.trangThai())) {
            JOptionPane.showMessageDialog(this,"Khoản thu không tồn tại hoặc đã hủy.");return;
        }
        if(accounts.isEmpty()){
            JOptionPane.showMessageDialog(this,"Chưa có tài khoản đang hoạt động tại cơ sở.");return;
        }
        JTextField amount=new JTextField(14),reason=new JTextField(24);
        JComboBox<TaiChinhDTO.LuaChon> actor=new JComboBox<>(
            accounts.toArray(new TaiChinhDTO.LuaChon[0]));
        JLabel after=new JLabel("Nhập số tiền điều chỉnh để xem kết quả.");
        after.setFont(TaiChinhUi.font(Font.BOLD,13));after.setForeground(TaiChinhUi.XANH_DAM);
        amount.getDocument().addDocumentListener(new DocumentListener(){
            private void update(){
                try{
                    BigDecimal next=selected.phaiThu().add(TaiChinhService.tienDieuChinh(amount.getText()));
                    after.setText("Phải thu mới: "+tien(next)+"   |   Còn nợ: "+tien(next.subtract(selected.daThu())));
                    after.setForeground(next.compareTo(selected.daThu())<0?new Color(190,42,42):TaiChinhUi.XANH_DAM);
                }catch(Exception ex){after.setText("Nhập + hoặc - và số tiền, ví dụ -100000");}
            }
            public void insertUpdate(DocumentEvent e){update();}
            public void removeUpdate(DocumentEvent e){update();}
            public void changedUpdate(DocumentEvent e){update();}
        });
        JPanel reasonRow=new JPanel(new BorderLayout(0,5));reasonRow.setOpaque(false);
        JLabel reasonTitle=new JLabel("Lý do điều chỉnh (*)");
        reasonTitle.setFont(TaiChinhUi.font(Font.BOLD,12));reasonTitle.setForeground(TaiChinhUi.CHU);
        TaiChinhUi.field(reason);
        reasonRow.add(reasonTitle,BorderLayout.NORTH);reasonRow.add(reason,BorderLayout.CENTER);
        JPanel edit=new JPanel(new BorderLayout(0,9));edit.setOpaque(false);
        edit.add(TaiChinhUi.formHaiCot("Tăng / giảm (+/-) (*)",amount,
            "Tài khoản thực hiện (*)",actor),BorderLayout.NORTH);
        edit.add(reasonRow,BorderLayout.CENTER);
        JPanel result=new JPanel(new BorderLayout());result.setBackground(new Color(235,249,239));
        result.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        result.add(after,BorderLayout.CENTER);
        edit.add(result,BorderLayout.SOUTH);
        JPanel form=TaiChinhUi.doc(
            TaiChinhUi.theTien(new String[]{"Phải thu hiện tại","Đã đóng","Còn nợ"},
                new String[]{tien(selected.phaiThu()),tien(selected.daThu()),tien(selected.conNo())},
                new Color[]{TaiChinhUi.XANH_DAM,new Color(27,91,168),new Color(184,48,39)}),
            TaiChinhUi.nhom("Điều chỉnh khoản #"+selected.maKhoan()+" – "+selected.hocVien(),edit),
            TaiChinhUi.chuThich("Không thể giảm tổng phải thu thấp hơn số tiền đã thanh toán. Lý do sẽ được lưu vào lịch sử.",
                new Color(255,248,232)));
        if(!TaiChinhUi.hopThoaiForm(this,"Điều chỉnh khoản phải thu",
            "Tăng hoặc giảm khoản thu; lịch sử điều chỉnh vẫn được lưu",form,"Tiếp tục",()->{
                BigDecimal delta=TaiChinhService.tienDieuChinh(amount.getText());
                if(actor.getSelectedItem()==null||reason.getText().isBlank())
                    throw new IllegalArgumentException("Chọn tài khoản và nhập lý do điều chỉnh.");
                if(selected.phaiThu().add(delta).compareTo(selected.daThu())<0)
                    throw new IllegalArgumentException("Phải thu sau điều chỉnh không được thấp hơn số đã đóng.");
            }))return;
        try {
            BigDecimal delta=TaiChinhService.tienDieuChinh(amount.getText());
            TaiChinhDTO.LuaChon user=(TaiChinhDTO.LuaChon)actor.getSelectedItem();
            if(user==null||reason.getText().isBlank())
                throw new IllegalArgumentException("Phải chọn tài khoản và nhập lý do điều chỉnh.");
            BigDecimal newDue=selected.phaiThu().add(delta);
            if(newDue.compareTo(selected.daThu())<0)
                throw new IllegalArgumentException("Phải thu sau điều chỉnh không thể thấp hơn số đã đóng.");
            if(!TaiChinhUi.xacNhan(this,
                "Xác nhận điều chỉnh khoản #"+selected.maKhoan()+" thành "+tien(newDue)+"?"))return;
            TaiChinhUi.ghi(this,()->{ctrl.dieuChinhKhoan(coSo,selected.maKhoan(),delta,
                reason.getText().trim(),user.id());return null;},this::load);
        }catch(Exception ex){TaiChinhUi.loi(this,ex);}
    }
    private void xemMucHocPhi() {
        if(coSo!=CoSo.MAM_NON)return;
        DefaultTableModel m=TaiChinhUi.model("Mã","Lớp","Áp dụng từ","Học phí/tháng");
        for(TaiChinhDTO.MucPhi p:phi)if(!p.loai().contains("Tiền ăn"))
            m.addRow(new Object[]{p.id(),p.lop(),p.apDung(),tien(p.donGia())});
        JTable table=TaiChinhUi.bang(m);
        JScrollPane pane=TaiChinhUi.scroll(table);pane.setPreferredSize(new Dimension(690,310));
        TaiChinhUi.xemThongTin(this,"Mức học phí Mầm non theo lớp",
            "Danh sách mức phí đã thiết lập",pane);
    }
    private void thietLapPhi(boolean isMeal) {
        if(coSo!=CoSo.MAM_NON)return;
        if(classes.isEmpty()||accounts.isEmpty()) {
            JOptionPane.showMessageDialog(this,"Chưa có lớp Mầm non hoặc tài khoản hợp lệ tại cơ sở.");return;
        }
        JComboBox<TaiChinhDTO.LuaChon> cboClass=new JComboBox<>(
            classes.toArray(new TaiChinhDTO.LuaChon[0]));
        JComboBox<TaiChinhDTO.LuaChon> cboActor=new JComboBox<>(
            accounts.toArray(new TaiChinhDTO.LuaChon[0]));
        JTextField month=new JTextField(YearMonth.now().toString()),amount=new JTextField(15);
        JPanel form=TaiChinhUi.doc(
            TaiChinhUi.chuThich("Mức phí áp dụng theo lớp từ tháng được chọn; các khoản đã thu trước đó không bị sửa.",
                new Color(240,249,243)),
            TaiChinhUi.nhom("Thiết lập mức phí",TaiChinhUi.formHaiCot(
                "Lớp Mầm non (*)",cboClass,"Áp dụng từ tháng",TaiChinhUi.oThang(month,this),
                isMeal?"Đơn giá/ngày (*)":"Học phí/tháng (*)",amount,
                "Tài khoản thiết lập (*)",cboActor)));
        if(!TaiChinhUi.hopThoaiForm(this,
            isMeal?"Thiết lập tiền ăn":"Thiết lập học phí lớp",
            "Mức phí áp dụng theo lớp và kỳ bắt đầu",form,"Lưu mức phí",()->{
                if(cboClass.getSelectedItem()==null||cboActor.getSelectedItem()==null)
                    throw new IllegalArgumentException("Chọn lớp và tài khoản.");
                TaiChinhService.thangBatDau(month.getText());
                TaiChinhService.tien(amount.getText());
            }))return;
        try {
            TaiChinhDTO.LuaChon cl=(TaiChinhDTO.LuaChon)cboClass.getSelectedItem();
            TaiChinhDTO.LuaChon actor=(TaiChinhDTO.LuaChon)cboActor.getSelectedItem();
            if(cl==null||actor==null)throw new IllegalArgumentException("Chọn lớp và tài khoản.");
            LocalDate date=TaiChinhService.thangBatDau(month.getText());
            BigDecimal money=TaiChinhService.tien(amount.getText());
            TaiChinhUi.ghi(this,()->{ctrl.thietLapPhi(cl.id(),isMeal,date,money,actor.id());
                return null;},this::load);
        }catch(Exception ex){TaiChinhUi.loi(this,ex);}
    }
}
