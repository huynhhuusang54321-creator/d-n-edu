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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.ChiTietHocVienTrungTamController;
import vn.vinabacus.controller.HocVienController;
import vn.vinabacus.model.ChiTietHocVienTrungTam;
import vn.vinabacus.model.ChiTietHocVienTrungTam.Khoa;
import vn.vinabacus.model.ChiTietHocVienTrungTam.SuKien;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.HocVien;

/** Cua so chi tiet Trung tam: ho so, tung khoa/lop/tien do va lich su thuc te. */
final class ChiTietHocVienDialog extends JDialog {
    private final HocVienController hocVienController = new HocVienController(CoSo.TRUNG_TAM);
    private final ChiTietHocVienTrungTamController chiTietController =
            new ChiTietHocVienTrungTamController();
    private final Runnable onChanged;
    private HocVien hocVien;
    private boolean dangLuu;
    private final JTextField ten = new JTextField(26);
    private final NgaySinhPicker ngay = new NgaySinhPicker(null);
    private final JComboBox<String> gioiTinh = new JComboBox<>(
            new String[]{"", "NAM", "NU", "KHAC"});
    private final JTextField lienHe = new JTextField(26);
    private final JTextField sdt = new JTextField(20);
    private final JTextField diaChi = new JTextField(30);
    private final JTextArea ghiChu = new JTextArea(3, 30);
    private final JButton luu = new JButton("Lưu thay đổi");
    private final JButton huySua = new JButton("Hủy thay đổi");
    private final JLabel tieuDe = new JLabel();
    private final JLabel trangThaiHoSo = new JLabel();
    private final JLabel thongBao = new JLabel(" ");
    private final JPanel theKhoa = new JPanel();
    private final DefaultTableModel lichSuModel = new DefaultTableModel(
            new String[]{"Ngày", "Sự kiện", "Nội dung đã lưu"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private int luotTai;

    static void mo(Component parent, HocVien hocVien, Runnable onChanged) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        ChiTietHocVienDialog dialog = new ChiTietHocVienDialog(owner, hocVien, onChanged);
        dialog.setVisible(true);
    }

    private ChiTietHocVienDialog(Window owner, HocVien hocVien, Runnable onChanged) {
        super(owner, "CHI TIẾT HỌC VIÊN", Dialog.ModalityType.APPLICATION_MODAL);
        this.hocVien = Objects.requireNonNull(hocVien);
        this.onChanged = onChanged;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(0, 8));
        setSize(1000, 650);
        setMinimumSize(new Dimension(760, 500));

        tieuDe.setFont(GiaoDienMau.font(Font.BOLD, 20));
        tieuDe.setForeground(GiaoDienMau.XANH_DAM);
        tieuDe.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        add(tieuDe, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Thông tin hồ sơ", hoSo());
        tabs.addTab("Khóa học & Lớp học", khoaVaLop());
        tabs.addTab("Lịch sử học tập", lichSu());
        add(tabs, BorderLayout.CENTER);
        JPanel chan = new JPanel(new BorderLayout(8, 0));
        chan.setBorder(BorderFactory.createEmptyBorder(2, 16, 10, 16));
        JButton dong = new JButton("Đóng");
        chan.add(thongBao, BorderLayout.CENTER);
        chan.add(dong, BorderLayout.EAST);
        add(chan, BorderLayout.SOUTH);
        dong.addActionListener(e -> dong());
        datDuLieuHoSo();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dong(); }
        });
        setLocationRelativeTo(owner);
        taiChiTiet();
    }

    private JPanel hoSo() {
        JPanel noiDung = new JPanel(new BorderLayout(0, 10));
        noiDung.setBackground(Color.WHITE);
        noiDung.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 9));
        form.setOpaque(false);
        form.add(new JLabel("Mã học viên"));
        form.add(new JLabel(String.valueOf(hocVien.maHocVien())));
        form.add(new JLabel("Họ tên (*)")); form.add(ten);
        form.add(new JLabel("Ngày sinh")); form.add(ngay);
        form.add(new JLabel("Giới tính")); form.add(gioiTinh);
        form.add(new JLabel("Người liên hệ")); form.add(lienHe);
        form.add(new JLabel("Số điện thoại")); form.add(sdt);
        form.add(new JLabel("Địa chỉ")); form.add(diaChi);
        form.add(new JLabel("Ghi chú"));
        ghiChu.setLineWrap(true);
        ghiChu.setWrapStyleWord(true);
        form.add(new JScrollPane(ghiChu));
        form.add(new JLabel("Trạng thái hồ sơ"));
        form.add(trangThaiHoSo);
        noiDung.add(form, BorderLayout.NORTH);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        nut.add(huySua); nut.add(luu);
        noiDung.add(nut, BorderLayout.SOUTH);
        huySua.addActionListener(e -> {
            if (!dangLuu) datDuLieuHoSo();
        });
        luu.addActionListener(e -> luuHoSo());
        return noiDung;
    }

    private JPanel khoaVaLop() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        JPanel dau = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        dau.setOpaque(false);
        JButton them = new JButton("Đăng ký thêm khóa");
        JButton taiLai = new JButton("Tải lại");
        dau.add(them); dau.add(taiLai);
        p.add(dau, BorderLayout.NORTH);
        theKhoa.setBackground(Color.WHITE);
        theKhoa.setLayout(new BoxLayout(theKhoa, BoxLayout.Y_AXIS));
        theKhoa.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        p.add(new JScrollPane(theKhoa), BorderLayout.CENTER);
        them.addActionListener(e -> {
            if (dangLuu) return;
            DangKyKhoaHocPanel.moDangKyTuHocVien(this, hocVien, k -> {
                    taiChiTiet();
                    if (onChanged != null) onChanged.run();
            });
        });
        taiLai.addActionListener(e -> taiChiTiet());
        return p;
    }

    private JPanel lichSu() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextArea chuThich = new JTextArea("Chỉ hiển thị đăng ký, xếp lớp, chuyển lớp và hoàn thành có bản ghi thực tế."
                + " Lịch sử Bảo lưu/Kích hoạt lại cần cấu trúc CSDL riêng.");
        chuThich.setEditable(false);
        chuThich.setOpaque(false);
        chuThich.setLineWrap(true);
        chuThich.setWrapStyleWord(true);
        chuThich.setRows(2);
        chuThich.setFont(new JLabel().getFont());
        chuThich.setForeground(GiaoDienMau.CHU_MO);
        p.add(chuThich, BorderLayout.NORTH);
        JTable bang = new JTable(lichSuModel);
        bang.setRowHeight(28);
        bang.setAutoCreateRowSorter(true);
        bang.getColumnModel().getColumn(0).setPreferredWidth(110);
        bang.getColumnModel().getColumn(1).setPreferredWidth(160);
        bang.getColumnModel().getColumn(2).setPreferredWidth(600);
        p.add(new JScrollPane(bang), BorderLayout.CENTER);
        return p;
    }

    private void datDuLieuHoSo() {
        tieuDe.setText("CHI TIẾT HỌC VIÊN  #" + hocVien.maHocVien()
                + " · " + hocVien.hoTen());
        ten.setText(hocVien.hoTen());
        ngay.datNgay(hocVien.ngaySinh());
        gioiTinh.setSelectedItem(hocVien.gioiTinh() == null ? "" : hocVien.gioiTinh());
        lienHe.setText(chuoi(hocVien.tenNguoiLienHe()));
        sdt.setText(chuoi(hocVien.soDienThoaiLienHe()));
        diaChi.setText(chuoi(hocVien.diaChi()));
        ghiChu.setText(chuoi(hocVien.ghiChu()));
        trangThaiHoSo.setText("NGUNG_SU_DUNG".equals(hocVien.trangThaiHoSo())
                ? "Ngừng sử dụng · chưa hỗ trợ Bảo lưu/Tạm nghỉ"
                : "Đang hoạt động · chưa hỗ trợ Bảo lưu/Tạm nghỉ");
    }

    private boolean coThayDoiChuaLuu() {
        return !Objects.equals(ten.getText().trim(), hocVien.hoTen())
                || !Objects.equals(ngay.layNgay(), hocVien.ngaySinh())
                || !Objects.equals(rongThanhNull((String) gioiTinh.getSelectedItem()), hocVien.gioiTinh())
                || !Objects.equals(rongThanhNull(lienHe.getText()), hocVien.tenNguoiLienHe())
                || !Objects.equals(rongThanhNull(sdt.getText()), hocVien.soDienThoaiLienHe())
                || !Objects.equals(rongThanhNull(diaChi.getText()), hocVien.diaChi())
                || !Objects.equals(rongThanhNull(ghiChu.getText()), hocVien.ghiChu());
    }

    private void dong() {
        if (dangLuu) return;
        if (coThayDoiChuaLuu() && JOptionPane.showConfirmDialog(this,
                "Thông tin chưa lưu sẽ bị bỏ. Đóng cửa sổ?", "Xác nhận đóng",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        if (onChanged != null) onChanged.run();
        dispose();
    }

    private void luuHoSo() {
        if (dangLuu) return;
        String hoTen = ten.getText().trim();
        if (hoTen.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn chưa nhập họ tên học viên.");
            return;
        }
        if (sdt.getText().trim().length() > 20) {
            JOptionPane.showMessageDialog(this, "Số điện thoại tối đa 20 ký tự.");
            return;
        }
        if (hoTen.length() > 150 || lienHe.getText().trim().length() > 150
                || diaChi.getText().trim().length() > 400
                || ghiChu.getText().trim().length() > 500) {
            JOptionPane.showMessageDialog(this,
                    "Họ tên/người liên hệ tối đa 150, địa chỉ 400 và ghi chú 500 ký tự.");
            return;
        }
        HocVien moi = new HocVien(hocVien.maHocVien(), hocVien.maCoSo(), hoTen,
                ngay.layNgay(), rongThanhNull((String) gioiTinh.getSelectedItem()),
                rongThanhNull(lienHe.getText()), rongThanhNull(sdt.getText()),
                rongThanhNull(diaChi.getText()), rongThanhNull(ghiChu.getText()),
                hocVien.trangThaiHoSo());
        datDangLuu(true);
        thongBao.setText("Đang lưu thông tin hồ sơ...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                hocVienController.sua(moi);
                return null;
            }
            @Override protected void done() {
                datDangLuu(false);
                try {
                    get();
                    hocVien = moi;
                    datDuLieuHoSo();
                    thongBao.setText("Đã lưu thông tin hồ sơ.");
                    if (onChanged != null) onChanged.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) {
                    hienLoi(ex);
                }
            }
        }.execute();
    }

    private void datDangLuu(boolean dangLuu) {
        this.dangLuu = dangLuu;
        luu.setEnabled(!dangLuu);
        huySua.setEnabled(!dangLuu);
        ten.setEnabled(!dangLuu);
        gioiTinh.setEnabled(!dangLuu);
        lienHe.setEnabled(!dangLuu);
        sdt.setEnabled(!dangLuu);
        diaChi.setEnabled(!dangLuu);
        ghiChu.setEnabled(!dangLuu);
        for (Component c : ngay.getComponents()) c.setEnabled(!dangLuu);
    }

    private void taiChiTiet() {
        int lan = ++luotTai;
        thongBao.setText("Đang tải khóa học và lịch sử...");
        new SwingWorker<ChiTietHocVienTrungTam, Void>() {
            @Override protected ChiTietHocVienTrungTam doInBackground() throws Exception {
                return chiTietController.tai(hocVien.maHocVien());
            }
            @Override protected void done() {
                if (lan != luotTai) return;
                try {
                    ChiTietHocVienTrungTam duLieu = get();
                    hienThiKhoa(duLieu.khoaHoc());
                    lichSuModel.setRowCount(0);
                    for (SuKien sk : duLieu.lichSu())
                        lichSuModel.addRow(new Object[]{NgaySinhPicker.hienThiNgay(sk.ngay()),
                            sk.loai(), sk.noiDung()});
                    thongBao.setText("Có " + duLieu.khoaHoc().size()
                            + " đăng ký và " + duLieu.lichSu().size() + " sự kiện đã lưu.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) {
                    hienLoi(ex);
                }
            }
        }.execute();
    }

    private void hienThiKhoa(List<Khoa> danhSach) {
        theKhoa.removeAll();
        if (danhSach.isEmpty()) theKhoa.add(new JLabel("Học viên chưa có đăng ký khóa học."));
        for (Khoa k : danhSach) {
            theKhoa.add(theKhoa(k));
            theKhoa.add(Box.createVerticalStrut(8));
        }
        theKhoa.revalidate();
        theKhoa.repaint();
    }

    private JPanel theKhoa(Khoa k) {
        DangKyKhoaHoc dk = k.dangKy();
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Math.max(355,300+k.lichHoc().size()*28)));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GiaoDienMau.DUONG_VIEN),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel tenKhoa = new JLabel(dk.tenMonHoc() + " · " + dk.tenKhoaHoc());
        tenKhoa.setFont(GiaoDienMau.font(Font.BOLD, 16));
        tenKhoa.setForeground(GiaoDienMau.XANH_DAM);
        p.add(tenKhoa, BorderLayout.NORTH);
        JPanel nhom = new JPanel(new java.awt.GridLayout(1,3,9,0));
        nhom.setOpaque(false);
        JPanel thongTin = theNho("THÔNG TIN KHÓA");
        thongTin.add(dong("Đăng ký: " + tenTrangThai(dk.trangThai())));
        thongTin.add(dong("Xếp lớp: " + (k.dangChoXepLop()?"Chờ xếp lớp":"Đã xếp")));
        thongTin.add(dong("Lớp chính: " + (k.dangChoXepLop()?"Chưa có":chuoi(dk.tenLop()))));
        thongTin.add(dong("Giáo viên: " + (k.tenGiaoVien()==null?"Chưa phân công":k.tenGiaoVien())));
        thongTin.add(dong("Lịch học:"));
        if (k.lichHoc().isEmpty()) thongTin.add(dong("Chưa có lịch hiệu lực"));
        else for (String lich : k.lichHoc()) thongTin.add(dong("• " + lich));
        nhom.add(thongTin);
        JPanel tienDo = theNho("TIẾN ĐỘ HỌC TẬP");
        tienDo.add(dong("Tổng số buổi: " + dk.tongSoBuoi()));
        tienDo.add(dong("Hoàn thành hợp lệ: " + (k.soBuoiHoanThanh()==null
                ?"Chưa có dữ liệu tiến độ":k.soBuoiHoanThanh())));
        tienDo.add(dong("Còn lại: " + (k.soBuoiHoanThanh()==null?"Chưa xác định"
                :Math.max(0,dk.tongSoBuoi()-k.soBuoiHoanThanh()))));
        tienDo.add(dong("Kết thúc dự kiến: " + (k.ngayKetThucDuKien()==null
                ?"Chưa xác định":NgaySinhPicker.hienThiNgay(k.ngayKetThucDuKien()))));
        tienDo.add(dong("Hoàn thành thực tế: " + (k.ngayKetThucThucTe()==null
                ?"Chưa ghi nhận":NgaySinhPicker.hienThiNgay(k.ngayKetThucThucTe()))));
        if (k.soBuoiHoanThanh() != null && dk.tongSoBuoi() > 0 && !k.dangChoXepLop()) {
            int phanTram = Math.min(100,
                    (int) Math.round(100.0 * k.soBuoiHoanThanh() / dk.tongSoBuoi()));
            JProgressBar thanh = new JProgressBar(0, 100);
            thanh.setValue(phanTram);
            thanh.setStringPainted(true);
            thanh.setString(phanTram + "% · " + k.soBuoiHoanThanh()
                    + "/" + dk.tongSoBuoi() + " buổi được công nhận");
            thanh.setForeground(GiaoDienMau.XANH_THANH);
            tienDo.add(thanh);
        } else tienDo.add(dong("Chưa có dữ liệu tiến độ đáng tin cậy"));
        nhom.add(tienDo);
        JPanel hocPhi = theNho("THÔNG TIN HỌC PHÍ");
        hocPhi.add(dong("Học phí gốc: " + tien(k.hocPhiGoc())));
        hocPhi.add(dong("Giảm giá: " + tien(k.soTienGiam())));
        hocPhi.add(dong("Phải thu: " + tien(dk.hocPhiSauGiam())));
        hocPhi.add(dong("Đã thu: " + tien(k.daDong())));
        hocPhi.add(dong("Còn nợ: " + tien(k.conNo())));
        nhom.add(hocPhi);
        p.add(nhom, BorderLayout.CENTER);
        JPanel duoi = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        duoi.setOpaque(false);
        if ("CHO_XEP_LOP".equals(dk.trangThai())) {
            JButton xep = new JButton("Xếp lớp");
            xep.addActionListener(e -> {
                if (dangLuu) return;
                DangKyKhoaHocPanel.moXepLop(this, dk, () -> {
                    taiChiTiet();
                    if (onChanged != null) onChanged.run();
                });
            });
            duoi.add(xep);
        }
        p.add(duoi, BorderLayout.SOUTH);
        return p;
    }

    private static JPanel theNho(String ten) {
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(new Color(248,251,248));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(ten),BorderFactory.createEmptyBorder(8,9,8,9)));
        return p;
    }

    private static JTextArea dong(String noiDung) {
        JTextArea label = new JTextArea(noiDung);
        label.setEditable(false);
        label.setLineWrap(true);
        label.setWrapStyleWord(true);
        label.setOpaque(false);
        label.setFont(new JLabel().getFont());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        return label;
    }

    private static String tien(BigDecimal so) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                .format(so == null ? BigDecimal.ZERO : so) + " đ";
    }

    private static String tenTrangThai(String code) {
        return switch (code) {
            case "CHO_XEP_LOP" -> "Chờ xếp lớp";
            case "DANG_HOC" -> "Đang học";
            case "HOAN_THANH" -> "Đã hoàn thành";
            case "DA_CHUYEN" -> "Đã chuyển";
            case "NGHI_HAN" -> "Đã nghỉ học";
            case "HUY" -> "Đã hủy";
            default -> code;
        };
    }

    private static String chuoi(String s) { return s == null ? "" : s; }
    private static String rongThanhNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
    private void hienLoi(Exception ex) {
        Throwable t = ex;
        while (t.getCause() != null && t != t.getCause()) t = t.getCause();
        thongBao.setText("Lỗi: " + t.getMessage());
        JOptionPane.showMessageDialog(this, t.getMessage(), "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
