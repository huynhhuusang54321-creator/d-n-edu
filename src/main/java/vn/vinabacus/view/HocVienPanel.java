package vn.vinabacus.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import vn.vinabacus.controller.ChiTietHocVienTrungTamController;
import vn.vinabacus.controller.HocVienController;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.ChiTietHocVienTrungTam.TongQuan;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.LopMamNon;
import vn.vinabacus.model.NamHocMamNon;
import vn.vinabacus.model.XepLopMamNon;

public final class HocVienPanel extends JPanel {
    private static final String[] COT_TRUNG_TAM = {
        "Mã", "Họ tên", "Ngày sinh", "Giới tính", "Người liên hệ", "SĐT",
        "Khóa đã đăng ký", "Đã xếp", "Đang chờ", "Tình trạng xếp lớp", "Hồ sơ"
    };
    private static final String[] COT_MAM_NON = {
        "Mã", "Họ tên", "Ngày sinh", "Giới tính", "Người liên hệ", "SĐT",
        "Địa chỉ", "Năm học", "Lớp", "Trạng thái", "Ghi chú"
    };
    private static final String[] LOC_TRUNG_TAM = {
        "Tất cả", "Chưa đăng ký khóa", "Chưa xếp lớp", "Xếp lớp một phần", "Đã xếp đủ"
    };
    private static final TongQuan CHUA_DANG_KY = new TongQuan(List.of(), 0, 0, 0, false);

    private final HocVienController controller;
    private final ChiTietHocVienTrungTamController chiTiet =
            new ChiTietHocVienTrungTamController();
    private final CoSo coSo;
    private final JTextField txtTim = new JTextField(18);
    private final JComboBox<String> locTrangThai;
    private final DefaultTableModel model;
    private final JTable bang;
    private List<HocVien> danhSach = List.of();
    private final JLabel thongBao = new JLabel("Sẵn sàng");
    private int luotTai;
    private Integer maCanChon;

    private record DuLieu(List<HocVien> hocVien, Map<Integer, TongQuan> tinhTrang,
                          Map<Integer,String> namHoc,Map<Integer,String> lop) { }

    public HocVienPanel(CoSo coSo) {
        super(new BorderLayout(10, 10));
        this.coSo = coSo;
        this.controller = new HocVienController(coSo);
        this.locTrangThai = new JComboBox<>(coSo == CoSo.TRUNG_TAM
                ? LOC_TRUNG_TAM : new String[]{"Tất cả", "Đang hoạt động", "Ngừng sử dụng"});
        this.model = new DefaultTableModel(coSo == CoSo.TRUNG_TAM
                ? COT_TRUNG_TAM : COT_MAM_NON, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 || (coSo == CoSo.TRUNG_TAM && (c == 7 || c == 8))
                        ? Integer.class : String.class;
            }
        };
        this.bang = new JTable(model);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel tren = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("QUẢN LÝ " + (coSo == CoSo.MAM_NON
                ? "HỌC SINH MẦM NON" : "HỌC VIÊN TRUNG TÂM"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        tren.add(title, BorderLayout.NORTH);
        JPanel thaoTac = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton tim = new JButton("Tìm kiếm"), taiLai = new JButton("Tải lại"),
                them = new JButton("Thêm"), sua = new JButton("Sửa"),
                xem = new JButton("Xem chi tiết"), dangKy = new JButton("Đăng ký thêm khóa");
        thaoTac.add(new JLabel("Mã / tên / SĐT:"));
        thaoTac.add(txtTim);
        thaoTac.add(new JLabel(coSo == CoSo.TRUNG_TAM ? "Xếp lớp:" : "Trạng thái:"));
        thaoTac.add(locTrangThai);
        thaoTac.add(tim);
        thaoTac.add(taiLai);
        if (coSo == CoSo.TRUNG_TAM) {
            JPanel haiHang = new JPanel(new GridLayout(0, 1, 0, 2));
            JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            hangNut.add(sua);
            hangNut.add(dangKy);
            haiHang.add(thaoTac);
            haiHang.add(hangNut);
            tren.add(haiHang, BorderLayout.CENTER);
        } else {
            thaoTac.add(them);
            thaoTac.add(sua);
            thaoTac.add(xem);
            tren.add(thaoTac, BorderLayout.CENTER);
        }
        add(tren, BorderLayout.NORTH);

        bang.setRowHeight(30);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.getTableHeader().setReorderingAllowed(false);
        TableRowSorter<DefaultTableModel> sapXep = new TableRowSorter<>(model);
        DateTimeFormatter ngayVN = DateTimeFormatter.ofPattern("dd/MM/uuuu");
        sapXep.setComparator(2, (String a, String b) -> {
            if (a == null || a.isBlank()) return (b == null || b.isBlank()) ? 0 : -1;
            if (b == null || b.isBlank()) return 1;
            return LocalDate.parse(a, ngayVN).compareTo(LocalDate.parse(b, ngayVN));
        });
        bang.setRowSorter(sapXep);
        bang.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton()==MouseEvent.BUTTON1&&bang.rowAtPoint(e.getPoint())>=0&&
                    (e.getClickCount()==2||e.getClickCount()==1&&bang.columnAtPoint(e.getPoint())==1))
                    moChiTiet();
            }
        });
        bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] rong = coSo == CoSo.TRUNG_TAM
                ? new int[]{52, 160, 100, 75, 140, 110, 230, 70, 75, 155, 120}
                : new int[]{52, 170, 100, 75, 145, 115, 210, 145, 170, 125, 210};
        for (int i = 0; i < rong.length; i++)
            bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        add(new JScrollPane(bang), BorderLayout.CENTER);
        add(thongBao, BorderLayout.SOUTH);

        tim.addActionListener(e -> taiDuLieu());
        txtTim.addActionListener(e -> taiDuLieu());
        locTrangThai.addActionListener(e -> taiDuLieu());
        taiLai.addActionListener(e -> {
            txtTim.setText("");
            if (locTrangThai.getSelectedIndex() == 0) taiDuLieu();
            else locTrangThai.setSelectedIndex(0);
        });
        them.addActionListener(e -> form(null));
        sua.addActionListener(e -> {
            if (coSo == CoSo.TRUNG_TAM) moChiTiet();
            else {
                HocVien h = duocChon();
                if (h != null) form(h);
            }
        });
        xem.addActionListener(e -> moChiTiet());
        dangKy.addActionListener(e -> {
            HocVien h = duocChon();
            if (h != null) DangKyKhoaHocPanel.moDangKyTuHocVien(this, h,
                    ketQua -> {
                        maCanChon = h.maHocVien();
                        taiDuLieu();
                    });
        });
        SwingUtilities.invokeLater(this::taiDuLieu);
    }

    private String maTrangThaiMamNon() {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 -> "HOAT_DONG";
            case 2 -> "NGUNG_SU_DUNG";
            default -> "TAT_CA";
        };
    }

    private boolean khopBoLoc(TongQuan q) {
        return switch (locTrangThai.getSelectedIndex()) {
            case 1 -> q.tongKhoa() == 0;
            case 2 -> q.tongKhoa() > 0 && q.daXep() == 0;
            case 3 -> q.daXep() > 0 && q.dangCho() > 0;
            case 4 -> q.tongKhoa() > 0 && q.dangCho() == 0;
            default -> true;
        };
    }

    private void taiDuLieu() {
        final int lan = ++luotTai;
        thongBao.setText("Đang tải dữ liệu...");
        final String tuKhoa = txtTim.getText().trim();
        final String trangThai = coSo == CoSo.TRUNG_TAM ? "TAT_CA" : maTrangThaiMamNon();
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                List<HocVien> hocVien = controller.timKiem(tuKhoa, trangThai);
                if(coSo==CoSo.TRUNG_TAM)return new DuLieu(hocVien,chiTiet.tongQuan(),Map.of(),Map.of());
                MamNonController mamNon=new MamNonController();
                Map<Integer,LopMamNon> classes=new java.util.HashMap<>();
                for(LopMamNon l:mamNon.lop(null,""))classes.put(l.maLop(),l);
                Map<Integer,NamHocMamNon> schoolYears=new java.util.HashMap<>();
                for(NamHocMamNon y:mamNon.namHoc())schoolYears.put(y.maNamHoc(),y);
                Map<Integer,String> years=new java.util.HashMap<>(),names=new java.util.HashMap<>();
                LocalDate today=LocalDate.now();
                for(XepLopMamNon x:mamNon.xepLop(null))if("HIEU_LUC".equals(x.trangThai())&&x.denNgay()==null&&!today.isBefore(x.tuNgay())){
                    LopMamNon l=classes.get(x.maLop());
                    NamHocMamNon y=l==null?null:schoolYears.get(l.maNamHoc());
                    if(y!=null&&!today.isBefore(y.tuNgay())&&!today.isAfter(y.denNgay())){
                        years.put(x.maHocVien(),l.tenNamHoc());names.put(x.maHocVien(),l.tenLop());
                    }
                }
                return new DuLieu(hocVien,Map.of(),years,names);
            }

            @Override protected void done() {
                if (lan != luotTai) return;
                try {
                    DuLieu duLieu = get();
                    List<HocVien> hienThi = new ArrayList<>();
                    model.setRowCount(0);
                    for (HocVien h : duLieu.hocVien()) {
                        if (coSo == CoSo.TRUNG_TAM) {
                            TongQuan q = duLieu.tinhTrang().getOrDefault(
                                    h.maHocVien(), CHUA_DANG_KY);
                            if (q.hoSoTam() || !khopBoLoc(q)) continue;
                            hienThi.add(h);
                            model.addRow(new Object[]{h.maHocVien(), h.hoTen(),
                                NgaySinhPicker.hienThiNgay(h.ngaySinh()), h.gioiTinh(),
                                h.tenNguoiLienHe(), h.soDienThoaiLienHe(),
                                String.join(", ", q.tenKhoa()), q.daXep(), q.dangCho(),
                                q.tinhTrangXepLop(), tenTrangThaiHoSo(h.trangThaiHoSo())});
                        } else {
                            hienThi.add(h);
                            model.addRow(new Object[]{h.maHocVien(), h.hoTen(),
                                NgaySinhPicker.hienThiNgay(h.ngaySinh()), h.gioiTinh(),
                                h.tenNguoiLienHe(), h.soDienThoaiLienHe(),
                                h.diaChi(),duLieu.namHoc().getOrDefault(h.maHocVien(),"Chưa xếp"),
                                duLieu.lop().getOrDefault(h.maHocVien(),"Chưa xếp"),h.trangThaiHoSo(),h.ghiChu()});
                        }
                    }
                    danhSach = hienThi;
                    chonLai();
                    thongBao.setText("Tìm thấy " + danhSach.size() + " học viên"
                            + (coSo == CoSo.TRUNG_TAM
                               ? " · tình trạng xếp lớp tính từ đăng ký thực tế" : ""));
                } catch (Exception ex) {
                    hienLoi(ex);
                }
            }
        }.execute();
    }

    private void chonLai() {
        if (maCanChon == null) return;
        for (int i = 0; i < danhSach.size(); i++) {
            if (danhSach.get(i).maHocVien() == maCanChon) {
                int dong = bang.convertRowIndexToView(i);
                if (dong >= 0) bang.getSelectionModel().setSelectionInterval(dong, dong);
                break;
            }
        }
        maCanChon = null;
    }

    private HocVien duocChon() {
        int r = bang.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một học viên trong bảng");
            return null;
        }
        return danhSach.get(bang.convertRowIndexToModel(r));
    }

    private void moChiTiet() {
        HocVien h = duocChon();
        if (h == null) return;
        if(coSo==CoSo.MAM_NON){HocSinhMamNonDialog.mo(this,h);return;}
        ChiTietHocVienDialog.mo(this, h, () -> {
            maCanChon = h.maHocVien();
            taiDuLieu();
        });
    }

    /** Form cu cua Mam non duoc giu nguyen; them moi hoc vien truc tiep van su dung. */
    private void form(HocVien cu) {
        JTextField ten = new JTextField(cu == null ? "" : cu.hoTen(), 24);
        NgaySinhPicker ngay = new NgaySinhPicker(cu == null ? null : cu.ngaySinh());
        JComboBox<String> gioiTinh = new JComboBox<>(new String[]{"", "NAM", "NU", "KHAC"});
        JTextField lienHe = new JTextField(cu == null ? "" : chuoi(cu.tenNguoiLienHe()));
        JTextField sdt = new JTextField(cu == null ? "" : chuoi(cu.soDienThoaiLienHe()));
        JTextField diaChi = new JTextField(cu == null ? "" : chuoi(cu.diaChi()));
        JTextField ghiChu = new JTextField(cu == null ? "" : chuoi(cu.ghiChu()));
        JComboBox<String> trangThai = new JComboBox<>(new String[]{"HOAT_DONG", "NGUNG_SU_DUNG"});
        if (cu != null) {
            gioiTinh.setSelectedItem(cu.gioiTinh() == null ? "" : cu.gioiTinh());
            trangThai.setSelectedItem(cu.trangThaiHoSo());
        }
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Họ tên (*)")); p.add(ten);
        p.add(new JLabel("Ngày sinh (dd/MM/yyyy)")); p.add(ngay);
        p.add(new JLabel("Giới tính")); p.add(gioiTinh);
        p.add(new JLabel("Người liên hệ")); p.add(lienHe);
        p.add(new JLabel("Số điện thoại")); p.add(sdt);
        p.add(new JLabel("Địa chỉ")); p.add(diaChi);
        p.add(new JLabel("Ghi chú")); p.add(ghiChu);
        // Trung tam chua co ngay hieu luc / ly do / lich su cho Bao luu va Nghi hoc.
        if (coSo == CoSo.MAM_NON) {
            p.add(new JLabel("Trạng thái hồ sơ")); p.add(trangThai);
        }
        if (JOptionPane.showConfirmDialog(this, p, cu == null ? "Thêm học viên" : "Sửa học viên",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            HocVien h = new HocVien(cu == null ? 0 : cu.maHocVien(), 0,
                    ten.getText(), ngay.layNgay(),
                    rongThanhNull((String) gioiTinh.getSelectedItem()),
                    rongThanhNull(lienHe.getText()), rongThanhNull(sdt.getText()),
                    rongThanhNull(diaChi.getText()), rongThanhNull(ghiChu.getText()),
                    coSo == CoSo.TRUNG_TAM
                            ? (cu == null ? "HOAT_DONG" : cu.trangThaiHoSo())
                            : (String) trangThai.getSelectedItem());
            if (cu == null) {
                int id = controller.them(h);
                JOptionPane.showMessageDialog(this, "Đã thêm học viên mã " + id);
            } else {
                controller.sua(h);
                JOptionPane.showMessageDialog(this, "Đã lưu thay đổi");
            }
            taiDuLieu();
        } catch (SQLException | IllegalArgumentException ex) {
            hienLoi(ex);
        }
    }

    private static String tenTrangThaiHoSo(String trangThai) {
        return "NGUNG_SU_DUNG".equals(trangThai) ? "Ngừng sử dụng" : "Đang hoạt động";
    }
    private static String chuoi(String s) { return s == null ? "" : s; }
    private static String rongThanhNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
    private void hienLoi(Exception ex) {
        Throwable t = ex;
        while (t.getCause() != null && t != t.getCause()) t = t.getCause();
        thongBao.setText("Lỗi: " + t.getMessage());
        JOptionPane.showMessageDialog(this, t.getMessage(), "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
    }
}
