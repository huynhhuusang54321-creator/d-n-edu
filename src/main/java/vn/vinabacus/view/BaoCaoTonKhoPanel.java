package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import vn.vinabacus.controller.BaoCaoTonKhoController;
import vn.vinabacus.model.BaoCaoTonKho;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.util.XuatBaoCaoTonKhoExcel;

public class BaoCaoTonKhoPanel extends JPanel {

    private final CoSo coSo;
    private final BaoCaoTonKhoController controller = new BaoCaoTonKhoController();
    private final JComboBox<Integer> cboThang;
    private final JSpinner spNam;
    private final JTextField txtTimKiem;
    private final DefaultTableModel model;
    private final JTable bangBaoCao;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JLabel lblKetQua = new JLabel(" ");
    private YearMonth thangHeThong = YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    public BaoCaoTonKhoPanel(CoSo coSo) {
        this.coSo = coSo;
        setLayout(new BorderLayout(8, 8));

        LocalDate homNay = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        JPanel panelTren = new JPanel(new BorderLayout(0, 8));
        JLabel lblTieuDe = new JLabel("BÁO CÁO TỒN KHO THEO THÁNG");
        lblTieuDe.setFont(new Font("Arial", Font.BOLD, 18));
        panelTren.add(lblTieuDe, BorderLayout.NORTH);

        // Thanh cong cu: da bo nut "Xem bao cao" bi trung chuc nang.
        JPanel congCu = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        cboThang = new JComboBox<>();
        for (int i = 1; i <= 12; i++) {
            cboThang.addItem(i);
        }
        cboThang.setSelectedItem(homNay.getMonthValue());
        spNam = new JSpinner(new SpinnerNumberModel(homNay.getYear(), 2000, 2100, 1));
        ((JSpinner.DefaultEditor) spNam.getEditor()).getTextField().setColumns(5);
        txtTimKiem = new JTextField(18);
        JButton btnLamMoi = new JButton("Làm mới");
        JButton btnXuatExcel = new JButton("Xuất Excel");

        congCu.add(new JLabel("Tháng:"));
        congCu.add(cboThang);
        congCu.add(new JLabel("Năm:"));
        congCu.add(spNam);
        congCu.add(btnLamMoi);
        congCu.add(btnXuatExcel);
        congCu.add(new JLabel("Tìm kiếm:"));
        congCu.add(txtTimKiem);
        panelTren.add(congCu, BorderLayout.CENTER);
        add(panelTren, BorderLayout.NORTH);

        // Bang bao cao ton kho: giu nguyen don gia ban, xuat HV, xuat CS.
        String[] tenCot = {
            "Mã HH", "Mã SP", "Tên hàng hóa", "ĐVT", "Đơn giá xuất bán",
            "Tồn đầu", "Nhập", "Xuất HV", "Xuất CS", "Điều chỉnh", "Tồn cuối"
        };
        model = new DefaultTableModel(tenCot, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 0) return Integer.class;
                if (c >= 4) return BigDecimal.class;
                return String.class;
            }
        };

        bangBaoCao = new JTable(model);
        bangBaoCao.setRowHeight(30);
        bangBaoCao.setFont(new Font("Arial", Font.PLAIN, 13));
        bangBaoCao.getTableHeader().setReorderingAllowed(false);
        bangBaoCao.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        bangBaoCao.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {65, 120, 210, 65, 135, 90, 75, 90, 90, 95, 95};
        for (int i = 0; i < widths.length; i++) {
            bangBaoCao.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer rendererSo = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof BigDecimal bd) {
                    super.setValue(bd.stripTrailingZeros().toPlainString());
                } else {
                    super.setValue(value);
                }
            }
        };
        rendererSo.setHorizontalAlignment(JLabel.RIGHT);
        bangBaoCao.setDefaultRenderer(BigDecimal.class, rendererSo);
        sorter = new TableRowSorter<>(model);
        bangBaoCao.setRowSorter(sorter);
        add(new JScrollPane(bangBaoCao), BorderLayout.CENTER);

        JPanel chanTrang = new JPanel(new BorderLayout());
        chanTrang.add(lblKetQua, BorderLayout.NORTH);
        chanTrang.add(new JLabel(
            "Kỳ mở: tính các phiếu xác nhận trong tháng. Kỳ chốt: đọc số liệu đã chốt."
        ), BorderLayout.SOUTH);
        add(chanTrang, BorderLayout.SOUTH);

        btnLamMoi.addActionListener(e -> taiBaoCao());
        btnXuatExcel.addActionListener(e -> xuatExcel());
        cboThang.addActionListener(e -> taiBaoCao());
        spNam.addChangeListener(e -> taiBaoCao());
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apDungTimKiem(); }
            @Override public void removeUpdate(DocumentEvent e) { apDungTimKiem(); }
            @Override public void changedUpdate(DocumentEvent e) { apDungTimKiem(); }
        });
        taiBaoCao();

        // Moi phut kiem tra qua thang moi va tai lai bao cao neu dang mo tab.
        // Neu dang xem thang cu de xuat Excel, khong ep chuyen thang.
        Timer tuCapNhat = new Timer(60_000, e -> {
            YearMonth moi = YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            if (!moi.equals(thangHeThong)) {
                boolean dangXemThangHienTai =
                        ((Integer) cboThang.getSelectedItem()) == thangHeThong.getMonthValue()
                        && ((Number) spNam.getValue()).intValue() == thangHeThong.getYear();
                thangHeThong = moi;
                if (dangXemThangHienTai) {
                    spNam.setValue(moi.getYear());
                    cboThang.setSelectedItem(moi.getMonthValue());
                }
            }
            if (isShowing()) {
                taiBaoCao(false);
            }
        });
        tuCapNhat.start();
    }

    // Tai lai du lieu theo thang va nam dang chon.
    private void taiBaoCao() {
        taiBaoCao(true);
    }

    // Tu dong lam moi: khong hien hop thoai loi lap lai moi phut.
    private void taiBaoCao(boolean hienThiLoi) {
        int thang = (Integer) cboThang.getSelectedItem();
        int nam = ((Number) spNam.getValue()).intValue();
        model.setRowCount(0);
        try {
            List<BaoCaoTonKho> ds = controller.layBaoCao(coSo, nam, thang);
            for (BaoCaoTonKho bc : ds) {
                model.addRow(new Object[]{
                    bc.getMaHangHoa(), bc.getMaSanPham(), bc.getTenHangHoa(),
                    bc.getDonViTinh(), bc.getDonGiaXuatBan(), bc.getTonDau(),
                    bc.getNhap(), bc.getXuatHocVien(), bc.getXuatCoSo(),
                    bc.getDieuChinh(), bc.getTonCuoi()
                });
            }
            apDungTimKiem();
        } catch (SQLException | IllegalArgumentException ex) {
            lblKetQua.setText("Không tải được báo cáo tháng " + thang + "/" + nam);
            if (hienThiLoi) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Báo cáo tồn kho", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // Xoa dau tieng Viet va khong phan biet chu HOA/thuong.
    private String chuanHoa(String chuoi) {
        if (chuoi == null) return "";
        String s = Normalizer.normalize(chuoi, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replace('đ', 'd');
    }

    // Tim o bat ky vi tri nao, khong dau, nhieu tu khong can dung thu tu.
    private void apDungTimKiem() {
        String tuKhoa = chuanHoa(txtTimKiem.getText()).trim();
        if (tuKhoa.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            String[] cacTu = tuKhoa.split("\\s+");
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel,
                        ? extends Integer> entry) {
                    // Tim theo Ma SP, Ten hang hoa va Don vi tinh.
                    String noiDung = chuanHoa(
                        String.valueOf(entry.getValue(1)) + " " +
                        String.valueOf(entry.getValue(2)) + " " +
                        String.valueOf(entry.getValue(3))
                    );
                    for (String tu : cacTu) {
                        if (!noiDung.contains(tu)) return false;
                    }
                    return true;
                }
            });
        }
        int nam = ((Number) spNam.getValue()).intValue();
        lblKetQua.setText("Tháng " + cboThang.getSelectedItem() + "/" + nam
            + " — " + bangBaoCao.getRowCount() + " mặt hàng phù hợp");
    }

    // Xuat dung cac dong dang hien thi sau khi loc/sap xep.
    private void xuatExcel() {
        if (bangBaoCao.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Không có hàng hóa nào đang hiển thị để xuất Excel!");
            return;
        }
        int thang = (Integer) cboThang.getSelectedItem();
        int nam = ((Number) spNam.getValue()).intValue();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu báo cáo tồn kho Excel");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        chooser.setSelectedFile(new File(
            "BaoCaoTonKho_" + coSo.name() + "_" + thang + "_" + nam + ".xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }
        if (file.exists()) {
            int xacNhan = JOptionPane.showConfirmDialog(this,
                "File đã tồn tại. Bạn muốn ghi đè không?\n" + file.getAbsolutePath(),
                "Xác nhận ghi đè", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (xacNhan != JOptionPane.YES_OPTION) return;
        }

        List<BaoCaoTonKho> hienThi = new ArrayList<>();
        for (int row = 0; row < bangBaoCao.getRowCount(); row++) {
            int r = bangBaoCao.convertRowIndexToModel(row);
            hienThi.add(new BaoCaoTonKho(
                ((Number) model.getValueAt(r, 0)).intValue(),
                String.valueOf(model.getValueAt(r, 1)),
                String.valueOf(model.getValueAt(r, 2)),
                String.valueOf(model.getValueAt(r, 3)),
                (BigDecimal) model.getValueAt(r, 5),
                (BigDecimal) model.getValueAt(r, 6),
                (BigDecimal) model.getValueAt(r, 7),
                (BigDecimal) model.getValueAt(r, 8),
                (BigDecimal) model.getValueAt(r, 9),
                (BigDecimal) model.getValueAt(r, 10),
                (BigDecimal) model.getValueAt(r, 4)
            ));
        }
        try {
            XuatBaoCaoTonKhoExcel.xuat(
                file.toPath(), hienThi, coSo.toString(), thang, nam);
            JOptionPane.showMessageDialog(this,
                "Xuất Excel thành công: " + hienThi.size() + " mặt hàng.\n"
                + file.getAbsolutePath());
        } catch (IOException | SecurityException ex) {
            JOptionPane.showMessageDialog(this,
                "Không xuất được file Excel:\n" + ex.getMessage(),
                "Lỗi xuất Excel", JOptionPane.ERROR_MESSAGE);
        }
    }
}
