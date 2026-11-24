package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import vn.vinabacus.controller.DiemDanhTrungTamController;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.Buoi;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.HocVienBuoi;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.LichSuSua;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.TaiKhoan;
import vn.vinabacus.repository.DiemDanhTrungTamRepository.ThayDoi;
import vn.vinabacus.repository.HocBuTrungTamRepository.BuoiDich;
import vn.vinabacus.repository.HocBuTrungTamRepository.BuoiVang;
import vn.vinabacus.repository.HocBuTrungTamRepository.LichHocBu;
import vn.vinabacus.service.CaHocTrungTam;

/** Diem danh tung buoi va hoc bu tung hoc vien cua Trung tam. */
public final class DiemDanhTrungTamPanel extends JPanel {
    private static final Color DAM = new Color(7, 92, 53);
    private static final Color XANH = new Color(20, 138, 73);
    private static final Color VANG = new Color(255, 223, 0);
    private static final Color NHAT = new Color(234, 248, 237);
    private static final Color VIEN = new Color(210, 225, 214);
    private final DiemDanhTrungTamController controller = new DiemDanhTrungTamController();
    private final NgayKhaiGiangPicker ngay = new NgayKhaiGiangPicker(LocalDate.now());
    private final JComboBox<TaiKhoan> taiKhoan = new JComboBox<>();
    private final JComboBox<String> mon = new JComboBox<>();
    private final JComboBox<String> khoa = new JComboBox<>();
    private final JComboBox<String> lop = new JComboBox<>();
    private final JComboBox<String> giaoVien = new JComboBox<>();
    private final JComboBox<String> trangThaiBuoi = new JComboBox<>();
    private Buoi buoiDangChon;
    private final CardLayout cacManHinh = new CardLayout();
    private final JPanel manHinhDiemDanh = new JPanel(cacManHinh);
    private final JPanel cacThe = new JPanel();
    private final JLabel soBuoiHienThi = new JLabel("Danh sách buổi học");
    private final JLabel thongBaoDanhSach = new JLabel();
    private final JLabel lopDangDiemDanh = new JLabel("Chọn lớp để điểm danh");
    private final JLabel thongTinLop = new JLabel();
    private final JLabel thongTinCa = new JLabel();
    private final JLabel tongKet = new JLabel();
    private final JLabel chuaLuu = new JLabel();
    private final Map<Integer,List<HocVienBuoi>> hocVienTheoBuoi = new LinkedHashMap<>();
    private final List<Buoi> buoiDaLoc = new ArrayList<>();
    private final List<JPanel> theDangHien = new ArrayList<>();
    private final JTextField noiDung = new JTextField(35);
    private final JLabel thongBao = new JLabel("Chọn ngày và buổi để xem danh sách.");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "STT","Mã học viên","Họ và tên","Trạng thái điểm danh","Ghi chú"},0) {
        @Override public boolean isCellEditable(int row,int col) { return col==3 || col==4; }
        @Override public Class<?> getColumnClass(int col) {
            return col==0 ? Integer.class : String.class;
        }
    };
    private final JTable bang = new JTable(model);
    private final List<Buoi> tatCaBuoi = new ArrayList<>();
    private final List<HocVienBuoi> hocVien = new ArrayList<>();
    private final DefaultTableModel vangModel = new DefaultTableModel(new String[]{
            "Học viên","Khóa","Lớp","Ngày vắng","Ca","Trạng thái","Nội dung"},0) {
        @Override public boolean isCellEditable(int row,int col) { return false; }
    };
    private final JTable bangVang = new JTable(vangModel);
    private final JComboBox<BuoiDich> buoiDich = new JComboBox<>();
    private final DefaultTableModel buModel = new DefaultTableModel(new String[]{
            "Học viên","Lớp bù","Ngày","Ca","Trạng thái","Mã nguồn","Lý do hủy"},0) {
        @Override public boolean isCellEditable(int row,int col) { return false; }
    };
    private final JTable bangBu = new JTable(buModel);
    private List<BuoiVang> vang = List.of();
    private List<LichHocBu> lichBu = List.of();
    private boolean dangLoc;
    private boolean dangXemDanhSach;
    private int lichChuaCoBuoi;
    private int soThayDoiChuaLuu;

    public DiemDanhTrungTamPanel() {
        super(new BorderLayout());
        setBackground(GiaoDienMau.NEN);
        JPanel thanhTieuDe = new JPanel(new BorderLayout());
        thanhTieuDe.setBackground(DAM);
        thanhTieuDe.setPreferredSize(new Dimension(1,58));
        JLabel chuTieuDe = new JLabel("ĐIỂM DANH – TRUNG TÂM");
        chuTieuDe.setFont(GiaoDienMau.font(Font.BOLD,23));
        chuTieuDe.setForeground(Color.WHITE);
        chuTieuDe.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
        thanhTieuDe.add(chuTieuDe,BorderLayout.CENTER);
        JPanel vang = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(VANG);
                g.fillPolygon(new int[]{35,getWidth(),getWidth(),0},
                        new int[]{0,0,getHeight(),getHeight()},4);
            }
        };
        vang.setOpaque(false);
        vang.setPreferredSize(new Dimension(190,1));
        thanhTieuDe.add(vang,BorderLayout.EAST);
        add(thanhTieuDe,BorderLayout.NORTH);
        JTabbedPane tab = new JTabbedPane();
        tab.setFont(GiaoDienMau.font(Font.BOLD,13));
        tab.addTab("Điểm danh theo lớp",taoTabDiemDanh());
        tab.addTab("Học bù từng học viên",taoTabHocBu());
        tab.addChangeListener(e -> { if (tab.getSelectedIndex()==1) taiHocBu(); });
        add(tab,BorderLayout.CENTER);
        JPanel chan = new JPanel(new BorderLayout(12,0));
        chan.setBackground(Color.WHITE);
        chan.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,VIEN),
                BorderFactory.createEmptyBorder(5,14,5,14)));
        thongBao.setForeground(DAM);
        chan.add(thongBao,BorderLayout.CENTER);
        JPanel nguoiGhi = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        nguoiGhi.setOpaque(false);
        nguoiGhi.add(new JLabel("Tài khoản ghi:"));
        taiKhoan.setPreferredSize(new Dimension(185,27));
        nguoiGhi.add(taiKhoan);
        chan.add(nguoiGhi,BorderLayout.EAST);
        add(chan,BorderLayout.SOUTH);
        model.addTableModelListener(e -> capNhatThongKe());
        taiTaiKhoan();
        ngay.addPropertyChangeListener("ngay", e -> taiBuoi());
        taiBuoi();
    }

    private JPanel taoTabDiemDanh() {
        manHinhDiemDanh.setBackground(GiaoDienMau.NEN);
        manHinhDiemDanh.add(taoDanhSachBuoi(),"danhSach");
        manHinhDiemDanh.add(taoChiTietBuoi(),"chiTiet");
        cacManHinh.show(manHinhDiemDanh,"danhSach");
        return manHinhDiemDanh;
    }

    private JPanel taoDanhSachBuoi() {
        JPanel goc = new JPanel(new BorderLayout(0,12));
        goc.setBackground(GiaoDienMau.NEN);
        goc.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        JPanel tren = new JPanel(new BorderLayout(0,10));
        tren.setOpaque(false);
        JPanel loc = new JPanel(new GridLayout(2,3,10,8));
        loc.setBackground(Color.WHITE);
        loc.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        loc.add(truongLoc("Ngày học",ngay));
        loc.add(truongLoc("Môn",mon));
        loc.add(truongLoc("Khóa",khoa));
        loc.add(truongLoc("Giáo viên",giaoVien));
        loc.add(truongLoc("Trạng thái",trangThaiBuoi));
        loc.add(truongLoc("Lớp",lop));
        for (JComboBox<String> cb : List.of(mon,khoa,giaoVien,trangThaiBuoi,lop))
            cb.addActionListener(e -> locBuoi());
        JPanel boLoc = the("Lọc buổi học",loc);
        tren.add(boLoc,BorderLayout.CENTER);
        JButton tai = nutChinh("Tải danh sách");
        tai.addActionListener(e -> taiBuoi());
        JPanel nutTai = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        nutTai.setOpaque(false);nutTai.add(tai);
        tren.add(nutTai,BorderLayout.SOUTH);
        goc.add(tren,BorderLayout.NORTH);
        JPanel giua = new JPanel(new BorderLayout(0,10));
        giua.setOpaque(false);
        JPanel tieuDeDanhSach = new JPanel(new BorderLayout());
        tieuDeDanhSach.setBackground(NHAT);
        tieuDeDanhSach.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        soBuoiHienThi.setFont(GiaoDienMau.font(Font.BOLD,16));
        soBuoiHienThi.setForeground(DAM);
        tieuDeDanhSach.add(soBuoiHienThi,BorderLayout.WEST);
        JPanel chonDang = new JPanel(new FlowLayout(FlowLayout.RIGHT,7,0));
        chonDang.setOpaque(false);
        JButton the = nutChinh("Dạng thẻ");
        JButton danhSach = nut("Dạng danh sách");
        the.addActionListener(e -> { dangXemDanhSach=false; hienTheBuoi(); });
        danhSach.addActionListener(e -> { dangXemDanhSach=true; hienTheBuoi(); });
        chonDang.add(the);chonDang.add(danhSach);
        tieuDeDanhSach.add(chonDang,BorderLayout.EAST);
        giua.add(tieuDeDanhSach,BorderLayout.NORTH);
        cacThe.setLayout(new BoxLayout(cacThe,BoxLayout.Y_AXIS));
        cacThe.setBackground(Color.WHITE);
        JScrollPane cuon = new JScrollPane(cacThe);
        cuon.setBorder(null);
        cuon.getVerticalScrollBar().setUnitIncrement(18);
        giua.add(cuon,BorderLayout.CENTER);
        thongBaoDanhSach.setOpaque(true);
        thongBaoDanhSach.setBackground(new Color(229,239,255));
        thongBaoDanhSach.setForeground(new Color(24,76,140));
        thongBaoDanhSach.setBorder(BorderFactory.createEmptyBorder(9,12,9,12));
        giua.add(thongBaoDanhSach,BorderLayout.SOUTH);
        goc.add(giua,BorderLayout.CENTER);
        return goc;
    }

    private JPanel taoChiTietBuoi() {
        JPanel goc = new JPanel(new BorderLayout(0,10));
        goc.setBackground(GiaoDienMau.NEN);
        goc.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        JPanel tren = new JPanel(new BorderLayout(0,8));
        tren.setOpaque(false);
        JPanel thongTin = new JPanel(new BorderLayout(0,7));
        thongTin.setBackground(Color.WHITE);
        thongTin.setBorder(BorderFactory.createLineBorder(VIEN));
        lopDangDiemDanh.setFont(GiaoDienMau.font(Font.BOLD,18));
        lopDangDiemDanh.setForeground(Color.WHITE);
        lopDangDiemDanh.setBackground(DAM);
        lopDangDiemDanh.setOpaque(true);
        lopDangDiemDanh.setBorder(BorderFactory.createEmptyBorder(9,12,9,12));
        thongTin.add(lopDangDiemDanh,BorderLayout.NORTH);
        JPanel chiTiet = new JPanel(new GridLayout(2,1,0,5));
        chiTiet.setBackground(Color.WHITE);
        chiTiet.setBorder(BorderFactory.createEmptyBorder(7,12,7,12));
        chiTiet.add(thongTinLop);chiTiet.add(thongTinCa);
        thongTin.add(chiTiet,BorderLayout.CENTER);
        tren.add(thongTin,BorderLayout.NORTH);
        JPanel congCu = new JPanel(new BorderLayout(8,0));
        congCu.setOpaque(false);
        JPanel ngayVaNhanh = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        ngayVaNhanh.setOpaque(false);
        JButton coMat = nut("Có mặt nhanh");
        coMat.addActionListener(e -> coMatNhanh());
        ngayVaNhanh.add(coMat);
        ngayVaNhanh.add(new JLabel("Nội dung:"));
        noiDung.setPreferredSize(new Dimension(260,28));
        ngayVaNhanh.add(noiDung);
        JButton luuNoiDung = nut("Xác nhận nội dung");
        luuNoiDung.addActionListener(e -> luuNoiDung());
        ngayVaNhanh.add(luuNoiDung);
        congCu.add(ngayVaNhanh,BorderLayout.WEST);
        tren.add(congCu,BorderLayout.SOUTH);
        goc.add(tren,BorderLayout.NORTH);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setRowHeight(43);
        bang.setShowVerticalLines(false);
        bang.setGridColor(VIEN);
        bang.setSelectionBackground(NHAT);
        bang.setSelectionForeground(DAM);
        bang.getTableHeader().setFont(GiaoDienMau.font(Font.BOLD,12));
        bang.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table,Object value,
                    boolean selected,boolean focused,int row,int column) {
                super.getTableCellRendererComponent(table,value,selected,focused,row,column);
                setBackground(DAM);setForeground(Color.WHITE);setOpaque(true);
                setFont(GiaoDienMau.font(Font.BOLD,12));
                setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
                return this;
            }
        });
        bang.getTableHeader().setPreferredSize(new Dimension(1,34));
        int[] rong = {50,100,200,440,210};
        for (int i=0;i<rong.length;i++) bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        bang.getColumnModel().getColumn(3).setCellRenderer(new LuaChonDiemDanh());
        bang.getColumnModel().getColumn(3).setCellEditor(new SuaLuaChonDiemDanh());
        bang.putClientProperty("terminateEditOnFocusLost",Boolean.TRUE);
        JPopupMenu boChon = new JPopupMenu();
        JMenuItem xoaChon = new JMenuItem("Bỏ chọn trạng thái học viên này");
        xoaChon.addActionListener(e -> {
            if(bang.isEditing()) bang.getCellEditor().stopCellEditing();
            int i=bang.getSelectedRow();
            if (i>=0) model.setValueAt("Chưa điểm danh",i,3);
        });
        boChon.add(xoaChon);
        bang.setComponentPopupMenu(boChon);
        bang.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { chonDongPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { chonDongPopup(e); }
            private void chonDongPopup(MouseEvent e) {
                if(e.isPopupTrigger()) {
                    int i=bang.rowAtPoint(e.getPoint());
                    if(i>=0) bang.setRowSelectionInterval(i,i);
                }
            }
        });
        goc.add(the("Danh sách học viên và điểm danh",new JScrollPane(bang)),BorderLayout.CENTER);
        JPanel duoi = new JPanel(new BorderLayout(0,8));
        duoi.setOpaque(false);
        JPanel thongKe = new JPanel(new BorderLayout());
        thongKe.setBackground(NHAT);
        thongKe.setBorder(BorderFactory.createEmptyBorder(9,12,9,12));
        JPanel soLieu = new JPanel(new GridLayout(2,1));
        soLieu.setOpaque(false);soLieu.add(tongKet);soLieu.add(chuaLuu);
        thongKe.add(soLieu,BorderLayout.CENTER);
        JButton lichSu = nut("Lịch sử sửa");
        lichSu.addActionListener(e -> hienThiLichSu());
        thongKe.add(lichSu,BorderLayout.EAST);
        duoi.add(thongKe,BorderLayout.NORTH);
        JPanel dieuHuong = new JPanel(new BorderLayout());
        dieuHuong.setOpaque(false);
        JButton quayLai = nut("Quay lại danh sách lớp");
        quayLai.addActionListener(e -> {
            if(soThayDoiChuaLuu>0 && JOptionPane.showConfirmDialog(this,
                    "Có "+soThayDoiChuaLuu+" học viên thay đổi chưa lưu. Quay lại và bỏ các thay đổi này?",
                    "Điểm danh chưa lưu",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            cacManHinh.show(manHinhDiemDanh,"danhSach");
        });
        dieuHuong.add(quayLai,BorderLayout.WEST);
        JButton luu = nutChinh("Lưu điểm danh");
        luu.addActionListener(e -> luuDiemDanh());
        dieuHuong.add(luu,BorderLayout.EAST);
        duoi.add(dieuHuong,BorderLayout.SOUTH);
        goc.add(duoi,BorderLayout.SOUTH);
        return goc;
    }

    private static JPanel truongLoc(String ten,Component nhap) {
        JPanel p=new JPanel(new BorderLayout(0,3));
        p.setBackground(Color.WHITE);
        JLabel nhan=new JLabel(ten);
        nhan.setFont(GiaoDienMau.font(Font.BOLD,12));
        nhan.setForeground(DAM);
        p.add(nhan,BorderLayout.NORTH);
        if (nhap instanceof JComboBox<?> cb) cb.setPreferredSize(new Dimension(180,29));
        p.add(nhap,BorderLayout.CENTER);
        return p;
    }
    private static JPanel the(String tieuDe,Component noiDung) {
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createLineBorder(VIEN));
        JLabel dau=new JLabel(tieuDe);
        dau.setFont(GiaoDienMau.font(Font.BOLD,14));
        dau.setForeground(DAM);
        dau.setBackground(NHAT);
        dau.setOpaque(true);
        dau.setBorder(BorderFactory.createEmptyBorder(7,12,7,12));
        p.add(dau,BorderLayout.NORTH);
        p.add(noiDung,BorderLayout.CENTER);
        return p;
    }
    private static JButton nutChinh(String ten) {
        return new NutMau(ten,NutMau.Kieu.CHINH);
    }
    private void hienTheBuoi() {
        cacThe.removeAll();theDangHien.clear();
        soBuoiHienThi.setText("Danh sách buổi học ("+buoiDaLoc.size()+")");
        if (buoiDaLoc.isEmpty()) {
            String s=tatCaBuoi.isEmpty()
                    ? lichChuaCoBuoi>0
                        ? "Có lịch tuần nhưng chưa có buổi học thực tế. Hãy tạo buổi trong Lịch học trước khi điểm danh."
                        : "Ngày đã chọn không có buổi học cần điểm danh."
                    : "Không có buổi học phù hợp bộ lọc.";
            JLabel rong=new JLabel(s,JLabel.CENTER);
            rong.setFont(GiaoDienMau.font(Font.PLAIN,14));
            rong.setForeground(DAM);
            rong.setBorder(BorderFactory.createEmptyBorder(30,10,30,10));
            cacThe.add(rong);
            thongBaoDanhSach.setText(s);
        } else {
            int soCot=dangXemDanhSach?1:(cacThe.getWidth()>1250?3:2);
            for (int i=0;i<buoiDaLoc.size();i+=soCot) {
                JPanel hang=new JPanel(new GridLayout(1,soCot,12,0));
                hang.setBackground(Color.WHITE);
                hang.setAlignmentX(Component.LEFT_ALIGNMENT);
                hang.setMaximumSize(new Dimension(Integer.MAX_VALUE,170));
                hang.setPreferredSize(new Dimension(800,160));
                for(int j=0;j<soCot;j++) {
                    if(i+j<buoiDaLoc.size()) hang.add(taoTheBuoi(buoiDaLoc.get(i+j)));
                    else { JPanel rong=new JPanel();rong.setOpaque(false);hang.add(rong); }
                }
                cacThe.add(hang);
                cacThe.add(javax.swing.Box.createVerticalStrut(12));
            }
            thongBaoDanhSach.setText("Nhấn đúp vào thẻ để mở điểm danh đúng buổi học; thẻ phân biệt theo mã buổi và ca.");
        }
        cacThe.revalidate();cacThe.repaint();
    }
    private JPanel taoTheBuoi(Buoi b) {
        JPanel card=new JPanel(new BorderLayout(0,7));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN,2,true),
                BorderFactory.createEmptyBorder(9,12,9,12)));
        card.putClientProperty("maBuoi",b.maBuoi());
        JLabel ten=new JLabel("#"+b.maLop()+" · "+b.tenLop()+"  (buổi #"+b.maBuoi()+")");
        ten.setFont(GiaoDienMau.font(Font.BOLD,15));ten.setForeground(DAM);
        card.add(ten,BorderLayout.NORTH);
        JPanel dong=new JPanel(new GridLayout(4,1,0,3));dong.setOpaque(false);
        dong.add(new JLabel("Môn: "+hienThi(b.tenMon())+" · Khóa: "+hienThi(b.tenKhoa())));
        dong.add(new JLabel("Ngày/ca: "+NgayKhaiGiangPicker.hienThiNgay(b.ngay())+" · "+caHoc(b)));
        dong.add(new JLabel("Phòng: "+hienThi(b.phong())));
        dong.add(new JLabel("Giáo viên: "+hienThi(b.giaoVien())));
        card.add(dong,BorderLayout.CENTER);
        List<HocVienBuoi> ds=hocVienTheoBuoi.getOrDefault(b.maBuoi(),List.of());
        long daDiem=ds.stream().filter(h -> h.trangThai()!=null
                && !"CHUA_DIEM_DANH".equals(h.trangThai())).count();
        String tt=daDiem==0?"Chưa điểm danh":daDiem==ds.size()?"Đã điểm danh":"Đang điểm danh";
        JLabel chan=new JLabel(ds.size()+" học viên  ·  "+tt+"  ·  "+nhanBuoi(b.trangThai()));
        chan.setFont(GiaoDienMau.font(Font.BOLD,12));chan.setForeground(XANH);
        card.add(chan,BorderLayout.SOUTH);
        MouseAdapter chon=new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                buoiDangChon=b;
                capNhatTheDuocChon();
                if(e.getClickCount()>=2) moChiTietBuoi(b);
            }
        };
        ganClick(card,chon);
        theDangHien.add(card);
        return card;
    }
    private static void ganClick(Component c,MouseAdapter chon) {
        c.addMouseListener(chon);
        if(c instanceof JPanel p)
            for(Component con:p.getComponents()) ganClick(con,chon);
    }
    private void capNhatTheDuocChon() {
        for(JPanel card:theDangHien) {
            boolean chon=buoiDangChon!=null && Objects.equals(
                    card.getClientProperty("maBuoi"),buoiDangChon.maBuoi());
            card.setBackground(chon?NHAT:Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(chon?XANH:VIEN,2,true),
                    BorderFactory.createEmptyBorder(9,12,9,12)));
        }
    }
    private void moChiTietBuoi(Buoi b) {
        buoiDangChon=b;
        lopDangDiemDanh.setText("Điểm danh lớp: #"+b.maLop()+" · "+b.tenLop()+" · Buổi #"+b.maBuoi());
        thongTinLop.setText("Môn: "+hienThi(b.tenMon())+"    Khóa: "+hienThi(b.tenKhoa())
                +"    Giáo viên: "+hienThi(b.giaoVien())+"    Phòng: "+hienThi(b.phong()));
        thongTinCa.setText("Ngày học: "+NgayKhaiGiangPicker.hienThiNgay(b.ngay())+"    Ca/giờ: "+caHoc(b)
                +"    Học viên: "+hocVienTheoBuoi.getOrDefault(b.maBuoi(),List.of()).size());
        taiHocVien();
        cacManHinh.show(manHinhDiemDanh,"chiTiet");
    }
    private static String hienThi(String s) {
        return s==null || s.isBlank()?"Chưa phân công":s;
    }
    private static String nhanBuoi(String ma) {
        return switch(Objects.toString(ma,"")) {
            case "DA_DIEN_RA" -> "Đã diễn ra";
            case "SAP_DIEN_RA" -> "Sắp diễn ra";
            case "DANG_DIEN_RA" -> "Đang diễn ra";
            case "HOAN_THANH" -> "Hoàn thành";
            default -> hienThi(ma);
        };
    }
    private static String caHoc(Buoi b) {
        var ca=CaHocTrungTam.layTheoSoCa(b.soCa());
        return ca==null?"Ca "+b.soCa():"Ca "+b.soCa()+" · "+ca.batDau()+"–"+ca.ketThuc();
    }
    private void coMatNhanh() {
        if(buoiDangChon==null) return;
        if(bang.isEditing()) bang.getCellEditor().stopCellEditing();
        int so=0;
        for(int i=0;i<model.getRowCount();i++)
            if("Chưa điểm danh".equals(model.getValueAt(i,3))) {
                model.setValueAt("Có mặt",i,3);so++;
            }
        thongBao.setText(so==0?"Không có học viên chưa điểm danh để đánh dấu Có mặt."
                :"Đã chọn Có mặt cho "+so+" học viên chưa điểm danh. Nhấn Lưu điểm danh để ghi.");
    }
    private void capNhatThongKe() {
        if(tongKet==null || chuaLuu==null) return;
        int co=0,phep=0,khong=0,chua=0,dirty=0;
        for(int i=0;i<model.getRowCount();i++) {
            String tt=Objects.toString(model.getValueAt(i,3),"Chưa điểm danh");
            switch(tt) {
                case "Có mặt" -> co++;
                case "Vắng có phép" -> phep++;
                case "Vắng không phép" -> khong++;
                default -> chua++;
            }
            if(i<hocVien.size()) {
                HocVienBuoi h=hocVien.get(i);
                if(!Objects.equals(tt,nhan(h.trangThai()))
                        || !Objects.equals(Objects.toString(model.getValueAt(i,4),"").trim(),
                                Objects.toString(h.ghiChu(),""))) dirty++;
            }
        }
        tongKet.setText("Tổng: "+model.getRowCount()+"   |   Có mặt: "+co
                +"   |   Vắng có phép: "+phep+"   |   Vắng không phép: "+khong
                +"   |   Chưa điểm danh: "+chua);
        soThayDoiChuaLuu=dirty;
        chuaLuu.setText(dirty==0?"Đã đồng bộ dữ liệu đang hiển thị."
                :"Có "+dirty+" học viên thay đổi chưa lưu.");
        chuaLuu.setForeground(dirty==0?DAM:new Color(159,90,0));
    }

    private JPanel taoTabHocBu() {
        JPanel goc=new JPanel(new BorderLayout(8,8));goc.setOpaque(false);
        JPanel tren=hang();
        JButton tai=nut("Tải danh sách vắng");tai.addActionListener(e -> taiHocBu());
        tren.add(tai);
        tren.add(new JLabel("Chỉ chọn buổi vắng đã điểm danh; nội dung phải trùng."));
        goc.add(tren,BorderLayout.NORTH);
        JPanel giua=new JPanel(new java.awt.GridLayout(2,1,0,8));
        giua.setOpaque(false);
        JPanel pVang=new JPanel(new BorderLayout());
        pVang.setBorder(BorderFactory.createTitledBorder("Buổi vắng"));
        bangVang.setRowHeight(26);
        bangVang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bangVang.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) taiBuoiDich();
        });
        pVang.add(new JScrollPane(bangVang),BorderLayout.CENTER);
        giua.add(pVang);
        JPanel pBu=new JPanel(new BorderLayout());
        pBu.setBorder(BorderFactory.createTitledBorder("Lịch học bù đã lưu"));
        bangBu.setRowHeight(26);
        bangBu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pBu.add(new JScrollPane(bangBu),BorderLayout.CENTER);
        giua.add(pBu);
        goc.add(giua,BorderLayout.CENTER);
        JPanel duoi=hang();
        duoi.add(new JLabel("Buổi bù phù hợp:"));
        buoiDich.setPreferredSize(new Dimension(540,29));
        duoi.add(buoiDich);
        JButton xep=nut("Xếp học bù");xep.addActionListener(e -> xepHocBu());
        duoi.add(xep);
        JButton huy=nut("Hủy lịch bù đã chọn");huy.addActionListener(e -> huyHocBu());
        duoi.add(huy);
        goc.add(duoi,BorderLayout.SOUTH);
        return goc;
    }

    private void taiTaiKhoan() {
        try {
            taiKhoan.removeAllItems();
            for (TaiKhoan t:controller.taiKhoan()) taiKhoan.addItem(t);
            if (taiKhoan.getItemCount()==0)
                thongBao.setText("Không có tài khoản Trung tâm đang hoạt động để ghi điểm danh.");
        } catch (SQLException e) { loi(e); }
    }
    private void taiBuoi() {
        try {
            LocalDate d=ngay.layNgay();
            if (d==null) { thongBao.setText("Hãy chọn ngày học."); return; }
            tatCaBuoi.clear();tatCaBuoi.addAll(controller.buoi(d));
            hocVienTheoBuoi.clear();
            for (Buoi b:tatCaBuoi)
                hocVienTheoBuoi.put(b.maBuoi(),controller.hocVien(b.maBuoi()));
            lichChuaCoBuoi=tatCaBuoi.isEmpty()?controller.lichChuaCoBuoi(d):0;
            buoiDangChon=null;
            dangLoc=true;
            datLoc(mon,tatCaBuoi.stream().map(Buoi::tenMon).distinct().toList());
            datLoc(khoa,tatCaBuoi.stream().map(Buoi::tenKhoa).distinct().toList());
            datLoc(lop,tatCaBuoi.stream().map(Buoi::tenLop).distinct().toList());
            datLoc(giaoVien,tatCaBuoi.stream().map(Buoi::giaoVien)
                    .filter(Objects::nonNull).distinct().toList());
            datLoc(trangThaiBuoi,tatCaBuoi.stream().map(b -> nhanBuoi(b.trangThai())).distinct().toList());
            dangLoc=false;locBuoi();
            thongBao.setText(tatCaBuoi.isEmpty()
                    ? lichChuaCoBuoi>0 ? "Có " + lichChuaCoBuoi + " lịch tuần chưa có buổi thực tế; tạo buổi trong Lịch học trước khi điểm danh."
                        : d.equals(LocalDate.now()) ? "Hôm nay không có buổi học cần điểm danh."
                        : "Ngày " + d + " không có buổi học cần điểm danh."
                    : "Ngày "+d+": "+tatCaBuoi.size()+" buổi.");
            thongBaoDanhSach.setText(thongBao.getText());
            cacManHinh.show(manHinhDiemDanh,"danhSach");
        } catch (SQLException e) { loi(e); }
    }
    private void locBuoi() {
        if (dangLoc) return;
        buoiDaLoc.clear();
        for (Buoi b:tatCaBuoi) {
            if (khop(mon,b.tenMon()) && khop(khoa,b.tenKhoa())
                    && khop(lop,b.tenLop()) && khop(giaoVien,b.giaoVien())
                    && khop(trangThaiBuoi,nhanBuoi(b.trangThai()))) buoiDaLoc.add(b);
        }
        hienTheBuoi();
    }
    private void taiHocVien() {
        Buoi b=buoiDangChon;
        model.setRowCount(0);hocVien.clear();
        noiDung.setText(b==null || b.noiDung()==null ? "" : b.noiDung());
        if (b==null) return;
        try {
            hocVien.addAll(controller.hocVien(b.maBuoi()));
            hocVienTheoBuoi.put(b.maBuoi(),List.copyOf(hocVien));
            for (int i=0;i<hocVien.size();i++) {
                HocVienBuoi h=hocVien.get(i);
                model.addRow(new Object[]{i+1,"HV"+h.maHocVien(),
                    h.hoTen()+(h.hocBu()?" · Học bù":""),
                    nhan(h.trangThai()),h.ghiChu()});
            }
            capNhatThongKe();
            thongBao.setText(b.tenLop()+": "+hocVien.size()+" học viên/lượt đã xếp.");
        } catch (SQLException e) { loi(e); }
    }
    private void luuDiemDanh() {
        Buoi b=buoiDangChon;
        TaiKhoan t=(TaiKhoan)taiKhoan.getSelectedItem();
        if (b==null || t==null) {
            thongBao.setText("Chọn buổi và tài khoản ghi điểm danh.");return;
        }
        if (bang.isEditing()) bang.getCellEditor().stopCellEditing();
        List<ThayDoi> thayDoi=new ArrayList<>();
        boolean suaDaLuu=false;
        for (int i=0;i<hocVien.size();i++) {
            HocVienBuoi h=hocVien.get(i);
            String moi=maTrangThai((String)model.getValueAt(i,3));
            String note=Objects.toString(model.getValueAt(i,4),"").trim();
            String cu=h.trangThai()==null?"CHUA_DIEM_DANH":h.trangThai();
            if ("CHUA_DIEM_DANH".equals(moi) && h.trangThai()==null && !note.isEmpty()) {
                thongBao.setText("Hãy chọn kết quả điểm danh trước khi ghi chú cho " + h.hoTen());
                return;
            }
            if (!Objects.equals(moi,cu)
                    || !Objects.equals(note,Objects.toString(h.ghiChu(),""))) {
                if (h.trangThai()!=null) suaDaLuu=true;
                thayDoi.add(new ThayDoi(h,moi,note,null));
            }
        }
        if (thayDoi.isEmpty()) { thongBao.setText("Không có thay đổi điểm danh để lưu."); return; }
        String lyDo=null;
        if (suaDaLuu) {
            lyDo=JOptionPane.showInputDialog(this,"Lý do sửa những điểm danh đã lưu:");
            if (lyDo==null) return;
        }
        final String ghiLyDo=lyDo;
        thayDoi=thayDoi.stream().map(d -> new ThayDoi(d.hocVien(),d.trangThai(),
                d.ghiChu(),d.hocVien().trangThai()==null?null:ghiLyDo)).toList();
        try {
            int so=thayDoi.size();
            controller.luuNhieu(b.maBuoi(),thayDoi,t.ma());
            taiHocVien();hienTheBuoi();
            thongBao.setText("Đã lưu " + so + " thay đổi điểm danh trong một giao dịch.");
        } catch (SQLException | IllegalArgumentException e) { loi(e); }
    }
    private void luuNoiDung() {
        Buoi b=buoiDangChon;
        if (b==null) return;
        try {
            controller.noiDung(b.maBuoi(),noiDung.getText());
            buoiDangChon=new Buoi(b.maBuoi(),b.maLop(),b.maKhoa(),b.maMon(),
                    b.tenLop(),b.tenKhoa(),b.tenMon(),b.giaoVien(),b.phong(),
                    b.ngay(),b.soCa(),b.trangThai(),noiDung.getText().trim());
            for (int i=0;i<tatCaBuoi.size();i++)
                if (tatCaBuoi.get(i).maBuoi()==b.maBuoi()) {
                    tatCaBuoi.set(i,buoiDangChon);break;
                }
            thongBao.setText("Đã xác nhận nội dung buổi học.");
        } catch (SQLException | IllegalArgumentException e) { loi(e); }
    }
    private void hienThiLichSu() {
        int i=bang.getSelectedRow();
        if (i<0 || i>=hocVien.size() || hocVien.get(i).maSapXep()==null) {
            thongBao.setText("Chọn học viên đã có lượt học để xem lịch sử.");return;
        }
        try {
            List<LichSuSua> ds=controller.lichSuSua(hocVien.get(i).maSapXep());
            DefaultTableModel m=new DefaultTableModel(new String[]{
                    "Trước","Sau","Lý do","Tài khoản","Thời điểm"},0) {
                @Override public boolean isCellEditable(int row,int col) { return false; }
            };
            for (LichSuSua s:ds) m.addRow(new Object[]{nhan(s.truoc()),nhan(s.sau()),
                    s.lyDo(),s.taiKhoan(),s.ngaySua()});
            if (ds.isEmpty()) {
                JOptionPane.showMessageDialog(this,"Chưa có lần sửa điểm danh nào được lưu.");
                return;
            }
            JTable t=new JTable(m);t.setRowHeight(26);
            JScrollPane cuon=new JScrollPane(t);
            cuon.setPreferredSize(new Dimension(720,Math.min(340,70+ds.size()*27)));
            JOptionPane.showMessageDialog(this,cuon,"Lịch sử sửa điểm danh",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) { loi(e); }
    }
    private void taiHocBu() {
        try {
            vang=controller.buoiVang();lichBu=controller.lichHocBu();
            vangModel.setRowCount(0);buModel.setRowCount(0);
            for (BuoiVang v:vang) vangModel.addRow(new Object[]{v.hocVien(),v.khoa(),
                    v.lop(),v.ngay(),v.soCa(),nhan(v.trangThai()),v.noiDung()});
            for (LichHocBu b:lichBu) buModel.addRow(new Object[]{b.hocVien(),b.lop(),
                    b.ngay(),b.soCa(),b.trangThai(),b.maNguon(),b.lyDoHuy()});
            buoiDich.removeAllItems();
            thongBao.setText(vang.size()+" buổi vắng; "+lichBu.size()+" lịch học bù.");
        } catch (SQLException e) { loi(e); }
    }
    private void taiBuoiDich() {
        int i=bangVang.getSelectedRow();buoiDich.removeAllItems();
        if (i<0 || i>=vang.size()) return;
        try {
            for (BuoiDich b:controller.buoiDich(vang.get(i).maSapXep()))
                buoiDich.addItem(b);
            if (buoiDich.getItemCount()==0)
                thongBao.setText("Chưa có buổi khác lớp cùng môn/nội dung, còn chỗ và đủ phòng.");
        } catch (SQLException e) { loi(e); }
    }
    private void xepHocBu() {
        int i=bangVang.getSelectedRow();
        BuoiDich dich=(BuoiDich)buoiDich.getSelectedItem();
        TaiKhoan tk=(TaiKhoan)taiKhoan.getSelectedItem();
        if (i<0 || i>=vang.size() || dich==null || tk==null) {
            thongBao.setText("Chọn buổi vắng, buổi bù và tài khoản ghi.");return;
        }
        BuoiVang nguon=vang.get(i);
        if (JOptionPane.showConfirmDialog(this,"Xếp "+nguon.hocVien()+" học bù tại "
                +dich+"?","Xác nhận học bù",JOptionPane.YES_NO_OPTION)
                !=JOptionPane.YES_OPTION) return;
        try {
            controller.xepHocBu(nguon.maSapXep(),dich.maBuoi(),tk.ma());
            taiHocBu();thongBao.setText("Đã xếp học bù; lớp chính giữ nguyên.");
        } catch (SQLException e) { loi(e); }
    }
    private void huyHocBu() {
        int i=bangBu.getSelectedRow();
        TaiKhoan tk=(TaiKhoan)taiKhoan.getSelectedItem();
        if (i<0 || i>=lichBu.size() || tk==null) {
            thongBao.setText("Chọn lịch bù và tài khoản ghi.");return;
        }
        String lyDo=JOptionPane.showInputDialog(this,"Lý do hủy lịch học bù:");
        if (lyDo==null) return;
        try {
            controller.huyHocBu(lichBu.get(i).maSapXep(),tk.ma(),lyDo);
            taiHocBu();thongBao.setText("Đã hủy lượt bù; lịch sử được giữ lại.");
        } catch (SQLException | IllegalArgumentException e) { loi(e); }
    }
    private static JPanel hang() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));p.setOpaque(false);return p;
    }
    private static JPanel nhomRadio(String giaTri, Consumer<String> khiChon) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setBackground("Có mặt".equals(giaTri) ? new Color(230, 247, 232)
                : giaTri != null && giaTri.startsWith("Vắng") ? new Color(255, 245, 229)
                : Color.WHITE);
        ButtonGroup nhom = new ButtonGroup();
        for (String ten : new String[]{"Có mặt", "Vắng có phép", "Vắng không phép"}) {
            JRadioButton o = new JRadioButton(ten);
            o.setOpaque(false);
            o.setSelected(ten.equals(giaTri));
            o.addActionListener(e -> khiChon.accept(ten));
            nhom.add(o); p.add(o);
        }
        return p;
    }
    private static final class LuaChonDiemDanh implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table,Object value,
                boolean selected,boolean focused,int row,int column) {
            return nhomRadio(Objects.toString(value,"Chưa điểm danh"),v -> {});
        }
    }
    private static final class SuaLuaChonDiemDanh extends AbstractCellEditor
            implements TableCellEditor {
        private String giaTri;
        @Override public Component getTableCellEditorComponent(JTable table,Object value,
                boolean selected,int row,int column) {
            giaTri=Objects.toString(value,"Chưa điểm danh");
            return nhomRadio(giaTri,v -> { giaTri=v; fireEditingStopped(); });
        }
        @Override public Object getCellEditorValue() { return giaTri; }
    }
    private static JButton nut(String ten) {
        return new NutMau(ten,NutMau.Kieu.PHU);
    }
    private static void datLoc(JComboBox<String> cb,List<String> ds) {
        cb.removeAllItems();cb.addItem("Tất cả");
        for (String s:ds) if (s!=null) cb.addItem(s);
    }
    private static boolean khop(JComboBox<String> cb,String s) {
        return cb.getSelectedIndex()<=0 || Objects.equals(cb.getSelectedItem(),s);
    }
    private static String nhan(String trangThai) {
        if (trangThai==null) return "Chưa điểm danh";
        return switch (trangThai) {
            case "CO_MAT" -> "Có mặt";
            case "VANG_CO_PHEP" -> "Vắng có phép";
            case "VANG_KHONG_PHEP" -> "Vắng không phép";
            default -> "Chưa điểm danh";
        };
    }
    private static String nhanLoai(String loai) {
        return switch (loai) {
            case "HOC_BU" -> "Học bù";
            case "DOI_BUOI" -> "Đổi buổi";
            case "HOC_BO_SUNG" -> "Học bổ sung";
            default -> "Chính thức";
        };
    }
    private static String maTrangThai(String nhan) {
        return switch (nhan) {
            case "Có mặt" -> "CO_MAT";
            case "Vắng có phép" -> "VANG_CO_PHEP";
            case "Vắng không phép" -> "VANG_KHONG_PHEP";
            default -> "CHUA_DIEM_DANH";
        };
    }
    private void loi(Exception e) {
        thongBao.setText("Lỗi: "+e.getMessage());
        JOptionPane.showMessageDialog(this,e.getMessage(),"Không thực hiện được",
                JOptionPane.WARNING_MESSAGE);
    }
}
