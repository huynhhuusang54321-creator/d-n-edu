package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import vn.vinabacus.controller.TuyenSinhController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhachTiemNang;

/**
 * Giao diện tuyển sinh: 6 tab theo trạng thái, bố cục và bảng màu đồng bộ với HocVienPanel.
 * Chỉ dựng giao diện, thu thập dữ liệu nhập và hiển thị những gì Controller gửi xuống.
 */
public final class TuyenSinhPanel extends JPanel implements TuyenSinhController.View {

    private static final Color NEN = new Color(0xFFFDF0);
    private static final Color XANH_TIEU_DE = new Color(0xC5E1A5);
    private static final Color XANH_NUT = new Color(0xA5D66A);
    private static final Color XANH_DAM = new Color(0x8BC34A);
    private static final Color VANG_NUT = new Color(0xFFE57F);
    private static final Color VANG_DAM = new Color(0xFFD54F);
    private static final Color VANG_NHAT = new Color(0xFFF9C4);
    private static final Color XANH_NHAT = new Color(0xDCEDC8);
    private static final Color HANG_LE = new Color(0xF4FAE8);
    private static final Color CHON = new Color(0xFFF176);
    private static final Color VIEN = new Color(0x8BC34A);
    private static final Color LUOI = new Color(0xDDEBC0);
    private static final Color DO_CANH_BAO = new Color(0xB42318);

    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_DAM = new Font("SansSerif", Font.BOLD, 13);

    private static final String[] COT = {
        "Mã", "Họ tên", "SĐT", "Người liên hệ",
        "Nhu cầu", "Trạng thái", "Đã học thử", "Ghi chú"
    };
    private static final int[] DO_RONG = {70, 200, 120, 160, 200, 150, 110, 200};
    private static final int COT_TRANG_THAI = 5;

    private final TuyenSinhController controller;

    private final JTextField txtTim = styleField(new JTextField(20));
    private final JComboBox<String> locHocThu = styleCombo(TuyenSinhController.LOC_HOC_THU);
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);

    private final Map<String, DefaultTableModel> models = new LinkedHashMap<>();
    private final Map<String, JTable> bangs = new LinkedHashMap<>();
    private final Map<String, List<KhachTiemNang>> dsTab = new LinkedHashMap<>();

    private final JLabel lblCanXuLy = new JLabel();
    private final JLabel thongBao = new JLabel("Sẵn sàng");

    private boolean dangReset = false;

    private final JButton btnTim = nutXanh("Tìm kiếm");
    private final JButton btnReset = nutVang("Reset");
    private final JButton btnTiepNhan = nutXanh("+ Tiếp nhận");

    private final JPopupMenu menuChuotPhai = new JPopupMenu();
    private final JMenuItem mXem;
    private final JMenuItem mSua;
    private final JMenuItem mChuyen;
    private final JMenuItem mCopy;

    private KhachTiemNang khachDangChon;

    public TuyenSinhPanel(CoSo coSo) {
        super(new BorderLayout(0, 10));
        this.controller = new TuyenSinhController(coSo, this);

        setBackground(NEN);
        setBorder(new EmptyBorder(16, 16, 12, 16));

        add(taoDau(coSo), BorderLayout.NORTH);
        add(taoThanTab(), BorderLayout.CENTER);
        add(taoThanhTrangThai(), BorderLayout.SOUTH);

        mXem = menuItem("Xem hồ sơ", controller::xemHoSo);
        mSua = menuItem("Sửa hồ sơ", controller::sua);
        mChuyen = menuItem("Chuyển thành học viên", controller::chuyenDoi);
        mCopy = menuItem("Sao chép SĐT", controller::saoChepSdt);

        menuChuotPhai.setBackground(Color.WHITE);
        menuChuotPhai.setBorder(new LineBorder(VIEN, 1, true));
        menuChuotPhai.add(mXem);
        menuChuotPhai.add(mSua);
        menuChuotPhai.addSeparator();
        menuChuotPhai.add(mChuyen);
        menuChuotPhai.addSeparator();
        menuChuotPhai.add(mCopy);

        btnTim.addActionListener(e -> controller.tim(txtTim.getText()));
        txtTim.addActionListener(e -> controller.tim(txtTim.getText()));
        locHocThu.addActionListener(e -> {
            if (!dangReset) controller.apDungLocHocThu(locHocThu.getSelectedIndex());
        });
        btnReset.addActionListener(e -> controller.taiLai());
        btnTiepNhan.addActionListener(e -> controller.tiepNhan());
        tabs.addChangeListener(e -> controller.khiChonDong(duocChon()));

        SwingUtilities.invokeLater(() -> controller.taiDanhSach(false));
    }

    private JPanel taoDau(CoSo coSo) {
        JPanel tieuDeKhung = new JPanel(new BorderLayout());
        tieuDeKhung.setBackground(XANH_TIEU_DE);
        tieuDeKhung.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("QUẢN LÝ TUYỂN SINH – " + coSo.ten());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Color.BLACK);

        JLabel phuDe = new JLabel("Theo dõi hồ sơ từ tiếp nhận đến chuyển đổi thành học viên");
        phuDe.setFont(FONT);
        phuDe.setForeground(Color.BLACK);

        JPanel khoiTieuDe = new JPanel();
        khoiTieuDe.setLayout(new BoxLayout(khoiTieuDe, BoxLayout.Y_AXIS));
        khoiTieuDe.setOpaque(false);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        phuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        khoiTieuDe.add(title);
        khoiTieuDe.add(Box.createVerticalStrut(2));
        khoiTieuDe.add(phuDe);
        tieuDeKhung.add(khoiTieuDe, BorderLayout.WEST);

        JPanel hangLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hangLoc.setBackground(VANG_NHAT);
        txtTim.setPreferredSize(new Dimension(260, 32));
        txtTim.setToolTipText("Nhập mã khách, họ tên hoặc số điện thoại");
        hangLoc.add(nhan("Mã / tên / SĐT:"));
        hangLoc.add(txtTim);
        hangLoc.add(btnTim);
        hangLoc.add(Box.createHorizontalStrut(8));
        hangLoc.add(nhan("Học thử:"));
        hangLoc.add(locHocThu);
        hangLoc.add(btnReset);

        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hangNut.setBackground(VANG_NHAT);
        hangNut.add(btnTiepNhan);

        JPanel thanh = new JPanel(new BorderLayout(0, 6));
        thanh.setBackground(VANG_NHAT);
        thanh.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(VIEN, 1, true), new EmptyBorder(8, 10, 8, 10)));
        thanh.add(hangLoc, BorderLayout.NORTH);
        thanh.add(hangNut, BorderLayout.CENTER);

        JPanel khung = new JPanel(new BorderLayout(0, 10));
        khung.setOpaque(false);
        khung.add(tieuDeKhung, BorderLayout.NORTH);
        khung.add(thanh, BorderLayout.CENTER);
        return khung;
    }

    private JComponent taoThanTab() {
        tabs.setBackground(NEN);
        tabs.setFont(FONT_DAM);
        tabs.setForeground(Color.BLACK);

        for (int i = 0; i < TuyenSinhController.MA_TAB.length; i++) {
            final String ma = TuyenSinhController.MA_TAB[i];
            dsTab.put(ma, new ArrayList<>());

            DefaultTableModel model = new DefaultTableModel(COT, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int c) {
                    return c == 0 ? Integer.class : String.class;
                }
            };
            models.put(ma, model);

            JTable bang = new JTable(model);
            trangBang(bang);
            bang.setRowSorter(new TableRowSorter<>(model));

            bang.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && ma.equals(tabHienTai())) {
                    controller.khiChonDong(duocChon());
                }
            });

            bang.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    xuLyChuotPhai(bang, ma, e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    xuLyChuotPhai(bang, ma, e);
                }
            });
            bangs.put(ma, bang);

            JPanel trang = new JPanel(new BorderLayout(0, 8));
            trang.setBackground(Color.WHITE);

            JLabel moTa = new JLabel(TuyenSinhController.MO_TA_TAB[i]);
            moTa.setFont(FONT);
            moTa.setForeground(Color.BLACK);
            moTa.setBorder(new EmptyBorder(10, 12, 4, 12));
            trang.add(moTa, BorderLayout.NORTH);

            JScrollPane cuon = new JScrollPane(bang,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            cuon.getVerticalScrollBar().setUnitIncrement(30);
            cuon.getViewport().setBackground(Color.WHITE);
            cuon.setBorder(new LineBorder(VIEN, 1, true));
            trang.add(cuon, BorderLayout.CENTER);

            tabs.addTab(TuyenSinhController.TEN_TAB[i], trang);
        }

        JPanel khung = new JPanel(new BorderLayout());
        khung.setOpaque(false);
        khung.add(tabs, BorderLayout.CENTER);
        return khung;
    }

    private JPanel taoThanhTrangThai() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(XANH_TIEU_DE);
        p.setBorder(new EmptyBorder(6, 12, 6, 12));

        lblCanXuLy.setFont(FONT_DAM);
        lblCanXuLy.setForeground(Color.BLACK);

        thongBao.setFont(FONT);
        thongBao.setForeground(Color.BLACK);

        p.add(lblCanXuLy, BorderLayout.NORTH);
        p.add(thongBao, BorderLayout.SOUTH);
        return p;
    }

    private void trangBang(JTable bang) {
        bang.setRowHeight(30);
        bang.setFont(FONT);
        bang.setForeground(Color.BLACK);
        bang.setGridColor(LUOI);
        bang.setShowVerticalLines(false);
        bang.setIntercellSpacing(new Dimension(0, 1));
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setSelectionBackground(CHON);
        bang.setSelectionForeground(Color.BLACK);
        bang.setFillsViewportHeight(true);

        BangRenderer renderer = new BangRenderer();
        bang.setDefaultRenderer(Object.class, renderer);
        bang.setDefaultRenderer(Number.class, renderer);

        JTableHeader hd = bang.getTableHeader();
        hd.setReorderingAllowed(false);
        hd.setPreferredSize(new Dimension(0, 34));
        hd.setDefaultRenderer(new TieuDeRenderer());

        for (int i = 0; i < DO_RONG.length && i < bang.getColumnCount(); i++) {
            bang.getColumnModel().getColumn(i).setPreferredWidth(DO_RONG[i]);
        }
    }

    private void xuLyChuotPhai(JTable bang, String ma, MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int dong = bang.rowAtPoint(e.getPoint());
        if (dong < 0) return;
        bang.setRowSelectionInterval(dong, dong);
        KhachTiemNang k = khachTai(ma, dong);
        if (k == null) return;
        khachDangChon = k;
        controller.khiChuotPhai(k);
        menuChuotPhai.show(bang, e.getX(), e.getY());
    }

    private JMenuItem menuItem(String text, Consumer<KhachTiemNang> hanh) {
        JMenuItem m = new JMenuItem(text);
        m.setFont(FONT);
        m.setForeground(Color.BLACK);
        m.setBackground(Color.WHITE);
        m.addActionListener(e -> {
            KhachTiemNang k = khachDangChon;
            if (k != null) SwingUtilities.invokeLater(() -> hanh.accept(k));
        });
        return m;
    }

    private String tabHienTai() {
        int viTri = tabs.getSelectedIndex();
        if (viTri < 0 || viTri >= TuyenSinhController.MA_TAB.length) {
            return TuyenSinhController.MA_TAB[0];
        }
        return TuyenSinhController.MA_TAB[viTri];
    }

    private KhachTiemNang khachTai(String ma, int viewRow) {
        JTable bang = bangs.get(ma);
        if (bang == null) return null;
        int index = bang.convertRowIndexToModel(viewRow);
        List<KhachTiemNang> ds = dsTab.getOrDefault(ma, List.of());
        return index >= 0 && index < ds.size() ? ds.get(index) : null;
    }

    private KhachTiemNang duocChon() {
        String ma = tabHienTai();
        JTable bang = bangs.get(ma);
        if (bang == null) return null;
        int dong = bang.getSelectedRow();
        return dong < 0 ? null : khachTai(ma, dong);
    }

    @Override
    public void hienDanhSach(Map<String, List<KhachTiemNang>> theoTab, int canXuLy) {
        for (int i = 0; i < TuyenSinhController.MA_TAB.length; i++) {
            String ma = TuyenSinhController.MA_TAB[i];
            List<KhachTiemNang> ds = theoTab.getOrDefault(ma, List.of());
            dsTab.put(ma, ds);

            DefaultTableModel model = models.get(ma);
            List<Vector<Object>> rows = new ArrayList<>(ds.size());
            for (KhachTiemNang k : ds) {
                rows.add(new Vector<>(Arrays.<Object>asList(
                        k.maKhach(),
                        k.hoTen(),
                        k.soDienThoai(),
                        k.tenNguoiLienHe(),
                        k.nhuCau(),
                        TuyenSinhController.tenTrangThai(k.trangThai()),
                        k.daHocThu() ? "Có" : "Chưa",
                        k.ghiChu())));
            }
            model.getDataVector().clear();
            model.getDataVector().addAll(rows);
            model.fireTableDataChanged();

            int n = ds.size();
            String tenTab = TuyenSinhController.TEN_TAB[i];
            tabs.setTitleAt(i, n > 0
                    ? "<html>" + tenTab + " <font color='#B42318'><b>(" + n + ")</b></font></html>"
                    : tenTab + " (0)");
        }

        lblCanXuLy.setText("Cần xử lý: " + canXuLy + " hồ sơ");
        lblCanXuLy.setForeground(canXuLy > 0 ? DO_CANH_BAO : Color.BLACK);
    }

    @Override
    public void chonTab(String maTab) {
        for (int i = 0; i < TuyenSinhController.MA_TAB.length; i++) {
            if (TuyenSinhController.MA_TAB[i].equals(maTab)) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public void chonDongTheoMa(int maKhach) {
        String ma = tabHienTai();
        List<KhachTiemNang> ds = dsTab.getOrDefault(ma, List.of());
        JTable bang = bangs.get(ma);
        if (bang == null) return;

        for (int i = 0; i < ds.size(); i++) {
            if (ds.get(i).maKhach() == maKhach) {
                int view = bang.convertRowIndexToView(i);
                if (view >= 0) {
                    bang.getSelectionModel().setSelectionInterval(view, view);
                    bang.scrollRectToVisible(bang.getCellRect(view, 0, true));
                    controller.khiChonDong(ds.get(i));
                }
                return;
            }
        }
    }

    @Override
    public void capNhatNut(boolean suaDuoc, boolean chuyenDuoc) {
        // Các nút Sửa hồ sơ và Chuyển thành học viên đã được đưa về menu chuột phải.
    }

    @Override
    public void capNhatMenu(boolean suaDuoc, boolean chuyenDuoc, boolean copyDuoc) {
        mSua.setEnabled(suaDuoc);
        mChuyen.setEnabled(chuyenDuoc);
        mCopy.setEnabled(copyDuoc);
    }

    @Override
    public void datLaiBoLoc() {
        dangReset = true;
        txtTim.setText("");
        locHocThu.setSelectedIndex(0);
        dangReset = false;
    }

    @Override
    public void hienThongBao(String noiDung) {
        thongBao.setForeground(Color.BLACK);
        thongBao.setText(noiDung);
    }

    @Override
    public void hienThanhCong(String noiDung) {
        hienThongBao(noiDung);
        JOptionPane.showMessageDialog(this, noiDung, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void hienCanhBao(String noiDung) {
        JOptionPane.showMessageDialog(this, noiDung, "Chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void hienLoi(String noiDung) {
        String thongDiep = (noiDung == null || noiDung.isBlank())
                ? "Đã xảy ra lỗi không xác định" : noiDung;
        thongBao.setForeground(DO_CANH_BAO);
        thongBao.setText("Lỗi: " + thongDiep);
        JOptionPane.showMessageDialog(this, thongDiep, "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public boolean xacNhan(String noiDung) {
        return JOptionPane.showConfirmDialog(this, noiDung, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    @Override
    public void saoChepVaoClipboard(String noiDung) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(noiDung), null);
    }

    @Override
    public <T> void chayNen(String dangLam, Callable<T> viec, Consumer<T> khiXong, Consumer<Throwable> khiLoi) {
        hienThongBao(dangLam);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return viec.call();
            }

            @Override
            protected void done() {
                try {
                    khiXong.accept(get());
                } catch (Exception ex) {
                    khiLoi.accept(ex);
                }
            }
        }.execute();
    }

    @Override
    public void moForm(KhachTiemNang cu) {
        JTextField ten = styleField(new JTextField(cu == null ? "" : cu.hoTen(), 24));
        JTextField sdt = styleField(new JTextField(cu == null ? "" : chuoi(cu.soDienThoai()), 24));
        JTextField lienHe = styleField(new JTextField(cu == null ? "" : chuoi(cu.tenNguoiLienHe()), 24));
        JTextField nhuCau = styleField(new JTextField(cu == null ? "" : chuoi(cu.nhuCau()), 24));
        JTextField ghiChu = styleField(new JTextField(cu == null ? "" : chuoi(cu.ghiChu()), 24));

        JComboBox<String> trangThai = styleCombo(new String[0]);
        for (String ma : TuyenSinhController.TRANG_THAI_SUA) {
            trangThai.addItem(TuyenSinhController.tenTrangThai(ma));
        }
        if (cu != null) {
            for (int i = 0; i < TuyenSinhController.TRANG_THAI_SUA.length; i++) {
                if (TuyenSinhController.TRANG_THAI_SUA[i].equals(cu.trangThai())) {
                    trangThai.setSelectedIndex(i);
                    break;
                }
            }
        }

        JCheckBox daHocThu = new JCheckBox("Đã học thử", cu != null && cu.daHocThu());
        daHocThu.setBackground(Color.WHITE);
        daHocThu.setForeground(Color.BLACK);
        daHocThu.setFont(FONT);

        JPanel nhomThongTin = nhomForm("Thông tin hồ sơ",
                new JLabel[]{nhan("Họ tên (*)"), nhan("SĐT"), nhan("Người liên hệ"), nhan("Nhu cầu")},
                new JComponent[]{ten, sdt, lienHe, nhuCau});

        JPanel nhomTrangThai = nhomForm("Trạng thái & ghi chú",
                new JLabel[]{nhan("Trạng thái mới"), nhan("Học thử"), nhan("Ghi chú")},
                new JComponent[]{trangThai, daHocThu, ghiChu});

        JPanel noiDung = new JPanel();
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));
        noiDung.setBackground(NEN);
        noiDung.setBorder(new EmptyBorder(8, 8, 8, 8));
        noiDung.add(nhomThongTin);
        noiDung.add(Box.createVerticalStrut(12));
        noiDung.add(nhomTrangThai);

        if (JOptionPane.showConfirmDialog(this, noiDung,
                cu == null ? "Tiếp nhận tuyển sinh" : "Sửa hồ sơ / Chuyển trạng thái",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        controller.luu(cu, new TuyenSinhController.DuLieuForm(
                ten.getText(),
                sdt.getText(),
                lienHe.getText(),
                nhuCau.getText(),
                TuyenSinhController.TRANG_THAI_SUA[trangThai.getSelectedIndex()],
                daHocThu.isSelected(),
                ghiChu.getText()));
    }

    @Override
    public void hienHoSo(KhachTiemNang k) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(NEN);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        themDong(p, g, r++, "Mã khách", giaTriLabel(String.valueOf(k.maKhach())));
        themDong(p, g, r++, "Họ tên", giaTriLabel(k.hoTen()));
        themDong(p, g, r++, "Số điện thoại", giaTriLabel(k.soDienThoai()));
        themDong(p, g, r++, "Người liên hệ", giaTriLabel(k.tenNguoiLienHe()));
        themDong(p, g, r++, "Nhu cầu", giaTriLabel(k.nhuCau()));
        themDong(p, g, r++, "Trạng thái", giaTriLabel(TuyenSinhController.tenTrangThai(k.trangThai())));
        themDong(p, g, r++, "Đã học thử", giaTriLabel(k.daHocThu() ? "Có" : "Chưa"));
        themDong(p, g, r++, "Ghi chú", giaTriLabel(k.ghiChu()));

        JOptionPane.showMessageDialog(this, p, "Hồ sơ tuyển sinh", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel nhomForm(String tieuDe, JLabel[] nhan, JComponent[] o) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        new LineBorder(VIEN, 1, true), tieuDe, 0, 0, FONT_DAM, Color.BLACK),
                new EmptyBorder(8, 10, 10, 10)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < nhan.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.weightx = 0;
            p.add(nhan[i], g);

            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            o[i].setPreferredSize(new Dimension(280, 30));
            p.add(o[i], g);
            g.fill = GridBagConstraints.NONE;
        }
        return p;
    }

    private static void themDong(JPanel p, GridBagConstraints g, int dong, String nhanText, JComponent thanhPhan) {
        g.gridy = dong;
        g.gridx = 0;
        g.weightx = 0;
        p.add(nhan(nhanText), g);

        g.gridx = 1;
        g.weightx = 1;
        p.add(thanhPhan, g);
    }

    private static String chuoi(String s) {
        return s == null ? "" : s;
    }

    private static JLabel nhan(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.BLACK);
        l.setFont(FONT_DAM);
        return l;
    }

    private static JLabel giaTriLabel(String text) {
        JLabel l = new JLabel(text == null || text.isBlank() ? "—" : text);
        l.setForeground(Color.BLACK);
        l.setFont(FONT);
        return l;
    }

    private static JTextField styleField(JTextField f) {
        f.setFont(FONT);
        f.setForeground(Color.BLACK);
        f.setBackground(Color.WHITE);
        f.setCaretColor(Color.BLACK);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(VIEN, 1, true), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    private static JComboBox<String> styleCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(FONT);
        c.setForeground(Color.BLACK);
        c.setBackground(Color.WHITE);
        return c;
    }

    private static JButton nutXanh(String text) {
        return nut(text, XANH_NUT, XANH_DAM);
    }

    private static JButton nutVang(String text) {
        return nut(text, VANG_NUT, VANG_DAM);
    }

    private static JButton nut(String text, Color nen, Color hover) {
        JButton b = new JButton(text);
        b.setFont(FONT_DAM);
        b.setForeground(Color.BLACK);
        b.setBackground(nen);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(nen);
            }
        });
        return b;
    }

    private static final class BangRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            setForeground(Color.BLACK);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            if (sel) {
                setBackground(CHON);
            } else if (c == COT_TRANG_THAI) {
                setBackground(TuyenSinhController.tenTrangThai("DA_CHUYEN_DOI").equals(v) ? XANH_NHAT : VANG_NHAT);
            } else {
                setBackground(r % 2 == 0 ? Color.WHITE : HANG_LE);
            }
            return this;
        }
    }

    private static final class TieuDeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            setBackground(XANH_TIEU_DE);
            setForeground(Color.BLACK);
            setFont(FONT_DAM);
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 1, XANH_NUT),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            return this;
        }
    }
}