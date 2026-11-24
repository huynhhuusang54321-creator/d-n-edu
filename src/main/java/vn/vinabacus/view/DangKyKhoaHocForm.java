package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.DangKyKhoaHocController;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.DangKyKhoaHoc.ThongTinLop;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.ChuongTrinhGiamGia;
import vn.vinabacus.model.TaiChinhDTO;
import vn.vinabacus.service.GiamGiaService;
import vn.vinabacus.service.PhanBoThanhToanService;

/** Form Trung tâm: chọn người, nhiều khóa và lớp riêng cho từng khóa. */
final class DangKyKhoaHocForm extends JPanel {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final Color DAM = new Color(0, 84, 51);
    private static final Color XANH = new Color(0, 108, 56);
    private static final Color VANG = new Color(255, 222, 0);
    private static final Color NEN = new Color(248, 251, 249);
    private static final Color XANH_NHAT = new Color(230, 245, 234);
    private static final Color VIEN = new Color(210, 227, 215);
    private static final String[] COT = {
        "Môn học", "Khóa học", "Số buổi", "Đăng ký", "Lớp", "Xếp lớp", "Thao tác",
        "Học phí gốc", "Giảm giá", "Phải thu"
    };
    private final DangKyKhoaHocController controller = new DangKyKhoaHocController();
    private final Component parent;
    private final JDialog dialog;
    private final KhachTiemNang hoSoDau;
    private final HocVien hocVienDau;
    private final Consumer<DangKyKhoaHoc.KetQua> onSaved;
    private final JComboBox<String> cboNguon = new JComboBox<>(new String[]{"Hồ sơ tuyển sinh", "Học viên hiện tại"});
    private final JTextField timNguoi = new JTextField(17);
    private final JComboBox<Nguoi> cboNguoi = new JComboBox<>();
    private final JLabel thongTinNguoi = new JLabel("Chọn hồ sơ tuyển sinh hoặc học viên.");
    private final NgayKhaiGiangPicker ngayBatDau = new NgayKhaiGiangPicker(LocalDate.now());
    private final JComboBox<String> cboMon = new JComboBox<>();
    private final JTextField timKhoa = new JTextField(16);
    private final JPanel danhSachKhoa = new JPanel();
    private final DefaultTableModel model = new DefaultTableModel(COT, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
        @Override public Class<?> getColumnClass(int col) { return col == 2 ? Integer.class : String.class; }
    };
    private final JTable bangDaChon = new JTable(model);
    private final JComboBox<LopChon> cboLop = new JComboBox<>();
    private final JPanel danhSachLopChon = new JPanel();
    private final JTextArea chiTietLop = new JTextArea(5, 38);
    private final NgayKhaiGiangPicker ngayXep = new NgayKhaiGiangPicker(LocalDate.now());
    private final JButton btnXep = new JButton("Xếp lớp khóa đang chờ");
    private final JButton btnCho = new JButton("Để chờ xếp lớp");
    private final JButton btnLuu = new NutMau("Lưu đăng ký", NutMau.Kieu.CHINH);
    private final JButton btnChuyen = new JButton("Chuyển thành học viên");
    private final JButton btnLui = new JButton("Quay lại");
    private final JButton btnTiep = new JButton("Tiếp tục");
    private final CardLayout boCucBuoc = new CardLayout();
    private final JPanel cacBuoc = new JPanel(boCucBuoc);
    private final JPanel gioKhoaCards = new JPanel();
    private final JPanel[] dauBuoc = new JPanel[6];
    private final JLabel[] nhanBuoc = new JLabel[6];
    private final JLabel tenNguoiBanner = new JLabel("Chưa chọn người đăng ký");
    private final JLabel dienThoaiBanner = new JLabel("Số điện thoại: —");
    private final JLabel trangThaiBanner = new JLabel("Hồ sơ: —");
    private final JLabel maHoSo = new JLabel("—");
    private final JLabel maHoSoTomTat = new JLabel("—");
    private final JLabel hoTen = new JLabel("—");
    private final JLabel ngaySinh = new JLabel("—");
    private final JLabel gioiTinh = new JLabel("—");
    private final JLabel soDienThoai = new JLabel("—");
    private final JLabel nguoiLienHe = new JLabel("—");
    private final JLabel nhuCau = new JLabel("—");
    private final JLabel trangThai = new JLabel("—");
    private final JLabel tongGio = new JLabel("0 đ");
    private final JLabel tongGocGiam = new JLabel("0 đ");
    private final JLabel tongGiam = new JLabel("0 đ");
    private final JLabel tongPhaiThu = new JLabel("0 đ");
    private final JLabel lopPhong = new JLabel("—");
    private final JLabel lopGiaoVien = new JLabel("—");
    private final JLabel lopSiSo = new JLabel("—");
    private final JLabel lopThoiGian = new JLabel("—");
    private final DefaultTableModel modelGia = new DefaultTableModel(
            new String[]{"Khóa học", "Học phí gốc", "Giảm giá", "Phải thu"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final DefaultTableModel modelXacNhan = new DefaultTableModel(
            new String[]{"Khóa học", "Xếp lớp", "Học phí", "Giảm giá", "Phải thu"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private int buoc = 1;
    private static final String[] TEN_BUOC = {"Hồ sơ", "Giỏ khóa", "Xếp lớp", "Giảm giá", "Thanh toán", "Xác nhận"};
    private final JLabel tongQuan = new JLabel("Tổng khóa: 0  ·  Đã xếp: 0  ·  Chờ: 0");
    private final JLabel tongHocPhi = new JLabel("Học phí gốc: 0 đ  ·  Giảm: 0 đ  ·  Phải thu: 0 đ");
    private final JLabel phaiThuBuoc5 = new JLabel("Tổng phải thu: 0 đ");
    private final JLabel hocPhiBuoc5 = new JLabel("Học phí gốc: 0 đ");
    private final JLabel giamGiaBuoc5 = new JLabel("Giảm giá: 0 đ");
    private final JLabel thucThuBuoc5 = new JLabel("Đóng lần này: 0 đ");
    private final JLabel conNoBuoc5 = new JLabel("Còn nợ: 0 đ");
    private final JLabel phaiThuBuoc6 = new JLabel("Tổng phải thu: 0 đ");
    private final JLabel thucThuBuoc6 = new JLabel("Đóng trước: 0 đ");
    private final JLabel conNoBuoc6 = new JLabel("Còn nợ: 0 đ");
    private final JPanel danhSachUuDai = new JPanel();
    private final JTextArea chiTietTien = new JTextArea(6, 42);
    private final JComboBox<String> cboThanhToan = new JComboBox<>(new String[]{
            "Chưa đóng", "Đóng một phần", "Đóng toàn bộ"});
    private final JTextField tienThucThu = new JTextField(13);
    private final JComboBox<String> hinhThucThu = new JComboBox<>(new String[]{
            "Tiền mặt", "Chuyển khoản", "Khác"});
    private final JComboBox<TaiChinhDTO.LuaChon> taiKhoanThu = new JComboBox<>();
    private final JTextField ghiChuThu = new JTextField(24);
    private final Map<Integer, JCheckBox> oUuDai = new LinkedHashMap<>();
    private List<ChuongTrinhGiamGia> uuDai = List.of();
    private boolean giamGiaSanSang;
    private GiamGiaService.BangGia bangGiaHienTai;
    private boolean giaHopLe;
    private final JLabel thongBao = new JLabel("Đang tải dữ liệu...");
    private final Map<Integer, LuaChonMoi> moiChon = new LinkedHashMap<>();
    private final Map<Integer, JCheckBox> oKhoa = new LinkedHashMap<>();
    private final List<DongDaChon> dongBang = new ArrayList<>();
    private List<KhachTiemNang> hoSo = List.of();
    private List<HocVien> hocVien = List.of();
    private List<KhoaHoc> khoaHoc = List.of();
    private List<ThongTinLop> lopHoc = List.of();
    private List<DangKyKhoaHoc> dangKy = List.of();
    private Nguoi nguoiDangChon;
    private boolean dangCapNhat;
    private boolean canLamMoiNguoi;
    private boolean dangLuu;
    private boolean vuaLuuDangKy;

    private record Nguoi(KhachTiemNang hoSo, HocVien hocVien) {
        int ma() { return hoSo != null ? hoSo.maKhach() : hocVien.maHocVien(); }
        String ten() { return hoSo != null ? hoSo.hoTen() : hocVien.hoTen(); }
        String dienThoai() { return hoSo != null ? hoSo.soDienThoai() : hocVien.soDienThoaiLienHe(); }
        @Override public String toString() {
            return "#" + ma() + " · " + ten() + (dienThoai() == null || dienThoai().isBlank()
                    ? "" : " · " + dienThoai());
        }
    }
    private record DuLieu(List<KhachTiemNang> hoSo, List<HocVien> hocVien,
            List<KhoaHoc> khoaHoc, List<ThongTinLop> lopHoc, List<DangKyKhoaHoc> dangKy,
            boolean giamGiaSanSang, List<ChuongTrinhGiamGia> uuDai,
            List<TaiChinhDTO.LuaChon> taiKhoanThu) { }
    private static final class LuaChonMoi {
        final KhoaHoc khoa;
        Integer maLop;
        LuaChonMoi(KhoaHoc khoa) { this.khoa = khoa; }
    }
    private record DongDaChon(KhoaHoc khoa, DangKyKhoaHoc daLuu, LuaChonMoi moi) { }
    private record LopChon(ThongTinLop lop) {
        @Override public String toString() {
            if (lop == null) return "Chờ xếp lớp";
            return lop.tenLop() + "  ·  " + lop.soDaGiuCho() + "/" + siSoToiDa(lop);
        }
    }

    private DangKyKhoaHocForm(Component parent, JDialog dialog, KhachTiemNang hoSoDau,
            HocVien hocVienDau, Consumer<DangKyKhoaHoc.KetQua> onSaved) {
        super(new BorderLayout());
        this.parent = parent;
        this.dialog = dialog;
        this.hoSoDau = hoSoDau;
        this.hocVienDau = hocVienDau;
        this.onSaved = onSaved;
        setBackground(NEN);
        add(taoDauTrang(), BorderLayout.NORTH);
        cacBuoc.setBackground(NEN);
        cacBuoc.add(cuonBuoc(buocNoiDung("1. Xác nhận thông tin người đăng ký", taoNguoi(),
                "Kiểm tra đúng thông tin trước khi chọn khóa học.")), "1");
        cacBuoc.add(cuonBuoc(buocNoiDung("2. Chọn nhiều khóa học · Thêm vào giỏ", taoGio(),
                "Các khóa đã đăng ký trước đó không được thêm trùng.")), "2");
        cacBuoc.add(cuonBuoc(buocNoiDung("3. Xếp lớp riêng cho từng khóa học", taoXepLop(),
                "Có thể để một khóa chờ, khóa còn lại xếp ngay. Chỉ chọn lớp đủ điều kiện nhận.")), "3");
        cacBuoc.add(cuonBuoc(buocNoiDung("4. Áp dụng chương trình giảm giá", taoGiamGia(),
                "Ưu đãi được chốt khi lưu đăng ký; bảng dưới đây tính từ dữ liệu hiện có.")), "4");
        cacBuoc.add(cuonBuoc(buocNoiDung("5. Thanh toán học phí", taoThanhToan(),
                "Có thể đóng 0 đ để lưu đăng ký trước. Phiếu thu chỉ tạo khi có tiền thực thu.")), "5");
        cacBuoc.add(cuonBuoc(buocNoiDung("6. Kiểm tra thông tin và xác nhận", khuXacNhan(),
                "Xác nhận sẽ ghi đăng ký và khoản thu (nếu có) qua giao dịch SQL.")), "6");
        add(cacBuoc, BorderLayout.CENTER);
        add(taoTongQuan(), BorderLayout.SOUTH);
        cboNguon.addActionListener(e -> capNhatNguoi());
        cboNguoi.addActionListener(e -> chonNguoi());
        ngheTim(timNguoi, this::capNhatNguoi);
        cboMon.addActionListener(e -> locKhoa());
        ngheTim(timKhoa, this::locKhoa);
        bangDaChon.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) capNhatLopChoDong();
        });
        bangDaChon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int col = bangDaChon.columnAtPoint(e.getPoint());
                if (e.getClickCount() == 1 && col >= 0
                        && bangDaChon.convertColumnIndexToModel(col) == 6) {
                    thucHienCotThaoTac();
                }
            }
        });
        cboLop.addActionListener(e -> chonLop());
        btnCho.addActionListener(e -> {
            DongDaChon dong = dongDangChon();
            if (dong != null && dong.moi() != null) {
                dong.moi().maLop = null;
                capNhatLopChoDong();
                capNhatBang();
            }
        });
        btnXep.addActionListener(e -> xepLopDaLuu());
        btnLuu.addActionListener(e -> luuDangKy());
        btnLui.addActionListener(e -> hienBuoc(buoc - 1));
        btnTiep.addActionListener(e -> {
            if (buoc == 1 && nguoiDangChon == null) {
                JOptionPane.showMessageDialog(dialog, "Hãy chọn hồ sơ hoặc học viên trước khi tiếp tục."); return;
            }
            if (buoc == 2 && moiChon.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Hãy chọn ít nhất một khóa học."); return;
            }
            if (buoc >= 4 && !giaHopLe) {
                JOptionPane.showMessageDialog(dialog, "Tạm tính chưa hợp lệ. Hãy kiểm tra giảm giá và thanh toán."); return;
            }
            hienBuoc(buoc + 1);
        });
        cboThanhToan.addActionListener(e -> capNhatTaiChinh());
        ngheTim(tienThucThu, this::capNhatTaiChinh);
        btnChuyen.addActionListener(e -> {
            if (nguoiDangChon != null && nguoiDangChon.hoSo() != null) {
                moChuyenThanhHocVien(this, nguoiDangChon.hoSo(), () -> {
                    thongBaoThayDoi(true);
                    taiDuLieu();
                });
            }
        });
        if (hoSoDau != null || hocVienDau != null) {
            cboNguon.setSelectedIndex(hocVienDau == null ? 0 : 1);
            cboNguon.setEnabled(false);
            cboNguoi.setEnabled(false);
            timNguoi.setEnabled(false);
        }
        capNhatNut();
        hienBuoc(1);
        taiDuLieu();
    }

    private JScrollPane cuonBuoc(JPanel noiDung) {
        JScrollPane cuon = new JScrollPane(noiDung);
        cuon.setBorder(null);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        return cuon;
    }

    private JPanel taoDauTrang() {
        JPanel dau = new JPanel(new BorderLayout());
        JPanel tieuDe = new JPanel(new BorderLayout());
        tieuDe.setBackground(DAM);
        tieuDe.setPreferredSize(new Dimension(1, 56));
        JLabel ten = new JLabel("  VINABACUS · ĐĂNG KÝ NHIỀU KHÓA HỌC");
        ten.setFont(GiaoDienMau.font(Font.BOLD, 19));
        ten.setForeground(Color.WHITE);
        tieuDe.add(ten, BorderLayout.CENTER);
        JPanel vang = new JPanel();
        vang.setBackground(VANG);
        vang.setPreferredSize(new Dimension(180, 1));
        tieuDe.add(vang, BorderLayout.EAST);
        dau.add(tieuDe, BorderLayout.NORTH);
        JPanel tienTrinh = new JPanel(new GridLayout(1, 6, 6, 0));
        tienTrinh.setBackground(Color.WHITE);
        tienTrinh.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        for (int i = 0; i < 6; i++) {
            JPanel o = new JPanel(new BorderLayout());
            JLabel nhan = new JLabel((i + 1) + ". " + TEN_BUOC[i], JLabel.CENTER);
            nhan.setFont(GiaoDienMau.font(Font.BOLD, 12));
            o.add(nhan);
            dauBuoc[i] = o;
            nhanBuoc[i] = nhan;
            tienTrinh.add(o);
        }
        dau.add(tienTrinh, BorderLayout.CENTER);
        JPanel banner = new JPanel(new GridLayout(1, 3, 8, 0));
        banner.setBackground(XANH_NHAT);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN), BorderFactory.createEmptyBorder(8, 13, 8, 13)));
        for (JLabel nhan : new JLabel[]{tenNguoiBanner, dienThoaiBanner, trangThaiBanner}) {
            nhan.setFont(GiaoDienMau.font(Font.BOLD, 12));
            nhan.setForeground(DAM);
            banner.add(nhan);
        }
        JPanel bao = new JPanel(new BorderLayout());
        bao.setBackground(NEN);
        bao.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        bao.add(banner);
        dau.add(bao, BorderLayout.SOUTH);
        return dau;
    }

    private JPanel buocNoiDung(String ten, JPanel noiDung, String goiY) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(NEN);
        p.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel tieuDe = new JLabel(ten);
        tieuDe.setFont(GiaoDienMau.font(Font.PLAIN, 17));
        tieuDe.setForeground(DAM);
        p.add(tieuDe, BorderLayout.NORTH);
        noiDung.setPreferredSize(new Dimension(1140, 365));
        p.add(noiDung, BorderLayout.CENTER);
        JLabel nhan = new JLabel(goiY);
        nhan.setForeground(new Color(118, 83, 0));
        nhan.setFont(GiaoDienMau.font(Font.PLAIN, 11));
        nhan.setBackground(new Color(255, 247, 215));
        nhan.setOpaque(true);
        nhan.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(245, 208, 83)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        p.add(nhan, BorderLayout.SOUTH);
        return p;
    }

    private static JPanel the(String ten, Component noiDung) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createLineBorder(VIEN));
        JLabel dau = new JLabel(ten);
        dau.setFont(GiaoDienMau.font(Font.BOLD, 13));
        dau.setForeground(DAM);
        dau.setBackground(XANH_NHAT);
        dau.setOpaque(true);
        dau.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        p.add(dau, BorderLayout.NORTH);
        if (noiDung != null) p.add(noiDung, BorderLayout.CENTER);
        return p;
    }

    private static JPanel truong(String ten, Component giaTri) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel nhan = new JLabel(ten);
        nhan.setForeground(DAM);
        nhan.setFont(GiaoDienMau.font(Font.PLAIN, 11));
        p.add(nhan, BorderLayout.NORTH);
        p.add(giaTri, BorderLayout.CENTER);
        return p;
    }

    private static JLabel giaTri(JLabel nhan) {
        nhan.setFont(GiaoDienMau.font(Font.PLAIN, 13));
        nhan.setBackground(Color.WHITE);
        nhan.setOpaque(true);
        nhan.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(VIEN),
                BorderFactory.createEmptyBorder(3, 9, 3, 9)));
        return nhan;
    }

    private static JPanel dongGia(String ten, JLabel gia) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, VIEN));
        p.add(new JLabel(ten), BorderLayout.WEST);
        gia.setForeground(DAM);
        gia.setFont(GiaoDienMau.font(Font.BOLD, 14));
        p.add(gia, BorderLayout.EAST);
        return p;
    }

    private JPanel khuXacNhan() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(NEN);
        JTable bang = new JTable(modelXacNhan);
        dinhDangBang(bang);
        int[] rong = {360, 250, 130, 130, 130};
        for (int i = 0; i < rong.length; i++) bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        p.add(the("Đăng ký và xếp lớp", new JScrollPane(bang)), BorderLayout.CENTER);
        JPanel duoi = new JPanel(new GridLayout(1, 2, 12, 0));
        duoi.setOpaque(false);
        JPanel tien = new JPanel(new GridLayout(3, 1, 0, 6));
        tien.setBackground(new Color(244, 250, 244));
        tien.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        tien.add(phaiThuBuoc6); tien.add(thucThuBuoc6); tien.add(conNoBuoc6);
        duoi.add(the("Tổng hợp thanh toán", tien));
        JPanel ket = new JPanel(new BorderLayout());
        ket.setBackground(Color.WHITE);
        JLabel nhan = new JLabel("Kiểm tra bảng đăng ký rồi bấm Lưu đăng ký bên dưới.");
        nhan.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        ket.add(nhan, BorderLayout.NORTH);
        duoi.add(the("Thao tác cuối", ket));
        duoi.setPreferredSize(new Dimension(1, 130));
        p.add(duoi, BorderLayout.SOUTH);
        return p;
    }

    private void hienBuoc(int moi) {
        if (moi < 1 || moi > 6) return;
        buoc = moi;
        boCucBuoc.show(cacBuoc, Integer.toString(buoc));
        for (int i = 0; i < 6; i++) {
            dauBuoc[i].setBackground(i + 1 == buoc ? VANG : i + 1 < buoc ? XANH_NHAT : new Color(239, 242, 242));
            nhanBuoc[i].setForeground(DAM);
        }
        btnLui.setEnabled(buoc > 1);
        btnTiep.setVisible(buoc < 6);
        btnLuu.setVisible(buoc == 6);
        btnChuyen.setVisible(buoc == 6 && vuaLuuDangKy);
        if (buoc == 6) capNhatXacNhan();
    }

    private void capNhatXacNhan() {
        modelXacNhan.setRowCount(0);
        for (LuaChonMoi m : moiChon.values()) {
            ThongTinLop lop = timLop(m.maLop);
            GiamGiaService.Dong gia = bangGiaHienTai == null ? null
                    : bangGiaHienTai.choKhoa(m.khoa.maKhoaHoc());
            modelXacNhan.addRow(new Object[]{m.khoa.tenMonHoc() + " · " + m.khoa.tenKhoaHoc(),
                lop == null ? "Chờ xếp lớp" : lop.tenLop(),
                gia == null ? "—" : tien(gia.hocPhiGoc()),
                gia == null ? "—" : tien(gia.tongGiam()),
                gia == null ? "—" : tien(gia.phaiThu())});
        }
    }

    private JPanel taoNguoi() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(NEN);
        JPanel trai = new JPanel(new BorderLayout(0, 10));
        trai.setOpaque(false);
        JPanel chon = new JPanel(new GridLayout(1, 3, 9, 0));
        chon.setBackground(Color.WHITE);
        chon.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        chon.add(truong("Nguồn hồ sơ", cboNguon));
        chon.add(truong("Tìm mã / tên / số điện thoại", timNguoi));
        chon.add(truong("Chọn người đăng ký", cboNguoi));
        JPanel duLieu = new JPanel(new GridLayout(3, 2, 10, 10));
        duLieu.setBackground(Color.WHITE);
        duLieu.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        duLieu.add(truong("Mã hồ sơ / học viên", giaTri(maHoSo)));
        duLieu.add(truong("Họ và tên", giaTri(hoTen)));
        duLieu.add(truong("Ngày sinh", giaTri(ngaySinh)));
        duLieu.add(truong("Giới tính", giaTri(gioiTinh)));
        duLieu.add(truong("Số điện thoại", giaTri(soDienThoai)));
        duLieu.add(truong("Người liên hệ", giaTri(nguoiLienHe)));
        JPanel noiDungThongTin = new JPanel(new BorderLayout(0, 3));
        noiDungThongTin.setBackground(Color.WHITE);
        noiDungThongTin.add(chon, BorderLayout.NORTH);
        noiDungThongTin.add(duLieu, BorderLayout.CENTER);
        trai.add(the("Thông tin học viên", noiDungThongTin), BorderLayout.CENTER);
        JPanel ngay = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 9));
        ngay.setBackground(Color.WHITE);
        ngay.add(new JLabel("Ngày bắt đầu đăng ký:")); ngay.add(ngayBatDau);
        trai.add(the("Ngày bắt đầu đăng ký", ngay), BorderLayout.SOUTH);
        p.add(trai, BorderLayout.CENTER);
        JPanel tomTat = new JPanel(new GridLayout(4, 1, 0, 5));
        tomTat.setBackground(Color.WHITE);
        tomTat.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tomTat.add(dongGia("Mã hồ sơ", maHoSoTomTat));
        tomTat.add(dongGia("Nhu cầu", nhuCau));
        tomTat.add(dongGia("Trạng thái", trangThai));
        JLabel luuY = new JLabel("Chọn đúng người trước khi thêm khóa học.");
        luuY.setOpaque(true); luuY.setBackground(new Color(255, 247, 215));
        luuY.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tomTat.add(luuY);
        JPanel phai = the("Tóm tắt hồ sơ", tomTat);
        phai.setPreferredSize(new Dimension(320, 1));
        p.add(phai, BorderLayout.EAST);
        return p;
    }

    private JPanel taoGio() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(NEN);
        p.add(taoKhoa(), BorderLayout.CENTER);
        gioKhoaCards.setLayout(new BoxLayout(gioKhoaCards, BoxLayout.Y_AXIS));
        gioKhoaCards.setBackground(Color.WHITE);
        JPanel noi = new JPanel(new BorderLayout());
        noi.setBackground(Color.WHITE);
        noi.add(new JScrollPane(gioKhoaCards), BorderLayout.CENTER);
        JPanel tong = new JPanel(new BorderLayout());
        tong.setBackground(Color.WHITE);
        tong.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        tong.add(new JLabel("Tổng học phí"), BorderLayout.WEST);
        tongGio.setForeground(DAM);
        tongGio.setFont(GiaoDienMau.font(Font.BOLD, 18));
        tong.add(tongGio, BorderLayout.EAST);
        noi.add(tong, BorderLayout.SOUTH);
        JPanel phai = the("Giỏ khóa học", noi);
        phai.setPreferredSize(new Dimension(330, 1));
        p.add(phai, BorderLayout.EAST);
        return p;
    }

    private JPanel taoKhoa() {
        JPanel khu = new JPanel(new BorderLayout(0, 8));
        khu.setBackground(Color.WHITE);
        JPanel loc = new JPanel(new GridLayout(1, 2, 8, 0));
        loc.setBackground(Color.WHITE);
        loc.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        loc.add(truong("Môn học", cboMon)); loc.add(truong("Tìm khóa học", timKhoa));
        khu.add(loc, BorderLayout.NORTH);
        danhSachKhoa.setLayout(new BoxLayout(danhSachKhoa, BoxLayout.Y_AXIS));
        danhSachKhoa.setBackground(Color.WHITE);
        JScrollPane cuon = new JScrollPane(danhSachKhoa);
        cuon.setBorder(null);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        khu.add(cuon, BorderLayout.CENTER);
        return the("Danh sách khóa học đang mở", khu);
    }

    private JPanel taoXepLop() {
        JPanel p = new JPanel(new BorderLayout(0, 11));
        p.setBackground(NEN);
        JPanel bang = taoBang();
        bang.setPreferredSize(new Dimension(1, 185));
        p.add(bang, BorderLayout.NORTH);
        p.add(taoLop(), BorderLayout.CENTER);
        return p;
    }

    private static void dinhDangBang(JTable bang) {
        bang.setRowHeight(37);
        bang.setShowVerticalLines(false);
        bang.setGridColor(VIEN);
        bang.setSelectionBackground(XANH_NHAT);
        bang.setSelectionForeground(DAM);
        bang.getTableHeader().setBackground(DAM);
        bang.getTableHeader().setForeground(Color.WHITE);
        bang.getTableHeader().setFont(GiaoDienMau.font(Font.BOLD, 12));
        bang.getTableHeader().setReorderingAllowed(false);
    }

    private JPanel taoBang() {
        JPanel khu = new JPanel(new BorderLayout());
        khu.setBackground(Color.WHITE);
        dinhDangBang(bangDaChon);
        bangDaChon.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int i : new int[]{9, 8, 7, 5, 3, 0})
            bangDaChon.removeColumn(bangDaChon.getColumnModel().getColumn(i));
        int[] rong = {280, 80, 190, 135};
        for (int i = 0; i < rong.length; i++) bangDaChon.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        khu.add(new JScrollPane(bangDaChon), BorderLayout.CENTER);
        return the("Danh sách cần xếp · Chọn một khóa để xem lớp", khu);
    }

    private JPanel taoLop() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setBackground(NEN);
        JPanel chon = new JPanel(new BorderLayout(0, 7));
        chon.setBackground(Color.WHITE);
        chon.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        danhSachLopChon.setLayout(new BoxLayout(danhSachLopChon, BoxLayout.Y_AXIS));
        danhSachLopChon.setBackground(Color.WHITE);
        JScrollPane cuonLop = new JScrollPane(danhSachLopChon);
        cuonLop.setBorder(null);
        chon.add(cuonLop, BorderLayout.CENTER);
        JPanel xuLy = new JPanel(new GridLayout(2, 1));
        xuLy.setOpaque(false);
        JPanel ngay = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ngay.setOpaque(false);
        ngay.add(new JLabel("Ngày xếp:")); ngay.add(ngayXep);
        xuLy.add(ngay);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        nut.setOpaque(false);
        nut.add(btnCho); nut.add(btnXep);
        xuLy.add(nut);
        chon.add(xuLy, BorderLayout.SOUTH);
        p.add(the("Danh sách lớp phù hợp", chon));
        JPanel chiTiet = new JPanel(new GridLayout(4, 1, 0, 8));
        chiTiet.setBackground(Color.WHITE);
        chiTiet.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        chiTiet.add(dongGia("Phòng", lopPhong));
        chiTiet.add(dongGia("Giáo viên", lopGiaoVien));
        chiTiet.add(dongGia("Sĩ số", lopSiSo));
        chiTiet.add(dongGia("Lịch học", lopThoiGian));
        p.add(the("Thông tin lớp đã chọn", chiTiet));
        return p;
    }

    private JPanel taoGiamGia() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(NEN);
        danhSachUuDai.setLayout(new BoxLayout(danhSachUuDai, BoxLayout.Y_AXIS));
        danhSachUuDai.setBackground(Color.WHITE);
        p.add(the("Ưu đãi hợp lệ", new JScrollPane(danhSachUuDai)), BorderLayout.CENTER);
        JPanel tien = new JPanel(new BorderLayout(0, 9));
        tien.setBackground(Color.WHITE);
        JTable bang = new JTable(modelGia);
        dinhDangBang(bang);
        int[] rong = {170, 78, 75, 80};
        for (int i = 0; i < rong.length; i++) bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        tien.add(new JScrollPane(bang), BorderLayout.CENTER);
        JPanel tong = new JPanel(new GridLayout(3, 1, 0, 6));
        tong.setBackground(Color.WHITE);
        tong.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        tong.add(dongGia("Học phí gốc", tongGocGiam));
        tong.add(dongGia("Giảm giá", tongGiam));
        tong.add(dongGia("Phải thu", tongPhaiThu));
        tien.add(tong, BorderLayout.SOUTH);
        JPanel phai = the("Tạm tính học phí", tien);
        phai.setPreferredSize(new Dimension(410, 1));
        p.add(phai, BorderLayout.EAST);
        return p;
    }

    private JPanel taoThanhToan() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(NEN);
        JPanel cards = new JPanel(new BorderLayout(12, 0));
        cards.setBackground(NEN);
        cards.setPreferredSize(new Dimension(1, 315));
        JPanel nhap = new JPanel(new GridLayout(3, 2, 12, 15));
        nhap.setBackground(Color.WHITE);
        nhap.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        nhap.add(truong("Trạng thái", cboThanhToan));
        nhap.add(truong("Số tiền đóng lần này (đ)", tienThucThu));
        nhap.add(truong("Phương thức", hinhThucThu));
        nhap.add(truong("Tài khoản thu", taiKhoanThu));
        nhap.add(truong("Ghi chú phiếu thu", ghiChuThu));
        JPanel nhapBoc = new JPanel(new BorderLayout());
        nhapBoc.setBackground(Color.WHITE);
        nhapBoc.add(nhap, BorderLayout.NORTH);
        cards.add(the("Thông tin thu tiền", nhapBoc), BorderLayout.CENTER);
        JPanel tong = new JPanel(new GridLayout(5, 1, 0, 10));
        tong.setBackground(Color.WHITE);
        tong.setBorder(BorderFactory.createEmptyBorder(15, 14, 15, 14));
        tong.add(hocPhiBuoc5);
        tong.add(giamGiaBuoc5);
        tong.add(phaiThuBuoc5);
        tong.add(thucThuBuoc5);
        conNoBuoc5.setFont(GiaoDienMau.font(Font.BOLD, 19));
        conNoBuoc5.setForeground(DAM);
        tong.add(conNoBuoc5);
        JPanel tongBoc = new JPanel(new BorderLayout());
        tongBoc.setBackground(Color.WHITE);
        tongBoc.add(tong, BorderLayout.NORTH);
        JPanel phai = the("Chi tiết thanh toán", tongBoc);
        phai.setPreferredSize(new Dimension(365, 1));
        cards.add(phai, BorderLayout.EAST);
        p.add(cards, BorderLayout.NORTH);
        return p;
    }

    private JPanel taoTongQuan() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, VIEN));
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 7));
        nut.setOpaque(false);
        JButton btnDong = new JButton("Đóng");
        btnDong.addActionListener(e -> dialog.dispose());
        btnLuu.setBackground(DAM); btnLuu.setForeground(Color.WHITE);
        btnTiep.setBackground(VANG); btnTiep.setForeground(DAM);
        btnTiep.setOpaque(true);
        btnTiep.setFont(GiaoDienMau.font(Font.BOLD, 13));
        nut.add(btnLui); nut.add(btnTiep); nut.add(btnLuu); nut.add(btnChuyen); nut.add(btnDong);
        p.add(nut, BorderLayout.EAST);
        thongBao.setForeground(DAM);
        thongBao.setFont(GiaoDienMau.font(Font.PLAIN, 11));
        thongBao.setBorder(BorderFactory.createEmptyBorder(0, 17, 0, 0));
        p.add(thongBao, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(1, 51));
        return p;
    }

    private static JPanel khu(String ten, java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GiaoDienMau.DUONG_VIEN),
                        ten), BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        return panel;
    }

    private static void ngheTim(JTextField txt, Runnable task) {
        txt.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { task.run(); }
            @Override public void removeUpdate(DocumentEvent e) { task.run(); }
            @Override public void changedUpdate(DocumentEvent e) { task.run(); }
        });
    }

    private void taiDuLieu() {
        thongBao.setText("Đang tải hồ sơ, khóa học và lớp học...");
        btnLuu.setEnabled(false);
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                boolean coUuDai = controller.giamGiaSanSang();
                return new DuLieu(controller.hoSoTuyenSinh(), controller.hocVien(),
                        controller.khoaHoc(), controller.thongTinLop(),
                        controller.timKiem("", null), coUuDai,
                        coUuDai ? controller.chuongTrinhGiamGia() : List.of(),
                        controller.taiKhoanThu());
            }
            @Override protected void done() {
                try {
                    DuLieu dl = get();
                    hoSo = dl.hoSo(); hocVien = dl.hocVien(); khoaHoc = dl.khoaHoc();
                    lopHoc = dl.lopHoc(); dangKy = dl.dangKy();
                    giamGiaSanSang = dl.giamGiaSanSang(); uuDai = dl.uuDai();
                    taiKhoanThu.removeAllItems();
                    for (TaiChinhDTO.LuaChon tk : dl.taiKhoanThu()) taiKhoanThu.addItem(tk);
                    napUuDai();
                    napMon();
                    canLamMoiNguoi = true;
                    capNhatNguoi();
                    canLamMoiNguoi = false;
                    taoTheKhoa();
                    locKhoa();
                    capNhatBang();
                    thongBao.setText(giamGiaSanSang
                            ? "Dữ liệu đã cập nhật. Mỗi khóa được lưu và xếp lớp độc lập."
                            : "CSDL chưa áp dụng migration giảm giá; đăng ký và thu học phí gốc vẫn dùng được.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(dialog, ex);
                } catch (ExecutionException ex) {
                    hienLoi(dialog, ex);
                }
            }
        }.execute();
    }

    private void napMon() {
        Object dangChonMon = cboMon.getSelectedItem();
        cboMon.removeAllItems();
        cboMon.addItem("Tất cả môn");
        khoaHoc.stream().map(KhoaHoc::tenMonHoc).distinct().sorted()
                .forEach(cboMon::addItem);
        if (dangChonMon != null) cboMon.setSelectedItem(dangChonMon);
    }

    private void napUuDai() {
        Set<Integer> daChon = new HashSet<>();
        oUuDai.forEach((id, o) -> { if (o.isSelected()) daChon.add(id); });
        oUuDai.clear(); danhSachUuDai.removeAll();
        if (!giamGiaSanSang) {
            danhSachUuDai.add(new JLabel("Chưa có migration Giai đoạn 3; chưa thể áp dụng ưu đãi."));
        } else if (uuDai.isEmpty()) {
            danhSachUuDai.add(new JLabel("Chưa có chương trình giảm giá nào cho Trung tâm."));
        } else for (ChuongTrinhGiamGia ct : uuDai) {
            String tenGiaTri = "PHAN_TRAM".equals(ct.loaiGiam())
                    ? ct.giaTri().stripTrailingZeros().toPlainString() + "%"
                    : tien(ct.giaTri());
            JCheckBox o = new JCheckBox("#" + ct.maChuongTrinh() + " · " + ct.ten()
                    + " · " + tenGiaTri + " · "
                    + (ct.apDungTatCaMon() ? "Tất cả môn" : ct.maMonHoc().size() + " môn")
                    + (ct.choPhepKetHop() ? " · Có thể kết hợp" : " · Không kết hợp"));
            o.setEnabled(ct.hieuLuc(LocalDate.now()));
            o.setSelected(daChon.contains(ct.maChuongTrinh()) && o.isEnabled());
            o.setToolTipText(o.isEnabled() ? "Chỉ giảm trên khóa đủ điều kiện"
                    : "Chưa đến hạn, hết hạn hoặc đã ngừng áp dụng");
            o.addActionListener(e -> capNhatTaiChinh());
            oUuDai.put(ct.maChuongTrinh(), o);
            o.setBackground(Color.WHITE);
            o.setFont(GiaoDienMau.font(Font.BOLD, 13));
            JPanel the = new JPanel(new BorderLayout());
            the.setBackground(Color.WHITE);
            the.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(VIEN),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            the.add(o, BorderLayout.NORTH);
            JLabel moTa = new JLabel(ct.hieuLuc(LocalDate.now())
                    ? (ct.apDungTatCaMon() ? "Áp dụng tất cả môn" : "Áp dụng cho " + ct.maMonHoc().size() + " môn")
                        + (ct.choPhepKetHop() ? " · Có thể kết hợp" : " · Không kết hợp")
                    : "Chưa có hiệu lực hoặc đã ngừng áp dụng");
            moTa.setForeground(GiaoDienMau.CHU_MO);
            the.add(moTa, BorderLayout.SOUTH);
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setBackground(Color.WHITE);
            wrap.setBorder(BorderFactory.createEmptyBorder(9, 11, 0, 11));
            wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
            wrap.add(the);
            danhSachUuDai.add(wrap);
        }
        danhSachUuDai.revalidate(); danhSachUuDai.repaint();
    }

    private List<ChuongTrinhGiamGia> uuDaiDaChon() {
        return uuDai.stream().filter(ct -> {
            JCheckBox o = oUuDai.get(ct.maChuongTrinh());
            return o != null && o.isSelected();
        }).toList();
    }

    private BigDecimal tienDangThu() {
        if (bangGiaHienTai == null || cboThanhToan.getSelectedIndex() == 0)
            return BigDecimal.ZERO;
        if (cboThanhToan.getSelectedIndex() == 2) return bangGiaHienTai.phaiThu();
        BigDecimal so;
        try { so = new BigDecimal(tienThucThu.getText().trim()); }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Hãy nhập số tiền thực thu hợp lệ, không cần dấu phân cách");
        }
        if (so.signum() <= 0 || so.scale() > 2
                || so.compareTo(bangGiaHienTai.phaiThu()) > 0)
            throw new IllegalArgumentException("Tiền đóng một phần phải lớn hơn 0 và không vượt phải thu");
        return so;
    }

    private void capNhatTaiChinh() {
        tienThucThu.setEnabled(cboThanhToan.getSelectedIndex() == 1);
        hinhThucThu.setEnabled(cboThanhToan.getSelectedIndex() != 0);
        taiKhoanThu.setEnabled(cboThanhToan.getSelectedIndex() != 0);
        ghiChuThu.setEnabled(cboThanhToan.getSelectedIndex() != 0);
        bangGiaHienTai = null;
        giaHopLe = false;
        modelGia.setRowCount(0);
        if (moiChon.isEmpty()) {
            tongHocPhi.setText("Khóa chưa lưu: Học phí gốc 0 đ  ·  Giảm 0 đ  ·  Phải thu 0 đ");
            chiTietTien.setText("Chọn ít nhất một khóa để xem tạm tính học phí.");
            phaiThuBuoc5.setText("Tổng phải thu: 0 đ");
            thucThuBuoc5.setText("Đóng lần này: 0 đ");
            conNoBuoc5.setText("Còn nợ: 0 đ");
            hocPhiBuoc5.setText("Học phí gốc: 0 đ");
            giamGiaBuoc5.setText("Giảm giá: 0 đ");
            phaiThuBuoc6.setText("Tổng phải thu: 0 đ");
            thucThuBuoc6.setText("Đóng trước: 0 đ");
            conNoBuoc6.setText("Còn nợ: 0 đ");
            tongGocGiam.setText("0 đ"); tongGiam.setText("0 đ"); tongPhaiThu.setText("0 đ");
            capNhatNut(); return;
        }
        try {
            bangGiaHienTai = GiamGiaService.tinh(moiChon.values().stream()
                    .map(m -> m.khoa).toList(), uuDaiDaChon(), LocalDate.now());
            BigDecimal thu = tienDangThu();
            if (thu.signum() > 0 && taiKhoanThu.getSelectedItem() == null)
                throw new IllegalArgumentException("Chưa có tài khoản thu đang hoạt động tại Trung tâm");
            Map<Integer, BigDecimal> phaiThu = new LinkedHashMap<>();
            for (GiamGiaService.Dong dong : bangGiaHienTai.dong())
                phaiThu.put(dong.maKhoaHoc(), dong.phaiThu());
            Map<Integer, BigDecimal> phanBo = PhanBoThanhToanService.tinh(phaiThu, thu);
            StringBuilder chiTiet = new StringBuilder();
            for (GiamGiaService.Dong dong : bangGiaHienTai.dong()) {
                KhoaHoc khoa = moiChon.get(dong.maKhoaHoc()).khoa;
                modelGia.addRow(new Object[]{khoa.tenMonHoc() + " · " + khoa.tenKhoaHoc(),
                    tien(dong.hocPhiGoc()), tien(dong.tongGiam()), tien(dong.phaiThu())});
                chiTiet.append(khoa.tenKhoaHoc()).append(": gốc ")
                        .append(tien(dong.hocPhiGoc())).append(" · giảm ")
                        .append(tien(dong.tongGiam())).append(" · phải thu ")
                        .append(tien(dong.phaiThu())).append(" · đóng lần này ")
                        .append(tien(phanBo.get(dong.maKhoaHoc()))).append('\n');
            }
            chiTiet.append("\nTổng: gốc ").append(tien(bangGiaHienTai.hocPhiGoc()))
                    .append(" · giảm ").append(tien(bangGiaHienTai.tongGiam()))
                    .append(" · phải thu ").append(tien(bangGiaHienTai.phaiThu()))
                    .append(" · thực thu ").append(tien(thu))
                    .append(" · còn nợ ").append(tien(bangGiaHienTai.phaiThu().subtract(thu)));
            chiTietTien.setText(chiTiet.toString());
            tongHocPhi.setText("Khóa chưa lưu: Gốc " + tien(bangGiaHienTai.hocPhiGoc())
                    + "  ·  Giảm " + tien(bangGiaHienTai.tongGiam())
                    + "  ·  Phải thu " + tien(bangGiaHienTai.phaiThu())
                    + "  ·  Đã thu " + tien(thu)
                    + "  ·  Còn nợ " + tien(bangGiaHienTai.phaiThu().subtract(thu)));
            phaiThuBuoc5.setText("Tổng phải thu: " + tien(bangGiaHienTai.phaiThu()));
            thucThuBuoc5.setText("Đóng lần này: " + tien(thu));
            conNoBuoc5.setText("Còn nợ: " + tien(bangGiaHienTai.phaiThu().subtract(thu)));
            hocPhiBuoc5.setText("Học phí gốc: " + tien(bangGiaHienTai.hocPhiGoc()));
            giamGiaBuoc5.setText("Giảm giá: −" + tien(bangGiaHienTai.tongGiam()));
            phaiThuBuoc6.setText("Tổng phải thu: " + tien(bangGiaHienTai.phaiThu()));
            thucThuBuoc6.setText("Đóng trước: " + tien(thu));
            conNoBuoc6.setText("Còn nợ: " + tien(bangGiaHienTai.phaiThu().subtract(thu)));
            tongGocGiam.setText(tien(bangGiaHienTai.hocPhiGoc()));
            tongGiam.setText("−" + tien(bangGiaHienTai.tongGiam()));
            tongPhaiThu.setText(tien(bangGiaHienTai.phaiThu()));
            for (int i = 0; i < dongBang.size(); i++) {
                DongDaChon d = dongBang.get(i);
                if (d.moi() == null) continue;
                GiamGiaService.Dong gia = bangGiaHienTai.choKhoa(d.khoa().maKhoaHoc());
                model.setValueAt(tien(gia.hocPhiGoc()), i, 7);
                model.setValueAt(tien(gia.tongGiam()), i, 8);
                model.setValueAt(tien(gia.phaiThu()), i, 9);
            }
            giaHopLe = true;
        } catch (IllegalArgumentException ex) {
            modelGia.setRowCount(0);
            chiTietTien.setText(ex.getMessage() + "\nHãy điều chỉnh lựa chọn trước khi lưu.");
            tongHocPhi.setText("Tạm tính chưa hợp lệ: " + ex.getMessage());
            phaiThuBuoc5.setText("Tạm tính chưa hợp lệ: " + ex.getMessage());
            thucThuBuoc5.setText("Đóng lần này: —");
            conNoBuoc5.setText("Còn nợ: —");
            phaiThuBuoc6.setText("Tạm tính chưa hợp lệ");
            thucThuBuoc6.setText("Đóng trước: —");
            conNoBuoc6.setText("Còn nợ: —");
            tongGocGiam.setText("—"); tongGiam.setText("—"); tongPhaiThu.setText("—");
        }
        capNhatNut();
    }

    private void capNhatNguoi() {
        if (dangCapNhat) return;
        dangCapNhat = true;
        String q = boDau(timNguoi.getText().trim());
        boolean chonHocVien = cboNguon.getSelectedIndex() == 1;
        int maTruoc = nguoiDangChon == null ? -1 : nguoiDangChon.ma();
        cboNguoi.removeAllItems();
        if (chonHocVien) {
            for (HocVien h : hocVien) {
                Nguoi n = new Nguoi(null, h);
                if (khopNguoi(n, q)) cboNguoi.addItem(n);
            }
        } else {
            for (KhachTiemNang k : hoSo) {
                Nguoi n = new Nguoi(k, null);
                if (khopNguoi(n, q)) cboNguoi.addItem(n);
            }
        }
        int maMucTieu = hoSoDau != null && !chonHocVien ? hoSoDau.maKhach()
                : hocVienDau != null && chonHocVien ? hocVienDau.maHocVien() : maTruoc;
        for (int i = 0; i < cboNguoi.getItemCount(); i++) {
            if (cboNguoi.getItemAt(i).ma() == maMucTieu) {
                cboNguoi.setSelectedIndex(i); break;
            }
        }
        if (hoSoDau == null && hocVienDau == null && maMucTieu < 0) cboNguoi.setSelectedIndex(-1);
        dangCapNhat = false;
        chonNguoi();
    }

    private static boolean khopNguoi(Nguoi n, String q) {
        return q.isEmpty() || boDau(n.ten()).contains(q)
                || boDau(n.dienThoai()).contains(q) || String.valueOf(n.ma()).contains(q);
    }

    private void chonNguoi() {
        if (dangCapNhat) return;
        Nguoi n = (Nguoi) cboNguoi.getSelectedItem();
        if (!canLamMoiNguoi && n != null && nguoiDangChon != null
                && n.ma() == nguoiDangChon.ma()
                && (n.hoSo() == null) == (nguoiDangChon.hoSo() == null)) return;
        boolean doiNguoi = n == null || nguoiDangChon == null
                || n.ma()!=nguoiDangChon.ma()
                || (n.hoSo()==null)!=(nguoiDangChon.hoSo()==null);
        nguoiDangChon = n;
        moiChon.clear();
        if (doiNguoi) vuaLuuDangKy=false;
        if (n == null) thongTinNguoi.setText("Chọn hồ sơ tuyển sinh hoặc học viên theo mã, tên, số điện thoại.");
        else if (n.hoSo() != null) {
            KhachTiemNang k = n.hoSo();
            thongTinNguoi.setText("Hồ sơ #" + k.maKhach() + " · " + k.hoTen()
                    + " · SĐT: " + trong(k.soDienThoai())
                    + " · Phụ huynh: " + trong(k.tenNguoiLienHe())
                    + " · Sinh: " + (k.ngaySinh()==null?"Chưa có":NgaySinhPicker.hienThiNgay(k.ngaySinh()))
                    + " · Giới tính: " + trong(k.gioiTinh())
                    + " · " + tenTrangThai(k.trangThai()));
        } else {
            HocVien h = n.hocVien();
            thongTinNguoi.setText("Học viên #" + h.maHocVien() + " · " + h.hoTen()
                    + " · Liên hệ: " + trong(h.tenNguoiLienHe())
                    + " · " + trong(h.soDienThoaiLienHe())
                    + " · Sinh: " + (h.ngaySinh()==null?"Chưa có":NgaySinhPicker.hienThiNgay(h.ngaySinh()))
                    + " · Giới tính: " + trong(h.gioiTinh()));
        }
        tenNguoiBanner.setText(n == null ? "Chưa chọn người đăng ký" : "#" + n.ma() + " · " + n.ten());
        dienThoaiBanner.setText(n == null ? "Số điện thoại: —" : "SĐT: " + trong(n.dienThoai()));
        trangThaiBanner.setText(n == null ? "Hồ sơ: —" : "Hồ sơ: "
                + tenTrangThai(n.hoSo() != null ? n.hoSo().trangThai() : n.hocVien().trangThaiHoSo()));
        maHoSo.setText(n == null ? "—" : "#" + n.ma());
        maHoSoTomTat.setText(n == null ? "—" : "#" + n.ma());
        hoTen.setText(n == null ? "—" : n.ten());
        ngaySinh.setText(n == null ? "—" : n.hoSo() != null
                ? hienNgay(n.hoSo().ngaySinh()) : hienNgay(n.hocVien().ngaySinh()));
        gioiTinh.setText(n == null ? "—" : trong(n.hoSo() != null ? n.hoSo().gioiTinh() : n.hocVien().gioiTinh()));
        soDienThoai.setText(n == null ? "—" : trong(n.dienThoai()));
        nguoiLienHe.setText(n == null ? "—" : trong(n.hoSo() != null
                ? n.hoSo().tenNguoiLienHe() : n.hocVien().tenNguoiLienHe()));
        nhuCau.setText(n == null || n.hoSo() == null ? "—" : trong(n.hoSo().nhuCau()));
        trangThai.setText(n == null ? "—" : tenTrangThai(n.hoSo() != null
                ? n.hoSo().trangThai() : n.hocVien().trangThaiHoSo()));
        taoTheKhoa();
        locKhoa();
        capNhatBang();
    }

    private static String hienNgay(LocalDate ngay) {
        return ngay == null ? "Chưa cập nhật" : NgaySinhPicker.hienThiNgay(ngay);
    }

    private List<DangKyKhoaHoc> dangKyNguoi() {
        if (nguoiDangChon == null) return List.of();
        Integer maHocVienLienKet = null;
        if (nguoiDangChon.hoSo() != null) {
            for (DangKyKhoaHoc d : dangKy) {
                if (d.maKhach() != null && d.maKhach() == nguoiDangChon.ma()) {
                    maHocVienLienKet = d.maHocVien();
                    break;
                }
            }
        }
        final Integer maHocVien = maHocVienLienKet;
        return dangKy.stream().filter(d -> !"HUY".equals(d.trangThai()))
                .filter(d -> nguoiDangChon.hoSo() != null
                        ? (d.maKhach() != null && d.maKhach() == nguoiDangChon.ma())
                                || (maHocVien != null && d.maHocVien() == maHocVien)
                        : d.maHocVien() == nguoiDangChon.ma())
                .sorted(Comparator.comparingInt(DangKyKhoaHoc::maDangKy)).toList();
    }

    private void taoTheKhoa() {
        oKhoa.clear();
        danhSachKhoa.removeAll();
        if (khoaHoc.isEmpty()) {
            danhSachKhoa.add(new JLabel("Chưa có khóa học đang mở."));
        }
        for (KhoaHoc khoa : khoaHoc) {
            boolean daCo = dangKyNguoi().stream().anyMatch(d -> d.maKhoaHoc() == khoa.maKhoaHoc());
            JCheckBox o = new JCheckBox(khoa.tenMonHoc() + " · " + khoa.tenKhoaHoc());
            o.setOpaque(false);
            o.setFont(GiaoDienMau.font(Font.BOLD, 13));
            o.setSelected(moiChon.containsKey(khoa.maKhoaHoc()));
            o.setEnabled(nguoiDangChon != null && !daCo);
            o.addActionListener(e -> {
                if (o.isSelected()) moiChon.put(khoa.maKhoaHoc(), new LuaChonMoi(khoa));
                else moiChon.remove(khoa.maKhoaHoc());
                capNhatBang();
                if (o.isSelected()) for (int i = 0; i < dongBang.size(); i++) {
                    if (dongBang.get(i).khoa().maKhoaHoc() == khoa.maKhoaHoc()) {
                        bangDaChon.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            });
            JPanel the = new JPanel(new BorderLayout(0, 3));
            the.setBackground(o.isSelected() ? new Color(234, 248, 237) : Color.WHITE);
            the.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            the.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(o.isSelected() ? new Color(112, 191, 127) : VIEN),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            JPanel dau = new JPanel(new BorderLayout());
            dau.setOpaque(false);
            dau.add(o, BorderLayout.CENTER);
            JLabel gia = new JLabel(tien(khoa.hocPhiMacDinh()));
            gia.setForeground(DAM);
            gia.setFont(GiaoDienMau.font(Font.BOLD, 13));
            dau.add(gia, BorderLayout.EAST);
            the.add(dau, BorderLayout.NORTH);
            JLabel ghiChu = new JLabel(khoa.tongSoBuoi() + " buổi · "
                    + (daCo ? "Đã đăng ký trước đó" : o.isSelected() ? "Đang chọn" : "Có thể chọn"));
            ghiChu.setForeground(GiaoDienMau.CHU_MO);
            the.add(ghiChu, BorderLayout.SOUTH);
            o.addActionListener(e -> {
                the.setBackground(o.isSelected() ? new Color(234, 248, 237) : Color.WHITE);
                ghiChu.setText(khoa.tongSoBuoi() + " buổi · "
                        + (daCo ? "Đã đăng ký trước đó" : o.isSelected() ? "Đang chọn" : "Có thể chọn"));
            });
            the.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setBackground(Color.WHITE);
            wrap.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
            wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            wrap.add(the);
            danhSachKhoa.add(wrap);
            oKhoa.put(khoa.maKhoaHoc(), o);
        }
        danhSachKhoa.revalidate(); danhSachKhoa.repaint();
    }

    private void locKhoa() {
        // napMon() phát sự kiện chọn trước khi taoTheKhoa() tạo đủ thành phần.
        if (khoaHoc.isEmpty() || danhSachKhoa.getComponentCount() != khoaHoc.size()) return;
        String mon = (String) cboMon.getSelectedItem();
        String q = boDau(timKhoa.getText().trim());
        int i = 0;
        for (KhoaHoc khoa : khoaHoc) {
            Component the = danhSachKhoa.getComponent(i++);
            the.setVisible((mon == null || "Tất cả môn".equals(mon) || mon.equals(khoa.tenMonHoc()))
                    && (q.isEmpty() || boDau(khoa.tenKhoaHoc()).contains(q)
                    || boDau(khoa.tenMonHoc()).contains(q)
                    || String.valueOf(khoa.maKhoaHoc()).contains(q)));
        }
        danhSachKhoa.revalidate(); danhSachKhoa.repaint();
    }

    private void capNhatBang() {
        int maDangChon = -1;
        DongDaChon truoc = dongDangChon();
        if (truoc != null) maDangChon = truoc.khoa().maKhoaHoc();
        dongBang.clear();
        model.setRowCount(0);
        for (LuaChonMoi m : moiChon.values()) {
            ThongTinLop lop = timLop(m.maLop);
            dongBang.add(new DongDaChon(m.khoa, null, m));
            model.addRow(new Object[]{m.khoa.tenMonHoc(), m.khoa.tenMonHoc() + " · " + m.khoa.tenKhoaHoc(),
                m.khoa.tongSoBuoi(), "Chưa lưu", lop == null ? "—" : lop.tenLop(),
                lop == null ? "Chờ xếp lớp" : "Xếp lớp khi lưu", "Bỏ",
                "—", "—", "—"});
        }
        for (DangKyKhoaHoc d : dangKyNguoi()) {
            KhoaHoc khoa = khoaHoc.stream().filter(k -> k.maKhoaHoc() == d.maKhoaHoc())
                    .findFirst().orElse(new KhoaHoc(d.maKhoaHoc(), 0, 0,
                            d.tenMonHoc(), d.tenKhoaHoc(), d.tongSoBuoi(),
                            BigDecimal.ZERO, false));
            dongBang.add(new DongDaChon(khoa, d, null));
            model.addRow(new Object[]{d.tenMonHoc(), d.tenMonHoc() + " · " + d.tenKhoaHoc(), d.tongSoBuoi(),
                tenTrangThai(d.trangThai()),
                d.tenLop() == null ? "—" : d.tenLop(),
                tenTrangThaiXep(d),
                "CHO_XEP_LOP".equals(d.trangThai()) ? "Xếp lớp" : "Đã lưu",
                "—", "—", tien(d.hocPhiSauGiam())});
        }
        if (maDangChon > 0) for (int i = 0; i < dongBang.size(); i++) {
            if (dongBang.get(i).khoa().maKhoaHoc() == maDangChon) {
                bangDaChon.setRowSelectionInterval(i, i); break;
            }
        }
        if (bangDaChon.getSelectedRow() < 0 && !dongBang.isEmpty())
            bangDaChon.setRowSelectionInterval(0, 0);
        capNhatLopChoDong();
        capNhatTongQuan();
        capNhatGioKhoa();
        capNhatTaiChinh();
    }

    private void capNhatGioKhoa() {
        gioKhoaCards.removeAll();
        BigDecimal tong = BigDecimal.ZERO;
        for (LuaChonMoi m : moiChon.values()) {
            JPanel dong = new JPanel(new BorderLayout(7, 0));
            dong.setBackground(Color.WHITE);
            dong.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, VIEN),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            JLabel ten = new JLabel("<html>" + html(m.khoa.tenMonHoc()) + " · " + html(m.khoa.tenKhoaHoc())
                    + "<br><small>" + m.khoa.tongSoBuoi() + " buổi</small></html>");
            dong.add(ten, BorderLayout.CENTER);
            JLabel gia = new JLabel(tien(m.khoa.hocPhiMacDinh()));
            gia.setForeground(DAM); gia.setFont(GiaoDienMau.font(Font.BOLD, 12));
            dong.add(gia, BorderLayout.EAST);
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
            gioKhoaCards.add(dong);
            tong = tong.add(m.khoa.hocPhiMacDinh());
        }
        if (moiChon.isEmpty()) {
            JLabel rong = new JLabel("Chưa chọn khóa học.");
            rong.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
            gioKhoaCards.add(rong);
        }
        tongGio.setText(tien(tong));
        gioKhoaCards.revalidate(); gioKhoaCards.repaint();
    }

    private DongDaChon dongDangChon() {
        int i = bangDaChon.getSelectedRow();
        return i >= 0 && i < dongBang.size() ? dongBang.get(i) : null;
    }

    private ThongTinLop timLop(Integer maLop) {
        if (maLop == null) return null;
        return lopHoc.stream().filter(l -> l.maLop() == maLop).findFirst().orElse(null);
    }

    private void capNhatLopChoDong() {
        DongDaChon dong = dongDangChon();
        dangCapNhat = true;
        cboLop.removeAllItems();
        cboLop.addItem(new LopChon(null));
        if (dong != null) {
            for (ThongTinLop lop : lopHoc) {
                if (lop.maKhoaHoc() == dong.khoa().maKhoaHoc()) cboLop.addItem(new LopChon(lop));
            }
            Integer maLop = dong.moi() != null ? dong.moi().maLop : dong.daLuu().maLop();
            if (maLop != null) for (int i = 1; i < cboLop.getItemCount(); i++) {
                ThongTinLop l = cboLop.getItemAt(i).lop();
                if (l != null && l.maLop() == maLop) { cboLop.setSelectedIndex(i); break; }
            }
        }
        dangCapNhat = false;
        cboLop.setEnabled(dong != null && (dong.moi() != null
                || "CHO_XEP_LOP".equals(dong.daLuu().trangThai())));
        napTheLop();
        if (dong != null && dong.daLuu() != null) {
            LocalDate d = dong.daLuu().ngayBatDau();
            ngayXep.datNgay(d != null && d.isAfter(LocalDate.now()) ? d : LocalDate.now());
        }
        hienChiTietLop();
        capNhatNut();
    }

    private void napTheLop() {
        danhSachLopChon.removeAll();
        ButtonGroup nhom = new ButtonGroup();
        for (int i = 0; i < cboLop.getItemCount(); i++) {
            LopChon chon = cboLop.getItemAt(i);
            ThongTinLop lop = chon.lop();
            JRadioButton radio = new JRadioButton(lop == null ? "Chờ xếp lớp" : lop.tenLop());
            radio.setFont(GiaoDienMau.font(Font.PLAIN, 12));
            radio.setBackground(Color.WHITE);
            if (lop != null) radio.setToolTipText("Lịch: " + trong(lop.lichHoc())
                    + " · " + trong(lop.trangThaiNhan()));
            radio.setEnabled(cboLop.isEnabled());
            radio.setSelected(i == cboLop.getSelectedIndex());
            final int index = i;
            radio.addActionListener(e -> cboLop.setSelectedIndex(index));
            nhom.add(radio);
            JPanel hang = new JPanel(new BorderLayout());
            hang.setBackground(Color.WHITE);
            hang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            hang.add(radio, BorderLayout.CENTER);
            if (lop != null) {
                JLabel siSo = new JLabel(lop.soDaGiuCho() + " / " + siSoToiDa(lop));
                siSo.setForeground(DAM);
                hang.add(siSo, BorderLayout.EAST);
            }
            danhSachLopChon.add(hang);
        }
        danhSachLopChon.revalidate(); danhSachLopChon.repaint();
    }

    private void chonLop() {
        if (dangCapNhat) return;
        DongDaChon dong = dongDangChon();
        LopChon chon = (LopChon) cboLop.getSelectedItem();
        if (dong != null && dong.moi() != null) {
            ThongTinLop lop = chon == null ? null : chon.lop();
            if (lop != null && !coTheXep(lop)) {
                JOptionPane.showMessageDialog(dialog,
                        "Lớp đã đầy hoặc chưa có lịch học hợp lệ. Hãy chọn lớp khác hoặc để chờ xếp lớp.",
                        "Lớp chưa đủ điều kiện", JOptionPane.WARNING_MESSAGE);
                dong.moi().maLop = null;
                dangCapNhat = true; cboLop.setSelectedIndex(0); dangCapNhat = false;
            } else dong.moi().maLop = lop == null ? null : lop.maLop();
            capNhatBang();
        }
        hienChiTietLop();
        capNhatNut();
    }

    private void hienChiTietLop() {
        DongDaChon dong = dongDangChon();
        LopChon chon = (LopChon) cboLop.getSelectedItem();
        ThongTinLop lop = chon == null ? null : chon.lop();
        if (dong == null) chiTietLop.setText("Chọn một khóa trong danh sách để xem các lớp phù hợp.");
        else if (lop == null && dong.daLuu() != null
                && "DANG_HOC".equals(dong.daLuu().trangThai())) {
            chiTietLop.setText(dong.khoa().tenKhoaHoc()
                    + "\nĐăng ký đang học nhưng không có lớp hiệu lực. Cần kiểm tra lịch sử xếp lớp.");
        } else if (lop == null && dong.daLuu() != null
                && "HOAN_THANH".equals(dong.daLuu().trangThai())) {
            chiTietLop.setText(dong.khoa().tenKhoaHoc()
                    + "\nKhóa đã hoàn thành nhưng không có lớp được ghi nhận.");
        } else if (lop == null) chiTietLop.setText(dong.khoa().tenKhoaHoc()
                + "\nChưa chọn lớp. Khóa sẽ chờ xếp lớp sau khi lưu đăng ký.");
        else chiTietLop.setText("Lớp: " + lop.tenLop()
                + "  ·  Giáo viên: " + trong(lop.tenGiaoVien())
                + "  ·  Phòng: " + trong(lop.tenPhong())
                + "\nLịch học: " + (lop.lichHoc() == null || lop.lichHoc().isBlank()
                        ? "Chưa có lịch học hợp lệ" : lop.lichHoc())
                + "\nGiới hạn lớp: " + (lop.siSoToiDaLop() == null
                        ? "Chưa đặt" : lop.siSoToiDaLop())
                + "  ·  Sức chứa phòng: " + (lop.sucChuaPhong() == null
                        ? "Chưa gán" : lop.sucChuaPhong())
                + "\nĐã giữ chỗ / giới hạn nhận: " + lop.soDaGiuCho() + "/" + siSoToiDa(lop)
                + "  ·  Khai giảng: " + NgayKhaiGiangPicker.hienThiNgay(lop.ngayKhaiGiang())
                + "  ·  Nhận học viên: " + trong(lop.trangThaiNhan()));
        lopPhong.setText(lop == null ? "Chưa chọn" : trong(lop.tenPhong()));
        lopGiaoVien.setText(lop == null ? "Chưa chọn" : trong(lop.tenGiaoVien()));
        lopSiSo.setText(lop == null ? "—" : lop.soDaGiuCho() + " / " + siSoToiDa(lop));
        lopThoiGian.setText(lop == null ? "—" : trong(lop.lichHoc()));
        lopThoiGian.setToolTipText(lop == null ? null : lop.lichHoc());
    }

    private static boolean coTheXep(ThongTinLop lop) {
        return lop != null && "Đang nhận".equals(lop.trangThaiNhan())
                && lop.lichHoc() != null && !lop.lichHoc().isBlank();
    }

    private void capNhatTongQuan() {
        int xep = 0;
        int cho = 0;
        int canKiemTra = 0;
        int soBuoi = 0;
        for (DongDaChon d : dongBang) {
            if ((d.moi() != null && d.moi().maLop != null)
                    || (d.daLuu() != null && coLopDuChuyen(d.daLuu()))) xep++;
            else if (d.moi() != null
                    || "CHO_XEP_LOP".equals(d.daLuu().trangThai())) cho++;
            else canKiemTra++;
            soBuoi += d.khoa().tongSoBuoi();
        }
        String trangThai = nguoiDangChon == null ? "Chưa chọn người đăng ký"
                : nguoiDangChon.hoSo() != null
                        ? tenTrangThai(nguoiDangChon.hoSo().trangThai())
                        : tenTrangThai(nguoiDangChon.hocVien().trangThaiHoSo());
        tongQuan.setText("Tổng khóa: " + dongBang.size() + "  ·  Đã xếp: " + xep
                + "  ·  Chờ: " + cho
                + (canKiemTra == 0 ? "" : "  ·  Cần kiểm tra: " + canKiemTra)
                + "  ·  Tổng số buổi: " + soBuoi
                + "  ·  Hồ sơ: " + trangThai);
        capNhatNut();
    }

    private void capNhatNut() {
        DongDaChon dong = dongDangChon();
        btnLuu.setEnabled(!dangLuu && nguoiDangChon != null
                && !moiChon.isEmpty() && giaHopLe);
        btnCho.setEnabled(dong != null && dong.moi() != null && dong.moi().maLop != null);
        LopChon lop = (LopChon) cboLop.getSelectedItem();
        btnXep.setEnabled(dong != null && dong.daLuu() != null
                && "CHO_XEP_LOP".equals(dong.daLuu().trangThai())
                && lop != null && coTheXep(lop.lop()));
        btnChuyen.setEnabled(nguoiDangChon != null && nguoiDangChon.hoSo() != null
                && !"DA_CHUYEN_DOI".equals(nguoiDangChon.hoSo().trangThai())
                && dangKyNguoi().stream().anyMatch(DangKyKhoaHocForm::coLopDuChuyen));
    }

    private void thucHienCotThaoTac() {
        DongDaChon d = dongDangChon();
        if (d == null) return;
        if (d.moi() != null) {
            moiChon.remove(d.khoa().maKhoaHoc());
            JCheckBox o = oKhoa.get(d.khoa().maKhoaHoc());
            if (o != null) o.setSelected(false);
            capNhatBang();
        } else if ("CHO_XEP_LOP".equals(d.daLuu().trangThai())) {
            moXepLop(this, d.daLuu(), () -> {
                thongBaoThayDoi(false);
                taiDuLieu();
            });
        }
    }

    private void thongBaoThayDoi(boolean chinhThuc) {
        if (onSaved == null || nguoiDangChon == null) return;
        int maHocVien = dangKyNguoi().isEmpty() ? 0 : dangKyNguoi().get(0).maHocVien();
        onSaved.accept(new DangKyKhoaHoc.KetQua(maHocVien, 0, 0, chinhThuc));
    }

    private void luuDangKy() {
        if (dangLuu || nguoiDangChon == null || moiChon.isEmpty() || !giaHopLe) return;
        LocalDate ngay = ngayBatDau.layNgay();
        if (ngay == null) {
            JOptionPane.showMessageDialog(dialog, "Hãy chọn ngày đăng ký.",
                    "Thiếu ngày", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<DangKyKhoaHoc.LuaChon> chon = new ArrayList<>();
        for (LuaChonMoi m : moiChon.values()) {
            ThongTinLop lop = timLop(m.maLop);
            if (lop != null && lop.ngayKhaiGiang() != null
                    && ngay.isBefore(lop.ngayKhaiGiang())) {
                JOptionPane.showMessageDialog(dialog,
                        "Ngày đăng ký/xếp lớp phải từ ngày khai giảng "
                        + NgayKhaiGiangPicker.hienThiNgay(lop.ngayKhaiGiang())
                        + " của lớp " + lop.tenLop() + " trở đi.",
                        "Ngày chưa phù hợp", JOptionPane.WARNING_MESSAGE);
                return;
            }
            chon.add(new DangKyKhoaHoc.LuaChon(m.khoa.maKhoaHoc(), m.maLop));
        }
        Nguoi nguoi = nguoiDangChon;
        BigDecimal thu;
        try { thu = tienDangThu(); }
        catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Tiền thực thu",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        TaiChinhDTO.LuaChon taiKhoan =
                (TaiChinhDTO.LuaChon) taiKhoanThu.getSelectedItem();
        String cachThu = switch (hinhThucThu.getSelectedIndex()) {
            case 0 -> "TIEN_MAT";
            case 1 -> "CHUYEN_KHOAN";
            default -> "KHAC";
        };
        DangKyKhoaHoc.TaiChinh giaoDich = new DangKyKhoaHoc.TaiChinh(
                uuDaiDaChon().stream().map(ChuongTrinhGiamGia::maChuongTrinh).toList(),
                bangGiaHienTai.phaiThu(), thu, cachThu,
                taiKhoan == null ? 0 : taiKhoan.id(), ghiChuThu.getText().trim(),
                UUID.randomUUID());
        dangLuu = true;
        capNhatNut();
        thongBao.setText("Đang lưu " + chon.size() + " khóa học...");
        new SwingWorker<DangKyKhoaHoc.KetQua, Void>() {
            @Override protected DangKyKhoaHoc.KetQua doInBackground() throws Exception {
                return nguoi.hoSo() != null
                        ? controller.dangKyTuHoSo(nguoi.ma(), ngay, chon, giaoDich)
                        : controller.dangKyChoHocVien(nguoi.ma(), ngay, chon, giaoDich);
            }
            @Override protected void done() {
                dangLuu = false;
                try {
                    DangKyKhoaHoc.KetQua kq = get();
                    vuaLuuDangKy=true;
                    moiChon.clear();
                    for (JCheckBox o : oUuDai.values()) o.setSelected(false);
                    cboThanhToan.setSelectedIndex(0);
                    tienThucThu.setText("");
                    ghiChuThu.setText("");
                    JOptionPane.showMessageDialog(dialog,
                            "Đã lưu " + kq.soDangKy() + " khóa. Mã học viên liên kết #"
                            + kq.maHocVien() + ".\nXếp lớp: " + kq.soXepNgay()
                            + "; chờ xếp lớp: " + (kq.soDangKy() - kq.soXepNgay())
                            + ".\nThực thu: " + tien(giaoDich.soTienThu())
                            + ". Chuyển thành học viên là thao tác riêng.");
                    if (onSaved != null) onSaved.accept(kq);
                    taiDuLieu();
                    hienBuoc(6);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(dialog, ex);
                    capNhatNut();
                } catch (ExecutionException ex) {
                    hienLoi(dialog, ex);
                    capNhatNut();
                }
            }
        }.execute();
    }

    private void xepLopDaLuu() {
        DongDaChon dong = dongDangChon();
        LopChon chon = (LopChon) cboLop.getSelectedItem();
        if (dong == null || dong.daLuu() == null || chon == null || !coTheXep(chon.lop())) return;
        LocalDate ngay = ngayXep.layNgay();
        if (ngay == null || ngay.isBefore(dong.daLuu().ngayBatDau())
                || chon.lop().ngayKhaiGiang() != null && ngay.isBefore(chon.lop().ngayKhaiGiang())) {
            JOptionPane.showMessageDialog(dialog, "Ngày xếp lớp chưa phù hợp với đăng ký và ngày khai giảng.",
                    "Ngày chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(dialog,
                "Xếp " + dong.daLuu().hoTen() + " vào lớp " + chon.lop().tenLop() + "?",
                "Xác nhận xếp lớp", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        btnXep.setEnabled(false);
        thongBao.setText("Đang xếp lớp...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.xepLop(dong.daLuu().maDangKy(), chon.lop().maLop(), ngay);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(dialog, "Đã xếp lớp " + chon.lop().tenLop() + ".");
                    if (onSaved != null) onSaved.accept(new DangKyKhoaHoc.KetQua(
                            dong.daLuu().maHocVien(), 0, 1, false));
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(dialog, ex);
                    capNhatNut();
                } catch (ExecutionException ex) {
                    hienLoi(dialog, ex);
                    capNhatNut();
                }
            }
        }.execute();
    }

    static void moDangKy(Component parent, KhachTiemNang hoSo, HocVien hocVien,
            Consumer<DangKyKhoaHoc.KetQua> onSaved) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Đăng ký nhiều khóa học", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(new DangKyKhoaHocForm(parent, dialog, hoSo, hocVien, onSaved));
        dialog.setSize(1220, 680);
        dialog.setMinimumSize(new Dimension(990, 530));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    static void moXemDangKy(Component parent, DangKyKhoaHoc dk,
            Consumer<DangKyKhoaHoc.KetQua> onSaved) {
        DangKyKhoaHocController controller = new DangKyKhoaHocController();
        new SwingWorker<Nguoi, Void>() {
            @Override protected Nguoi doInBackground() throws Exception {
                if (dk.maKhach() != null) {
                    for (KhachTiemNang k : controller.hoSoTuyenSinh()) {
                        if (k.maKhach() == dk.maKhach()) return new Nguoi(k, null);
                    }
                }
                for (HocVien h : controller.hocVien()) {
                    if (h.maHocVien() == dk.maHocVien()) return new Nguoi(null, h);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    Nguoi n = get();
                    if (n == null) JOptionPane.showMessageDialog(parent, "Không tìm thấy hồ sơ tương ứng.");
                    else moDangKy(parent, n.hoSo(), n.hocVien(), onSaved);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(parent, ex);
                } catch (ExecutionException ex) { hienLoi(parent, ex); }
            }
        }.execute();
    }

    static void moXepLop(Component parent, DangKyKhoaHoc dk, Runnable onSaved) {
        if (dk == null || !"CHO_XEP_LOP".equals(dk.trangThai())) return;
        DangKyKhoaHocController controller = new DangKyKhoaHocController();
        new SwingWorker<List<ThongTinLop>, Void>() {
            @Override protected List<ThongTinLop> doInBackground() throws Exception {
                return controller.thongTinLop().stream()
                        .filter(l -> l.maKhoaHoc() == dk.maKhoaHoc()).toList();
            }
            @Override protected void done() {
                try {
                    List<ThongTinLop> cacLop = get();
                    if (cacLop.isEmpty()) {
                        JOptionPane.showMessageDialog(parent,
                                "Khóa này chưa có lớp đang mở. Đăng ký tiếp tục chờ xếp lớp.");
                        return;
                    }
                    JComboBox<LopChon> cbo = new JComboBox<>();
                    for (ThongTinLop l : cacLop) cbo.addItem(new LopChon(l));
                    NgayKhaiGiangPicker ngay = new NgayKhaiGiangPicker(LocalDate.now()
                            .isAfter(dk.ngayBatDau()) ? LocalDate.now() : dk.ngayBatDau());
                    JPanel chiTiet = khu("THÔNG TIN LỚP ĐANG CHỌN",new GridLayout(0,2,8,7));
                    JLabel giaoVien=new JLabel(),lichHoc=new JLabel(),sucChua=new JLabel(),
                            giuCho=new JLabel(),khaiGiang=new JLabel(),tinhTrang=new JLabel();
                    chiTiet.add(new JLabel("Giáo viên:"));chiTiet.add(giaoVien);
                    JLabel phongHoc=new JLabel();
                    chiTiet.add(new JLabel("Phòng theo lịch:"));chiTiet.add(phongHoc);
                    chiTiet.add(new JLabel("Lịch hợp lệ:"));chiTiet.add(lichHoc);
                    chiTiet.add(new JLabel("Giới hạn lớp / phòng:"));chiTiet.add(sucChua);
                    chiTiet.add(new JLabel("Đã giữ / còn trống:"));chiTiet.add(giuCho);
                    chiTiet.add(new JLabel("Khai giảng:"));chiTiet.add(khaiGiang);
                    chiTiet.add(new JLabel("Khả năng nhận:"));chiTiet.add(tinhTrang);
                    Runnable hien = () -> {
                        LopChon c = (LopChon) cbo.getSelectedItem();
                        if (c != null && c.lop() != null) {
                            ThongTinLop l = c.lop();
                            giaoVien.setText(trong(l.tenGiaoVien()));
                            phongHoc.setText(trong(l.tenPhong()));
                            lichHoc.setText("<html>"+trong(l.lichHoc())+"</html>");
                            sucChua.setText((l.siSoToiDaLop()==null?"Chưa đặt":l.siSoToiDaLop())
                                    + " / " +(l.sucChuaPhong()==null?"Chưa gán":l.sucChuaPhong()));
                            giuCho.setText(l.soDaGiuCho()+" / "+Math.max(0,l.gioiHanTiepNhan()-l.soDaGiuCho()));
                            khaiGiang.setText(NgayKhaiGiangPicker.hienThiNgay(l.ngayKhaiGiang()));
                            tinhTrang.setText(trong(l.trangThaiNhan()));
                        }
                    };
                    cbo.addActionListener(e -> hien.run()); hien.run();
                    JPanel p = new JPanel(new BorderLayout(5, 6));
                    JPanel hang = new JPanel(new GridLayout(0, 2, 7, 6));
                    hang.add(new JLabel("Khóa học:")); hang.add(new JLabel(dk.tenKhoaHoc()));
                    hang.add(new JLabel("Lớp phù hợp:")); hang.add(cbo);
                    hang.add(new JLabel("Ngày xếp lớp:")); hang.add(ngay);
                    p.add(hang, BorderLayout.NORTH);
                    p.add(chiTiet, BorderLayout.CENTER);
                    if (JOptionPane.showConfirmDialog(parent, p,
                            "Xếp lớp cho đăng ký #" + dk.maDangKy(),
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
                    LopChon chon = (LopChon) cbo.getSelectedItem();
                    LocalDate d = ngay.layNgay();
                    if (chon == null || !coTheXep(chon.lop()) || d == null
                            || d.isBefore(dk.ngayBatDau())
                            || chon.lop().ngayKhaiGiang() != null
                                    && d.isBefore(chon.lop().ngayKhaiGiang())) {
                        JOptionPane.showMessageDialog(parent,
                                "Chưa thể xếp lớp: " + (chon==null?"chưa chọn lớp":
                                        !coTheXep(chon.lop())?chon.lop().trangThaiNhan():
                                        "ngày xếp lớp chưa phù hợp"),
                                "Chưa thể xếp lớp", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    new SwingWorker<Void, Void>() {
                        @Override protected Void doInBackground() throws Exception {
                            controller.xepLop(dk.maDangKy(), chon.lop().maLop(), d);
                            return null;
                        }
                        @Override protected void done() {
                            try {
                                get();
                                JOptionPane.showMessageDialog(parent,
                                        "Đã xếp vào lớp " + chon.lop().tenLop() + ".");
                                if (onSaved != null) onSaved.run();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt(); hienLoi(parent, ex);
                            } catch (ExecutionException ex) { hienLoi(parent, ex); }
                        }
                    }.execute();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(parent, ex);
                } catch (ExecutionException ex) { hienLoi(parent, ex); }
            }
        }.execute();
    }

    static void moChuyenThanhHocVien(Component parent, KhachTiemNang hoSo, Runnable onSaved) {
        if (hoSo == null) return;
        if ("DA_CHUYEN_DOI".equals(hoSo.trangThai())) {
            JOptionPane.showMessageDialog(parent, "Hồ sơ này đã chuyển thành học viên.");
            return;
        }
        DangKyKhoaHocController controller = new DangKyKhoaHocController();
        new SwingWorker<List<DangKyKhoaHoc>, Void>() {
            @Override protected List<DangKyKhoaHoc> doInBackground() throws Exception {
                List<DangKyKhoaHoc> tatCa = controller.timKiem("", null);
                Integer maHocVien = tatCa.stream()
                        .filter(d -> d.maKhach() != null && d.maKhach() == hoSo.maKhach())
                        .map(DangKyKhoaHoc::maHocVien).findFirst().orElse(null);
                return tatCa.stream().filter(d -> !"HUY".equals(d.trangThai()))
                        .filter(d -> d.maKhach() != null && d.maKhach() == hoSo.maKhach()
                                || maHocVien != null && d.maHocVien() == maHocVien)
                        .toList();
            }
            @Override protected void done() {
                try {
                    List<DangKyKhoaHoc> cacKhoa = get();
                    long daXep = cacKhoa.stream().filter(DangKyKhoaHocForm::coLopDuChuyen).count();
                    long cho = cacKhoa.stream()
                            .filter(d -> "CHO_XEP_LOP".equals(d.trangThai())).count();
                    if (daXep == 0) {
                        JOptionPane.showMessageDialog(parent,
                                "Hồ sơ cần ít nhất một khóa hợp lệ đã xếp lớp trước khi chuyển thành học viên.",
                                "Chưa đủ điều kiện", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    StringBuilder moTa = new StringBuilder("Học sinh: ").append(hoSo.hoTen())
                            .append(" (#").append(hoSo.maKhach()).append(")\n\nKhóa đã đăng ký:\n");
                    for (DangKyKhoaHoc d : cacKhoa) {
                        moTa.append("• ").append(d.tenMonHoc()).append(" · ").append(d.tenKhoaHoc())
                                .append(" — ").append(tenTinhTrangLop(d))
                                .append('\n');
                    }
                    moTa.append("\nĐã xếp lớp: ").append(daXep)
                            .append(" · Còn chờ: ").append(cho)
                            .append(cacKhoa.size() == daXep + cho ? ""
                                    : " · Cần kiểm tra: " + (cacKhoa.size() - daXep - cho))
                            .append("\nSau xác nhận: hồ sơ Tuyển sinh chuyển sang Đã chuyển đổi;")
                            .append(" học viên xuất hiện trong Quản lý học viên.");
                    JTextArea noiDung = new JTextArea(moTa.toString(), 12, 48);
                    noiDung.setEditable(false); noiDung.setLineWrap(true); noiDung.setWrapStyleWord(true);
                    if (JOptionPane.showConfirmDialog(parent, new JScrollPane(noiDung),
                            "Xác nhận chuyển thành học viên", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) return;
                    new SwingWorker<Integer, Void>() {
                        @Override protected Integer doInBackground() throws Exception {
                            return controller.chuyenThanhHocVien(hoSo.maKhach());
                        }
                        @Override protected void done() {
                            try {
                                int maHocVien = get();
                                JOptionPane.showMessageDialog(parent,
                                        "Đã xác nhận học viên chính thức #" + maHocVien + ".");
                                if (onSaved != null) onSaved.run();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt(); hienLoi(parent, ex);
                            } catch (ExecutionException ex) { hienLoi(parent, ex); }
                        }
                    }.execute();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(parent, ex);
                } catch (ExecutionException ex) { hienLoi(parent, ex); }
            }
        }.execute();
    }

    static boolean coLopDuChuyen(DangKyKhoaHoc dk) {
        return dk != null && dk.maLop() != null
                && ("DANG_HOC".equals(dk.trangThai())
                || "HOAN_THANH".equals(dk.trangThai()));
    }

    private static String tenTrangThaiXep(DangKyKhoaHoc dk) {
        if (coLopDuChuyen(dk)) return "Đã xếp lớp";
        return switch (dk.trangThai() == null ? "" : dk.trangThai()) {
            case "CHO_XEP_LOP" -> "Chờ xếp lớp";
            case "DANG_HOC" -> "Không có lớp hiệu lực";
            case "HOAN_THANH" -> "Không có lớp được ghi nhận";
            default -> dk.maLop() == null ? "Chưa có lớp" : "Đã từng xếp lớp";
        };
    }

    static String tenTinhTrangLop(DangKyKhoaHoc dk) {
        if ("CHO_XEP_LOP".equals(dk.trangThai()) || dk.maLop() == null)
            return tenTrangThaiXep(dk);
        return dk.tenLop() == null || dk.tenLop().isBlank()
                ? tenTrangThaiXep(dk) : dk.tenLop();
    }

    static String tenTrangThai(String ma) {
        return switch (ma == null ? "" : ma) {
            case "TIEP_NHAN" -> "Tiếp nhận";
            case "HOC_THU" -> "Học thử";
            case "CHO_XAC_NHAN" -> "Chờ xác nhận";
            case "CHO_XEP_LOP" -> "Chờ xếp lớp";
            case "DA_CHUYEN_DOI" -> "Đã chuyển đổi";
            case "KHONG_DANG_KY" -> "Không đăng ký";
            case "DANG_HOC" -> "Đang học";
            case "HOAN_THANH" -> "Hoàn thành";
            case "DA_CHUYEN" -> "Đã chuyển";
            case "NGHI_HAN" -> "Nghỉ hẳn";
            case "HUY" -> "Đã hủy";
            case "HOAT_DONG" -> "Hoạt động";
            case "NGUNG_SU_DUNG" -> "Ngừng sử dụng";
            default -> ma;
        };
    }

    private static String trong(String s) { return s == null || s.isBlank() ? "Chưa có" : s; }
    private static String html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static String siSoToiDa(ThongTinLop lop) {
        return lop.gioiHanTiepNhan() > 0
                ? String.valueOf(lop.gioiHanTiepNhan()) : "Chưa nhận";
    }
    private static String tien(BigDecimal so) {
        return NumberFormat.getCurrencyInstance(VI).format(so == null ? BigDecimal.ZERO : so);
    }
    private static String boDau(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    static void hienLoi(Component parent, Exception ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) goc = goc.getCause();
        JOptionPane.showMessageDialog(parent,
                goc.getMessage() == null ? "Thao tác chưa thành công." : goc.getMessage(),
                "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
