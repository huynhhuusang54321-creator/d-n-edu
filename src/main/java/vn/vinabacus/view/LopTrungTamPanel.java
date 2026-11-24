package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
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
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.LopTrungTamController;
import vn.vinabacus.controller.PhongHocController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.KhoaHoc;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.model.MonHoc;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.repository.LichHocLopRepository.BuoiCoPhong;
import vn.vinabacus.repository.LopTrungTamRepository.GiaoVien;
import vn.vinabacus.repository.LopTrungTamRepository.LichSuPhanCong;
import vn.vinabacus.service.QuyTacSiSoLop;

/** Lop Trung tam: tab theo mon, loc khoa, an ma, chon ngay bang lich. */
public final class LopTrungTamPanel extends JPanel {
    private final LopTrungTamController controller = new LopTrungTamController(CoSo.TRUNG_TAM);
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai = new JComboBox<>(new String[]{
            "Tất cả", "Đang mở", "Tạm dừng", "Đã đóng"
    });
    private final JTabbedPane tabMonHoc = new JTabbedPane();
    private final CardLayout boChuyen = new CardLayout();
    private final JPanel vungNoiDung = new JPanel(boChuyen);
    private final PhongHocPanel panelPhong = new PhongHocPanel();
    private final JButton tabLop = new JButton("Quản lý lớp học");
    private final JButton tabPhong = new JButton("Quản lý phòng học");
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final JButton btnChiTiet = new JButton("Chi tiết lớp");
    private final JButton btnHocVien = new JButton("Học viên");
    private final JButton btnChuyenGiaoVien = new JButton("Chuyển giáo viên");
    private final JButton btnLichSuGiaoVien = new JButton("Lịch sử giáo viên");
    private final JButton btnChuyenMon = new JButton("Chuyên môn GV");
    private final JButton btnXepLop = new JButton("Mở đăng ký / xếp lớp");
    private final List<TabTheoMon> cacTab = new ArrayList<>();
    private int lanTai = 0;
    private Integer maMonCanChon;
    private Integer maKhoaCanLoc;

    private record DuLieu(List<MonHoc> mon, List<KhoaHoc> khoa,
                          List<LopTrungTam> lop) { }

    private record KhoaLuaChon(int maKhoaHoc, String tenKhoaHoc) {
        @Override public String toString() { return tenKhoaHoc; }
    }

    private static final class TabTheoMon {
        final int maMonHoc;
        final boolean monHoatDong;
        final JComboBox<KhoaLuaChon> locKhoa = new JComboBox<>();
        final DefaultTableModel model = new DefaultTableModel(new String[]{
                "Mã lớp", "Tên lớp", "Môn học", "Khóa học", "Giáo viên",
                "Phòng theo lịch", "Đang học/giữ chỗ", "Tối đa", "Ngày khai giảng", "Trạng thái"
        }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable bang = new JTable(model);
        final List<LopTrungTam> hienThi = new ArrayList<>();
        final List<LopTrungTam> theoMon = new ArrayList<>();
        final JPanel panel = new JPanel(new BorderLayout(5, 8));

        TabTheoMon(int maMonHoc, boolean monHoatDong) {
            this.maMonHoc = maMonHoc;
            this.monHoatDong = monHoatDong;
            bang.setRowHeight(30);
            bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            bang.setAutoCreateRowSorter(true);
            bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int[] rong = {65, 170, 135, 160, 155, 165, 75, 75, 115, 140};
            for (int i = 0; i < rong.length; i++)
                bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
            JPanel boLoc = new JPanel(new FlowLayout(FlowLayout.LEFT));
            boLoc.add(new JLabel("Lọc khóa học:"));
            locKhoa.setPrototypeDisplayValue(new KhoaLuaChon(-1, "Khóa học có tên rất dài"));
            boLoc.add(locKhoa);
            panel.add(boLoc, BorderLayout.NORTH);
            panel.add(new JScrollPane(bang), BorderLayout.CENTER);
        }

        void hienThiTheoKhoa() {
            KhoaLuaChon chon = (KhoaLuaChon) locKhoa.getSelectedItem();
            int maKhoa = chon == null ? -1 : chon.maKhoaHoc();
            model.setRowCount(0);
            hienThi.clear();
            for (LopTrungTam l : theoMon) {
                if (maKhoa > 0 && l.maKhoaHoc() != maKhoa) continue;
                hienThi.add(l);
                QuyTacSiSoLop.KetQua siSo = QuyTacSiSoLop.danhGia(
                        l.siSoToiDa(), l.sucChuaPhong(), l.siSoDaDat());
                String nhan = hienThiTrangThai(l.trangThai());
                if ("DANG_MO".equals(l.trangThai())) {
                    if (siSo.gioiHan() == 0) nhan = siSo.trangThai();
                    else if (siSo.conCho() == 0) nhan = "Đã đầy";
                    else nhan = "Còn " + siSo.conCho() + " chỗ";
                }
                model.addRow(new Object[]{
                        l.maLop(), l.tenLop(), l.tenMonHoc(), l.tenKhoaHoc(),
                        l.tenGiaoVien() == null ? "Chưa phân công" : l.tenGiaoVien(),
                        l.tenPhong() == null ? "Chưa gán" : l.tenPhong(),
                        l.siSoHienTai() + "/" + l.siSoDaDat(),
                        l.siSoToiDa() == null ? "Chưa đặt" : l.siSoToiDa(),
                        NgayKhaiGiangPicker.hienThiNgay(l.ngayKhaiGiang()),
                        nhan
                });
            }
        }
    }

    public LopTrungTamPanel() {
        super(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        JLabel title = new JLabel("QUẢN LÝ LỚP HỌC – TRUNG TÂM");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JPanel chuyenTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chuyenTab.setBackground(Color.WHITE);
        chuyenTab.add(tabLop);
        chuyenTab.add(tabPhong);
        for (JButton b : new JButton[]{tabLop, tabPhong}) {
            b.setFont(GiaoDienMau.font(Font.BOLD, 15));
            b.setPreferredSize(new Dimension(205, 42));
            b.setFocusPainted(false);
            b.setOpaque(true);
        }
        hienThiTab(false);
        JPanel dauChung = new JPanel(new BorderLayout(0, 10));
        dauChung.setBackground(Color.WHITE);
        dauChung.add(title, BorderLayout.NORTH);
        dauChung.add(chuyenTab, BorderLayout.SOUTH);
        add(dauChung, BorderLayout.NORTH);

        JPanel thanhCongCu = new JPanel(new GridLayout(2, 1, 0, 2));
        JPanel hangLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        JButton btnTim = new JButton("Tìm kiếm");
        JButton btnTaiLai = new JButton("Tải lại");
        JButton btnThem = new JButton("Thêm");
        JButton btnSua = new JButton("Sửa");
        hangLoc.add(new JLabel("Tên lớp / khóa / giáo viên:"));
        hangLoc.add(txtTim);
        hangLoc.add(new JLabel("Trạng thái:"));
        hangLoc.add(locTrangThai);
        hangLoc.add(btnTim); hangLoc.add(btnTaiLai);
        hangNut.add(btnThem); hangNut.add(btnSua);
        hangNut.add(btnLichSuGiaoVien);
        hangNut.add(btnChiTiet); hangNut.add(btnHocVien);
        hangNut.add(btnXepLop);
        thanhCongCu.add(hangLoc);
        thanhCongCu.add(hangNut);
        JPanel trangLop = new JPanel(new BorderLayout(5, 8));
        trangLop.setBackground(Color.WHITE);
        trangLop.add(thanhCongCu, BorderLayout.NORTH);
        trangLop.add(tabMonHoc, BorderLayout.CENTER);
        trangLop.add(thongBao, BorderLayout.SOUTH);
        vungNoiDung.add(trangLop, "LOP");
        vungNoiDung.add(panelPhong, "PHONG");
        add(vungNoiDung, BorderLayout.CENTER);
        tabLop.addActionListener(e -> {
            boChuyen.show(vungNoiDung, "LOP");
            hienThiTab(false);
        });
        tabPhong.addActionListener(e -> {
            boChuyen.show(vungNoiDung, "PHONG");
            hienThiTab(true);
            panelPhong.taiDuLieu();
        });

        btnTim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        btnTaiLai.addActionListener(e -> {
            txtTim.setText(""); locTrangThai.setSelectedIndex(0);
            taiDuLieu();
        });
        btnThem.addActionListener(e -> moForm(null));
        btnSua.addActionListener(e -> {
            LopTrungTam lop = lopDuocChon();
            if (lop != null) moForm(lop);
        });
        btnLichSuGiaoVien.addActionListener(e -> moLichSuGiaoVien());
        btnChuyenMon.addActionListener(e -> {
            int chon = JOptionPane.showOptionDialog(this,
                    "Chọn thao tác chuyên môn của giáo viên cho môn đang mở.",
                    "Chuyên môn giáo viên", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null,
                    new String[]{"Khai báo mới", "Kết thúc đợt cũ", "Hủy"}, "Khai báo mới");
            if (chon == 0) moKhaiBaoChuyenMon();
            else if (chon == 1) moKetThucChuyenMon();
        });
        btnChiTiet.addActionListener(e -> moChiTiet(0));
        btnHocVien.addActionListener(e -> moChiTiet(1));
        btnXepLop.addActionListener(e -> moXepLop());
        tabMonHoc.addChangeListener(e -> capNhatThongBao());
        capNhatNut();
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    /** Mở đúng khóa khi đi từ chi tiết khóa học. */
    public void chonKhoaHoc(int maMonHoc, int maKhoaHoc) {
        maMonCanChon = maMonHoc;
        maKhoaCanLoc = maKhoaHoc;
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private void hienThiTab(boolean phong) {
        // Look and Feel cua NetBeans co the bo qua mau nen JButton. Giu chu
        // toi tren nen sang de tab dang chon khong thanh chu trang tren trang.
        tabLop.setBackground(Color.WHITE);
        tabLop.setForeground(GiaoDienMau.CHU);
        tabPhong.setBackground(Color.WHITE);
        tabPhong.setForeground(GiaoDienMau.CHU);
        tabLop.setBorder(BorderFactory.createMatteBorder(0, 1, 3, 1,
                phong ? GiaoDienMau.DUONG_VIEN : GiaoDienMau.VANG));
        tabPhong.setBorder(BorderFactory.createMatteBorder(0, 1, 3, 1,
                phong ? GiaoDienMau.VANG : GiaoDienMau.DUONG_VIEN));
    }

    private String trangThaiLoc() {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 -> "DANG_MO";
            case 2 -> "TAM_DUNG";
            case 3 -> "DA_DONG";
            default -> null;
        };
    }

    private int monDangChon() {
        int i = tabMonHoc.getSelectedIndex();
        return i >= 0 && i < cacTab.size() ? cacTab.get(i).maMonHoc : -1;
    }

    private TabTheoMon tabDangChon() {
        int i = tabMonHoc.getSelectedIndex();
        return i >= 0 && i < cacTab.size() ? cacTab.get(i) : null;
    }

    private void taiDuLieu() {
        final int monCu = monDangChon();
        final int luot = ++lanTai;
        final String tim = txtTim.getText();
        final String trangThai = trangThaiLoc();
        thongBao.setText("Đang tải danh sách lớp...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.layMonHoc(), controller.layKhoaHoc(),
                        controller.timKiem(tim, trangThai));
            }
            @Override protected void done() {
                if (luot != lanTai) return;
                try {
                    DuLieu dl = get();
                    int monChon = maMonCanChon == null ? monCu : maMonCanChon;
                    taoTabs(dl, monChon);
                    maMonCanChon = null;
                    maKhoaCanLoc = null;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void taoTabs(DuLieu dl, int monMuonChon) {
        // Giu khoa hoc dang loc trong tab hien tai khi chi tim kiem/trang thai.
        int khoaCu = -1;
        TabTheoMon cu = tabDangChon();
        if (cu != null) {
            KhoaLuaChon kc = (KhoaLuaChon) cu.locKhoa.getSelectedItem();
            if (kc != null) khoaCu = kc.maKhoaHoc();
        }
        cacTab.clear();
        tabMonHoc.removeAll();
        int viTri = -1;
        for (MonHoc mon : dl.mon()) {
            TabTheoMon tab = new TabTheoMon(mon.maMonHoc(), mon.trangThai());
            tab.bang.getSelectionModel().addListSelectionListener(e -> capNhatNut());
            tab.bang.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && tab.bang.getSelectedRow() >= 0)
                        moChiTiet(0);
                }
            });
            tab.locKhoa.addItem(new KhoaLuaChon(-1, "Tất cả khóa học"));
            for (KhoaHoc k : dl.khoa()) {
                if (k.maMonHoc() == mon.maMonHoc())
                    tab.locKhoa.addItem(new KhoaLuaChon(k.maKhoaHoc(), k.tenKhoaHoc()));
            }
            for (LopTrungTam lop : dl.lop()) {
                if (lop.maMonHoc() == mon.maMonHoc()) tab.theoMon.add(lop);
            }
            int khoaChon = maKhoaCanLoc == null ? khoaCu : maKhoaCanLoc;
            if (mon.maMonHoc() == monMuonChon && khoaChon > 0) {
                for (int j = 0; j < tab.locKhoa.getItemCount(); j++) {
                    if (tab.locKhoa.getItemAt(j).maKhoaHoc() == khoaChon) {
                        tab.locKhoa.setSelectedIndex(j); break;
                    }
                }
            }
            tab.locKhoa.addActionListener(e -> {
                tab.hienThiTheoKhoa(); capNhatThongBao();
            });
            tab.hienThiTheoKhoa();
            cacTab.add(tab);
            tabMonHoc.addTab(mon.tenMonHoc() + (mon.trangThai() ? "" : " (ngừng)")
                    + " (" + tab.theoMon.size() + ")", tab.panel);
            if (mon.maMonHoc() == monMuonChon) viTri = cacTab.size() - 1;
        }
        if (viTri >= 0) tabMonHoc.setSelectedIndex(viTri);
        capNhatThongBao();
        capNhatNut();
    }

    private void capNhatThongBao() {
        TabTheoMon tab = tabDangChon();
        if (tab == null) thongBao.setText("Chưa có môn học để hiển thị lớp.");
        else thongBao.setText("Có " + tab.hienThi.size() + " lớp phù hợp trong tab đang chọn");
        capNhatNut();
    }

    private void capNhatNut() {
        TabTheoMon tab = tabDangChon();
        boolean chon = tab != null && tab.bang.getSelectedRow() >= 0;
        btnChiTiet.setEnabled(chon);
        btnHocVien.setEnabled(chon);
        btnChuyenGiaoVien.setEnabled(chon);
        btnLichSuGiaoVien.setEnabled(chon);
        btnChuyenGiaoVien.setToolTipText(
                "Chỉ chuyển trong ngày cho lớp chưa từng có lịch hoặc học viên; cần bảng lịch sử.");
        btnXepLop.setToolTipText("Chọn đăng ký chờ xếp rồi chọn lớp phù hợp theo khóa.");
    }

    private void moLichSuGiaoVien() {
        LopTrungTam lop = lopDuocChon();
        if (lop == null) return;
        List<LichSuPhanCong> ds;
        try { ds = controller.lichSuPhanCong(lop.maLop()); }
        catch (SQLException ex) { hienLoi(ex); return; }
        StringBuilder noiDung = new StringBuilder();
        for (LichSuPhanCong p : ds) {
            noiDung.append("#").append(p.maPhanCong()).append("  ")
                    .append(p.giaoVienTruoc() == null ? "Chưa rõ trước đó"
                            : p.giaoVienTruoc())
                    .append(" → ").append(p.giaoVien())
                    .append("\nHiệu lực: ").append(p.tuNgay() == null
                            ? "Trước migration (không rõ ngày)" : p.tuNgay())
                    .append(" đến ").append(p.denNgay() == null
                            ? "hiện tại" : p.denNgay())
                    .append("\nGhi nhận UTC: ").append(p.ghiNhan())
                    .append("\nLý do/ghi chú: ").append(p.lyDo() == null
                            ? "Chưa có" : p.lyDo()).append("\n\n");
        }
        JTextArea vanBan = new JTextArea(noiDung.isEmpty()
                ? "Chưa có lịch sử phân công." : noiDung.toString(), 16, 55);
        vanBan.setEditable(false);
        vanBan.setLineWrap(true);
        vanBan.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(vanBan),
                "Lịch sử giáo viên – " + lop.tenLop(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void moChuyenGiaoVien() {
        LopTrungTam lop = lopDuocChon();
        if (lop == null) return;
        if (lop.maGiaoVien() == null) {
            JOptionPane.showMessageDialog(this, "Lớp chưa có giáo viên cũ. "
                    + "Hãy phân công lần đầu ở form Sửa lớp.");
            return;
        }
        if (lop.siSoDaDat() > 0) {
            JOptionPane.showMessageDialog(this, "Lớp đã có học viên. "
                    + "Chưa thể chuyển giáo viên khi chưa có quy trình theo buổi.");
            return;
        }
        try {
            int soLich = controller.lichLop(lop.maLop()).size();
            int soHocVien = controller.hocVienLop(lop.maLop()).size();
            if (soLich > 0 || soHocVien > 0) {
                JOptionPane.showMessageDialog(this, "Lớp có " + soLich
                        + " lịch và " + soHocVien + " lượt xếp học viên trong lịch sử. "
                        + "Chưa thể chuyển giáo viên mà vẫn xác định đúng người dạy từng buổi.");
                return;
            }
        } catch (SQLException ex) { hienLoi(ex); return; }
        List<GiaoVien> chuyenMon;
        try {
            chuyenMon = controller.layGiaoVienChuyenMon(lop.maMonHoc(), LocalDate.now());
        } catch (SQLException ex) { hienLoi(ex); return; }
        chuyenMon.removeIf(gv -> gv.maNhanVien() == lop.maGiaoVien());
        if (chuyenMon.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có giáo viên khác đang công tác "
                    + "và có chuyên môn hiệu lực cho môn này.");
            return;
        }
        JComboBox<GiaoVien> cboMoi = new JComboBox<>(chuyenMon.toArray(GiaoVien[]::new));
        JTextField txtNgay = new JTextField(LocalDate.now().toString(), 12);
        JTextField txtLyDo = new JTextField(28);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.add(new JLabel("Lớp")); form.add(new JLabel(lop.tenLop()));
        form.add(new JLabel("Giáo viên hiện tại"));
        form.add(new JLabel(lop.tenGiaoVien()));
        form.add(new JLabel("Giáo viên mới")); form.add(cboMoi);
        form.add(new JLabel("Ngày hiệu lực (chỉ hôm nay)")); form.add(txtNgay);
        form.add(new JLabel("Lý do chuyển (*)")); form.add(txtLyDo);
        if (JOptionPane.showConfirmDialog(this, form,
                "Chuyển giáo viên (chỉ lớp chưa có lịch/học viên)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) return;
        GiaoVien gvMoi = (GiaoVien) cboMoi.getSelectedItem();
        if (gvMoi == null) return;
        LocalDate ngayChuyen;
        try { ngayChuyen = LocalDate.parse(txtNgay.getText().trim()); }
        catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Ngày hiệu lực phải có dạng yyyy-MM-dd.");
            return;
        }
        String lyDo = txtLyDo.getText().trim();
        if (!ngayChuyen.equals(LocalDate.now()) || lyDo.isBlank() || lyDo.length() > 500) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ chuyển từ hôm nay và cần lý do từ 1 đến 500 ký tự.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Chuyển lớp '" + lop.tenLop()
                + "' từ " + lop.tenGiaoVien() + " sang " + gvMoi.hoTen()
                + " từ " + ngayChuyen + "?\nLý do: " + lyDo,
                "Xác nhận chuyển giáo viên", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        thongBao.setText("Đang chuyển giáo viên...");
        btnChuyenGiaoVien.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.chuyenGiaoVien(lop.maLop(), gvMoi.maNhanVien(),
                        ngayChuyen, lyDo);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(LopTrungTamPanel.this,
                            "Đã chuyển giáo viên và lưu lịch sử phân công.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
                finally { capNhatNut(); }
            }
        }.execute();
    }

    private void moKhaiBaoChuyenMon() {
        TabTheoMon tab = tabDangChon();
        if (tab == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn môn học trước.");
            return;
        }
        List<GiaoVien> giaoVien;
        try { giaoVien = controller.layGiaoVien(); }
        catch (SQLException ex) { hienLoi(ex); return; }
        if (giaoVien.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có nhân viên đang công tác tại Trung tâm.");
            return;
        }
        JComboBox<GiaoVien> cboGv = new JComboBox<>(giaoVien.toArray(GiaoVien[]::new));
        JTextField txtLyDo = new JTextField(28);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.add(new JLabel("Môn đang chọn"));
        form.add(new JLabel(tabMonHoc.getTitleAt(tabMonHoc.getSelectedIndex())));
        form.add(new JLabel("Giáo viên")); form.add(cboGv);
        form.add(new JLabel("Hiệu lực")); form.add(new JLabel("Từ hôm nay: " + LocalDate.now()));
        form.add(new JLabel("Lý do (*)")); form.add(txtLyDo);
        if (JOptionPane.showConfirmDialog(this, form, "Khai báo chuyên môn giáo viên",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) return;
        GiaoVien gv = (GiaoVien) cboGv.getSelectedItem();
        if (gv == null) return;
        String lyDo = txtLyDo.getText().trim();
        if (lyDo.isBlank() || lyDo.length() > 500) {
            JOptionPane.showMessageDialog(this, "Lý do phải có từ 1 đến 500 ký tự.");
            return;
        }
        thongBao.setText("Đang lưu chuyên môn...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.themChuyenMonGiaoVien(gv.maNhanVien(), tab.maMonHoc,
                        LocalDate.now(), lyDo);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(LopTrungTamPanel.this,
                            "Đã lưu chuyên môn mới. Các đợt cũ được giữ nguyên.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void moKetThucChuyenMon() {
        TabTheoMon tab = tabDangChon();
        if (tab == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn môn học trước.");
            return;
        }
        List<GiaoVien> giaoVien;
        try {
            giaoVien = controller.layGiaoVienChuyenMon(tab.maMonHoc, LocalDate.now());
        } catch (SQLException ex) { hienLoi(ex); return; }
        if (giaoVien.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có giáo viên đang có chuyên môn "
                    + "hiệu lực của môn này.");
            return;
        }
        JComboBox<GiaoVien> cboGv = new JComboBox<>(giaoVien.toArray(GiaoVien[]::new));
        JTextField txtLyDo = new JTextField(28);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.add(new JLabel("Môn đang chọn"));
        form.add(new JLabel(tabMonHoc.getTitleAt(tabMonHoc.getSelectedIndex())));
        form.add(new JLabel("Giáo viên")); form.add(cboGv);
        form.add(new JLabel("Kết thúc")); form.add(new JLabel("Từ hôm nay: " + LocalDate.now()));
        form.add(new JLabel("Lý do (*)")); form.add(txtLyDo);
        if (JOptionPane.showConfirmDialog(this, form, "Kết thúc chuyên môn giáo viên",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) return;
        GiaoVien gv = (GiaoVien) cboGv.getSelectedItem();
        if (gv == null) return;
        String lyDo = txtLyDo.getText().trim();
        if (lyDo.isBlank() || lyDo.length() > 500) {
            JOptionPane.showMessageDialog(this, "Lý do phải có từ 1 đến 500 ký tự.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Kết thúc chuyên môn "
                + tabMonHoc.getTitleAt(tabMonHoc.getSelectedIndex()) + " của "
                + gv.hoTen() + " từ hôm nay?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        thongBao.setText("Đang kết thúc chuyên môn...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.ketThucChuyenMonGiaoVien(gv.maNhanVien(), tab.maMonHoc,
                        LocalDate.now(), lyDo);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(LopTrungTamPanel.this,
                            "Đã kết thúc chuyên môn, giữ nguyên lịch sử cũ.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void moChiTiet(int tabBanDau) {
        LopTrungTam lop = lopDuocChon();
        if (lop == null) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        new ChiTietLopTrungTamDialog(owner, lop, controller, tabBanDau).setVisible(true);
    }

    private void moXepLop() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Đăng ký khóa học / xếp lớp",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(new DangKyKhoaHocPanel());
        dialog.setSize(1100, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        taiDuLieu();
    }

    private LopTrungTam lopDuocChon() {
        TabTheoMon tab = tabDangChon();
        if (tab == null || tab.bang.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một lớp trong tab đang mở");
            return null;
        }
        return tab.hienThi.get(tab.bang.convertRowIndexToModel(tab.bang.getSelectedRow()));
    }

    private void moForm(LopTrungTam cu) {
        TabTheoMon tab = tabDangChon();
        if (tab == null) {
            JOptionPane.showMessageDialog(this, "Chưa có môn học để tạo lớp"); return;
        }
        if (cu == null && !tab.monHoatDong) {
            JOptionPane.showMessageDialog(this,
                    "Môn đang ngừng sử dụng. Hãy kích hoạt môn trước khi thêm lớp.");
            return;
        }
        List<KhoaHoc> khoa;
        List<GiaoVien> giaoVien;
        try {
            khoa = controller.layKhoaHoc();
            giaoVien = controller.layGiaoVien();
        } catch (SQLException ex) { hienLoi(ex); return; }

        List<KhoaHoc> cacKhoa = new ArrayList<>();
        for (KhoaHoc k : khoa) {
            if (k.maMonHoc() == tab.maMonHoc
                    && (k.trangThai() || (cu != null && k.maKhoaHoc() == cu.maKhoaHoc()))) {
                cacKhoa.add(k);
            }
        }
        if (cacKhoa.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có khóa học đang hoạt động trong môn này");
            return;
        }
        JComboBox<KhoaHoc> cboKhoa = new JComboBox<>(
                new DefaultComboBoxModel<>(cacKhoa.toArray(KhoaHoc[]::new)));
        cboKhoa.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof KhoaHoc k) setText(k.tenKhoaHoc()
                        + (k.trangThai() ? "" : " (ngừng sử dụng)"));
                return this;
            }
        });
        int khoaMacDinh = cu == null ? -1 : cu.maKhoaHoc();
        if (cu == null) {
            KhoaLuaChon f = (KhoaLuaChon) tab.locKhoa.getSelectedItem();
            if (f != null) khoaMacDinh = f.maKhoaHoc();
        }
        for (KhoaHoc k : cacKhoa) {
            if (k.maKhoaHoc() == khoaMacDinh) { cboKhoa.setSelectedItem(k); break; }
        }

        JComboBox<GiaoVien> cboGv = new JComboBox<>();
        cboGv.addItem(null); // cho phep chua phan cong
        for (GiaoVien gv : giaoVien) cboGv.addItem(gv);
        cboGv.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof GiaoVien gv ? gv.hoTen() : "Chưa phân công");
                return this;
            }
        });
        if (cu != null && cu.maGiaoVien() != null) {
            boolean found = false;
            for (int i = 1; i < cboGv.getItemCount(); i++) {
                if (cboGv.getItemAt(i).maNhanVien() == cu.maGiaoVien()) {
                    cboGv.setSelectedIndex(i); found = true; break;
                }
            }
            if (!found) {
                GiaoVien gvCu = new GiaoVien(cu.maGiaoVien(),
                        (cu.tenGiaoVien() == null ? "Nhân viên cũ" : cu.tenGiaoVien())
                                + " (ngừng sử dụng)");
                cboGv.addItem(gvCu);
                cboGv.setSelectedItem(gvCu);
            }
            cboGv.setToolTipText("Đổi giáo viên sẽ dùng quy trình chuyển phân công có lưu lịch sử; lớp đã có lịch/học viên vẫn bị chặn an toàn.");
        }

        JTextField txtTen = new JTextField(cu == null ? "" : cu.tenLop(), 25);
        JTextField txtSiSo = new JTextField(cu == null || cu.siSoToiDa() == null
                ? "" : String.valueOf(cu.siSoToiDa()), 8);
        NgayKhaiGiangPicker lich = new NgayKhaiGiangPicker(
                cu == null ? null : cu.ngayKhaiGiang());
        String[] labels = {"Đang mở", "Tạm dừng", "Đã đóng"};
        String[] values = {"DANG_MO", "TAM_DUNG", "DA_DONG"};
        JComboBox<String> cboTrangThai = new JComboBox<>(labels);
        if (cu != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(cu.trangThai())) cboTrangThai.setSelectedIndex(i);
            }
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.add(new JLabel("Khóa học (*)")); form.add(cboKhoa);
        form.add(new JLabel("Tên lớp (*)")); form.add(txtTen);
        form.add(new JLabel(cu != null && cu.siSoToiDa() == null
                ? "Sĩ số tối đa (trống = chưa xác định)"
                : "Sĩ số tối đa (*)")); form.add(txtSiSo);
        form.add(new JLabel("Giáo viên phụ trách"));
        form.add(cboGv);
        form.add(new JLabel("Ngày khai giảng")); form.add(lich);
        form.add(new JLabel("Trạng thái")); form.add(cboTrangThai);
        form.add(new JLabel("Phòng học"));
        form.add(new JLabel("Gán theo từng buổi ở màn hình Lịch học"));

        JComboBox<String> cachLich = new JComboBox<>(cu==null?new String[]{
            "Tạo lớp trước, xếp lịch sau", "Tạo lớp và xếp lịch ngay"}
            :new String[]{"Giữ lịch hiện có", "Bổ sung lịch hiệu lực"});
        BangChonCaHocPanel bangCa = new BangChonCaHocPanel(false);
        JComboBox<PhongHoc> cboPhong = new JComboBox<>();
        NgayKhaiGiangPicker ngayLich = new NgayKhaiGiangPicker(LocalDate.now());
        JPanel cauHinhLich = new JPanel(new BorderLayout(5,5));
        cauHinhLich.setBackground(Color.WHITE);
        cauHinhLich.setBorder(BorderFactory.createTitledBorder("LỊCH HỌC HIỆU LỰC"));
        cauHinhLich.add(bangCa,BorderLayout.CENTER);
        JPanel hangLich = new JPanel(new GridLayout(0,2,8,5));
        hangLich.setBackground(Color.WHITE);
        hangLich.add(new JLabel("Ngày bắt đầu hiệu lực"));hangLich.add(ngayLich);
        hangLich.add(new JLabel("Một phòng cho các ca đã chọn"));hangLich.add(cboPhong);
        cauHinhLich.add(hangLich,BorderLayout.SOUTH);
        try {
            for (PhongHoc p:new PhongHocController().layDanhSach())
                if ("HOAT_DONG".equals(p.trangThai())) cboPhong.addItem(p);
        } catch (SQLException ex) { hienLoi(ex);return; }
        cauHinhLich.setVisible(false);
        JPanel noiDungForm = new JPanel(new BorderLayout(0,8));
        noiDungForm.setBackground(Color.WHITE);
        noiDungForm.add(form,BorderLayout.NORTH);
        JPanel lichVaCach = new JPanel(new BorderLayout(0,6));
        lichVaCach.setBackground(Color.WHITE);
        lichVaCach.add(cachLich,BorderLayout.NORTH);
        lichVaCach.add(cauHinhLich,BorderLayout.CENTER);
        noiDungForm.add(lichVaCach,BorderLayout.CENTER);
        cachLich.addActionListener(e -> {
            cauHinhLich.setVisible(cachLich.getSelectedIndex()==1);
            noiDungForm.revalidate();
        });
        JScrollPane cuonForm = new JScrollPane(noiDungForm);
        cuonForm.setPreferredSize(new Dimension(850,510));
        cuonForm.getVerticalScrollBar().setUnitIncrement(16);

        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, cuonForm,
                    cu == null ? "Thêm lớp Trung tâm" : "Sửa lớp Trung tâm",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            try {
                KhoaHoc khoaChon = (KhoaHoc) cboKhoa.getSelectedItem();
                if (khoaChon == null) throw new IllegalArgumentException("Hãy chọn khóa học");
                String ten = txtTen.getText().trim();
                if (ten.isBlank()) throw new IllegalArgumentException("Hãy nhập tên lớp");
                GiaoVien gv = (GiaoVien) cboGv.getSelectedItem();
                Integer maGv = gv == null ? null : gv.maNhanVien();
                String nhapSiSo = txtSiSo.getText().trim();
                Integer siSo = null;
                if (!nhapSiSo.isEmpty()) {
                    try { siSo = Integer.parseInt(nhapSiSo); }
                    catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Sĩ số tối đa phải là số nguyên lớn hơn 0");
                    }
                    if (siSo <= 0)
                        throw new IllegalArgumentException("Sĩ số tối đa phải lớn hơn 0");
                }
                if (cu == null && siSo == null)
                    throw new IllegalArgumentException("Lớp mới phải có sĩ số tối đa riêng");
                if (cu != null && cu.siSoToiDa() != null && siSo == null)
                    throw new IllegalArgumentException("Không thể xóa giới hạn sĩ số đã đặt");
                String trangThai = values[cboTrangThai.getSelectedIndex()];
                if (cu != null && cu.maGiaoVien() != null
                        && !Objects.equals(cu.maGiaoVien(),maGv)) {
                    if (maGv == null) throw new IllegalArgumentException(
                            "Không thể bỏ giáo viên đã phân công; hãy chọn giáo viên mới hợp lệ.");
                    if (khoaChon.maKhoaHoc()!=cu.maKhoaHoc()
                            || cachLich.getSelectedIndex()!=0
                            || !ten.equals(cu.tenLop())
                            || !Objects.equals(lich.layNgay(),cu.ngayKhaiGiang())
                            || !Objects.equals(siSo,cu.siSoToiDa())
                            || !trangThai.equals(cu.trangThai()))
                        throw new IllegalArgumentException("Hãy lưu các thay đổi khác trước, "
                                + "sau đó đổi giáo viên để bảo đảm lịch sử phân công chính xác.");
                    String lyDo=JOptionPane.showInputDialog(this,"Lý do đổi giáo viên (bắt buộc):");
                    if (lyDo==null) return;
                    if (lyDo.isBlank() || lyDo.length()>500)
                        throw new IllegalArgumentException("Lý do phải có 1–500 ký tự.");
                    chuyenGiaoVienTrongForm(cu,maGv,lyDo.trim());
                    return;
                }
                List<BuoiCoPhong> caDaChon=List.of();
                if (cachLich.getSelectedIndex()==1) {
                    PhongHoc phong=(PhongHoc)cboPhong.getSelectedItem();
                    if (phong==null) throw new IllegalArgumentException("Hãy chọn phòng cho lịch học");
                    if (phong.sucChua()<siSo)
                        throw new IllegalArgumentException("Sĩ số lớp vượt sức chứa phòng đã chọn");
                    caDaChon=bangCa.layCacBuoi().stream()
                            .map(b -> new BuoiCoPhong(b,phong.maPhong())).toList();
                    if (caDaChon.isEmpty())
                        throw new IllegalArgumentException("Hãy chọn ít nhất một ô thứ/ca học");
                }
                luuDuLieu(cu, khoaChon, ten, maGv, lich.layNgay(), trangThai,
                        siSo,caDaChon,ngayLich.layNgay());
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void chuyenGiaoVienTrongForm(LopTrungTam cu,int maGv,String lyDo) {
        thongBao.setText("Đang kiểm tra và chuyển giáo viên...");
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.chuyenGiaoVien(cu.maLop(),maGv,LocalDate.now(),lyDo);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    thongBao.setText("Đã đổi giáo viên và lưu lịch sử phân công.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void luuDuLieu(LopTrungTam cu, KhoaHoc khoa, String ten,
                          Integer maGiaoVien, LocalDate ngay, String trangThai,
                          Integer siSoToiDa,List<BuoiCoPhong> cacBuoi,LocalDate ngayLich) {
        thongBao.setText("Đang lưu lớp...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (cu == null) {
                    if (cacBuoi.isEmpty()) controller.them(khoa.maKhoaHoc(), ten,
                            maGiaoVien, ngay,trangThai,siSoToiDa.intValue());
                    else controller.themVoiLich(khoa.maKhoaHoc(),ten,maGiaoVien,
                            ngay,trangThai,siSoToiDa.intValue(),cacBuoi,ngayLich);
                    return cacBuoi.isEmpty() ? "Đã thêm lớp Trung tâm (chưa có lịch)"
                            : "Đã tạo lớp và lịch học trong cùng một giao dịch";
                }
                if (cacBuoi.isEmpty()) controller.sua(cu.maLop(),khoa.maKhoaHoc(),
                        ten,maGiaoVien,ngay,trangThai,siSoToiDa);
                else controller.suaVoiLich(cu.maLop(),khoa.maKhoaHoc(),ten,
                        maGiaoVien,ngay,trangThai,siSoToiDa,cacBuoi,ngayLich);
                return cacBuoi.isEmpty() ? "Đã cập nhật lớp Trung tâm"
                        : "Đã cập nhật lớp và bổ sung lịch trong cùng một giao dịch";
            }
            @Override protected void done() {
                try {
                    String thongDiep = get();
                    maMonCanChon = khoa.maMonHoc();
                    maKhoaCanLoc = khoa.maKhoaHoc();
                    JOptionPane.showMessageDialog(LopTrungTamPanel.this, thongDiep);
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); hienLoi(ex);
                } catch (ExecutionException ex) { hienLoi(ex); }
            }
        }.execute();
    }

    private void hienLoi(Exception ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) goc = goc.getCause();
        String msg = goc.getMessage();
        if (goc instanceof SQLException sql) {
            if (sql.getErrorCode() == 547) {
                msg = "Liên kết khóa học, giáo viên hoặc cơ sở không hợp lệ. Có thể lớp đã có học viên.";
            } else if (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627) {
                msg = "Lớp bị trùng theo ràng buộc SQL Server.";
            }
        }
        if (msg == null || msg.isBlank()) msg = "Không thể thực hiện thao tác.";
        thongBao.setText("Lỗi: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }

    private static String hienThiTrangThai(String trangThai) {
        return switch (trangThai) {
            case "DANG_MO" -> "Đang mở";
            case "TAM_DUNG" -> "Tạm dừng";
            case "DA_DONG" -> "Đã đóng";
            default -> trangThai;
        };
    }
}
