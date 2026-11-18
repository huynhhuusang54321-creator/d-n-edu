package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Bo chon ngày áp dụng bang lich, ho tro ngay qua khu va tuong lai. */
public final class NgayLichHocPicker extends JPanel {
    private static final DateTimeFormatter DINH_DANG =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", new Locale("vi", "VN"));
    private final JTextField txtNgay = new JTextField(13);
    private LocalDate ngayDaChon;

    public NgayLichHocPicker(LocalDate ngay) {
        super(new BorderLayout(6, 0));
        txtNgay.setEditable(false);
        txtNgay.setToolTipText("Bấm Chọn ngày để mở lịch");
        JButton btn = new JButton("📅 Chọn ngày");
        btn.addActionListener(e -> moLich());
        add(txtNgay, BorderLayout.CENTER);
        add(btn, BorderLayout.EAST);
        datNgay(ngay);
    }

    public LocalDate layNgay() { return ngayDaChon; }
    public void datNgay(LocalDate ngay) {
        ngayDaChon = ngay;
        txtNgay.setText(ngay == null ? "" : ngay.format(DINH_DANG));
    }
    public static String hienThiNgay(LocalDate ngay) {
        return ngay == null ? "" : ngay.format(DINH_DANG);
    }

    private void moLich() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new HopThoaiLich(owner).setVisible(true);
    }

    private final class HopThoaiLich extends JDialog {
        private final JPanel ngayPanel = new JPanel(new GridLayout(7, 7, 4, 4));
        private final JSpinner spThang;
        private final JSpinner spNam;
        private final JButton btnTruoc = new JButton("<");
        private final JButton btnSau = new JButton(">");
        private YearMonth thangDangXem;
        private boolean dangCapNhat;
        private static final int NAM_MIN = 1900;
        private static final int NAM_MAX = 2100;

        HopThoaiLich(Window owner) {
            super(owner, "Chọn ngày lịch học", Dialog.ModalityType.APPLICATION_MODAL);
            thangDangXem = YearMonth.from(ngayDaChon == null ? LocalDate.now() : ngayDaChon);
            spThang = new JSpinner(new SpinnerNumberModel(
                    thangDangXem.getMonthValue(), 1, 12, 1));
            spNam = new JSpinner(new SpinnerNumberModel(
                    Math.max(NAM_MIN, Math.min(NAM_MAX, thangDangXem.getYear())), NAM_MIN, NAM_MAX, 1));

            JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 7));
            header.add(btnTruoc);
            header.add(new JLabel("Tháng:")); header.add(spThang);
            header.add(new JLabel("Năm:")); header.add(spNam);
            header.add(btnSau);
            btnTruoc.addActionListener(e -> doiThang(-1));
            btnSau.addActionListener(e -> doiThang(1));
            spThang.addChangeListener(e -> doiThangNam());
            spNam.addChangeListener(e -> doiThangNam());

            ngayPanel.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton homNay = new JButton("Hôm nay");
            JButton xoa = new JButton("Xóa ngày");
            JButton huy = new JButton("Hủy");
            homNay.addActionListener(e -> chon(LocalDate.now()));
            xoa.addActionListener(e -> chon(null));
            huy.addActionListener(e -> dispose());
            footer.add(homNay); footer.add(xoa); footer.add(huy);
            setLayout(new BorderLayout());
            add(header, BorderLayout.NORTH);
            add(ngayPanel, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
            capNhatNgay();
            setSize(440, 405);
            setResizable(false);
            setLocationRelativeTo(owner);
        }

        private void doiThangNam() {
            if (dangCapNhat) return;
            thangDangXem = YearMonth.of((int) spNam.getValue(), (int) spThang.getValue());
            capNhatNgay();
        }
        private void doiThang(int thang) {
            YearMonth moi = thangDangXem.plusMonths(thang);
            if (moi.getYear() < NAM_MIN || moi.getYear() > NAM_MAX) return;
            thangDangXem = moi;
            dangCapNhat = true;
            spThang.setValue(moi.getMonthValue());
            spNam.setValue(moi.getYear());
            dangCapNhat = false;
            capNhatNgay();
        }
        private void capNhatNgay() {
            ngayPanel.removeAll();
            btnTruoc.setEnabled(thangDangXem.isAfter(YearMonth.of(NAM_MIN, 1)));
            btnSau.setEnabled(thangDangXem.isBefore(YearMonth.of(NAM_MAX, 12)));
            for (String thu : new String[]{"T2", "T3", "T4", "T5", "T6", "T7", "CN"}) {
                ngayPanel.add(new JLabel(thu, SwingConstants.CENTER));
            }
            int batDau = thangDangXem.atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < 42; i++) {
                int ngay = i - batDau + 1;
                if (ngay <= 0 || ngay > thangDangXem.lengthOfMonth()) {
                    ngayPanel.add(new JLabel(""));
                    continue;
                }
                LocalDate ngayGiaTri = thangDangXem.atDay(ngay);
                JButton o = new JButton(String.valueOf(ngay));
                o.setMargin(new java.awt.Insets(2, 2, 2, 2));
                if (ngayGiaTri.equals(ngayDaChon)) o.setSelected(true);
                o.addActionListener(e -> chon(ngayGiaTri));
                ngayPanel.add(o);
            }
            ngayPanel.revalidate();
            ngayPanel.repaint();
        }
        private void chon(LocalDate ngay) {
            datNgay(ngay);
            dispose();
        }
    }
}
