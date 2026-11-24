package vn.vinabacus.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.MamNonController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.HocVien;
import vn.vinabacus.model.LopMamNon;
import vn.vinabacus.model.TaiChinhDTO.KhoanThu;
import vn.vinabacus.model.XepLopMamNon;
import vn.vinabacus.repository.MamNonRepository.LichSuDiemDanh;
import vn.vinabacus.service.TaiChinhService;

/** Xem hồ sơ Mầm non và lịch sử từ dữ liệu đã lưu; không chỉnh sửa tài chính. */
public final class HocSinhMamNonDialog {
    private HocSinhMamNonDialog(){}
    private record Data(List<XepLopMamNon> xep,List<LichSuDiemDanh> diemDanh,
        List<KhoanThu> khoan,Map<Integer,LopMamNon> lop){}
    public static void mo(Component parent,HocVien hocSinh){
        Window owner=SwingUtilities.getWindowAncestor(parent);
        JDialog dialog=new JDialog(owner,"Chi tiết học sinh Mầm non – "+hocSinh.hoTen(),java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        root.add(MamNonUi.tieuDe("HỒ SƠ HỌC SINH – "+hocSinh.hoTen()),BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();
        JPanel profile=MamNonUi.form("Mã học sinh",new JLabel("#"+hocSinh.maHocVien()),
            "Họ tên",new JLabel(hocSinh.hoTen()),"Ngày sinh",new JLabel(NgaySinhPicker.hienThiNgay(hocSinh.ngaySinh())),
            "Giới tính",new JLabel(value(hocSinh.gioiTinh())),"Người liên hệ",new JLabel(value(hocSinh.tenNguoiLienHe())),
            "Số điện thoại",new JLabel(value(hocSinh.soDienThoaiLienHe())),"Địa chỉ",new JLabel(value(hocSinh.diaChi())),
            "Trạng thái",new JLabel(value(hocSinh.trangThaiHoSo())));
        tabs.addTab("Hồ sơ",new JScrollPane(profile));
        DefaultTableModel placements=MamNonUi.model("Năm học","Khối","Lớp","Từ ngày","Đến ngày","Trạng thái","Lý do");
        tabs.addTab("Quá trình xếp lớp",new JScrollPane(MamNonUi.bang(placements)));
        DefaultTableModel attendance=MamNonUi.model("Ngày","Lớp","Điểm danh","Ghi chú","Tài khoản");
        tabs.addTab("Điểm danh",new JScrollPane(MamNonUi.bang(attendance)));
        DefaultTableModel finance=MamNonUi.model("Mã khoản","Loại","Phải thu","Đã thu","Còn nợ","Trạng thái");
        JPanel money=new JPanel(new BorderLayout(8,8));money.add(new JScrollPane(MamNonUi.bang(finance)),BorderLayout.CENTER);
        JLabel debt=new JLabel("Đang tải công nợ...");money.add(debt,BorderLayout.SOUTH);tabs.addTab("Học phí & tiền ăn",money);
        root.add(tabs,BorderLayout.CENTER);
        JLabel status=new JLabel("Đang tải lịch sử học sinh...");
        JButton close=MamNonUi.nut("Đóng",false);close.addActionListener(e->dialog.dispose());
        JPanel foot=new JPanel(new BorderLayout());foot.add(status,BorderLayout.CENTER);foot.add(close,BorderLayout.EAST);root.add(foot,BorderLayout.SOUTH);
        dialog.setContentPane(root);dialog.setSize(new Dimension(960,590));dialog.setMinimumSize(new Dimension(700,450));dialog.setLocationRelativeTo(parent);
        MamNonController ctrl=new MamNonController();TaiChinhService taiChinh=new TaiChinhService();
        MamNonUi.tai(root,()->{
            List<XepLopMamNon> xep=ctrl.xepLop(null).stream().filter(x->x.maHocVien()==hocSinh.maHocVien()).toList();
            List<LichSuDiemDanh> diemDanh=ctrl.lichSu(null,hocSinh.maHocVien(),LocalDate.of(2000,1,1),LocalDate.of(2100,12,31));
            List<KhoanThu> khoan=taiChinh.khoanThu(CoSo.MAM_NON).stream().filter(k->k.maHocVien()==hocSinh.maHocVien()).toList();
            Map<Integer,LopMamNon> lop=ctrl.lop(null,"").stream().collect(Collectors.toMap(LopMamNon::maLop,Function.identity()));
            return new Data(xep,diemDanh,khoan,lop);
        },data->{
            for(XepLopMamNon x:data.xep()){
                LopMamNon l=data.lop().get(x.maLop());
                placements.addRow(new Object[]{l==null?"—":l.tenNamHoc(),l==null?"—":tenKhoi(l.khoi()),x.tenLop(),x.tuNgay(),MamNonUi.ngay(x.denNgay()),x.trangThai(),value(x.lyDo())});
            }
            for(LichSuDiemDanh d:data.diemDanh())attendance.addRow(new Object[]{d.ngayHoc(),d.tenLop(),d.trangThai(),value(d.ghiChu()),d.nguoiGhi()});
            BigDecimal sum=BigDecimal.ZERO;
            for(KhoanThu k:data.khoan()){
                finance.addRow(new Object[]{k.maKhoan(),k.tenLoai(),k.phaiThu().toPlainString(),k.daThu().toPlainString(),k.conNo().toPlainString(),k.trangThai()});
                sum=sum.add(k.conNo());
            }
            debt.setText("Công nợ hiện tại: "+sum.toPlainString()+" · Học phí, tiền ăn và phiếu thu đã xác nhận được đối soát theo khoản.");
            status.setText(data.xep().size()+" lần xếp lớp · "+data.diemDanh().size()+" ngày điểm danh · "+data.khoan().size()+" khoản thu");
        });
        dialog.setVisible(true);
    }
    private static String value(String s){return s==null||s.isBlank()?"—":s;}
    private static String tenKhoi(String k){return "MAM".equals(k)?"Mầm":"CHOI".equals(k)?"Chồi":"LA".equals(k)?"Lá":"Chưa xác định";}
}
