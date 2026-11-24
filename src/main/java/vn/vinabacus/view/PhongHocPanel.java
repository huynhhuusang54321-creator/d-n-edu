package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import vn.vinabacus.controller.PhongHocController;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.PhongHocRepository;
import vn.vinabacus.service.PhongHocService;

/** Quản lý phòng học nằm trong cùng trang Quản lý lớp học của Trung tâm. */
public final class PhongHocPanel extends JPanel {
    private static final Color XANH = GiaoDienMau.XANH_DAM;
    private static final Color XANH_NEN = new Color(232, 246, 234);
    private static final Color HANG_CHAN = new Color(245, 250, 245);
    private static final Color DO = new Color(173, 33, 33);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final PhongHocController controller;
    private final JTextField timKiem = new JTextField(20);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Sẵn sàng", "Bảo trì", "Ngừng sử dụng"
    });
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "Mã phòng", "Tên phòng", "Sức chứa", "Trạng thái", "Lớp đang dùng",
            "Ghi chú", "Thao tác"
    }, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final JLabel thongBao = new JLabel("Danh sách phòng học");
    private final List<PhongHoc> tatCa = new ArrayList<>();
    private final List<PhongHoc> hienThi = new ArrayList<>();
    private final List<PhongHocRepository.LopSuDung> lopSuDung = new ArrayList<>();
    private final List<LichHocLop> lichSuDung = new ArrayList<>();
    private int lanTai;

    private record DuLieu(List<PhongHoc> phong,
                          List<PhongHocRepository.LopSuDung> lop,
                          List<LichHocLop> lich) { }

    public PhongHocPanel() { this(new PhongHocController()); }

    public PhongHocPanel(PhongHocController controller) {
        super(new BorderLayout(0, 12));
        this.controller = controller;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JPanel tren = new JPanel(new BorderLayout(0, 12));
        tren.setOpaque(false);
        JLabel title = new JLabel("QUẢN LÝ PHÒNG HỌC – TRUNG TÂM");
        title.setOpaque(true);
        title.setBackground(XANH);
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 5, 0, GiaoDienMau.VANG),
                BorderFactory.createEmptyBorder(11, 18, 11, 12)));
        title.setFont(GiaoDienMau.font(Font.BOLD, 21));
        tren.add(title, BorderLayout.NORTH);
        tren.add(taoThanhCongCu(), BorderLayout.CENTER);
        add(tren, BorderLayout.NORTH);
        taoBang();
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(GiaoDienMau.DUONG_VIEN));
        cuon.getViewport().setBackground(Color.WHITE);
        add(cuon, BorderLayout.CENTER);
        thongBao.setFont(GiaoDienMau.font(Font.PLAIN, 12));
        thongBao.setForeground(GiaoDienMau.CHU_MO);
        add(thongBao, BorderLayout.SOUTH);
    }

    private JPanel taoThanhCongCu() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(Color.WHITE);
        JPanel trai = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        trai.setOpaque(false);
        timKiem.setToolTipText("Nhập mã, tên hoặc ghi chú phòng học");
        timKiem.setPreferredSize(new Dimension(230, 34));
        locTrangThai.setPreferredSize(new Dimension(156, 34));
        JButton btnTim = new NutMau("Tìm kiếm", NutMau.Kieu.CHINH);
        JButton btnLamMoi = new NutMau("Làm mới", NutMau.Kieu.PHU);
        trai.add(new JLabel("Mã / tên phòng:"));
        trai.add(timKiem);
        trai.add(new JLabel("Trạng thái:"));
        trai.add(locTrangThai);
        trai.add(btnTim);
        trai.add(btnLamMoi);
        JButton btnThem = new NutMau("+ Thêm phòng", NutMau.Kieu.CHINH);
        JButton btnChiTiet = new NutMau("Chi tiết", NutMau.Kieu.PHU);
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 3));
        phai.setOpaque(false);
        phai.add(btnChiTiet);
        phai.add(btnThem);
        p.add(trai, BorderLayout.CENTER);
        p.add(phai, BorderLayout.EAST);
        btnTim.addActionListener(e -> apDungLoc());
        timKiem.addActionListener(e -> apDungLoc());
        locTrangThai.addActionListener(e -> apDungLoc());
        btnLamMoi.addActionListener(e -> {
            timKiem.setText("");
            locTrangThai.setSelectedIndex(0);
            taiDuLieu();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnChiTiet.addActionListener(e -> {
            PhongHoc pChon = phongDuocChon();
            if (pChon != null) moChiTiet(pChon);
        });
        return p;
    }

    private void taoBang() {
        bang.setRowHeight(49);
        bang.setFont(GiaoDienMau.font(Font.PLAIN, 14));
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getTableHeader().setPreferredSize(new Dimension(0, 45));
        bang.getTableHeader().setBackground(new Color(211, 239, 215));
        bang.getTableHeader().setForeground(GiaoDienMau.CHU);
        bang.getTableHeader().setFont(GiaoDienMau.font(Font.BOLD, 14));
        bang.setShowVerticalLines(true);
        bang.setGridColor(new Color(219, 231, 219));
        bang.setSelectionBackground(new Color(216, 243, 221));
        bang.setAutoCreateRowSorter(true);
        int[] w = {95, 155, 87, 123, 108, 170, 218};
        for (int i = 0; i < w.length; i++) bang.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        bang.getColumnModel().getColumn(0).setCellRenderer(new MauChuRenderer(true, false));
        bang.getColumnModel().getColumn(1).setCellRenderer(new MauChuRenderer(false, false));
        bang.getColumnModel().getColumn(2).setCellRenderer(new MauChuRenderer(false, true));
        bang.getColumnModel().getColumn(3).setCellRenderer(new TrangThaiRenderer());
        bang.getColumnModel().getColumn(4).setCellRenderer(new MauChuRenderer(false, true));
        bang.getColumnModel().getColumn(5).setCellRenderer(new MauChuRenderer(false, false));
        bang.getColumnModel().getColumn(6).setCellRenderer(new ThaoTacRenderer());
        bang.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int hang = bang.rowAtPoint(e.getPoint());
                int cot = bang.columnAtPoint(e.getPoint());
                if (hang < 0) return;
                int chiSo = bang.convertRowIndexToModel(hang);
                if (chiSo >= hienThi.size()) return;
                PhongHoc p = hienThi.get(chiSo);
                if (e.getClickCount() >= 2) { moChiTiet(p); return; }
                if (cot != 6) return;
                java.awt.Rectangle o = bang.getCellRect(hang, cot, true);
                int x = e.getX() - o.x;
                if (x < o.width / 2) moForm(p);
                else doiTrangThai(p);
            }
        });
    }

    public void taiDuLieu() {
        final int luot = ++lanTai;
        thongBao.setText("Đang tải dữ liệu phòng học...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.layDanhSach(), controller.layLopSuDung(),
                        controller.layLichSuDung());
            }
            @Override protected void done() {
                if (luot != lanTai) return;
                try {
                    DuLieu dl = get();
                    tatCa.clear(); tatCa.addAll(dl.phong());
                    lopSuDung.clear(); lopSuDung.addAll(dl.lop());
                    lichSuDung.clear(); lichSuDung.addAll(dl.lich());
                    apDungLoc();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); xoaDuLieuCu(); hienLoi(ex);
                } catch (ExecutionException ex) { xoaDuLieuCu(); hienLoi(ex); }
            }
        }.execute();
    }

    private void xoaDuLieuCu() {
        tatCa.clear(); hienThi.clear(); lopSuDung.clear(); lichSuDung.clear();
        model.setRowCount(0);
    }

    private void apDungLoc() {
        String tt = switch (locTrangThai.getSelectedIndex()) {
            case 1 -> PhongHocService.HOAT_DONG;
            case 2 -> PhongHocService.BAO_TRI;
            case 3 -> PhongHocService.NGUNG_SU_DUNG;
            default -> null;
        };
        hienThi.clear();
        hienThi.addAll(PhongHocService.loc(tatCa, timKiem.getText(), tt));
        model.setRowCount(0);
        for (PhongHoc p : hienThi) {
            model.addRow(new Object[]{PhongHocService.maHienThi(p),
                    PhongHocService.tenHienThi(p), p.sucChua() + " chỗ",
                    PhongHocService.nhanTrangThai(p.trangThai()),
                    PhongHocService.soLopDangSuDung(lopSuDung, p.maPhong()),
                    p.ghiChu() == null || p.ghiChu().isBlank() ? "–" : p.ghiChu(), ""});
        }
        thongBao.setText(tatCa.isEmpty() ? "Chưa có phòng học tại Trung tâm."
                : "Có " + hienThi.size() + " / " + tatCa.size() + " phòng học phù hợp");
    }

    private PhongHoc phongDuocChon() {
        int hang = bang.getSelectedRow();
        if (hang < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một phòng học trong bảng.");
            return null;
        }
        int chiSo = bang.convertRowIndexToModel(hang);
        return chiSo < hienThi.size() ? hienThi.get(chiSo) : null;
    }

    private void moChiTiet(PhongHoc phong) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Chi tiết phòng học", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(taoTieuDeForm("CHI TIẾT PHÒNG HỌC – "
                + PhongHocService.maHienThi(phong)), BorderLayout.NORTH);

        JPanel thongTin = new JPanel(new GridLayout(0, 2, 12, 14));
        thongTin.setBackground(Color.WHITE);
        thongTin.setBorder(BorderFactory.createEmptyBorder(25, 28, 25, 28));
        themThongTin(thongTin, "Mã phòng", PhongHocService.maHienThi(phong));
        themThongTin(thongTin, "Tên phòng", PhongHocService.tenHienThi(phong));
        themThongTin(thongTin, "Cơ sở", "Trung tâm");
        themThongTin(thongTin, "Sức chứa", phong.sucChua() + " chỗ");
        themThongTin(thongTin, "Trạng thái", PhongHocService.nhanTrangThai(phong.trangThai()));
        themThongTin(thongTin, "Lớp đang sử dụng",
                String.valueOf(PhongHocService.soLopDangSuDung(lopSuDung, phong.maPhong())));
        themThongTin(thongTin, "Ghi chú", phong.ghiChu() == null ? "–" : phong.ghiChu());

        DefaultTableModel lopModel = bangChiTiet(new String[]{"Mã lớp", "Tên lớp", "Môn học",
                "Khóa học", "Giáo viên", "Sĩ số", "Trạng thái", "Sử dụng phòng"});
        for (PhongHocRepository.LopSuDung lop : lopSuDung) {
            if (lop.maPhong() != phong.maPhong()) continue;
            lopModel.addRow(new Object[]{lop.maLop(), lop.tenLop(), lop.tenMonHoc(),
                    lop.tenKhoaHoc(), lop.tenGiaoVien() == null ? "Chưa phân công" : lop.tenGiaoVien(),
                    lop.siSoDangHoc() + " / " + (lop.siSoToiDa() == null ? "?" : lop.siSoToiDa()),
                    lop.trangThaiLop(), lop.dangSuDung() ? "Đang sử dụng" : "Lịch sử"});
        }
        DefaultTableModel lichModel = bangChiTiet(new String[]{"Lớp", "Thứ", "Giờ học",
                "Từ ngày", "Đến ngày", "Trạng thái"});
        for (LichHocLop lich : lichSuDung) {
            if (lich.maPhong() == null || lich.maPhong() != phong.maPhong()) continue;
            String gio = lich.gioBatDau() == null || lich.gioKetThuc() == null
                    ? "Chưa có giờ" : lich.gioBatDau() + " – " + lich.gioKetThuc();
            lichModel.addRow(new Object[]{lich.tenLop(), lich.thuTrongTuan() == 8
                    ? "Chủ nhật" : "Thứ " + lich.thuTrongTuan(), gio,
                    hienNgay(lich.tuNgay()), hienNgay(lich.denNgay()), lich.trangThai()});
        }
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Thông tin phòng", new JScrollPane(thongTin));
        tabs.addTab("Lớp đã sử dụng (" + lopModel.getRowCount() + ")", bangHoacRong(lopModel,
                "Chưa có lớp nào được xếp lịch tại phòng này."));
        tabs.addTab("Lịch định kỳ (" + lichModel.getRowCount() + ")", bangHoacRong(lichModel,
                "Chưa có lịch học định kỳ tại phòng này."));
        dialog.add(tabs, BorderLayout.CENTER);
        JButton dong = new NutMau("Đóng", NutMau.Kieu.PHU);
        dong.addActionListener(e -> dialog.dispose());
        JPanel duoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        duoi.setBackground(Color.WHITE);
        duoi.add(dong);
        dialog.add(duoi, BorderLayout.SOUTH);
        dialog.setSize(950, 590);
        dialog.setMinimumSize(new Dimension(760, 500));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static void themThongTin(JPanel p, String nhan, String giaTri) {
        JLabel label = new JLabel(nhan + ":");
        label.setFont(GiaoDienMau.font(Font.BOLD, 15));
        label.setForeground(GiaoDienMau.CHU);
        JLabel value = new JLabel(giaTri == null || giaTri.isBlank() ? "–" : giaTri);
        value.setFont(GiaoDienMau.font(Font.PLAIN, 15));
        value.setForeground(GiaoDienMau.CHU);
        p.add(label); p.add(value);
    }

    private static DefaultTableModel bangChiTiet(String[] cot) {
        return new DefaultTableModel(cot, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private static Component bangHoacRong(DefaultTableModel model, String thongBaoRong) {
        if (model.getRowCount() == 0) {
            JLabel label = new JLabel(thongBaoRong, SwingConstants.CENTER);
            label.setForeground(GiaoDienMau.CHU);
            return label;
        }
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(i == 1 ? 170 : 125);
        }
        return new JScrollPane(table);
    }

    private static String hienNgay(LocalDate ngay) {
        return ngay == null ? "–" : NGAY.format(ngay);
    }

    private static JPanel taoTieuDeForm(String text) {
        JLabel title = new JLabel(text);
        title.setOpaque(true);
        title.setBackground(XANH);
        title.setForeground(Color.WHITE);
        title.setFont(GiaoDienMau.font(Font.BOLD, 24));
        title.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 5, 0, GiaoDienMau.VANG),
                BorderFactory.createEmptyBorder(17, 26, 17, 22)));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    /** Hop thoai duy nhat cho Them/Sua, giao dien nhap nam o PhongHocFormPanel. */
    private void moForm(PhongHoc cu) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                cu == null ? "Thêm phòng học" : "Sửa phòng học",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(taoTieuDeForm(cu == null ? "THÊM PHÒNG HỌC" : "SỬA PHÒNG HỌC"),
                BorderLayout.NORTH);
        PhongHocFormPanel form = new PhongHocFormPanel(cu);
        dialog.add(form, BorderLayout.CENTER);

        NutMau huy = new NutMau("Hủy", NutMau.Kieu.PHU);
        NutMau luu = new NutMau(cu == null ? "Lưu phòng" : "Lưu thay đổi", NutMau.Kieu.CHINH);
        huy.setPreferredSize(new Dimension(138, 43));
        luu.setPreferredSize(new Dimension(170, 43));
        JPanel duoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 15));
        duoi.setBackground(Color.WHITE);
        duoi.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GiaoDienMau.DUONG_VIEN));
        duoi.add(huy);
        duoi.add(luu);
        dialog.add(duoi, BorderLayout.SOUTH);
        huy.addActionListener(e -> dialog.dispose());
        luu.addActionListener(e -> {
            PhongHocFormPanel.DuLieu data = form.layDuLieu();
            try {
                PhongHocService.kiemTra(cu, data.ma(), data.ten(), data.sucChua(),
                        data.trangThai(), data.ghiChu());
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Dữ liệu chưa hợp lệ",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            luu.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (cu == null) controller.them(data.ma(), data.ten(), data.sucChua(),
                            data.trangThai(), data.ghiChu());
                    else controller.sua(cu, data.ma(), data.ten(), data.sucChua(),
                            data.trangThai(), data.ghiChu());
                    return null;
                }
                @Override protected void done() {
                    luu.setEnabled(true);
                    try {
                        get();
                        dialog.dispose();
                        taiDuLieu();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); hienLoi(ex);
                    } catch (ExecutionException ex) { hienLoi(ex); }
                }
            }.execute();
        });
        dialog.setSize(750, 550);
        dialog.setMinimumSize(new Dimension(675, 510));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void doiTrangThai(PhongHoc p) {
        String moi = PhongHocService.HOAT_DONG.equals(p.trangThai())
                ? PhongHocService.NGUNG_SU_DUNG : PhongHocService.HOAT_DONG;
        int chon = JOptionPane.showConfirmDialog(this,
                PhongHocService.HOAT_DONG.equals(moi)
                        ? "Kích hoạt lại phòng " + PhongHocService.maHienThi(p) + "?"
                        : "Ngừng sử dụng phòng " + PhongHocService.maHienThi(p)
                          + "? Nếu đang có lịch tương lai, hệ thống sẽ không cho ngừng.",
                "Xác nhận thay đổi", JOptionPane.YES_NO_OPTION);
        if (chon != JOptionPane.YES_OPTION) return;
        thongBao.setText("Đang cập nhật trạng thái phòng...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.doiTrangThai(p, moi);
                return null;
            }
            @Override protected void done() {
                try { get(); taiDuLieu(); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); hienLoi(ex); }
                catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void hienLoi(Exception ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) goc = goc.getCause();
        String msg = goc.getMessage();
        if (goc instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627))
            msg = "Tên/mã phòng đã tồn tại trong Trung tâm.";
        if (msg == null || msg.isBlank()) msg = "Không thể cập nhật phòng học.";
        thongBao.setText("Lỗi: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Lỗi phòng học", JOptionPane.ERROR_MESSAGE);
    }

    private static class MauChuRenderer extends DefaultTableCellRenderer {
        private final boolean dam;
        private final boolean canGiua;
        MauChuRenderer(boolean dam, boolean canGiua) {
            this.dam = dam; this.canGiua = canGiua;
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object value,
                                     boolean chon, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, chon, focus, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 6));
            setHorizontalAlignment(canGiua ? SwingConstants.CENTER : SwingConstants.LEFT);
            setFont(GiaoDienMau.font(dam ? Font.BOLD : Font.PLAIN, 13));
            setForeground(GiaoDienMau.CHU);
            setBackground(chon ? t.getSelectionBackground()
                    : (row % 2 == 0 ? Color.WHITE : HANG_CHAN));
            return this;
        }
    }

    private static final class TrangThaiRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value,
                                     boolean chon, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, chon, focus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(GiaoDienMau.font(Font.BOLD, 13));
            setOpaque(true);
            if ("Sẵn sàng".equals(value)) {
                setBackground(new Color(199, 248, 178)); setForeground(new Color(17, 91, 29));
            } else if ("Bảo trì".equals(value)) {
                setBackground(new Color(255, 231, 175)); setForeground(new Color(139, 71, 11));
            } else {
                setBackground(new Color(255, 212, 215)); setForeground(DO);
            }
            setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
            return this;
        }
    }

    private static final class ThaoTacRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value,
                                     boolean chon, boolean focus, int row, int col) {
            JPanel panel = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(7, 6, 7, 6));
            panel.setBackground(row % 2 == 0 ? Color.WHITE : HANG_CHAN);
            JLabel sua = new JLabel("Sửa", SwingConstants.CENTER);
            sua.setOpaque(true);
            sua.setBackground(new Color(233, 242, 235));
            sua.setForeground(GiaoDienMau.CHU);
            sua.setBorder(BorderFactory.createLineBorder(new Color(175, 196, 181)));
            sua.setFont(GiaoDienMau.font(Font.BOLD, 13));
            boolean dangMo = "Sẵn sàng".equals(t.getValueAt(row, 3));
            JLabel trangThai = new JLabel(dangMo ? "Ngừng sử dụng" : "Kích hoạt", SwingConstants.CENTER);
            trangThai.setOpaque(true);
            trangThai.setBackground(dangMo ? new Color(255, 231, 231) : new Color(225, 246, 232));
            trangThai.setForeground(dangMo ? DO : XANH);
            trangThai.setBorder(BorderFactory.createLineBorder(
                    dangMo ? new Color(231, 145, 145) : new Color(146, 192, 160)));
            trangThai.setFont(GiaoDienMau.font(Font.BOLD, 12));
            panel.add(sua);
            panel.add(trangThai);
            return panel;
        }
    }
}
