package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.DangKyKhoaHocController;
import vn.vinabacus.controller.TuyenSinhController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DangKyKhoaHoc;
import vn.vinabacus.model.KhachTiemNang;

/**
 * 6 tab tuyen sinh theo TrangThai dbo.KhachTiemNang, giong mo hinh tab Kho.
 * Khong tao them bang SQL; moi ho so chi duoc luu mot lan trong KhachTiemNang.
 */
public final class TuyenSinhPanel extends JPanel {

    private static final String[] MA_TAB = {
        "TIEP_NHAN", "HOC_THU", "CHO_XAC_NHAN",
        "CHO_XEP_LOP", "DA_CHUYEN_DOI", "KHONG_DANG_KY"
    };
    private static final String[] TEN_TAB = {
        "Tiếp nhận", "Học thử", "Chờ xác nhận",
        "Chờ xếp lớp", "Đã chuyển đổi", "Không đăng ký"
    };
    private static final String[] MO_TA_TAB = {
        "Hồ sơ mới tiếp nhận. Có thể sửa thông tin hoặc chuyển sang trạng thái tiếp theo.",
        "Theo dõi học thử; chuyển sang Chờ xác nhận khi đã hoàn tất học thử.",
        "Hồ sơ đang chờ xác nhận. Chọn 'Đăng ký khóa học' để lưu khóa học.",
        "Hồ sơ đang chờ xếp lớp. Khi đủ điều kiện, dùng 'Chuyển thành học viên'.",
        "Hồ sơ đã được chuyển thành học viên. Cập nhật thông tin ở màn hình Học viên.",
        "Hồ sơ không đăng ký, vẫn được lưu để tra cứu lịch sử."
    };
    private static final String[] COT = {
        "Mã", "Họ tên", "SĐT", "Người liên hệ", "Nhu cầu",
        "Trạng thái", "Đã học thử", "Ghi chú"
    };
    private static final String[] TRANG_THAI_SUA = {
        "TIEP_NHAN", "HOC_THU", "CHO_XAC_NHAN", "CHO_XEP_LOP", "KHONG_DANG_KY"
    };

    private final TuyenSinhController controller;
    private final CoSo coSo;
    private final JTextField txtTim = new JTextField(18);
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
    private final Map<String, DefaultTableModel> models = new LinkedHashMap<>();
    private final Map<String, JTable> bangs = new LinkedHashMap<>();
    private final Map<String, List<KhachTiemNang>> danhSachTheoTab = new LinkedHashMap<>();
    private Set<Integer> hoSoDuDieuKienChuyen = Set.of();
    private Set<Integer> hoSoConChoXep = Set.of();
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final JButton sua = new JButton("Sửa hồ sơ");
    private final JButton chuyen = new JButton("Chuyển thành học viên");
    private final JButton hocThu = new JButton("Chuyển sang học thử");
    private final JButton choXacNhan = new JButton("Chuyển chờ xác nhận");
    private final JButton xepLop = new JButton("Xếp lớp");
    private final JButton thanhHocVien = new JButton("Chuyển thành học viên");
    private final JButton khongDangKy = new JButton("Không đăng ký");
    private final JButton khoiPhuc = new JButton("Khôi phục hồ sơ");
    private long phienTai = 0;
    private int maCanChon = -1;

    private record DuLieu(List<KhachTiemNang> hoSo,
                          Set<Integer> hoSoDuDieuKienChuyen,Set<Integer> hoSoConChoXep) { }

    public TuyenSinhPanel(CoSo coSo) {
        super(new BorderLayout(10, 10));
        this.coSo = coSo;
        controller = new TuyenSinhController(coSo);
        if (coSo == CoSo.TRUNG_TAM) chuyen.setText("Đăng ký khóa học");
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("QUẢN LÝ TUYỂN SINH – " + coSo.ten());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        JPanel dau = new JPanel(new BorderLayout(0, 8));
        dau.add(title, BorderLayout.NORTH);

        JPanel congCu = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton tim = new JButton("Tìm"), taiLai = new JButton("Tải lại");
        JButton tiepNhan = new JButton("+ Tiếp nhận");
        congCu.add(new JLabel("Mã / tên / SĐT:"));
        congCu.add(txtTim);
        congCu.add(tim);
        congCu.add(taiLai);
        congCu.add(tiepNhan);
        JPanel cacThanh = new JPanel(new GridLayout(0, 1, 0, 2));
        cacThanh.add(congCu);
        if (coSo == CoSo.TRUNG_TAM) {
            JPanel hang1 = new JPanel(new GridLayout(1,4,8,0));
            hang1.add(sua);
            hang1.add(hocThu);
            hang1.add(choXacNhan);
            hang1.add(chuyen);
            JPanel hang2 = new JPanel(new GridLayout(1,4,8,0));
            hang2.add(xepLop);
            hang2.add(thanhHocVien);
            hang2.add(khongDangKy);
            hang2.add(khoiPhuc);
            cacThanh.add(hang1);
            cacThanh.add(hang2);
        } else {
            congCu.add(sua);
            congCu.add(chuyen);
        }
        dau.add(cacThanh, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);

        for (int i = 0; i < MA_TAB.length; i++) {
            final String trangThai = MA_TAB[i];
            danhSachTheoTab.put(trangThai, new ArrayList<>());
            DefaultTableModel model = new DefaultTableModel(COT, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
                @Override public Class<?> getColumnClass(int c) {
                    return c == 0 ? Integer.class : String.class;
                }
            };
            models.put(trangThai, model);
            JTable bang = new JTable(model);
            bang.setRowHeight(29);
            bang.setAutoCreateRowSorter(true);
            bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            bang.getTableHeader().setReorderingAllowed(false);
            bang.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && trangThai.equals(tabHienTai())) {
                    capNhatNut();
                }
            });
            if (coSo == CoSo.TRUNG_TAM) {
                bang.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1 &&
                            (e.getClickCount()==2 || e.getClickCount()==1 && bang.columnAtPoint(e.getPoint())==1)) {
                            moChiTiet(false);
                        }
                    }
                });
            }
            bangs.put(trangThai, bang);

            JPanel trang = new JPanel(new BorderLayout(0, 6));
            String moTaTab = MO_TA_TAB[i];
            if (coSo == CoSo.TRUNG_TAM && i == 3)
                moTaTab = "Đã đăng ký khóa, đang chờ lớp phù hợp. Xếp lớp tại mục Đăng ký khóa học.";
            if (coSo == CoSo.TRUNG_TAM && i == 4)
                moTaTab = "Học viên đã được xếp lớp. Có thể đăng ký thêm khóa tại đây.";
            JLabel moTa = new JLabel(moTaTab);
            moTa.setForeground(new Color(70, 78, 87));
            moTa.setBorder(BorderFactory.createEmptyBorder(7, 8, 4, 4));
            trang.add(moTa, BorderLayout.NORTH);
            trang.add(new JScrollPane(bang), BorderLayout.CENTER);
            tabs.addTab(TEN_TAB[i], trang);
        }
        tabs.addChangeListener(e -> capNhatNut());
        add(tabs, BorderLayout.CENTER);
        thongBao.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 0));
        add(thongBao, BorderLayout.SOUTH);

        tim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        taiLai.addActionListener(e -> {
            txtTim.setText("");
            taiDuLieu();
        });
        tiepNhan.addActionListener(e -> form(null));
        sua.addActionListener(e -> {
            KhachTiemNang khach = duocChon(true);
            if (khach != null) {
                if (coSo == CoSo.TRUNG_TAM) moChiTiet(true);
                else form(khach);
            }
        });
        chuyen.addActionListener(e -> chuyenDoi());
        if (coSo == CoSo.TRUNG_TAM) {
            hocThu.addActionListener(e -> chuyenTrangThai("HOC_THU"));
            choXacNhan.addActionListener(e -> chuyenTrangThai("CHO_XAC_NHAN"));
            khongDangKy.addActionListener(e -> chuyenTrangThai("KHONG_DANG_KY"));
            khoiPhuc.addActionListener(e -> chuyenTrangThai("TIEP_NHAN"));
            xepLop.addActionListener(e -> xepLopTuHoSo());
            thanhHocVien.addActionListener(e -> chuyenThanhHocVien());
        }
        capNhatNut();
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private String tabHienTai() {
        return MA_TAB[Math.max(0, tabs.getSelectedIndex())];
    }

    private KhachTiemNang duocChon(boolean canBaoLoi) {
        String ma = tabHienTai();
        JTable bang = bangs.get(ma);
        int dong = bang.getSelectedRow();
        List<KhachTiemNang> danhSach = danhSachTheoTab.get(ma);
        if (dong >= 0) {
            int index = bang.convertRowIndexToModel(dong);
            if (index >= 0 && index < danhSach.size()) return danhSach.get(index);
        }
        if (canBaoLoi) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một hồ sơ trong tab hiện tại");
        }
        return null;
    }

    private void capNhatNut() {
        KhachTiemNang k = duocChon(false);
        boolean coChon = k != null;
        boolean daChuyen = coChon && "DA_CHUYEN_DOI".equals(k.trangThai());
        boolean laKhongDangKy = coChon && "KHONG_DANG_KY".equals(k.trangThai());
        sua.setVisible(coChon && !daChuyen);
        chuyen.setVisible(coChon && !laKhongDangKy
                && (coSo == CoSo.TRUNG_TAM || !daChuyen));
        if (coSo == CoSo.TRUNG_TAM) {
            String tt = coChon ? k.trangThai() : "";
            hocThu.setVisible("TIEP_NHAN".equals(tt));
            choXacNhan.setVisible("TIEP_NHAN".equals(tt) || "HOC_THU".equals(tt));
            xepLop.setVisible(coChon&&hoSoConChoXep.contains(k.maKhach()));
            thanhHocVien.setVisible("CHO_XEP_LOP".equals(tt)
                    && hoSoDuDieuKienChuyen.contains(k.maKhach()));
            khongDangKy.setVisible("TIEP_NHAN".equals(tt)
                    || "HOC_THU".equals(tt) || "CHO_XAC_NHAN".equals(tt));
            khoiPhuc.setVisible(laKhongDangKy);
        }
    }

    private void taiDuLieu() {
        final long phien = ++phienTai;
        final String tuKhoa = txtTim.getText().trim();
        hoSoDuDieuKienChuyen = Set.of();
        hoSoConChoXep = Set.of();
        capNhatNut();
        thongBao.setText("Đang tải hồ sơ tuyển sinh...");
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                // Tra ve tat ca trang thai; giao dien chia vao dung 6 tab.
                // Viec tim theo ma/ten/sdt van dung Repository da duoc kiem thu.
                List<KhachTiemNang> hoSo = controller.timKiem(tuKhoa, "TAT_CA");
                if (coSo != CoSo.TRUNG_TAM) return new DuLieu(hoSo, Set.of(),Set.of());
                Set<Integer> duDieuKien = new HashSet<>();
                Set<Integer> conChoXep = new HashSet<>();
                for (DangKyKhoaHoc dk : new DangKyKhoaHocController().timKiem("", null)) {
                    if(dk.maKhach()!=null&&dk.maLop()==null&&"CHO_XEP_LOP".equals(dk.trangThai()))
                        conChoXep.add(dk.maKhach());
                    if (dk.maKhach() != null && dk.maLop() != null
                            && ("DANG_HOC".equals(dk.trangThai())
                                || "HOAN_THANH".equals(dk.trangThai()))) {
                        duDieuKien.add(dk.maKhach());
                    }
                }
                return new DuLieu(hoSo, Set.copyOf(duDieuKien),Set.copyOf(conChoXep));
            }
            @Override protected void done() {
                if (phien != phienTai) return;
                try {
                    DuLieu duLieu = get();
                    List<KhachTiemNang> ketQua = duLieu.hoSo();
                    hoSoDuDieuKienChuyen = duLieu.hoSoDuDieuKienChuyen();
                    hoSoConChoXep = duLieu.hoSoConChoXep();
                    for (String ma : MA_TAB) {
                        models.get(ma).setRowCount(0);
                        danhSachTheoTab.get(ma).clear();
                    }
                    for (KhachTiemNang k : ketQua) {
                        List<KhachTiemNang> danhSach = danhSachTheoTab.get(k.trangThai());
                        if (danhSach == null) continue;
                        danhSach.add(k);
                        models.get(k.trangThai()).addRow(new Object[]{
                            k.maKhach(), k.hoTen(), k.soDienThoai(), k.tenNguoiLienHe(),
                            k.nhuCau(), k.trangThai(), k.daHocThu() ? "Có" : "Chưa", k.ghiChu()
                        });
                    }
                    for (int i = 0; i < MA_TAB.length; i++) {
                        int n = danhSachTheoTab.get(MA_TAB[i]).size();
                        tabs.setTitleAt(i, TEN_TAB[i] + " (" + n + ")");
                    }
                    thongBao.setText("Tìm thấy " + ketQua.size()
                            + " hồ sơ trong " + (tuKhoa.isBlank() ? "toàn bộ tuyển sinh" : "kết quả tìm kiếm"));
                    if (maCanChon > 0) {
                        if (!chonDongTheoMa(maCanChon) && !tuKhoa.isBlank()) {
                            // Hồ sơ có thể đã đổi tên/SĐT; chỉ bỏ lọc khi hồ sơ không còn khớp.
                            txtTim.setText("");
                            taiDuLieu();
                            return;
                        }
                        maCanChon = -1;
                    }
                    capNhatNut();
                } catch (Exception e) {
                    loi(e);
                }
            }
        }.execute();
    }

    private boolean chonDongTheoMa(int maKhach) {
        String ma = tabHienTai();
        List<KhachTiemNang> danhSach = danhSachTheoTab.get(ma);
        JTable bang = bangs.get(ma);
        for (int i = 0; i < danhSach.size(); i++) {
            if (danhSach.get(i).maKhach() == maKhach) {
                int view = bang.convertRowIndexToView(i);
                if (view >= 0) {
                    bang.getSelectionModel().setSelectionInterval(view, view);
                    bang.scrollRectToVisible(bang.getCellRect(view, 0, true));
                }
                return true;
            }
        }
        return false;
    }

    private void moTabVaTaiLai(String trangThai, int maKhach) {
        for (int i = 0; i < MA_TAB.length; i++) {
            if (MA_TAB[i].equals(trangThai)) {
                tabs.setSelectedIndex(i);
                break;
            }
        }
        maCanChon = maKhach;
        taiDuLieu();
    }

    private void moChiTiet(boolean moThongTin) {
        KhachTiemNang k = duocChon(true);
        if (k == null) return;
        TuyenSinhChiTietDialog.mo(this, k, moThongTin, ketQua ->
                moTabVaTaiLai(ketQua.trangThai(), ketQua.maKhach()));
    }

    private void chuyenTrangThai(String trangThaiMoi) {
        KhachTiemNang k = duocChon(true);
        if (k == null) return;
        if (JOptionPane.showConfirmDialog(this,
                "Chuyển hồ sơ #" + k.maKhach() + " sang '" + tenTrangThai(trangThaiMoi) + "'?",
                "Xác nhận trạng thái", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        thongBao.setText("Đang cập nhật trạng thái hồ sơ...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controller.chuyenTrangThai(k.maKhach(), trangThaiMoi);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    moTabVaTaiLai(trangThaiMoi, k.maKhach());
                } catch (Exception ex) { loi(ex); }
            }
        }.execute();
    }

    private void xepLopTuHoSo() {
        KhachTiemNang k = duocChon(true);
        if (k == null) return;
        thongBao.setText("Đang tải khóa chờ xếp lớp...");
        new SwingWorker<List<vn.vinabacus.model.DangKyKhoaHoc>, Void>() {
            @Override protected List<vn.vinabacus.model.DangKyKhoaHoc> doInBackground() throws Exception {
                return new vn.vinabacus.controller.DangKyKhoaHocController()
                        .timKiem("", "CHO_XEP_LOP").stream()
                        .filter(d -> d.maKhach() != null && d.maKhach() == k.maKhach()
                            && d.maLop()==null).toList();
            }
            @Override protected void done() {
                try {
                    List<vn.vinabacus.model.DangKyKhoaHoc> ds = get();
                    if (ds.isEmpty()) {
                        thongBao.setText("Hồ sơ không có khóa đang chờ xếp lớp");
                        JOptionPane.showMessageDialog(TuyenSinhPanel.this,
                                "Hồ sơ này không có khóa đang chờ xếp lớp.");
                        return;
                    }
                    javax.swing.JComboBox<vn.vinabacus.model.DangKyKhoaHoc> cbo =
                            new javax.swing.JComboBox<>(ds.toArray(vn.vinabacus.model.DangKyKhoaHoc[]::new));
                    cbo.setRenderer(new javax.swing.DefaultListCellRenderer() {
                        @Override public Component getListCellRendererComponent(javax.swing.JList<?> list,
                                Object value, int index, boolean selected, boolean focus) {
                            super.getListCellRendererComponent(list, value, index, selected, focus);
                            if (value instanceof vn.vinabacus.model.DangKyKhoaHoc d)
                                setText(d.tenMonHoc() + " · " + d.tenKhoaHoc() + " (#" + d.maDangKy() + ")");
                            return this;
                        }
                    });
                    if (ds.size() > 1 && JOptionPane.showConfirmDialog(TuyenSinhPanel.this, cbo,
                            "Chọn khóa cần xếp lớp", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
                    vn.vinabacus.model.DangKyKhoaHoc d =
                            (vn.vinabacus.model.DangKyKhoaHoc) cbo.getSelectedItem();
                    if (d != null) DangKyKhoaHocPanel.moXepLop(TuyenSinhPanel.this, d,
                            () -> moTabVaTaiLai(k.trangThai(), k.maKhach()));
                } catch (Exception ex) { loi(ex); }
            }
        }.execute();
    }

    private void chuyenThanhHocVien() {
        KhachTiemNang k = duocChon(true);
        if (k == null) return;
        DangKyKhoaHocPanel.moChuyenThanhHocVien(this, k,
                () -> moTabVaTaiLai("DA_CHUYEN_DOI", k.maKhach()));
    }

    private void form(KhachTiemNang cu) {
        if (coSo == CoSo.TRUNG_TAM && cu != null) {
            TuyenSinhChiTietDialog.mo(this, cu, true, ketQua ->
                    moTabVaTaiLai(ketQua.trangThai(), ketQua.maKhach()));
            return;
        }
        if (cu != null && ("DA_CHUYEN_DOI".equals(cu.trangThai())
                || coSo == CoSo.TRUNG_TAM && "CHO_XEP_LOP".equals(cu.trangThai()))) {
            JOptionPane.showMessageDialog(this,
                    "Hồ sơ đã liên kết đăng ký; hãy cập nhật thông tin ở màn hình Học viên.");
            return;
        }
        JTextField ten = new JTextField(cu == null ? "" : cu.hoTen(), 24);
        JTextField sdt = new JTextField(cu == null ? "" : chuoi(cu.soDienThoai()));
        JTextField lienHe = new JTextField(cu == null ? "" : chuoi(cu.tenNguoiLienHe()));
        JTextField nhuCau = new JTextField(cu == null ? "" : chuoi(cu.nhuCau()));
        JTextField ghiChu = new JTextField(cu == null ? "" : chuoi(cu.ghiChu()));
        NgaySinhPicker ngaySinh = new NgaySinhPicker(cu==null?null:cu.ngaySinh());
        JButton xoaNgay = new JButton("Xóa ngày");xoaNgay.addActionListener(e->ngaySinh.datNgay(null));
        JPanel chonNgay=new JPanel(new BorderLayout(5,0));chonNgay.add(ngaySinh,BorderLayout.CENTER);chonNgay.add(xoaNgay,BorderLayout.EAST);
        JComboBox<String> gioiTinh=new JComboBox<>(new String[]{"Chưa chọn","Nam","Nữ","Khác"});
        if(cu!=null)gioiTinh.setSelectedIndex("NAM".equals(cu.gioiTinh())?1:"NU".equals(cu.gioiTinh())?2:"KHAC".equals(cu.gioiTinh())?3:0);
        JTextField diaChi=new JTextField(cu==null?"":chuoi(cu.diaChi()));
        JComboBox<TrangThaiLuaChon> trangThai = new JComboBox<>();
        for (String ma : TRANG_THAI_SUA) {
            if (coSo == CoSo.TRUNG_TAM && !"TIEP_NHAN".equals(ma)) continue;
            trangThai.addItem(new TrangThaiLuaChon(ma, tenTrangThai(ma)));
        }
        if (cu != null) {
            for (int i = 0; i < trangThai.getItemCount(); i++) {
                if (trangThai.getItemAt(i).ma().equals(cu.trangThai())) {
                    trangThai.setSelectedIndex(i);
                    break;
                }
            }
        }
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Họ tên (*)")); p.add(ten);
        if(coSo==CoSo.TRUNG_TAM){p.add(new JLabel("Ngày sinh"));p.add(chonNgay);
            p.add(new JLabel("Giới tính"));p.add(gioiTinh);
            p.add(new JLabel("Địa chỉ"));p.add(diaChi);}
        p.add(new JLabel("SĐT")); p.add(sdt);
        p.add(new JLabel("Người liên hệ")); p.add(lienHe);
        p.add(new JLabel("Nhu cầu")); p.add(nhuCau);
        p.add(new JLabel("Trạng thái mới")); p.add(trangThai);
        p.add(new JLabel("Ghi chú")); p.add(ghiChu);
        int traLoi = JOptionPane.showConfirmDialog(this, p,
                cu == null ? "Tiếp nhận tuyển sinh" : "Sửa hồ sơ / Chuyển trạng thái",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (traLoi != JOptionPane.OK_OPTION) return;
        try {
            TrangThaiLuaChon luaChon = (TrangThaiLuaChon) trangThai.getSelectedItem();
            String maMoi = luaChon.ma();
            KhachTiemNang k = new KhachTiemNang(cu == null ? 0 : cu.maKhach(), 0,
                    ten.getText(), rongThanhNull(sdt.getText()),
                    rongThanhNull(lienHe.getText()), rongThanhNull(nhuCau.getText()),
                    maMoi, cu!=null&&cu.daHocThu(), rongThanhNull(ghiChu.getText()),
                    coSo==CoSo.TRUNG_TAM?ngaySinh.layNgay():cu==null?null:cu.ngaySinh(),
                    coSo==CoSo.TRUNG_TAM?switch(gioiTinh.getSelectedIndex()){case 1->"NAM";case 2->"NU";case 3->"KHAC";default->null;}:cu==null?null:cu.gioiTinh(),
                    coSo==CoSo.TRUNG_TAM?rongThanhNull(diaChi.getText()):cu==null?null:cu.diaChi());
            int id;
            if (cu == null) {
                id = controller.them(k);
                JOptionPane.showMessageDialog(this, "Đã tiếp nhận hồ sơ #" + id);
            } else {
                controller.sua(k);
                id = cu.maKhach();
                JOptionPane.showMessageDialog(this,
                        "Đã lưu hồ sơ. Hồ sơ được hiển thị tại tab '" + tenTrangThai(maMoi) + "'.");
            }
            if (cu == null) txtTim.setText("");
            moTabVaTaiLai(maMoi, id);
        } catch (SQLException | IllegalArgumentException ex) {
            loi(ex);
        }
    }

    private void chuyenDoi() {
        KhachTiemNang k = duocChon(true);
        if (k == null) return;
        if (coSo == CoSo.TRUNG_TAM) {
            if ("KHONG_DANG_KY".equals(k.trangThai())) {
                JOptionPane.showMessageDialog(this, "Hồ sơ Không đăng ký không thể đăng ký khóa.");
                return;
            }
            DangKyKhoaHocPanel.moDangKyTuHoSo(this, k, ketQua ->
                    moTabVaTaiLai("DA_CHUYEN_DOI".equals(k.trangThai())
                            ? "DA_CHUYEN_DOI" : "CHO_XEP_LOP",
                            k.maKhach()));
            return;
        }
        if ("DA_CHUYEN_DOI".equals(k.trangThai()) || "KHONG_DANG_KY".equals(k.trangThai())) {
            JOptionPane.showMessageDialog(this, "Trạng thái hồ sơ này không cho phép chuyển đổi.");
            return;
        }
        int dongY = JOptionPane.showConfirmDialog(this,
                "Chuyển hồ sơ #" + k.maKhach() + " thành học viên và tạo yêu cầu xếp lớp?",
                "Xác nhận chuyển đổi", JOptionPane.YES_NO_OPTION);
        if (dongY != JOptionPane.YES_OPTION) return;
        try {
            int maHocVien = controller.chuyenDoi(k.maKhach());
            JOptionPane.showMessageDialog(this, "Đã tạo học viên #" + maHocVien
                    + ". Hãy bổ sung ngày sinh/giới tính tại màn hình Học viên.");
            moTabVaTaiLai("DA_CHUYEN_DOI", k.maKhach());
        } catch (SQLException | IllegalArgumentException ex) {
            loi(ex);
        }
    }

    private static String tenTrangThai(String ma) {
        for (int i = 0; i < MA_TAB.length; i++) {
            if (MA_TAB[i].equals(ma)) return TEN_TAB[i];
        }
        return ma;
    }

    private record TrangThaiLuaChon(String ma, String ten) {
        @Override public String toString() { return ten; }
    }

    private static String chuoi(String s) { return s == null ? "" : s; }
    private static String rongThanhNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private void loi(Exception ex) {
        Throwable goc = ex;
        while (goc.getCause() != null && goc.getCause() != goc) {
            goc = goc.getCause();
        }
        thongBao.setText("Lỗi: " + goc.getMessage());
        JOptionPane.showMessageDialog(this, goc.getMessage(), "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
