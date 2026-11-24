package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.MonHocController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.MonHoc;

/** Giao dien quan ly Mon hoc Trung tam: khong can sua MainFrame/MenuConfig. */
public final class MonHocPanel extends JPanel {
    private final MonHocController controller = new MonHocController(CoSo.TRUNG_TAM);
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Hoạt động", "Ngừng sử dụng"
    });
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Tên môn học", "Ghi chú", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final JLabel lblThongBao = new JLabel("Sẵn sàng");
    private List<MonHoc> danhSach = List.of();

    public MonHocPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel tieuDe = new JLabel("QUẢN LÝ MÔN HỌC – TRUNG TÂM");
        tieuDe.setFont(tieuDe.getFont().deriveFont(Font.BOLD, 22f));
        JPanel thaoTac = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton btnTim = new JButton("Tìm kiếm");
        JButton btnTaiLai = new JButton("Tải lại");
        JButton btnThem = new JButton("Thêm");
        JButton btnSua = new JButton("Sửa");
        thaoTac.add(new JLabel("Tên môn học:"));
        thaoTac.add(txtTim);
        thaoTac.add(new JLabel("Trạng thái:"));
        thaoTac.add(locTrangThai);
        thaoTac.add(btnTim);
        thaoTac.add(btnTaiLai);
        thaoTac.add(btnThem);
        thaoTac.add(btnSua);
        JPanel dau = new JPanel(new BorderLayout(6, 8));
        dau.add(tieuDe, BorderLayout.NORTH);
        dau.add(thaoTac, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);
        bang.setRowHeight(30);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setAutoCreateRowSorter(true);
        add(new JScrollPane(bang), BorderLayout.CENTER);
        add(lblThongBao, BorderLayout.SOUTH);
        btnTim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        btnTaiLai.addActionListener(e -> {
            txtTim.setText("");
            locTrangThai.setSelectedIndex(0);
            taiDuLieu();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnSua.addActionListener(e -> {
            MonHoc mon = monDuocChon();
            if (mon != null) moForm(mon);
        });
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private Boolean trangThaiLoc() {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 -> Boolean.TRUE;
            case 2 -> Boolean.FALSE;
            default -> null;
        };
    }

    private void taiDuLieu() {
        final String tim = txtTim.getText();
        final Boolean trangThai = trangThaiLoc();
        lblThongBao.setText("Đang tải dữ liệu...");
        new SwingWorker<List<MonHoc>, Void>() {
            @Override protected List<MonHoc> doInBackground() throws Exception {
                return controller.timKiem(tim, trangThai);
            }
            @Override protected void done() {
                try {
                    danhSach = get();
                    model.setRowCount(0);
                    for (MonHoc m : danhSach) {
                        model.addRow(new Object[]{m.tenMonHoc(),
                                m.ghiChu(), m.trangThai() ? "Hoạt động" : "Ngừng sử dụng"});
                    }
                    lblThongBao.setText("Tìm thấy " + danhSach.size() + " môn học");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    hienLoi(e);
                } catch (ExecutionException e) {
                    hienLoi(e);
                }
            }
        }.execute();
    }

    private MonHoc monDuocChon() {
        int row = bang.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một môn học trong bảng");
            return null;
        }
        return danhSach.get(bang.convertRowIndexToModel(row));
    }

    private void moForm(MonHoc cu) {
        JTextField ten = new JTextField(cu == null ? "" : cu.tenMonHoc(), 25);
        JTextField ghiChu = new JTextField(cu == null || cu.ghiChu() == null
                ? "" : cu.ghiChu(), 25);
        JComboBox<String> trangThai = new JComboBox<>(new String[]{"Hoạt động", "Ngừng sử dụng"});
        if (cu != null && !cu.trangThai()) trangThai.setSelectedIndex(1);
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Tên môn học (*)")); p.add(ten);
        p.add(new JLabel("Ghi chú")); p.add(ghiChu);
        p.add(new JLabel("Trạng thái")); p.add(trangThai);
        int ok = JOptionPane.showConfirmDialog(this, p,
                cu == null ? "Thêm môn học" : "Sửa môn học",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        String tenMoi = ten.getText();
        String ghiChuMoi = ghiChu.getText();
        boolean hoatDong = trangThai.getSelectedIndex() == 0;
        if (tenMoi == null || tenMoi.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên môn học");
            return;
        }
        lblThongBao.setText("Đang lưu dữ liệu...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (cu == null) {
                    controller.them(tenMoi, ghiChuMoi, hoatDong);
                    return "Đã thêm môn học";
                }
                controller.sua(cu.maMonHoc(), tenMoi, ghiChuMoi, hoatDong);
                return "Đã cập nhật môn học";
            }
            @Override protected void done() {
                try {
                    JOptionPane.showMessageDialog(MonHocPanel.this, get());
                    taiDuLieu();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    hienLoi(e);
                } catch (ExecutionException e) {
                    hienLoi(e);
                }
            }
        }.execute();
    }

    private void hienLoi(Exception e) {
        Throwable goc = e;
        while (goc.getCause() != null && goc.getCause() != goc) goc = goc.getCause();
        String msg = goc.getMessage();
        if (goc instanceof SQLException se && (se.getErrorCode() == 2601 || se.getErrorCode() == 2627)) {
            msg = "Tên môn học đã tồn tại trong Trung tâm";
        }
        if (msg == null || msg.isBlank()) msg = "Không thể xử lý yêu cầu";
        lblThongBao.setText("Lỗi: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
