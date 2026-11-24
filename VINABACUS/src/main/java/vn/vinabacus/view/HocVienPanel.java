package vn.vinabacus.view;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import vn.vinabacus.controller.HocVienController;
import vn.vinabacus.controller.HocVienController.DongLop;
import vn.vinabacus.controller.HocVienController.GoiY;
import vn.vinabacus.controller.HocVienController.KhoaTienDo;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HocVien;

/**
 * Trang học viên / học sinh + hồ sơ học viên.
 * Chỉ dựng giao diện, bắt sự kiện gửi sang controller và hiển thị những gì controller đẩy ra.
 * Không có logic lọc, tìm kiếm, định dạng nghiệp vụ hay truy cập dữ liệu.
 * Sơ đồ khóa học nằm ở HocVien_SoDoKhoaHocPanel (mở từ nút "Sơ đồ" trong hồ sơ).
 */
public final class HocVienPanel extends JPanel implements HocVienController.View {

    // ===== Bảng màu xanh lá - vàng, chữ đen (package-private để HocVien_SoDoKhoaHocPanel dùng lại) =====
    static final Color NEN = new Color(0xFFFDF0), XANH_TIEU_DE = new Color(0xC5E1A5),
            XANH_NUT = new Color(0xA5D66A), XANH_DAM = new Color(0x8BC34A), VANG_NUT = new Color(0xFFE57F),
            VANG_DAM = new Color(0xFFD54F), VANG_NHAT = new Color(0xFFF9C4), XANH_NHAT = new Color(0xDCEDC8),
            HANG_LE = new Color(0xF4FAE8), CHON = new Color(0xFFF176), VIEN = new Color(0x8BC34A),
            LUOI = new Color(0xDDEBC0), DO_CANH_BAO = new Color(0xB42318);

    static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    static final Font FONT_DAM = new Font("SansSerif", Font.BOLD, 13);

    private static final String[] COT = {"Mã", "Họ tên", "Ngày sinh", "Giới tính", "Người liên hệ",
            "SĐT", "Địa chỉ", "Trạng thái", "Ghi chú"};
    private static final int[] DO_RONG = {70, 180, 100, 90, 160, 120, 240, 130, 200};
    private static final int COT_NGAY_SINH = 2, COT_TRANG_THAI = 7;

    private final HocVienController controller;
    private final String tieuDe;

    private final JTextField txtTim = styleField(new JTextField(20));
    private final JComboBox<String> locTrangThai = styleCombo(HocVienController.LOC_TEN);
    private final JComboBox<String> locXepLop = styleCombo(HocVienController.LOC_XEP_LOP);
    private final JButton btnTim = nutXanh("Tìm kiếm");
    private final JButton btnReset = nutVang("Reset");
    private final JButton btnThem = nutXanh("+ Thêm");
    private final JLabel lblTongKet = new JLabel();
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private boolean dangDatLai = false;   // đang đặt lại combo bằng code: bỏ qua sự kiện chọn

    /** Cột 0 là Integer, cột Ngày sinh là LocalDate để bộ sắp xếp tự so sánh đúng kiểu. */
    private final DefaultTableModel model = new DefaultTableModel(COT, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return c == 0 ? Integer.class : c == COT_NGAY_SINH ? LocalDate.class : String.class;
        }
    };
    private final JTable bang = new JTable(model);
    private List<HocVien> danhSach = List.of();   // đúng thứ tự dòng của model

    // ===== Gợi ý tìm kiếm =====
    private final JPopupMenu goiYMenu = new JPopupMenu();
    private List<GoiY> goiY = List.of();
    private int viTriGoiY = -1;
    private boolean dangDatText = false;   // đang điền ô tìm kiếm bằng code: không sinh gợi ý

    // ===== Hồ sơ & sơ đồ khóa =====
    private JDialog hoSoDialog, soDoDialog;
    private final Map<Integer, JLabel> lblTotNghiep = new HashMap<>();   // mã lớp -> nhãn ngày tốt nghiệp trong hồ sơ

    private final JPopupMenu menuChuotPhai = new JPopupMenu();
    private JMenuItem mCopy;
    private HocVien hocVienDangChon;

    /** Dùng cho TV2Module và TV2Preview: chưa có màn Lớp. */
    public HocVienPanel(CoSo coSo) {
        this(coSo, null);
    }

    /** @param khiXemLop callback nhận mã lớp khi bấm "Xem lớp"; null nếu chưa có màn Lớp */
    public HocVienPanel(CoSo coSo, IntConsumer khiXemLop) {
        super(new BorderLayout(0, 10));
        this.tieuDe = coSo == CoSo.MAM_NON ? "QUẢN LÝ HỌC SINH MẦM NON" : "QUẢN LÝ HỌC VIÊN TRUNG TÂM";
        this.controller = new HocVienController(coSo, this, khiXemLop);

        setBackground(NEN);
        setBorder(new EmptyBorder(16, 16, 12, 16));
        add(taoDau(), BorderLayout.NORTH);
        add(taoBang(), BorderLayout.CENTER);
        add(taoThanhTrangThai(), BorderLayout.SOUTH);
        taoMenu();
        caiTimKiem();

        locTrangThai.addActionListener(e -> {
            if (!dangDatLai) controller.apDungTrangThai(locTrangThai.getSelectedIndex());
        });
        locXepLop.addActionListener(e -> {
            if (!dangDatLai) controller.apDungLocXepLop(locXepLop.getSelectedIndex());
        });
        btnReset.addActionListener(e -> controller.taiLai());
        btnThem.addActionListener(e -> controller.them());

        SwingUtilities.invokeLater(() -> controller.taiDanhSach(false));
    }

    // ================= Bố cục trang danh sách =================

    private JPanel taoDau() {
        JPanel tieuDeKhung = new JPanel(new BorderLayout());
        tieuDeKhung.setBackground(XANH_TIEU_DE);
        tieuDeKhung.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel(tieuDe);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Color.BLACK);
        tieuDeKhung.add(title, BorderLayout.WEST);

        // Một hàng: [ô tìm kiếm][Tìm kiếm] Trạng thái [combo] Xếp lớp [combo] [Reset]
        JPanel hangLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hangLoc.setBackground(VANG_NHAT);
        for (Component c : new Component[]{nhan("Mã / tên / SĐT:"), txtTim, btnTim, nhan("Trạng thái:"),
                locTrangThai, nhan("Xếp lớp:"), locXepLop, btnReset}) {
            hangLoc.add(c);
        }

        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hangNut.setBackground(VANG_NHAT);
        hangNut.add(btnThem);

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

    private JComponent taoBang() {
        bang.setRowHeight(30);
        bang.setFont(FONT);
        bang.setForeground(Color.BLACK);
        bang.setGridColor(LUOI);
        bang.setShowVerticalLines(false);
        bang.setIntercellSpacing(new Dimension(0, 1));
        bang.setSelectionBackground(CHON);
        bang.setSelectionForeground(Color.BLACK);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setFillsViewportHeight(true);
        BangRenderer renderer = new BangRenderer();
        bang.setDefaultRenderer(Object.class, renderer);
        bang.setDefaultRenderer(Number.class, renderer);   // cột Mã (Integer) cũng dùng renderer này
        bang.setRowSorter(new TableRowSorter<>(model));

        JTableHeader hd = bang.getTableHeader();
        hd.setReorderingAllowed(false);
        hd.setPreferredSize(new Dimension(0, 34));
        hd.setDefaultRenderer(new TieuDeRenderer());
        for (int i = 0; i < DO_RONG.length; i++) bang.getColumnModel().getColumn(i).setPreferredWidth(DO_RONG[i]);

        MouseAdapter chuotPhai = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                xuLyChuotPhai(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                xuLyChuotPhai(e);
            }
        };
        bang.addMouseListener(chuotPhai);

        // Thanh cuộn dọc luôn hiện; thanh cuộn ngang chỉ hiện khi cần.
        JScrollPane cuon = new JScrollPane(bang,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cuon.getVerticalScrollBar().setUnitIncrement(30);
        cuon.getViewport().setBackground(Color.WHITE);
        cuon.setBorder(new LineBorder(VIEN, 1, true));
        return cuon;
    }

    private JPanel taoThanhTrangThai() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(XANH_TIEU_DE);
        p.setBorder(new EmptyBorder(6, 12, 6, 12));
        lblTongKet.setFont(FONT_DAM);
        lblTongKet.setForeground(Color.BLACK);
        thongBao.setForeground(Color.BLACK);
        thongBao.setFont(FONT);
        p.add(lblTongKet, BorderLayout.NORTH);
        p.add(thongBao, BorderLayout.SOUTH);
        return p;
    }

    private void taoMenu() {
        menuChuotPhai.setBackground(Color.WHITE);
        menuChuotPhai.setBorder(new LineBorder(VIEN, 1, true));
        mCopy = menuItem("Sao chép SĐT", controller::saoChepSdt);
        menuChuotPhai.add(menuItem("Xem hồ sơ", controller::xemHoSo));
        menuChuotPhai.add(menuItem("Sửa thông tin", controller::sua));
        menuChuotPhai.addSeparator();
        menuChuotPhai.add(mCopy);
        menuChuotPhai.addSeparator();
        menuChuotPhai.add(menuItem("Xóa học viên", controller::xoa));
    }

    // ================= Tìm kiếm: chỉ bắt sự kiện, controller tính gợi ý và lọc =================

    private void caiTimKiem() {
        goiYMenu.setFocusable(false);
        goiYMenu.setBackground(Color.WHITE);
        goiYMenu.setBorder(new LineBorder(VIEN, 1));

        txtTim.getDocument().addDocumentListener(khiDoi(this::yeuCauGoiY));
        txtTim.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                goiYMenu.setVisible(false);
            }
        });
        txtTim.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!goiYMenu.isVisible()) return;
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_DOWN) toMauGoiY(Math.min(viTriGoiY + 1, goiY.size() - 1));
                else if (k == KeyEvent.VK_UP) toMauGoiY(Math.max(viTriGoiY - 1, -1));
                else if (k == KeyEvent.VK_ESCAPE) goiYMenu.setVisible(false);
            }
        });
        // Enter: đang chọn gợi ý bằng phím thì chọn gợi ý, ngược lại tìm theo nội dung đã nhập
        txtTim.addActionListener(e -> {
            if (goiYMenu.isVisible() && viTriGoiY >= 0) {
                controller.chonGoiY(txtTim.getText(), goiY.get(viTriGoiY).hocVien());
            } else {
                controller.timKiem(txtTim.getText());
            }
        });
        btnTim.addActionListener(e -> controller.timKiem(txtTim.getText()));
    }

    private static DocumentListener khiDoi(Runnable viec) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                viec.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                viec.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                viec.run();
            }
        };
    }

    private void yeuCauGoiY() {
        if (dangDatText) return;
        if (txtTim.isShowing()) controller.goiY(txtTim.getText());
        else goiYMenu.setVisible(false);
    }

    private JLabel dongGoiY(String text, Runnable khiBam, boolean dam) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(Color.WHITE);
        l.setForeground(Color.BLACK);
        l.setFont(dam ? FONT_DAM : FONT);
        l.setBorder(new EmptyBorder(5, 10, 5, 10));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setPreferredSize(new Dimension(Math.max(txtTim.getWidth(), 380), 28));
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                l.setBackground(VANG_NHAT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                l.setBackground(Color.WHITE);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                khiBam.run();
            }
        });
        return l;
    }

    private void toMauGoiY(int viTri) {
        viTriGoiY = viTri;
        for (int j = 0; j < goiY.size(); j++) {
            goiYMenu.getComponent(j).setBackground(j == viTri ? CHON : Color.WHITE);
        }
    }

    // ================= Chuột & menu (chỉ chuyển sự kiện) =================

    private void xuLyChuotPhai(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int dong = bang.rowAtPoint(e.getPoint());
        if (dong < 0) return;
        bang.setRowSelectionInterval(dong, dong);
        HocVien h = hocVienTai(dong);
        if (h == null) return;
        hocVienDangChon = h;
        controller.khiChuotPhai(h);
        menuChuotPhai.show(bang, e.getX(), e.getY());
    }

    private JMenuItem menuItem(String text, Consumer<HocVien> hanh) {
        JMenuItem m = new JMenuItem(text);
        m.setFont(FONT);
        m.setForeground(Color.BLACK);
        m.setBackground(Color.WHITE);
        m.addActionListener(e -> {
            HocVien h = hocVienDangChon;
            if (h != null) SwingUtilities.invokeLater(() -> hanh.accept(h));
        });
        return m;
    }

    private HocVien hocVienTai(int dongTrenBang) {
        int i = bang.convertRowIndexToModel(dongTrenBang);
        return i >= 0 && i < danhSach.size() ? danhSach.get(i) : null;
    }

    // ================= Triển khai HocVienController.View =================

    @Override
    public void hienDanhSach(List<HocVien> ds) {
        danhSach = ds;
        // Nạp một lần rồi báo đổi một lần, tránh bộ sắp xếp phải xử lý từng dòng thêm vào.
        List<Vector<Object>> dong = new ArrayList<>(ds.size());
        for (HocVien h : ds) {
            dong.add(new Vector<>(Arrays.<Object>asList(
                    h.maHocVien(), h.hoTen(), h.ngaySinh(), HocVienController.tenGioiTinh(h.gioiTinh()),
                    h.tenNguoiLienHe(), h.soDienThoaiLienHe(), h.diaChi(),
                    HocVienController.tenTrangThai(h.trangThaiHoSo()), h.ghiChu())));
        }
        model.getDataVector().clear();
        model.getDataVector().addAll(dong);
        model.fireTableDataChanged();
    }

    @Override
    public void hienTongKet(int hoatDong, int ngungSuDung) {
        lblTongKet.setText("Đang hoạt động: " + hoatDong + "   |   Ngừng sử dụng: " + ngungSuDung);
        lblTongKet.setForeground(ngungSuDung > 0 ? DO_CANH_BAO : Color.BLACK);
    }

    @Override
    public void hienThongBao(String noiDung) {
        thongBao.setForeground(Color.BLACK);
        thongBao.setText(noiDung);
    }

    @Override
    public void hienThanhCong(String noiDung) {
        hienThongBao(noiDung);
        hop(noiDung, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void hienCanhBao(String noiDung) {
        hop(noiDung, "Chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void hienLoi(String noiDung) {
        thongBao.setForeground(DO_CANH_BAO);
        thongBao.setText("Lỗi: " + noiDung);
        hop(noiDung, "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public boolean xacNhan(String noiDung) {
        return JOptionPane.showConfirmDialog(manHinhHienTai(), noiDung, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    @Override
    public void hienGoiY(List<GoiY> ds, int conLai) {
        goiYMenu.setVisible(false);
        goiYMenu.removeAll();
        goiY = ds;
        viTriGoiY = -1;
        if (ds.isEmpty() || !txtTim.isShowing()) return;
        for (GoiY g : ds) {
            goiYMenu.add(dongGoiY(g.nhan(), () -> controller.chonGoiY(txtTim.getText(), g.hocVien()), false));
        }
        if (conLai > 0) {
            goiYMenu.add(dongGoiY("More +" + conLai, () -> controller.timKiem(txtTim.getText()), true));
        }
        goiYMenu.show(txtTim, 0, txtTim.getHeight());
    }

    @Override
    public void datOTimKiem(String noiDung) {
        dangDatText = true;
        txtTim.setText(noiDung);
        dangDatText = false;
        goiYMenu.setVisible(false);
    }

    @Override
    public void datLaiBoLoc() {
        dangDatLai = true;
        datOTimKiem("");
        locTrangThai.setSelectedIndex(0);
        locXepLop.setSelectedIndex(0);
        dangDatLai = false;
    }

    @Override
    public void capNhatMenu(boolean coSoDienThoai) {
        mCopy.setEnabled(coSoDienThoai);
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
                final T kq;
                try {
                    kq = get();
                } catch (Exception ex) {
                    khiLoi.accept(ex);
                    return;
                }
                khiXong.accept(kq);
            }
        }.execute();
    }

    @Override
    public void moForm(HocVien cu) {
        JTextField ten = oNhap(lay(cu, HocVien::hoTen));
        NgaySinhPicker ngaySinh = new NgaySinhPicker(cu == null ? null : cu.ngaySinh());
        JComboBox<String> gioiTinh = styleCombo(HocVienController.GT_TEN);
        gioiTinh.setSelectedIndex(cu == null ? 0 : HocVienController.chiSoGioiTinh(cu.gioiTinh()));
        JTextField lienHe = oNhap(lay(cu, HocVien::tenNguoiLienHe));
        JTextField sdt = oNhap(lay(cu, HocVien::soDienThoaiLienHe));
        JTextField diaChi = oNhap(lay(cu, HocVien::diaChi));
        JTextField ghiChu = oNhap(lay(cu, HocVien::ghiChu));
        JComboBox<String> trangThai = styleCombo(HocVienController.TT_TEN);
        trangThai.setSelectedIndex(cu == null ? 0 : HocVienController.chiSoTrangThai(cu.trangThaiHoSo()));

        String[] nhanForm = {"Họ tên (*)", "Ngày sinh (dd/MM/yyyy)", "Giới tính", "Người liên hệ",
                "Số điện thoại", "Địa chỉ", "Ghi chú", "Trạng thái hồ sơ"};
        JComponent[] oForm = {ten, ngaySinh, gioiTinh, lienHe, sdt, diaChi, ghiChu, trangThai};
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(NEN);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = luoiMau(5);
        for (int i = 0; i < oForm.length; i++) themDong(p, g, i, nhanForm[i], oForm[i]);

        if (JOptionPane.showConfirmDialog(this, p, cu == null ? "Thêm học viên" : "Sửa học viên",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        final LocalDate ngay;
        try {
            ngay = ngaySinh.layNgay();
        } catch (IllegalArgumentException ex) {
            hienCanhBao(ex.getMessage());
            return;
        }
        controller.luu(cu, new HocVienController.DuLieuForm(ten.getText(), ngay, gioiTinh.getSelectedIndex(),
                lienHe.getText(), sdt.getText(), diaChi.getText(), ghiChu.getText(), trangThai.getSelectedIndex()));
    }

    // ================= Hồ sơ học viên =================

    @Override
    public void hienHoSo(HocVien h, List<DongLop> lop, List<KhoaTienDo> khoa, boolean coXemLop) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Hồ sơ học viên", Dialog.ModalityType.APPLICATION_MODAL);
        hoSoDialog = dialog;
        lblTotNghiep.clear();

        String[][] thongTinHs = {
                {"Mã học viên", String.valueOf(h.maHocVien())},
                {"Họ tên", h.hoTen()},
                {"Ngày sinh", NgaySinhPicker.hienThiNgay(h.ngaySinh())},
                {"Giới tính", HocVienController.tenGioiTinh(h.gioiTinh())},
                {"Người liên hệ", h.tenNguoiLienHe()},
                {"Số điện thoại", h.soDienThoaiLienHe()},
                {"Địa chỉ", h.diaChi()},
                {"Trạng thái hồ sơ", HocVienController.tenTrangThai(h.trangThaiHoSo())},
                {"Ghi chú", h.ghiChu()}};
        JPanel thongTin = new JPanel(new GridBagLayout());
        thongTin.setBackground(NEN);
        GridBagConstraints g = luoiMau(5);
        for (int i = 0; i < thongTinHs.length; i++) {
            themDong(thongTin, g, i, thongTinHs[i][0], giaTriLabel(thongTinHs[i][1]));
        }

        JPanel phanLop = new JPanel(new BorderLayout(0, 6));
        phanLop.setBackground(NEN);
        phanLop.setBorder(vienTieuDe(khoa.isEmpty() ? "Lớp đang học" : "Khóa học / Lớp đang học"));
        phanLop.add(!khoa.isEmpty() ? taoBangKhoa(dialog, h, khoa, coXemLop)
                : !lop.isEmpty() ? taoDanhSachLop(dialog, lop, coXemLop)
                : taoChuaXepLop(dialog, h), BorderLayout.CENTER);

        JButton btnDong = nutXanh("Đóng");
        btnDong.addActionListener(e -> dialog.dispose());
        JPanel duoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        duoi.setBackground(NEN);
        duoi.add(btnDong);

        JPanel noiDung = new JPanel(new BorderLayout(0, 10));
        noiDung.setBackground(NEN);
        noiDung.setBorder(new EmptyBorder(12, 12, 12, 12));
        noiDung.add(thongTin, BorderLayout.NORTH);
        noiDung.add(phanLop, BorderLayout.CENTER);
        noiDung.add(duoi, BorderLayout.SOUTH);

        dialog.setContentPane(noiDung);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);   // modal: trả về khi hồ sơ đóng
        hoSoDialog = null;
        soDoDialog = null;
    }

    private JComponent taoChuaXepLop(JDialog dialog, HocVien h) {
        JPanel trong = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        trong.setBackground(NEN);
        trong.add(giaTriLabel("Chưa xếp lớp"));
        JButton btnXepLop = nutVang("Xếp lớp");
        btnXepLop.addActionListener(e -> {
            dialog.dispose();
            SwingUtilities.invokeLater(() -> controller.xepLop(h));
        });
        trong.add(btnXepLop);
        return trong;
    }

    /** Dự phòng khi chỉ có DongLop: mỗi lớp một dòng kèm nút Xem lớp. */
    private JComponent taoDanhSachLop(JDialog dialog, List<DongLop> lop, boolean coXemLop) {
        JPanel danh = new JPanel(new GridBagLayout());
        danh.setBackground(NEN);
        GridBagConstraints g = luoiMau(4);
        for (DongLop l : lop) {
            themHang(danh, g, g.gridy + 1, giaTriLabel(l.moTa()), nutXemLop(dialog, l.maLop(), coXemLop));
        }
        return danh;
    }

    /** Mỗi khóa/lớp một dòng gồm nhập học, tốt nghiệp, tiến độ, số buổi vắng và nút thao tác. */
    private JComponent taoBangKhoa(JDialog dialog, HocVien h, List<KhoaTienDo> khoa, boolean coXemLop) {
        JPanel luoi = new JPanel(new GridBagLayout());
        luoi.setBackground(NEN);
        GridBagConstraints c = luoiMau(5);
        c.insets = new Insets(5, 8, 5, 8);
        themHang(luoi, c, 0, nhan("Khóa học / Lớp"), nhan("Nhập học"), nhan("Tốt nghiệp"),
                nhan("Tiến độ"), nhan("Vắng"), nhan(""));
        int dong = 1;
        for (KhoaTienDo k : khoa) {
            String ten = "<html><b>" + esc(k.tenChinh()) + "</b>"
                    + (k.tenMonHoc().isBlank() ? "" : " (" + esc(k.tenMonHoc()) + ")")
                    + "<br>Lớp: " + esc(k.tenLop()) + "</html>";
            String totNghiepText = k.lienTuc() ? "— (không kết thúc)" : k.textTotNghiep();
            JLabel tn = giaTriLabel(totNghiepText);
            if (!k.lienTuc()) lblTotNghiep.put(k.maLop(), tn);

            JLabel vang = giaTriLabel(k.textVang());
            if (!k.coDiemDanh()) vang.setToolTipText("Chưa có dữ liệu điểm danh");
            else if (!k.vang().isEmpty()) vang.setForeground(DO_CANH_BAO);

            JButton btnSoDo = nutVang("Sơ đồ");
            btnSoDo.addActionListener(e -> controller.xemSoDo(h, k.maLop()));
            JPanel nut = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            nut.setOpaque(false);
            nut.add(btnSoDo);
            nut.add(nutXemLop(dialog, k.maLop(), coXemLop));

            themHang(luoi, c, dong++, giaTriLabel(ten), giaTriLabel(HocVienController.ngay(k.ngayNhapHoc())),
                    tn, thanhTienDo(k), vang, nut);
        }
        JScrollPane cuon = new JScrollPane(luoi);
        cuon.setBorder(null);
        cuon.getViewport().setBackground(NEN);
        Dimension d = luoi.getPreferredSize();
        cuon.setPreferredSize(new Dimension(d.width + 20, Math.min(d.height + 4, 300)));
        return cuon;
    }

    private JButton nutXemLop(JDialog dialog, int maLop, boolean coXemLop) {
        JButton b = nutVang("Xem lớp");
        if (!coXemLop) {
            b.setEnabled(false);
            b.setToolTipText("Màn Lớp chưa được kết nối");
        } else {
            b.addActionListener(e -> {
                dialog.dispose();
                SwingUtilities.invokeLater(() -> controller.xemLop(maLop));
            });
        }
        return b;
    }

    // ================= Sơ đồ khóa học (giao diện nằm ở HocVien_SoDoKhoaHocPanel) =================

    @Override
    public void hienSoDo(HocVien h, KhoaTienDo k) {
        if (!k.lienTuc()) {
            JLabel tn = lblTotNghiep.get(k.maLop());
            if (tn != null) tn.setText(k.textTotNghiep());   // đồng bộ dòng trong hồ sơ
        }

        if (soDoDialog != null) soDoDialog.dispose();
        Window owner = hoSoDialog != null ? hoSoDialog : SwingUtilities.getWindowAncestor(this);
        // Không modal để tạo lại sơ đồ ngay trong nút Lưu / Tự tính mà không lồng vòng lặp sự kiện
        JDialog d = new JDialog(owner, k.lienTuc() ? "Sơ đồ học tập (Mầm non)" : "Sơ đồ khóa học",
                Dialog.ModalityType.MODELESS);
        soDoDialog = d;
        d.setContentPane(new HocVien_SoDoKhoaHocPanel(controller, h, k, d::dispose));
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    // ================= Tiện ích giao diện (package-private: HocVien_SoDoKhoaHocPanel dùng lại) =================

    /** Cửa sổ nên làm chủ hộp thoại: sơ đồ / hồ sơ đang mở, nếu không thì panel. */
    private Component manHinhHienTai() {
        if (soDoDialog != null && soDoDialog.isShowing()) return soDoDialog;
        if (hoSoDialog != null && hoSoDialog.isShowing()) return hoSoDialog;
        return this;
    }

    private void hop(String noiDung, String tieuDeHop, int loai) {
        JOptionPane.showMessageDialog(manHinhHienTai(), noiDung, tieuDeHop, loai);
    }

    private static String lay(HocVien cu, Function<HocVien, String> truong) {
        return cu == null ? "" : chuoi(truong.apply(cu));
    }

    private static JTextField oNhap(String giaTri) {
        return styleField(new JTextField(giaTri, 24));
    }

    static String chuoi(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        return chuoi(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static GridBagConstraints luoiMau(int le) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(le, 4, le, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    /** Một dòng "nhãn: thành phần" trong lưới 2 cột. */
    static void themDong(JPanel p, GridBagConstraints g, int dong, String nhanText, JComponent thanhPhan) {
        g.gridy = dong;
        g.gridx = 0;
        g.weightx = 0;
        p.add(nhan(nhanText), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(thanhPhan, g);
    }

    /** Một hàng nhiều cột; cột đầu giãn theo chiều ngang. */
    private static void themHang(JPanel p, GridBagConstraints c, int dong, JComponent... cot) {
        c.gridy = dong;
        for (int i = 0; i < cot.length; i++) {
            c.gridx = i;
            c.weightx = i == 0 ? 1 : 0;
            p.add(cot[i], c);
        }
    }

    static Border vienTieuDe(String tieuDeVien) {
        return BorderFactory.createTitledBorder(new LineBorder(VIEN, 1, true), tieuDeVien, 0, 0, FONT_DAM, Color.BLACK);
    }

    static JProgressBar thanhTienDo(KhoaTienDo k) {
        int max = Math.max(k.lienTuc() ? k.daHoc() : k.tongSoBuoi(), 1);
        JProgressBar p = new JProgressBar(0, max);
        p.setValue(Math.min(k.daHoc(), max));
        p.setStringPainted(true);
        p.setString(k.textTienDo());
        p.setForeground(XANH_DAM);
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(190, 22));
        return p;
    }

    static JLabel nhan(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.BLACK);
        l.setFont(FONT_DAM);
        return l;
    }

    static JLabel giaTriLabel(String text) {
        JLabel l = new JLabel(text == null || text.isBlank() ? "—" : text);
        l.setForeground(Color.BLACK);
        l.setFont(FONT);
        return l;
    }

    static JTextField styleField(JTextField f) {
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

    static JButton nutXanh(String text) {
        return nut(text, XANH_NUT, XANH_DAM);
    }

    static JButton nutVang(String text) {
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

    // ================= Renderer =================

    private static final class BangRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            if (c == COT_NGAY_SINH) setText(NgaySinhPicker.hienThiNgay((LocalDate) v));
            setForeground(Color.BLACK);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            if (sel) setBackground(CHON);
            else if (c == COT_TRANG_THAI) setBackground(HocVienController.TT_TEN[0].equals(v) ? XANH_NHAT : VANG_NHAT);
            else setBackground(r % 2 == 0 ? Color.WHITE : HANG_LE);
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