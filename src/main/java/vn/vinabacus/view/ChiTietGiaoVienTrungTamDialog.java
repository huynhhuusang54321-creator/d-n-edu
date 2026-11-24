package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.model.ChiTietGiaoVienTrungTam;
import vn.vinabacus.model.NhanVien;

/** Chi tiet chi doc, khong nham vai tro tai khoan voi chuyen mon giang day. */
public final class ChiTietGiaoVienTrungTamDialog extends JDialog {
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    public ChiTietGiaoVienTrungTamDialog(Window owner, NhanVien nhanVien,
                                          ChiTietGiaoVienTrungTam chiTiet) {
        super(owner, "Chi tiết lớp và lịch phụ trách", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));

        JPanel dau = new JPanel(new BorderLayout(5, 5));
        dau.setBorder(BorderFactory.createEmptyBorder(14, 16, 5, 16));
        JLabel ten = new JLabel(nhanVien.getHoTen() + "  ·  Mã NV " + nhanVien.getMaNhanVien());
        ten.setFont(ten.getFont().deriveFont(Font.BOLD, 19f));
        dau.add(ten, BorderLayout.NORTH);
        dau.add(new JLabel("Vai trò tài khoản: " + hienVaiTro(nhanVien.getVaiTroTaiKhoan())
                + "  ·  Lớp hiện được gán: " + chiTiet.lop().size()), BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);

        JTabbedPane tab = new JTabbedPane();
        DefaultTableModel bangLop = bang(new String[]{
                "Mã lớp", "Tên lớp", "Môn", "Khóa học", "Khai giảng", "Trạng thái"});
        for (ChiTietGiaoVienTrungTam.Lop lop : chiTiet.lop()) {
            bangLop.addRow(new Object[]{lop.maLop(), lop.tenLop(), lop.tenMon(),
                    lop.tenKhoa(), ngay(lop.ngayKhaiGiang()), lop.trangThai()});
        }
        tab.addTab("Lớp phụ trách (" + chiTiet.lop().size() + ")", cuon(bangLop));

        DefaultTableModel bangLich = bang(new String[]{
                "Mã lịch", "Lớp", "Thứ", "Bắt đầu", "Kết thúc", "Từ ngày", "Đến ngày", "Trạng thái"});
        JComboBox<String> locLop = new JComboBox<>();
        locLop.addItem("Tất cả lớp");
        for (ChiTietGiaoVienTrungTam.Lop lop : chiTiet.lop())
            locLop.addItem(lop.tenLop() + " (#" + lop.maLop() + ")");
        NgayLichHocPicker chonTuan = new NgayLichHocPicker(LocalDate.now());
        JCheckBox tatCa = new JCheckBox("Tất cả thời gian", true);
        Runnable napLich = () -> {
            bangLich.setRowCount(0);
            int viTri = locLop.getSelectedIndex() - 1;
            int maLop = viTri < 0 ? -1 : chiTiet.lop().get(viTri).maLop();
            LocalDate dauTuan = chonTuan.layNgay() == null ? LocalDate.now()
                    : chonTuan.layNgay();
            dauTuan = dauTuan.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (ChiTietGiaoVienTrungTam.Lich lich : chiTiet.lich()) {
                if (maLop > 0 && lich.maLop() != maLop) continue;
                if (!tatCa.isSelected()) {
                    if (!"HIEU_LUC".equals(lich.trangThai())
                            || lich.thuTrongTuan() < 2 || lich.thuTrongTuan() > 8) continue;
                    LocalDate ngayBuoi = dauTuan.plusDays(lich.thuTrongTuan() - 2L);
                    if (ngayBuoi.isBefore(lich.tuNgay())
                            || (lich.denNgay() != null && ngayBuoi.isAfter(lich.denNgay()))) continue;
                }
                bangLich.addRow(new Object[]{lich.maLich(), lich.tenLop(),
                        lich.thuTrongTuan() == 8 ? "Chủ nhật" : "Thứ " + lich.thuTrongTuan(),
                        gio(lich.batDau()), gio(lich.ketThuc()), ngay(lich.tuNgay()),
                        lich.denNgay() == null ? "Chưa xác định" : ngay(lich.denNgay()),
                        lich.trangThai()});
            }
        };
        JPanel locLich = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        locLich.add(new JLabel("Lớp:")); locLich.add(locLop);
        locLich.add(new JLabel("Tuần chứa ngày:")); locLich.add(chonTuan);
        locLich.add(tatCa);
        JButton apDung = new JButton("Áp dụng");
        locLich.add(apDung);
        locLop.addActionListener(e -> napLich.run());
        tatCa.addActionListener(e -> napLich.run());
        apDung.addActionListener(e -> napLich.run());
        napLich.run();
        JPanel lichPanel = new JPanel(new BorderLayout());
        lichPanel.add(locLich, BorderLayout.NORTH);
        lichPanel.add(cuon(bangLich), BorderLayout.CENTER);
        tab.addTab("Lịch các lớp (" + chiTiet.lich().size() + ")", lichPanel);
        tab.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        add(tab, BorderLayout.CENTER);

        JPanel duoi = new JPanel(new BorderLayout(12, 0));
        duoi.setBorder(BorderFactory.createEmptyBorder(7, 16, 14, 16));
        duoi.add(new JLabel("Chỉ đọc · Quản lý chuyên môn tại Nhân sự/Giáo viên; xem lịch sử phân công tại Chi tiết lớp."),
                BorderLayout.CENTER);
        JButton dong = new JButton("Đóng");
        dong.addActionListener(e -> dispose());
        duoi.add(dong, BorderLayout.EAST);
        add(duoi, BorderLayout.SOUTH);

        setSize(930, 530);
        setMinimumSize(new java.awt.Dimension(700, 420));
        setLocationRelativeTo(owner);
    }

    private static DefaultTableModel bang(String[] cot) {
        return new DefaultTableModel(cot, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JScrollPane cuon(DefaultTableModel model) {
        JTable bang = new JTable(model);
        bang.setAutoCreateRowSorter(true);
        bang.setRowHeight(27);
        return new JScrollPane(bang);
    }

    static String hienVaiTro(String maVaiTro) {
        if (maVaiTro == null) return "Chưa có vai trò đăng nhập tại cơ sở";
        return switch (maVaiTro) {
            case "GIAO_VIEN" -> "Giáo viên";
            case "TO_TRUONG" -> "Tổ trưởng";
            case "ADMIN" -> "Quản trị";
            default -> maVaiTro;
        };
    }

    private static String ngay(LocalDate ngay) {
        return ngay == null ? "Chưa có" : NGAY.format(ngay);
    }

    private static String gio(LocalTime gio) {
        return gio == null ? "Chưa có" : GIO.format(gio);
    }
}
