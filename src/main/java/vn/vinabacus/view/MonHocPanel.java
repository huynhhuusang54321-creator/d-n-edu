package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.MonHoc;

/** Giao dien quan ly Mon hoc Trung tam: khong can sua MainFrame/MenuConfig. */
public final class MonHocPanel extends JPanel {
    private final MonHocController controller = new MonHocController(CoSo.TRUNG_TAM);
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Hoạt động", "Ngừng sử dụng"
    });
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã môn", "Tên môn học", "Mô tả", "Số khóa", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final JLabel lblThongBao = new JLabel("Sẵn sàng");
    private final JButton btnSua = new JButton("Sửa môn học");
    private final JButton btnChiTiet = new JButton("Xem chi tiết");
    private final JButton btnTrangThai = new JButton("Ngừng / kích hoạt");
    private List<MonHoc> danhSach = List.of();
    private List<KhoaHoc> khoaHoc = List.of();
    private int lanTai;

    private record DuLieu(List<MonHoc> mon, List<KhoaHoc> khoa) { }

    public MonHocPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel tieuDe = new JLabel("QUẢN LÝ MÔN HỌC – TRUNG TÂM");
        tieuDe.setFont(tieuDe.getFont().deriveFont(Font.BOLD, 22f));
        JPanel thaoTac = new JPanel(new GridLayout(2, 1, 0, 2));
        JPanel hangLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        JButton btnTim = new JButton("Tìm kiếm");
        JButton btnTaiLai = new JButton("Tải lại");
        JButton btnThem = new JButton("Thêm môn học");
        hangLoc.add(new JLabel("Mã / tên môn:"));
        hangLoc.add(txtTim);
        hangLoc.add(new JLabel("Trạng thái:"));
        hangLoc.add(locTrangThai);
        hangLoc.add(btnTim);
        hangLoc.add(btnTaiLai);
        hangNut.add(btnThem);
        hangNut.add(btnSua);
        hangNut.add(btnChiTiet);
        hangNut.add(btnTrangThai);
        thaoTac.add(hangLoc);
        thaoTac.add(hangNut);
        JPanel dau = new JPanel(new BorderLayout(6, 8));
        dau.add(tieuDe, BorderLayout.NORTH);
        dau.add(thaoTac, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);
        bang.setRowHeight(30);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setAutoCreateRowSorter(true);
        bang.getSelectionModel().addListSelectionListener(e -> capNhatNut());
        bang.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && bang.getSelectedRow() >= 0) moChiTiet();
            }
        });
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
        btnChiTiet.addActionListener(e -> moChiTiet());
        btnTrangThai.addActionListener(e -> doiTrangThai());
        capNhatNut();
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
        final int luot = ++lanTai;
        lblThongBao.setText("Đang tải dữ liệu...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.timKiem(tim, trangThai), controller.khoaHoc());
            }
            @Override protected void done() {
                if (luot != lanTai) return;
                try {
                    DuLieu dl = get();
                    danhSach = dl.mon();
                    khoaHoc = dl.khoa();
                    model.setRowCount(0);
                    for (MonHoc m : danhSach) {
                        long soKhoa = khoaHoc.stream()
                                .filter(k -> k.maMonHoc() == m.maMonHoc()).count();
                        model.addRow(new Object[]{m.maMonHoc(), m.tenMonHoc(),
                                m.ghiChu(), soKhoa,
                                m.trangThai() ? "Hoạt động" : "Ngừng sử dụng"});
                    }
                    lblThongBao.setText(danhSach.isEmpty()
                            ? "Chưa có môn học phù hợp. Có thể thêm môn học mới."
                            : "Tìm thấy " + danhSach.size() + " môn học");
                    capNhatNut();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    danhSach = List.of(); khoaHoc = List.of(); model.setRowCount(0);
                    hienLoi(e);
                } catch (ExecutionException e) {
                    danhSach = List.of(); khoaHoc = List.of(); model.setRowCount(0);
                    hienLoi(e);
                }
            }
        }.execute();
    }

    private void capNhatNut() {
        boolean coChon = bang.getSelectedRow() >= 0;
        btnSua.setEnabled(coChon);
        btnChiTiet.setEnabled(coChon);
        btnTrangThai.setEnabled(coChon);
    }

    private void doiTrangThai() {
        MonHoc mon = monDuocChon();
        if (mon == null) return;
        String viec = mon.trangThai() ? "ngừng sử dụng" : "kích hoạt lại";
        if (JOptionPane.showConfirmDialog(this,
                "Xác nhận " + viec + " môn '" + mon.tenMonHoc() + "'?",
                "Trạng thái môn học", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        lblThongBao.setText("Đang cập nhật trạng thái...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.sua(mon.maMonHoc(), mon.tenMonHoc(), mon.ghiChu(), !mon.trangThai());
                return null;
            }
            @Override protected void done() {
                try { get(); taiDuLieu(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); hienLoi(e); }
                catch (ExecutionException e) { hienLoi(e); }
            }
        }.execute();
    }

    private void moChiTiet() {
        MonHoc mon = monDuocChon();
        if (mon == null) return;
        List<KhoaHoc> thuocMon = new ArrayList<>();
        for (KhoaHoc k : khoaHoc) if (k.maMonHoc() == mon.maMonHoc()) thuocMon.add(k);
        DefaultTableModel dsKhoa = new DefaultTableModel(
                new String[]{"Mã khóa", "Tên khóa", "Số buổi", "Học phí", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable bangKhoa = new JTable(dsKhoa);
        bangKhoa.setRowHeight(28);
        JComboBox<String> loc = new JComboBox<>(new String[]{
                "Tất cả", "Đang tuyển sinh", "Ngừng tuyển sinh"});
        Runnable hien = () -> {
            dsKhoa.setRowCount(0);
            for (KhoaHoc k : thuocMon) {
                if (loc.getSelectedIndex() == 1 && !k.trangThai()) continue;
                if (loc.getSelectedIndex() == 2 && k.trangThai()) continue;
                dsKhoa.addRow(new Object[]{k.maKhoaHoc(), k.tenKhoaHoc(),
                        k.tongSoBuoi(), k.hocPhiMacDinh(),
                        k.trangThai() ? "Đang tuyển sinh" : "Ngừng tuyển sinh"});
            }
        };
        loc.addActionListener(e -> hien.run());
        hien.run();
        JPanel noiDung = new JPanel(new BorderLayout(8, 8));
        noiDung.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        noiDung.add(new JLabel("Mã môn " + mon.maMonHoc() + " · " + mon.tenMonHoc()
                + " · " + (mon.trangThai() ? "Hoạt động" : "Ngừng sử dụng")
                + " · " + thuocMon.size() + " khóa"), BorderLayout.NORTH);
        noiDung.add(new JScrollPane(bangKhoa), BorderLayout.CENTER);
        JPanel cuoi = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cuoi.add(new JLabel("Lọc:")); cuoi.add(loc);
        JButton quanLy = new JButton("Mở quản lý khóa học");
        cuoi.add(quanLy);
        noiDung.add(cuoi, BorderLayout.SOUTH);
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Chi tiết môn học", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(noiDung);
        dialog.setSize(800, 470);
        dialog.setLocationRelativeTo(this);
        quanLy.addActionListener(e -> {
            JDialog quanLyKhoa = new JDialog(dialog, "Khóa học của " + mon.tenMonHoc(), true);
            KhoaHocPanel panel = new KhoaHocPanel();
            panel.chonMonHoc(mon.maMonHoc());
            quanLyKhoa.setContentPane(panel);
            quanLyKhoa.setSize(1050, 620);
            quanLyKhoa.setLocationRelativeTo(dialog);
            quanLyKhoa.setVisible(true);
            taiDuLieu();
            dialog.dispose();
        });
        dialog.setVisible(true);
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
