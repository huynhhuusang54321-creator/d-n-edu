package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import vn.vinabacus.controller.TuyenSinhController;
import vn.vinabacus.model.CoSo;
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
        "Theo dõi hồ sơ học thử; tích 'Đã học thử' khi cập nhật thông tin phù hợp.",
        "Hồ sơ đang chờ xác nhận đăng ký.",
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
    private final JTextField txtTim = new JTextField(18);
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
    private final Map<String, DefaultTableModel> models = new LinkedHashMap<>();
    private final Map<String, JTable> bangs = new LinkedHashMap<>();
    private final Map<String, List<KhachTiemNang>> danhSachTheoTab = new LinkedHashMap<>();
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private final JButton sua = new JButton("Sửa hồ sơ");
    private final JButton chuyen = new JButton("Chuyển thành học viên");
    private long phienTai = 0;
    private int maCanChon = -1;

    public TuyenSinhPanel(CoSo coSo) {
        super(new BorderLayout(10, 10));
        controller = new TuyenSinhController(coSo);
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
        congCu.add(sua);
        congCu.add(chuyen);
        dau.add(congCu, BorderLayout.CENTER);
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
            bangs.put(trangThai, bang);

            JPanel trang = new JPanel(new BorderLayout(0, 6));
            JLabel moTa = new JLabel(MO_TA_TAB[i]);
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
            if (khach != null) form(khach);
        });
        chuyen.addActionListener(e -> chuyenDoi());
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
        boolean khongDangKy = coChon && "KHONG_DANG_KY".equals(k.trangThai());
        sua.setEnabled(coChon && !daChuyen);
        chuyen.setEnabled(coChon && !daChuyen && !khongDangKy);
    }

    private void taiDuLieu() {
        final long phien = ++phienTai;
        final String tuKhoa = txtTim.getText().trim();
        thongBao.setText("Đang tải hồ sơ tuyển sinh...");
        new SwingWorker<List<KhachTiemNang>, Void>() {
            @Override protected List<KhachTiemNang> doInBackground() throws Exception {
                // Tra ve tat ca trang thai; giao dien chia vao dung 6 tab.
                // Viec tim theo ma/ten/sdt van dung Repository da duoc kiem thu.
                return controller.timKiem(tuKhoa, "TAT_CA");
            }
            @Override protected void done() {
                if (phien != phienTai) return;
                try {
                    List<KhachTiemNang> ketQua = get();
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
                        chonDongTheoMa(maCanChon);
                        maCanChon = -1;
                    }
                    capNhatNut();
                } catch (Exception e) {
                    loi(e);
                }
            }
        }.execute();
    }

    private void chonDongTheoMa(int maKhach) {
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
                return;
            }
        }
    }

    private void moTabVaTaiLai(String trangThai, int maKhach) {
        // Xoa bo loc tu khoa neu khong khop ho so sau khi sua.
        txtTim.setText("");
        for (int i = 0; i < MA_TAB.length; i++) {
            if (MA_TAB[i].equals(trangThai)) {
                tabs.setSelectedIndex(i);
                break;
            }
        }
        maCanChon = maKhach;
        taiDuLieu();
    }

    private void form(KhachTiemNang cu) {
        if (cu != null && "DA_CHUYEN_DOI".equals(cu.trangThai())) {
            JOptionPane.showMessageDialog(this,
                    "Hồ sơ đã chuyển đổi; hãy cập nhật thông tin ở màn hình Học viên.");
            return;
        }
        JTextField ten = new JTextField(cu == null ? "" : cu.hoTen(), 24);
        JTextField sdt = new JTextField(cu == null ? "" : chuoi(cu.soDienThoai()));
        JTextField lienHe = new JTextField(cu == null ? "" : chuoi(cu.tenNguoiLienHe()));
        JTextField nhuCau = new JTextField(cu == null ? "" : chuoi(cu.nhuCau()));
        JTextField ghiChu = new JTextField(cu == null ? "" : chuoi(cu.ghiChu()));
        JComboBox<TrangThaiLuaChon> trangThai = new JComboBox<>();
        for (String ma : TRANG_THAI_SUA) {
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
        JCheckBox daHocThu = new JCheckBox("Đã học thử", cu != null && cu.daHocThu());
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Họ tên (*)")); p.add(ten);
        p.add(new JLabel("SĐT")); p.add(sdt);
        p.add(new JLabel("Người liên hệ")); p.add(lienHe);
        p.add(new JLabel("Nhu cầu")); p.add(nhuCau);
        p.add(new JLabel("Trạng thái mới")); p.add(trangThai);
        p.add(new JLabel("Học thử")); p.add(daHocThu);
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
                    maMoi, daHocThu.isSelected(), rongThanhNull(ghiChu.getText()));
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
            moTabVaTaiLai(maMoi, id);
        } catch (SQLException | IllegalArgumentException ex) {
            loi(ex);
        }
    }

    private void chuyenDoi() {
        KhachTiemNang k = duocChon(true);
        if (k == null) return;
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
