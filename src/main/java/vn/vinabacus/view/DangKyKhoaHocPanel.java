package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
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
import vn.vinabacus.controller.DangKyKhoaHocController;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.KhachTiemNang;

/** Danh sách đăng ký Trung tâm và lối vào form nhiều khóa dùng chung. */
public final class DangKyKhoaHocPanel extends JPanel {
    private static final String[] COT = {
        "Mã ĐK", "Mã HV", "Học viên", "Môn học", "Khóa học", "Số buổi",
        "Bắt đầu", "Trạng thái", "Lớp"
    };
    private final DangKyKhoaHocController controller = new DangKyKhoaHocController();
    private final JTextField txtTim = new JTextField(19);
    private final JComboBox<TrangThaiChon> locTrangThai = new JComboBox<>(new TrangThaiChon[]{
        new TrangThaiChon(null, "Tất cả"),
        new TrangThaiChon("CHO_XEP_LOP", "Chờ xếp lớp"),
        new TrangThaiChon("DANG_HOC", "Đang học"),
        new TrangThaiChon("HOAN_THANH", "Hoàn thành"),
        new TrangThaiChon("DA_CHUYEN", "Đã chuyển"),
        new TrangThaiChon("NGHI_HAN", "Nghỉ hẳn"),
        new TrangThaiChon("HUY", "Đã hủy")
    });
    private final DefaultTableModel model = new DefaultTableModel(COT, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
        @Override public Class<?> getColumnClass(int col) {
            return col <= 1 || col == 5 ? Integer.class : String.class;
        }
    };
    private final JTable bang = new JTable(model);
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final JButton btnXem = new JButton("Xem đăng ký");
    private final JButton btnXep = new JButton("Xếp lớp");
    private final JButton btnChuyen = new JButton("Chuyển thành học viên");
    private List<DangKyKhoaHoc> danhSach = List.of();
    private int luotTai;
    private Integer maCanChon;

    private record TrangThaiChon(String ma, String ten) {
        @Override public String toString() { return ten; }
    }

    public DangKyKhoaHocPanel() {
        super(new BorderLayout(8, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel tieuDe = new JLabel("ĐĂNG KÝ KHÓA HỌC – TRUNG TÂM");
        tieuDe.setFont(GiaoDienMau.font(Font.BOLD, 22));
        tieuDe.setForeground(GiaoDienMau.XANH_DAM);
        JLabel moTa = new JLabel("Mỗi khóa có đăng ký và tình trạng xếp lớp riêng. Chọn một dòng để xem hoặc xếp lớp.");
        moTa.setForeground(GiaoDienMau.CHU_MO);
        JPanel dau = new JPanel(new BorderLayout(0, 8));
        dau.setOpaque(false);
        JPanel tieuDeVaMoTa = new JPanel(new BorderLayout(0, 3));
        tieuDeVaMoTa.setOpaque(false);
        tieuDeVaMoTa.add(tieuDe, BorderLayout.NORTH);
        tieuDeVaMoTa.add(moTa, BorderLayout.SOUTH);
        dau.add(tieuDeVaMoTa, BorderLayout.NORTH);
        JPanel boLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        boLoc.setOpaque(false);
        JButton tim = new JButton("Tìm kiếm");
        JButton taiLai = new JButton("Tải lại");
        boLoc.add(new JLabel("Mã / học viên / khóa học:"));
        boLoc.add(txtTim);
        boLoc.add(new JLabel("Trạng thái:"));
        boLoc.add(locTrangThai);
        boLoc.add(tim);
        boLoc.add(taiLai);
        JPanel thaoTac = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        thaoTac.setOpaque(false);
        JButton btnTao = new NutMau("+ Đăng ký khóa học", NutMau.Kieu.CHINH);
        thaoTac.add(btnTao);
        thaoTac.add(btnXem);
        thaoTac.add(btnXep);
        thaoTac.add(btnChuyen);
        JPanel congCu = new JPanel(new BorderLayout(0, 2));
        congCu.setOpaque(false);
        congCu.add(boLoc, BorderLayout.NORTH);
        congCu.add(thaoTac, BorderLayout.SOUTH);
        dau.add(congCu, BorderLayout.SOUTH);
        add(dau, BorderLayout.NORTH);

        bang.setRowHeight(30);
        bang.setAutoCreateRowSorter(true);
        bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] rongCot = {65, 65, 175, 155, 190, 70, 105, 125, 190};
        for (int i = 0; i < rongCot.length; i++)
            bang.getColumnModel().getColumn(i).setPreferredWidth(rongCot[i]);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) capNhatNut();
        });
        bang.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) xemDangKy();
            }
        });
        add(new JScrollPane(bang), BorderLayout.CENTER);
        thongBao.setBorder(BorderFactory.createEmptyBorder(3, 2, 0, 0));
        add(thongBao, BorderLayout.SOUTH);
        tim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        taiLai.addActionListener(e -> taiDuLieu());
        btnTao.addActionListener(e -> DangKyKhoaHocForm.moDangKy(this, null, null,
                k -> hienTatCaVaTaiLai()));
        btnXem.addActionListener(e -> xemDangKy());
        btnXep.addActionListener(e -> {
            DangKyKhoaHoc dk = dangChon();
            if (dk != null) moXepLop(this, dk, () -> {
                maCanChon = dk.maDangKy(); hienTatCaVaTaiLai();
            });
        });
        btnChuyen.addActionListener(e -> {
            DangKyKhoaHoc dk = dangChon();
            if (dk == null || dk.maKhach() == null) return;
            timHoSoVaChuyen(dk.maKhach());
        });
        capNhatNut();
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private DangKyKhoaHoc dangChon() {
        int dong = bang.getSelectedRow();
        int index = dong < 0 ? -1 : bang.convertRowIndexToModel(dong);
        return index >= 0 && index < danhSach.size() ? danhSach.get(index) : null;
    }

    private void capNhatNut() {
        DangKyKhoaHoc dk = dangChon();
        btnXem.setEnabled(dk != null);
        btnXep.setEnabled(dk != null && "CHO_XEP_LOP".equals(dk.trangThai()));
        btnChuyen.setEnabled(dk != null && dk.maKhach() != null
                && DangKyKhoaHocForm.coLopDuChuyen(dk));
    }

    private void taiDuLieu() {
        final int luot = ++luotTai;
        final String tuKhoa = txtTim.getText().trim();
        final TrangThaiChon chon = (TrangThaiChon) locTrangThai.getSelectedItem();
        thongBao.setText("Đang tải danh sách đăng ký...");
        new SwingWorker<List<DangKyKhoaHoc>, Void>() {
            @Override protected List<DangKyKhoaHoc> doInBackground() throws Exception {
                return controller.timKiem(tuKhoa, chon == null ? null : chon.ma());
            }
            @Override protected void done() {
                if (luot != luotTai) return;
                try {
                    danhSach = get();
                    model.setRowCount(0);
                    for (DangKyKhoaHoc dk : danhSach) {
                        model.addRow(new Object[]{dk.maDangKy(), dk.maHocVien(), dk.hoTen(),
                            dk.tenMonHoc(), dk.tenKhoaHoc(), dk.tongSoBuoi(),
                            NgayKhaiGiangPicker.hienThiNgay(dk.ngayBatDau()),
                            DangKyKhoaHocForm.tenTrangThai(dk.trangThai()),
                            DangKyKhoaHocForm.tenTinhTrangLop(dk)});
                    }
                    thongBao.setText("Có " + danhSach.size() + " đăng ký phù hợp");
                    chonTheoMa();
                    capNhatNut();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); DangKyKhoaHocForm.hienLoi(DangKyKhoaHocPanel.this, ex);
                } catch (ExecutionException ex) {
                    DangKyKhoaHocForm.hienLoi(DangKyKhoaHocPanel.this, ex);
                }
            }
        }.execute();
    }

    private void chonTheoMa() {
        if (maCanChon == null) return;
        for (int i = 0; i < danhSach.size(); i++) {
            if (danhSach.get(i).maDangKy() == maCanChon) {
                int view = bang.convertRowIndexToView(i);
                if (view >= 0) bang.getSelectionModel().setSelectionInterval(view, view);
                break;
            }
        }
        maCanChon = null;
    }

    private void hienTatCaVaTaiLai() {
        txtTim.setText("");
        if (locTrangThai.getSelectedIndex() == 0) taiDuLieu();
        else locTrangThai.setSelectedIndex(0);
    }

    private void xemDangKy() {
        DangKyKhoaHoc dk = dangChon();
        if (dk == null) return;
        DangKyKhoaHocForm.moXemDangKy(this, dk, k -> {
            maCanChon = dk.maDangKy(); hienTatCaVaTaiLai();
        });
    }

    private void timHoSoVaChuyen(int maKhach) {
        thongBao.setText("Đang tải hồ sơ tuyển sinh...");
        new SwingWorker<KhachTiemNang, Void>() {
            @Override protected KhachTiemNang doInBackground() throws Exception {
                return controller.hoSoTuyenSinh().stream()
                        .filter(k -> k.maKhach() == maKhach).findFirst().orElse(null);
            }
            @Override protected void done() {
                try {
                    KhachTiemNang hoSo = get();
                    if (hoSo == null) {
                        JOptionPane.showMessageDialog(DangKyKhoaHocPanel.this,
                                "Không tìm thấy hồ sơ tuyển sinh tương ứng.");
                    } else moChuyenThanhHocVien(DangKyKhoaHocPanel.this, hoSo,
                            () -> hienTatCaVaTaiLai());
                    thongBao.setText("Sẵn sàng");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); DangKyKhoaHocForm.hienLoi(DangKyKhoaHocPanel.this, ex);
                } catch (ExecutionException ex) {
                    DangKyKhoaHocForm.hienLoi(DangKyKhoaHocPanel.this, ex);
                }
            }
        }.execute();
    }

    public static void moDangKyTuHoSo(Component parent, KhachTiemNang hoSo,
            Consumer<DangKyKhoaHoc.KetQua> onSaved) {
        DangKyKhoaHocForm.moDangKy(parent, hoSo, null, onSaved);
    }

    public static void moDangKyTuHocVien(Component parent, HocVien hocVien,
            Consumer<DangKyKhoaHoc.KetQua> onSaved) {
        DangKyKhoaHocForm.moDangKy(parent, null, hocVien, onSaved);
    }

    public static void moXepLop(Component parent, DangKyKhoaHoc dk, Runnable onSaved) {
        DangKyKhoaHocForm.moXepLop(parent, dk, onSaved);
    }

    public static void moChuyenThanhHocVien(Component parent, KhachTiemNang hoSo,
            Runnable onSaved) {
        DangKyKhoaHocForm.moChuyenThanhHocVien(parent, hoSo, onSaved);
    }
}
