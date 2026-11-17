package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Bo chon ngay sinh Swing, khong can thu vien ngoai.
 * Hien thi dd/MM/yyyy; model giu LocalDate de JDBC luu vao SQL DATE.
 */
public final class NgaySinhPicker extends JPanel {
    private static final DateTimeFormatter DINH_DANG =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", new Locale("vi", "VN"));
    private final JTextField hienThi = new JTextField(13);
    private LocalDate ngayDaChon;

    public NgaySinhPicker(LocalDate banDau) {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        hienThi.setEditable(false);
        hienThi.setToolTipText("Bấm 'Chọn ngày' để chọn ngày, tháng và năm");
        JButton nutLich = new JButton("Chọn ngày", new IconLich());
        nutLich.setToolTipText("Mở lịch chọn ngày sinh");
        nutLich.addActionListener(e -> moLich());
        add(hienThi, BorderLayout.CENTER);
        add(nutLich, BorderLayout.EAST);
        datNgay(banDau);
    }

    public LocalDate layNgay() {
        return ngayDaChon;
    }

    public void datNgay(LocalDate ngay) {
        ngayDaChon = ngay;
        hienThi.setText(hienThiNgay(ngay));
    }

    public static String hienThiNgay(LocalDate ngay) {
        return ngay == null ? "" : DINH_DANG.format(ngay);
    }

    private void moLich() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new HopThoaiLich(owner).setVisible(true);
    }

    /** Icon lich ve bang Java2D, hien duoc tren Windows khong can font emoji. */
    private static final class IconLich implements Icon {
        @Override public int getIconWidth() { return 17; }
        @Override public int getIconHeight() { return 17; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(40, 102, 169));
            g2.fillRoundRect(x + 1, y + 3, 15, 13, 3, 3);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 3, y + 8, 11, 6);
            g2.setColor(new Color(40, 102, 169));
            g2.fillRect(x + 5, y + 1, 2, 5);
            g2.fillRect(x + 10, y + 1, 2, 5);
            g2.dispose();
        }
    }

    private final class HopThoaiLich extends JDialog {
        private final JSpinner thang;
        private final JSpinner nam;
        private final JPanel oNgay = new JPanel(new GridLayout(7, 7, 4, 4));
        private YearMonth dangXem;
        private boolean dangDongBo;
        private final JButton truoc = new JButton("<");
        private final JButton sau = new JButton(">");

        HopThoaiLich(Window chu) {
            super(chu, "Chọn ngày sinh", Dialog.ModalityType.APPLICATION_MODAL);
            LocalDate macDinh = ngayDaChon == null
                    ? LocalDate.now().minusYears(10) : ngayDaChon;
            dangXem = YearMonth.from(macDinh);
            thang = new JSpinner(new SpinnerNumberModel(dangXem.getMonthValue(), 1, 12, 1));
            nam = new JSpinner(new SpinnerNumberModel(dangXem.getYear(), 1800,
                    LocalDate.now().getYear(), 1));

            JPanel dau = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
            dau.add(truoc);
            dau.add(new JLabel("Tháng:"));
            dau.add(thang);
            dau.add(new JLabel("Năm:"));
            dau.add(nam);
            dau.add(sau);
            truoc.addActionListener(e -> chuyenThang(-1));
            sau.addActionListener(e -> chuyenThang(1));
            thang.addChangeListener(e -> doiThangNam());
            nam.addChangeListener(e -> doiThangNam());

            oNgay.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            JPanel chan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
            JButton homNay = new JButton("Hôm nay");
            JButton xoa = new JButton("Xóa ngày");
            JButton huy = new JButton("Hủy");
            homNay.addActionListener(e -> chon(LocalDate.now()));
            xoa.addActionListener(e -> chon(null));
            huy.addActionListener(e -> dispose());
            chan.add(homNay);
            chan.add(xoa);
            chan.add(huy);

            setLayout(new BorderLayout(5, 5));
            add(dau, BorderLayout.NORTH);
            add(oNgay, BorderLayout.CENTER);
            add(chan, BorderLayout.SOUTH);
            capNhatLich();
            setSize(460, 410);
            setResizable(false);
            setLocationRelativeTo(chu);
        }

        private void doiThangNam() {
            if (dangDongBo) return;
            dangXem = YearMonth.of((int) nam.getValue(), (int) thang.getValue());
            capNhatLich();
        }

        private void chuyenThang(int soThang) {
            YearMonth moi = dangXem.plusMonths(soThang);
            YearMonth homNay = YearMonth.from(LocalDate.now());
            if (moi.getYear() < 1800 || moi.isAfter(homNay)) return;
            dangXem = moi;
            dangDongBo = true;
            thang.setValue(dangXem.getMonthValue());
            nam.setValue(dangXem.getYear());
            dangDongBo = false;
            capNhatLich();
        }

        private void capNhatLich() {
            oNgay.removeAll();
            YearMonth gioiHan = YearMonth.from(LocalDate.now());
            truoc.setEnabled(dangXem.getYear() > 1800 || dangXem.getMonthValue() > 1);
            sau.setEnabled(dangXem.isBefore(gioiHan));
            String[] thu = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (String t : thu) {
                JLabel label = new JLabel(t, SwingConstants.CENTER);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                oNgay.add(label);
            }
            int batDau = dangXem.atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < 42; i++) {
                int ngay = i - batDau + 1;
                if (ngay < 1 || ngay > dangXem.lengthOfMonth()) {
                    oNgay.add(new JLabel(""));
                    continue;
                }
                LocalDate giaTri = dangXem.atDay(ngay);
                JButton nut = new JButton(String.valueOf(ngay));
                nut.setMargin(new java.awt.Insets(2, 2, 2, 2));
                nut.setEnabled(!giaTri.isAfter(LocalDate.now()));
                if (giaTri.equals(ngayDaChon)) {
                    Color nen = UIManager.getColor("Table.selectionBackground");
                    Color chu = UIManager.getColor("Table.selectionForeground");
                    if (nen != null) nut.setBackground(nen);
                    if (chu != null) nut.setForeground(chu);
                    nut.setOpaque(true);
                }
                nut.addActionListener(e -> chon(giaTri));
                oNgay.add(nut);
            }
            oNgay.revalidate();
            oNgay.repaint();
        }

        private void chon(LocalDate ngay) {
            datNgay(ngay);
            dispose();
        }
    }
}
