package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.LopTrungTamController;
import vn.vinabacus.model.LichHocLop;
import vn.vinabacus.model.LopTrungTam;
import vn.vinabacus.repository.LopTrungTamRepository.HocVienTrongLop;
import vn.vinabacus.service.QuyTacSiSoLop;

/** Chi tiết Trung tâm đọc trực tiếp từ đăng ký, xếp lớp và lịch học hiện có. */
public final class ChiTietLopTrungTamDialog extends JDialog {
    private final JLabel thongBao = new JLabel("Đang tải chi tiết lớp...");
    private final JLabel tongQuan = new JLabel("Đang tính sĩ số...");
    private final JLabel dangHoc = new JLabel("Đang tải...");
    private final DefaultTableModel modelHocVien = bangModel(
            "Mã HV", "Học viên", "Khóa đăng ký", "Ngày xếp", "Đến ngày", "Trạng thái");
    private final DefaultTableModel modelLich = bangModel(
            "Thứ", "Bắt đầu", "Kết thúc", "Từ ngày", "Đến ngày", "Phòng", "Trạng thái");

    private record DuLieu(List<HocVienTrongLop> hocVien, List<LichHocLop> lich) { }

    public ChiTietLopTrungTamDialog(Window owner, LopTrungTam lop,
                                    LopTrungTamController controller) {
        this(owner, lop, controller, 0);
    }

    public ChiTietLopTrungTamDialog(Window owner, LopTrungTam lop,
                                    LopTrungTamController controller, int tabBanDau) {
        super(owner, "Chi tiết lớp " + lop.tenLop(), Dialog.ModalityType.APPLICATION_MODAL);
        JPanel chinh = new JPanel(new BorderLayout(8, 8));
        chinh.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTabbedPane tabs = new JTabbedPane();
        JPanel thongTin = new JPanel(new GridLayout(0, 2, 12, 12));
        thongTin.setBackground(Color.WHITE);
        thongTin.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        thongTin.add(nhom("THÔNG TIN CƠ BẢN", new String[][]{
            {"Mã lớp", "#"+lop.maLop()}, {"Tên lớp",lop.tenLop()},
            {"Môn học",lop.tenMonHoc()}, {"Khóa học",lop.tenKhoaHoc()},
            {"Trạng thái",trangThai(lop.trangThai())}}));
        thongTin.add(nhom("THÔNG TIN PHỤ TRÁCH", new String[][]{
            {"Giáo viên",lop.tenGiaoVien()==null?"Chưa phân công":lop.tenGiaoVien()},
            {"Ngày khai giảng",NgayKhaiGiangPicker.hienThiNgay(lop.ngayKhaiGiang())},
            {"Phòng theo lịch",lop.tenPhong()==null?"Chưa có lịch/phòng":lop.tenPhong()}}));
        QuyTacSiSoLop.KetQua siSoHienThi = QuyTacSiSoLop.danhGia(
                lop.siSoToiDa(),lop.sucChuaPhong(),lop.siSoDaDat());
        JPanel sucChuaCard=nhom("SỨC CHỨA", new String[][]{
            {"Sĩ số tối đa",lop.siSoToiDa()==null?"Chưa đặt":lop.siSoToiDa().toString()},
            {"Đã giữ chỗ",String.valueOf(lop.siSoDaDat())},
            {"Chỗ còn lại",siSoHienThi.gioiHan()==0?"Chưa nhận học viên"
                    :String.valueOf(siSoHienThi.conCho())},
            {"Sức chứa phòng",lop.sucChuaPhong()==null?"Chưa xác định":lop.sucChuaPhong().toString()}});
        sucChuaCard.add(new JLabel("Đang học"));sucChuaCard.add(dangHoc);
        thongTin.add(sucChuaCard);
        thongTin.add(nhom("GHI CHÚ", new String[][]{
            {"Lưu ý","Phòng được gán theo từng lịch hoặc buổi học. Xem tab Lịch học."}}));
        tabs.addTab("Thông tin lớp", new JScrollPane(thongTin));
        JTable bangHocVien = new JTable(modelHocVien);
        bangHocVien.setRowHeight(28);
        tabs.addTab("Danh sách học viên", new JScrollPane(bangHocVien));
        JTable bangLich = new JTable(modelLich);
        bangLich.setRowHeight(28);
        tabs.addTab("Lịch học", new JScrollPane(bangLich));
        JPanel tabTongQuan = new JPanel(new BorderLayout());
        tongQuan.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        tabTongQuan.add(tongQuan, BorderLayout.NORTH);
        tabs.addTab("Tổng quan", tabTongQuan);
        tabs.setSelectedIndex(Math.max(0, Math.min(tabBanDau, tabs.getTabCount() - 1)));
        chinh.add(tabs, BorderLayout.CENTER);
        JPanel duoi = new JPanel(new BorderLayout());
        duoi.add(thongBao, BorderLayout.CENTER);
        JButton dong = new JButton("Đóng");
        dong.addActionListener(e -> dispose());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nut.add(dong);
        duoi.add(nut, BorderLayout.EAST);
        chinh.add(duoi, BorderLayout.SOUTH);
        setContentPane(chinh);
        setSize(900, 560);
        setLocationRelativeTo(owner);
        taiDuLieu(lop, controller);
    }

    private void taiDuLieu(LopTrungTam lop, LopTrungTamController controller) {
        new SwingWorker<DuLieu, Void>() {
            @Override protected DuLieu doInBackground() throws Exception {
                return new DuLieu(controller.hocVienLop(lop.maLop()),
                        controller.lichLop(lop.maLop()));
            }
            @Override protected void done() {
                try {
                    DuLieu dl = get();
                    for (HocVienTrongLop h : dl.hocVien()) {
                        modelHocVien.addRow(new Object[]{h.maHocVien(), h.hoTen(),
                                h.tenKhoaHoc(), NgayKhaiGiangPicker.hienThiNgay(h.tuNgay()),
                                NgayKhaiGiangPicker.hienThiNgay(h.denNgay()), h.trangThai()});
                    }
                    for (LichHocLop lich : dl.lich()) {
                        modelLich.addRow(new Object[]{lich.thuTrongTuan(),
                                lich.gioBatDau(), lich.gioKetThuc(),
                                NgayKhaiGiangPicker.hienThiNgay(lich.tuNgay()),
                                NgayKhaiGiangPicker.hienThiNgay(lich.denNgay()),
                                lich.tenPhong() == null ? "Chưa gán phòng" : lich.tenPhong(),
                                lich.trangThai()});
                    }
                    LocalDate nay = LocalDate.now();
                    long siSo = dl.hocVien().stream().filter(h -> "HIEU_LUC".equals(h.trangThai())
                            && !h.tuNgay().isAfter(nay)
                            && (h.denNgay() == null || !h.denNgay().isBefore(nay)))
                            .map(HocVienTrongLop::maHocVien).distinct().count();
                    dangHoc.setText(String.valueOf(siSo));
                    QuyTacSiSoLop.KetQua siSoLop = QuyTacSiSoLop.danhGia(
                            lop.siSoToiDa(), lop.sucChuaPhong(), lop.siSoDaDat());
                    String cho = siSoLop.gioiHan() == 0 ? "Chưa nhận học viên"
                            : String.valueOf(siSoLop.conCho());
                    tongQuan.setText("<html>Đang học: " + siSo + " · Đã giữ chỗ: "
                            + lop.siSoDaDat() + " · Giới hạn lớp: "
                            + (lop.siSoToiDa() == null ? "Chưa đặt" : lop.siSoToiDa())
                            + " · Sức chứa phòng thấp nhất: "
                            + (lop.sucChuaPhong() == null ? "Chưa xác định" : lop.sucChuaPhong())
                            + "<br>Còn chỗ theo giới hạn thấp nhất: " + cho
                            + " · Lịch đã ghi: " + dl.lich().size()
                            + " · Trạng thái: " + lop.trangThai() + "</html>");
                    thongBao.setText(dl.hocVien().isEmpty() ? "Lớp chưa có học viên xếp vào."
                            : "Đã tải " + dl.hocVien().size() + " lượt xếp lớp (gồm lịch sử).");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); thongBao.setText("Tải chi tiết bị gián đoạn.");
                    tongQuan.setText("Chưa đủ dữ liệu để tính sĩ số.");
                } catch (ExecutionException ex) {
                    Throwable goc = ex.getCause() == null ? ex : ex.getCause();
                    thongBao.setText("Không tải được chi tiết: " + goc.getMessage());
                    tongQuan.setText("Chưa đủ dữ liệu để tính sĩ số.");
                }
            }
        }.execute();
    }

    private static DefaultTableModel bangModel(String... cot) {
        return new DefaultTableModel(cot, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
    private static JPanel nhom(String ten,String[][] dong) {
        JPanel p=new JPanel(new GridLayout(0,2,8,9));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(ten),BorderFactory.createEmptyBorder(10,10,10,10)));
        for(String[] d:dong) {
            JLabel nhan=new JLabel(d[0]);nhan.setForeground(GiaoDienMau.XANH_DAM);p.add(nhan);
            p.add(new JLabel("<html>"+d[1]+"</html>"));
        }
        return p;
    }
    private static String trangThai(String ma) {
        return switch (ma) {
            case "DANG_MO" -> "Đang mở";
            case "TAM_DUNG" -> "Tạm dừng";
            case "DA_DONG" -> "Đã đóng";
            default -> ma;
        };
    }
}
