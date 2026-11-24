package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.DangKyKhoaHocController;
import vn.vinabacus.controller.TuyenSinhController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.KhachTiemNang;
import vn.vinabacus.repository.TuyenSinhChiTietRepository;

/** Four-tab detail window for a Center admission record. */
public final class TuyenSinhChiTietDialog extends JDialog {
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter NGAY_GIO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");

    private final TuyenSinhController controller = new TuyenSinhController(CoSo.TRUNG_TAM);
    private final DangKyKhoaHocController dangKyController = new DangKyKhoaHocController();
    private final TuyenSinhChiTietRepository repository = new TuyenSinhChiTietRepository();
    private final Consumer<KhachTiemNang> onChanged;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel thongBao = new JLabel("Đang tải hồ sơ...");
    private final JTextField ten = new JTextField(24);
    private final JTextField sdt = new JTextField(24);
    private final JTextField lienHe = new JTextField(24);
    private final JTextField nhuCau = new JTextField(24);
    private final JTextField ghiChu = new JTextField(24);
    private final NgaySinhPicker ngaySinh = new NgaySinhPicker(null);
    private final JComboBox<String> gioiTinh = new JComboBox<>(new String[]{"Chưa chọn","Nam","Nữ","Khác"});
    private final JTextField diaChi = new JTextField(24);
    private final JLabel trangThai = new JLabel();
    private final JLabel daHocThu = new JLabel();
    private final JLabel ghiChuThongTin = new JLabel();
    private final JButton luu = new JButton("Lưu thay đổi");
    private final JButton huySua = new JButton("Hủy chỉnh sửa");
    private final JButton themKhoa = new JButton("Đăng ký thêm khóa");
    private final JButton xemDangKy = new JButton("Xem đăng ký");
    private final JButton xepTuDangKy = new JButton("Xếp lớp cho khóa đang chờ");
    private final JButton xepTuTab = new JButton("Chọn lớp cho khóa đã chọn");
    private final JButton chuyenHocVien = new JButton("Chuyển thành học viên");
    private final DefaultTableModel modelDangKy = new DefaultTableModel(
            new String[]{"Môn học", "Khóa học", "Số buổi", "Trạng thái đăng ký", "Xếp lớp"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable bangDangKy = new JTable(modelDangKy);
    private final DefaultTableModel modelXepLop = new DefaultTableModel(
            new String[]{"Môn học", "Khóa học", "Lớp", "Tình trạng"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable bangXepLop = new JTable(modelXepLop);
    private final DefaultTableModel modelLichSu = new DefaultTableModel(
            new String[]{"Thời điểm lưu", "Sự kiện", "Chi tiết"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };

    private KhachTiemNang hoSo;
    private HocVien hocVien;
    private List<DangKyKhoaHoc> dangKy = List.of();
    private int luotTai;
    private boolean dangLuu;

    private record DuLieu(TuyenSinhChiTietRepository.ChiTiet chiTiet,
                          List<DangKyKhoaHoc> dangKy) { }

    public static void mo(Component parent, KhachTiemNang hoSo,
                          boolean moThongTin, Consumer<KhachTiemNang> onChanged) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        TuyenSinhChiTietDialog dialog = new TuyenSinhChiTietDialog(owner, hoSo, onChanged);
        if (moThongTin) dialog.tabs.setSelectedIndex(0);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private TuyenSinhChiTietDialog(Window owner, KhachTiemNang hoSo,
                                   Consumer<KhachTiemNang> onChanged) {
        super(owner, "CHI TIẾT HỒ SƠ TUYỂN SINH #" + hoSo.maKhach(),
                Dialog.ModalityType.APPLICATION_MODAL);
        this.hoSo = Objects.requireNonNull(hoSo);
        this.onChanged = onChanged;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(0, 8));
        setSize(960, 640);
        setMinimumSize(new Dimension(760, 500));

        JPanel dau = new JPanel(new BorderLayout());
        dau.setBackground(Color.WHITE);
        dau.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        JLabel title = new JLabel("CHI TIẾT HỒ SƠ TUYỂN SINH · #" + hoSo.maKhach());
        title.setFont(GiaoDienMau.font(Font.BOLD, 20));
        title.setForeground(GiaoDienMau.XANH_DAM);
        dau.add(title, BorderLayout.WEST);
        dau.add(chuyenHocVien, BorderLayout.EAST);
        add(dau, BorderLayout.NORTH);

        tabs.addTab("Thông tin hồ sơ", taoTabThongTin());
        tabs.addTab("Khóa học đăng ký", taoTabDangKy());
        tabs.addTab("Xếp lớp", taoTabXepLop());
        tabs.addTab("Lịch sử xử lý", taoTabLichSu());
        add(tabs, BorderLayout.CENTER);

        JPanel duoi = new JPanel(new BorderLayout());
        duoi.setBorder(BorderFactory.createEmptyBorder(2, 16, 10, 16));
        duoi.add(thongBao, BorderLayout.CENTER);
        JButton dong = new JButton("Đóng");
        duoi.add(dong, BorderLayout.EAST);
        add(duoi, BorderLayout.SOUTH);

        luu.addActionListener(e -> luuThongTin());
        huySua.addActionListener(e -> hienThiThongTin(hoSo, hocVien));
        themKhoa.addActionListener(e -> dangKyThem());
        xemDangKy.addActionListener(e -> xemDangKy());
        xepTuDangKy.addActionListener(e -> xepLop(dangChon(bangDangKy)));
        xepTuTab.addActionListener(e -> xepLop(dangChon(bangXepLop)));
        chuyenHocVien.addActionListener(e -> chuyenThanhHocVien());
        bangDangKy.getSelectionModel().addListSelectionListener(e -> capNhatNut());
        bangXepLop.getSelectionModel().addListSelectionListener(e -> capNhatNut());
        dong.addActionListener(e -> dongCuaSo());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dongCuaSo(); }
        });
        hienThiThongTin(hoSo, null);
        capNhatNut();
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private JPanel taoTabThongTin() {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));
        them(form, "Họ tên học sinh (*)", ten);
        JPanel chonNgay=new JPanel(new BorderLayout(5,0));chonNgay.setOpaque(false);
        JButton xoaNgay=new JButton("Xóa ngày");xoaNgay.addActionListener(e->ngaySinh.datNgay(null));
        chonNgay.add(ngaySinh,BorderLayout.CENTER);chonNgay.add(xoaNgay,BorderLayout.EAST);
        them(form, "Ngày sinh", chonNgay);
        them(form, "Giới tính", gioiTinh);
        them(form, "Người liên hệ / phụ huynh", lienHe);
        them(form, "Số điện thoại", sdt);
        them(form, "Địa chỉ", diaChi);
        them(form, "Nhu cầu học", nhuCau);
        them(form, "Trạng thái tuyển sinh", trangThai);
        them(form, "Đã học thử", daHocThu);
        them(form, "Ghi chú", ghiChu);
        ghiChuThongTin.setForeground(GiaoDienMau.CHU_MO);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.add(new JScrollPane(form), BorderLayout.CENTER);
        JPanel duoi = new JPanel(new BorderLayout());
        duoi.setBackground(Color.WHITE);
        duoi.setBorder(BorderFactory.createEmptyBorder(2, 20, 12, 20));
        duoi.add(ghiChuThongTin, BorderLayout.NORTH);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        nut.setOpaque(false);
        nut.add(huySua);
        nut.add(luu);
        duoi.add(nut, BorderLayout.SOUTH);
        p.add(duoi, BorderLayout.SOUTH);
        return p;
    }

    private JPanel taoTabDangKy() {
        cauHinhBang(bangDangKy);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JScrollPane(bangDangKy), BorderLayout.CENTER);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        nut.add(themKhoa);
        nut.add(xemDangKy);
        nut.add(xepTuDangKy);
        p.add(nut, BorderLayout.SOUTH);
        return p;
    }

    private JPanel taoTabXepLop() {
        cauHinhBang(bangXepLop);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JScrollPane(bangXepLop), BorderLayout.CENTER);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        nut.add(xepTuTab);
        p.add(nut, BorderLayout.SOUTH);
        return p;
    }

    private JPanel taoTabLichSu() {
        JTable bang = new JTable(modelLichSu);
        cauHinhBang(bang);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JScrollPane(bang), BorderLayout.CENTER);
        JLabel ghiChuLichSu = new JLabel("CSDL chưa lưu lịch sử đổi trạng thái hoặc nội dung chỉnh sửa hồ sơ; chỉ hiện các mốc đã lưu.");
        ghiChuLichSu.setForeground(GiaoDienMau.CHU_MO);
        ghiChuLichSu.setBorder(BorderFactory.createEmptyBorder(5, 12, 8, 12));
        p.add(ghiChuLichSu, BorderLayout.SOUTH);
        return p;
    }

    private static void them(JPanel p, String nhan, Component truong) {
        p.add(new JLabel(nhan));
        p.add(truong);
    }

    private static void cauHinhBang(JTable bang) {
        bang.setRowHeight(28);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.getTableHeader().setReorderingAllowed(false);
    }

    private void taiDuLieu() {
        final int luot = ++luotTai;
        final int maKhach = hoSo.maKhach();
        final int maCoSo = hoSo.maCoSo();
        thongBao.setText("Đang tải chi tiết hồ sơ...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                TuyenSinhChiTietRepository.ChiTiet chiTiet = repository.lay(maKhach, maCoSo);
                List<DangKyKhoaHoc> ds = dangKyController.timKiem("", null).stream()
                        .filter(d -> d.maKhach() != null && d.maKhach() == maKhach).toList();
                return new DuLieu(chiTiet, ds);
            }
            @Override protected void done() {
                if (luot != luotTai) return;
                try {
                    DuLieu d = get();
                    boolean giuForm = coThayDoi();
                    hoSo = d.chiTiet().hoSo();
                    hocVien = d.chiTiet().hocVien();
                    if (!giuForm) hienThiThongTin(hoSo, hocVien);
                    hienThiDangKy(d.dangKy());
                    hienThiLichSu(d.chiTiet().lichSu());
                    thongBao.setText("Đã tải hồ sơ #" + hoSo.maKhach()
                            + " · " + dangKy.size() + " khóa đã đăng ký");
                    capNhatNut();
                    baoThayDoi();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void hienThiThongTin(KhachTiemNang k, HocVien h) {
        ten.setText(chuoi(k.hoTen()));
        sdt.setText(chuoi(k.soDienThoai()));
        lienHe.setText(chuoi(k.tenNguoiLienHe()));
        nhuCau.setText(chuoi(k.nhuCau()));
        ghiChu.setText(chuoi(k.ghiChu()));
        ngaySinh.datNgay(k.ngaySinh());
        gioiTinh.setSelectedIndex("NAM".equals(k.gioiTinh())?1:"NU".equals(k.gioiTinh())?2:"KHAC".equals(k.gioiTinh())?3:0);
        diaChi.setText(chuoi(k.diaChi()));
        trangThai.setText(tenTrangThai(k.trangThai()));
        daHocThu.setText(k.daHocThu() ? "Có" : "Chưa");
    }

    private void hienThiDangKy(List<DangKyKhoaHoc> ds) {
        dangKy = ds;
        bangDangKy.clearSelection();
        bangXepLop.clearSelection();
        modelDangKy.setRowCount(0);
        modelXepLop.setRowCount(0);
        for (DangKyKhoaHoc d : ds) {
            String lop = tinhTrangXepLop(d);
            modelDangKy.addRow(new Object[]{d.tenMonHoc(), d.tenKhoaHoc(),
                    d.tongSoBuoi(), tenTrangThai(d.trangThai()), lop});
            modelXepLop.addRow(new Object[]{d.tenMonHoc(), d.tenKhoaHoc(),
                    d.tenLop() == null ? "—" : d.tenLop(),
                    d.tenLop() == null ? lop : tenTrangThai(d.trangThai())});
        }
    }

    private void hienThiLichSu(List<TuyenSinhChiTietRepository.SuKien> ds) {
        modelLichSu.setRowCount(0);
        for (TuyenSinhChiTietRepository.SuKien s : ds) {
            String ngay = s.chiCoNgay() ? NGAY.format(s.thoiDiem()) : NGAY_GIO.format(s.thoiDiem());
            modelLichSu.addRow(new Object[]{ngay, s.loai(), s.noiDung()});
        }
    }

    private DangKyKhoaHoc dangChon(JTable bang) {
        int dong = bang.getSelectedRow();
        int index = dong < 0 ? -1 : bang.convertRowIndexToModel(dong);
        return index >= 0 && index < dangKy.size() ? dangKy.get(index) : null;
    }

    private void capNhatNut() {
        DangKyKhoaHoc chonDk = dangChon(bangDangKy);
        DangKyKhoaHoc chonXep = dangChon(bangXepLop);
        xemDangKy.setEnabled(chonDk != null);
        xepTuDangKy.setEnabled(chonDk != null && "CHO_XEP_LOP".equals(chonDk.trangThai()));
        xepTuTab.setEnabled(chonXep != null && "CHO_XEP_LOP".equals(chonXep.trangThai()));
        themKhoa.setEnabled(!"KHONG_DANG_KY".equals(hoSo.trangThai()));
        chuyenHocVien.setEnabled("CHO_XEP_LOP".equals(hoSo.trangThai())
                && dangKy.stream().anyMatch(d -> d.tenLop() != null
                        && ("DANG_HOC".equals(d.trangThai())
                            || "HOAN_THANH".equals(d.trangThai()))));
        boolean coTheSua = !"DA_CHUYEN_DOI".equals(hoSo.trangThai());
        ghiChuThongTin.setText(coTheSua
                ? "Thông tin cá nhân lưu cùng hồ sơ; dữ liệu khác học viên liên kết sẽ được báo xung đột."
                : "Hồ sơ đã chuyển đổi. Sửa thông tin cá nhân tại màn hình Học viên.");
        ten.setEditable(coTheSua);
        sdt.setEditable(coTheSua);
        lienHe.setEditable(coTheSua);
        nhuCau.setEditable(coTheSua);
        ghiChu.setEditable(coTheSua);
        diaChi.setEditable(coTheSua);gioiTinh.setEnabled(coTheSua);
        for(Component child:ngaySinh.getComponents())child.setEnabled(coTheSua);
        luu.setEnabled(!dangLuu && coTheSua);
        huySua.setEnabled(!dangLuu && coTheSua);
    }

    private void luuThongTin() {
        if (dangLuu) return;
        String hoTen = ten.getText().trim();
        if (hoTen.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hãy nhập họ tên học sinh.");
            ten.requestFocusInWindow();
            return;
        }
        KhachTiemNang moi = new KhachTiemNang(hoSo.maKhach(), hoSo.maCoSo(), hoTen,
                rongThanhNull(sdt.getText()), rongThanhNull(lienHe.getText()),
                rongThanhNull(nhuCau.getText()), hoSo.trangThai(), hoSo.daHocThu(),
                rongThanhNull(ghiChu.getText()),ngaySinh.layNgay(),
                switch(gioiTinh.getSelectedIndex()){case 1->"NAM";case 2->"NU";case 3->"KHAC";default->null;},
                rongThanhNull(diaChi.getText()));
        dangLuu = true;
        capNhatNut();
        thongBao.setText("Đang lưu thông tin hồ sơ...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.suaThongTin(moi);
                return null;
            }
            @Override protected void done() {
                dangLuu = false;
                try {
                    get();
                    hoSo = moi;
                    thongBao.setText("Đã lưu thay đổi. Cửa sổ vẫn mở để tiếp tục xử lý.");
                    baoThayDoi();
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
                capNhatNut();
            }
        }.execute();
    }

    private void dangKyThem() {
        if (coThayDoi() && !xacNhanBoThayDoi()) return;
        DangKyKhoaHocPanel.moDangKyTuHoSo(this, hoSo, kq -> taiDuLieu());
    }

    private void xemDangKy() {
        DangKyKhoaHoc d = dangChon(bangDangKy);
        if (d == null) return;
        JOptionPane.showMessageDialog(this,
                "Đăng ký #" + d.maDangKy() + "\n"
                + "Môn học: " + d.tenMonHoc() + "\n"
                + "Khóa học: " + d.tenKhoaHoc() + "\n"
                + "Số buổi: " + d.tongSoBuoi() + "\n"
                + "Trạng thái: " + tenTrangThai(d.trangThai()) + "\n"
                + "Lớp: " + tinhTrangXepLop(d),
                "Chi tiết đăng ký", JOptionPane.INFORMATION_MESSAGE);
    }

    private void xepLop(DangKyKhoaHoc d) {
        if (d == null || !"CHO_XEP_LOP".equals(d.trangThai())) return;
        if (coThayDoi() && !xacNhanBoThayDoi()) return;
        DangKyKhoaHocPanel.moXepLop(this, d, this::taiDuLieu);
    }

    private void chuyenThanhHocVien() {
        if (coThayDoi() && !xacNhanBoThayDoi()) return;
        DangKyKhoaHocPanel.moChuyenThanhHocVien(this, hoSo, this::taiDuLieu);
    }

    private boolean coThayDoi() {
        return !ten.getText().trim().equals(chuoi(hoSo.hoTen()))
                || !Objects.equals(rongThanhNull(sdt.getText()), hoSo.soDienThoai())
                || !Objects.equals(rongThanhNull(lienHe.getText()), hoSo.tenNguoiLienHe())
                || !Objects.equals(rongThanhNull(nhuCau.getText()), hoSo.nhuCau())
                || !Objects.equals(rongThanhNull(ghiChu.getText()), hoSo.ghiChu())
                || !Objects.equals(ngaySinh.layNgay(),hoSo.ngaySinh())
                || !Objects.equals(switch(gioiTinh.getSelectedIndex()){case 1->"NAM";case 2->"NU";case 3->"KHAC";default->null;},hoSo.gioiTinh())
                || !Objects.equals(rongThanhNull(diaChi.getText()),hoSo.diaChi());
    }

    private boolean xacNhanBoThayDoi() {
        if (JOptionPane.showConfirmDialog(this,
                "Thông tin hồ sơ chưa lưu. Bỏ các thay đổi này?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;
        hienThiThongTin(hoSo, hocVien);
        return true;
    }

    private void dongCuaSo() {
        if (dangLuu) return;
        if (coThayDoi() && !xacNhanBoThayDoi()) return;
        dispose();
        baoThayDoi();
    }

    private void baoThayDoi() {
        if (onChanged != null) onChanged.accept(hoSo);
    }

    private static String tenTrangThai(String ma) {
        return switch (ma == null ? "" : ma) {
            case "TIEP_NHAN" -> "Tiếp nhận";
            case "HOC_THU" -> "Học thử";
            case "CHO_XAC_NHAN" -> "Chờ xác nhận";
            case "CHO_XEP_LOP" -> "Chờ xếp lớp";
            case "DA_CHUYEN_DOI" -> "Đã chuyển đổi";
            case "KHONG_DANG_KY" -> "Không đăng ký";
            case "DANG_HOC" -> "Đang học";
            case "HOAN_THANH" -> "Hoàn thành";
            case "DA_CHUYEN" -> "Đã chuyển lớp";
            case "NGHI_HAN" -> "Nghỉ học";
            case "HUY" -> "Đã hủy";
            default -> ma;
        };
    }

    private static String tinhTrangXepLop(DangKyKhoaHoc d) {
        if ("HUY".equals(d.trangThai())) return "Đã hủy";
        if (d.tenLop() != null) return d.tenLop();
        return "CHO_XEP_LOP".equals(d.trangThai())
                ? "Chờ xếp lớp" : "Chưa có lớp hiện tại";
    }

    private static String chuoi(String s) { return s == null ? "" : s; }
    private static String rongThanhNull(String s) {
        return s == null || s.trim().isBlank() ? null : s.trim();
    }

    private void hienLoi(Exception ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) goc = goc.getCause();
        thongBao.setText("Lỗi: " + goc.getMessage());
        JOptionPane.showMessageDialog(this,
                goc.getMessage() == null ? "Thao tác chưa thành công." : goc.getMessage(),
                "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
