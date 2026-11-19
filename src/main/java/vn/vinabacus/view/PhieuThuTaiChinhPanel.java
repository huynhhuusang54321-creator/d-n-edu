package vn.vinabacus.view;

import java.awt.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.TaiChinhController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.TaiChinhDTO;
import vn.vinabacus.service.TaiChinhService;

/** Phiếu thu TV5: giao diện thống nhất với Quản lý thu phí, có kiểm tra khi bấm Lưu. */
public final class PhieuThuTaiChinhPanel extends JPanel {
    private final CoSo coSo;
    private final TaiChinhController ctrl = new TaiChinhController();
    private final JTextField thang = new JTextField(8), search = new JTextField(20);
    private final JLabel status = new JLabel("Đang chờ dữ liệu...");
    private final JLabel count = new JLabel("0 phiếu"), total = new JLabel("0đ");
    private final DefaultTableModel model = TaiChinhUi.model("Mã phiếu", "Số phiếu", "Học viên",
        "Số tiền", "Ngày thu", "Hình thức", "Trạng thái");
    private final JTable table = TaiChinhUi.bang(model);
    private List<TaiChinhDTO.KhoanThu> khoan = List.of();
    private List<TaiChinhDTO.LuaChon> taiKhoan = List.of();
    private List<TaiChinhDTO.PhieuThu> all = List.of(), shown = List.of();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PhieuThuTaiChinhPanel(CoSo coSo) {
        super(new BorderLayout(0,10));this.coSo=coSo;
        setBackground(TaiChinhUi.NEN);
        setBorder(BorderFactory.createEmptyBorder(12,8,8,8));
        thang.setText(YearMonth.now().toString());
        TaiChinhUi.field(search);
        JButton add=TaiChinhUi.nut("+ Lập phiếu thu",true);
        JButton refresh=TaiChinhUi.nut("Làm mới",false);
        JPanel top=new JPanel(new BorderLayout(0,10));top.setOpaque(false);
        JPanel titleRow=new JPanel(new BorderLayout());titleRow.setOpaque(false);
        JLabel title=TaiChinhUi.title("PHIẾU THU – "+coSo.ten().toUpperCase());
        title.setFont(TaiChinhUi.font(Font.BOLD,17));titleRow.add(title,BorderLayout.WEST);
        titleRow.add(add,BorderLayout.EAST);
        top.add(titleRow,BorderLayout.NORTH);
        JPanel filter=new JPanel(new BorderLayout(10,0));filter.setOpaque(false);
        JPanel left=new JPanel(new BorderLayout(6,0));left.setOpaque(false);
        left.add(new JLabel("Tìm phiếu thu:"),BorderLayout.WEST);
        left.add(search,BorderLayout.CENTER);
        filter.add(left,BorderLayout.CENTER);
        filter.add(TaiChinhUi.hang(new JLabel("Tháng:"),TaiChinhUi.oThang(thang,this),refresh),BorderLayout.EAST);
        top.add(filter,BorderLayout.CENTER);
        JPanel overview=TaiChinhUi.card(new Color(235,249,239),TaiChinhUi.VIEN,14);
        overview.setLayout(new BorderLayout());overview.setBorder(BorderFactory.createEmptyBorder(9,16,9,16));
        count.setFont(TaiChinhUi.font(Font.BOLD,13));count.setForeground(TaiChinhUi.XANH_DAM);
        total.setFont(TaiChinhUi.font(Font.BOLD,16));total.setForeground(TaiChinhUi.XANH);
        overview.add(count,BorderLayout.WEST);overview.add(total,BorderLayout.EAST);
        top.add(overview,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] widths={85,220,180,120,150,115,100};
        for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,
                    boolean focus,int row,int col){
                super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(TaiChinhUi.font(Font.BOLD,13));
                setForeground(TaiChinhUi.XANH_DAM);
                setBackground(selected?new Color(224,245,231):Color.WHITE);
                return this;
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override public void mouseClicked(java.awt.event.MouseEvent e){
                if(e.getClickCount()==2&&SwingUtilities.isLeftMouseButton(e))xemChiTiet();
            }
        });
        add(TaiChinhUi.scroll(table),BorderLayout.CENTER);
        status.setForeground(TaiChinhUi.CHU_MO);
        status.setFont(TaiChinhUi.font(Font.PLAIN,11));
        JPanel footer=TaiChinhUi.hang(status);
        JButton detail=TaiChinhUi.nut("Xem chi tiết phiếu",false);
        detail.addActionListener(e->xemChiTiet());
        footer.add(detail);add(footer,BorderLayout.SOUTH);
        refresh.addActionListener(e->load());
        thang.addActionListener(e->load());
        add.addActionListener(e->taoPhieu());
        search.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){filter();}
            public void removeUpdate(DocumentEvent e){filter();}
            public void changedUpdate(DocumentEvent e){filter();}
        });
        SwingUtilities.invokeLater(this::load);
    }
    private YearMonth ky(){
        try{return YearMonth.parse(thang.getText().trim());}
        catch(Exception ex){throw new IllegalArgumentException("Tháng không hợp lệ, nhập yyyy-MM.");}
    }
    public void load(){loadInternal(null);}
    public void loadVaLapPhieu(int maKhoan){loadInternal(maKhoan);}
    private record Data(List<TaiChinhDTO.PhieuThu> receipts,
        List<TaiChinhDTO.KhoanThu> khoan,List<TaiChinhDTO.LuaChon> accounts){}
    private void loadInternal(Integer maKhoanCanThu){
        YearMonth month;
        try{month=ky();}catch(Exception ex){TaiChinhUi.loi(this,ex);return;}
        status.setText("Đang tải phiếu thu – "+coSo.ten()+"...");
        TaiChinhUi.tai(this,()->new Data(ctrl.phieu(coSo,month),ctrl.khoan(coSo),ctrl.taiKhoan(coSo)),result->{
            khoan=result.khoan();taiKhoan=result.accounts();all=result.receipts();filter();
            status.setText("Đã tải "+all.size()+" phiếu trong tháng – "+coSo.ten());
            if(maKhoanCanThu!=null)taoPhieu(maKhoanCanThu);
        });
    }
    private void filter(){
        String q=search.getText().toLowerCase(Locale.ROOT).trim();
        List<TaiChinhDTO.PhieuThu> filtered=new ArrayList<>();
        BigDecimal amount=BigDecimal.ZERO;
        model.setRowCount(0);
        for(TaiChinhDTO.PhieuThu p:all){
            String haystack=(p.soPhieu()+" "+p.hocVien()+" "+p.maPhieu()).toLowerCase(Locale.ROOT);
            if(!haystack.contains(q))continue;
            filtered.add(p);
            model.addRow(new Object[]{p.maPhieu(),p.soPhieu(),p.hocVien(),
                TaiChinhService.hienTien(p.soTien()),p.ngayThu()==null?"—":p.ngayThu().format(DATE),
                tenHinhThuc(p.hinhThuc()),"XAC_NHAN".equals(p.trangThai())?"Đã xác nhận":p.trangThai()});
            if("XAC_NHAN".equals(p.trangThai()))amount=amount.add(p.soTien());
        }
        shown=filtered;
        count.setText("Tổng: "+shown.size()+" phiếu thu");
        total.setText("Đã thu theo bộ lọc: "+TaiChinhService.hienTien(amount));
    }
    private static String tenHinhThuc(String method){
        if(method==null)return "—";
        return switch(method){case "TIEN_MAT"->"Tiền mặt";case "CHUYEN_KHOAN"->"Chuyển khoản";
            case "KHAC"->"Khác";default->method;};
    }
    private void xemChiTiet(){
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Hãy chọn phiếu thu cần xem.");return;}
        int modelRow=table.convertRowIndexToModel(row);
        if(modelRow<0||modelRow>=shown.size())return;
        TaiChinhDTO.PhieuThu p=shown.get(modelRow);
        JPanel details=TaiChinhUi.doc(
            TaiChinhUi.theTien(new String[]{"Số tiền thực thu"},
                new String[]{TaiChinhService.hienTien(p.soTien())+"đ"},new Color[]{TaiChinhUi.XANH}),
            TaiChinhUi.nhom("Thông tin giao dịch",TaiChinhUi.thongTinHaiCot(
                "Số phiếu",p.soPhieu(),"Học viên",p.hocVien(),
                "Ngày thu",p.ngayThu()==null?"—":p.ngayThu().format(DATE),
                "Hình thức",tenHinhThuc(p.hinhThuc()),
                "Trạng thái","XAC_NHAN".equals(p.trangThai())?"Đã xác nhận":p.trangThai(),
                "Mã phiếu",String.valueOf(p.maPhieu()))));
        TaiChinhUi.xemThongTin(this,"Chi tiết phiếu thu #"+p.maPhieu(),
            "Thông tin giao dịch đã ghi nhận",details);
    }
    private void taoPhieu(){taoPhieu(null);}
    private void taoPhieu(Integer maKhoanUuTien){
        List<TaiChinhDTO.KhoanThu> debt=khoan.stream().filter(k->k.conNo().signum()>0).toList();
        if(debt.isEmpty()){
            JOptionPane.showMessageDialog(this,"Không có khoản thu còn nợ thuộc cơ sở này.");return;
        }
        if(taiKhoan.isEmpty()){
            JOptionPane.showMessageDialog(this,"Chưa có tài khoản đang hoạt động thuộc cơ sở để ghi phiếu.");return;
        }
        JComboBox<TaiChinhDTO.LuaChon> cbKhoan=new JComboBox<>();
        for(TaiChinhDTO.KhoanThu k:debt)
            cbKhoan.addItem(new TaiChinhDTO.LuaChon(k.maKhoan(),
                "#"+k.maKhoan()+" | "+k.hocVien()+" | "+k.tenLoai()+" | Nợ "+TaiChinhService.hienTien(k.conNo())));
        JComboBox<TaiChinhDTO.LuaChon> cbTaiKhoan=new JComboBox<>(taiKhoan.toArray(new TaiChinhDTO.LuaChon[0]));
        JComboBox<String> cbHinhThuc=new JComboBox<>(new String[]{"Tiền mặt","Chuyển khoản","Khác"});
        JTextField amount=new JTextField(15),note=new JTextField(22);
        JLabel remaining=new JLabel("Số dư còn phải thu: —");
        remaining.setFont(TaiChinhUi.font(Font.BOLD,13));remaining.setForeground(new Color(176,55,39));
        cbKhoan.addActionListener(e->{
            TaiChinhDTO.LuaChon item=(TaiChinhDTO.LuaChon)cbKhoan.getSelectedItem();
            if(item!=null)for(TaiChinhDTO.KhoanThu k:debt)if(k.maKhoan()==item.id()){
                amount.setText(TaiChinhService.hienTien(k.conNo()));
                remaining.setText("Số dư còn phải thu: "+TaiChinhService.hienTien(k.conNo()));
                break;
            }
        });
        if(maKhoanUuTien!=null){
            boolean found=false;
            for(int i=0;i<cbKhoan.getItemCount();i++)if(cbKhoan.getItemAt(i).id()==maKhoanUuTien){
                cbKhoan.setSelectedIndex(i);found=true;break;
            }
            if(!found){
                JOptionPane.showMessageDialog(this,"Khoản thu đã thanh toán đủ hoặc không còn hiệu lực. Hãy làm mới.");
                return;
            }
        }
        if(cbKhoan.getSelectedIndex()>=0){
            // Đồng bộ số dư ngay cả khi mục mặc định không phát sinh sự kiện thay đổi.
            TaiChinhDTO.LuaChon item=(TaiChinhDTO.LuaChon)cbKhoan.getSelectedItem();
            for(TaiChinhDTO.KhoanThu k:debt)if(k.maKhoan()==item.id()){
                amount.setText(TaiChinhService.hienTien(k.conNo()));
                remaining.setText("Số dư còn phải thu: "+TaiChinhService.hienTien(k.conNo()));break;
            }
        }
        JLabel after=new JLabel("Còn lại sau thanh toán: —");
        after.setFont(TaiChinhUi.font(Font.BOLD,13));after.setForeground(TaiChinhUi.XANH_DAM);
        Runnable update=()->{
            TaiChinhDTO.LuaChon item=(TaiChinhDTO.LuaChon)cbKhoan.getSelectedItem();
            if(item==null)return;
            for(TaiChinhDTO.KhoanThu k:debt)if(k.maKhoan()==item.id()){
                try{
                    BigDecimal pay=TaiChinhService.tien(amount.getText());
                    BigDecimal balance=k.conNo().subtract(pay);
                    after.setText("Còn lại sau thanh toán: "+TaiChinhService.hienTien(balance)+"đ");
                    after.setForeground(balance.signum()<0?new Color(185,41,38):TaiChinhUi.XANH_DAM);
                }catch(Exception ex){after.setText("Nhập số tiền hợp lệ để xem số dư mới.");}
                break;
            }
        };
        amount.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){update.run();}
            public void removeUpdate(DocumentEvent e){update.run();}
            public void changedUpdate(DocumentEvent e){update.run();}
        });
        cbKhoan.addActionListener(e->SwingUtilities.invokeLater(update));update.run();
        // Kết quả dự kiến thay đổi trực tiếp khi nhập tiền.
        JPanel choose=new JPanel(new BorderLayout(0,6));choose.setOpaque(false);
        JLabel chooseTitle=new JLabel("Khoản phải thu (*)");
        chooseTitle.setForeground(TaiChinhUi.CHU);chooseTitle.setFont(TaiChinhUi.font(Font.BOLD,12));
        TaiChinhUi.field(cbKhoan);
        choose.add(chooseTitle,BorderLayout.NORTH);
        choose.add(cbKhoan,BorderLayout.CENTER);
        choose.add(remaining,BorderLayout.SOUTH);
        JPanel estimate=new JPanel(new BorderLayout());estimate.setOpaque(true);
        estimate.setBackground(new Color(235,249,239));
        estimate.setBorder(BorderFactory.createEmptyBorder(10,13,10,13));
        estimate.add(after,BorderLayout.CENTER);
        JPanel form=TaiChinhUi.doc(
            TaiChinhUi.chuThich("Có thể thanh toán một phần hoặc toàn bộ số dư. Mỗi lần thu sẽ tạo một phiếu riêng.",
                new Color(240,249,243)),
            TaiChinhUi.nhom("Chọn khoản cần thu",choose),
            TaiChinhUi.nhom("Thông tin thanh toán",TaiChinhUi.formHaiCot(
                "Số tiền thực thu (*)",amount,"Hình thức thanh toán",cbHinhThuc,
                "Tài khoản thực hiện (*)",cbTaiKhoan,"Ghi chú",note)),estimate);
        if(!TaiChinhUi.hopThoaiForm(this,"Lập phiếu thu – "+coSo.ten(),
            "Ghi nhận số tiền thực thu, cho phép thanh toán nhiều lần",form,"Tiếp tục",()->{
                TaiChinhDTO.LuaChon selected=(TaiChinhDTO.LuaChon)cbKhoan.getSelectedItem();
                if(selected==null||cbTaiKhoan.getSelectedItem()==null)
                    throw new IllegalArgumentException("Chọn khoản thu và tài khoản thực hiện.");
                BigDecimal money=TaiChinhService.tien(amount.getText());
                if(money.signum()<=0)throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
                for(TaiChinhDTO.KhoanThu k:debt)if(k.maKhoan()==selected.id()&&money.compareTo(k.conNo())>0)
                    throw new IllegalArgumentException("Không được thu vượt công nợ: "+TaiChinhService.hienTien(k.conNo()));
            }))return;
        try{
            TaiChinhDTO.LuaChon selected=(TaiChinhDTO.LuaChon)cbKhoan.getSelectedItem();
            TaiChinhDTO.LuaChon actor=(TaiChinhDTO.LuaChon)cbTaiKhoan.getSelectedItem();
            BigDecimal money=TaiChinhService.tien(amount.getText());
            String method=switch(cbHinhThuc.getSelectedIndex()){
                case 0->"TIEN_MAT";case 1->"CHUYEN_KHOAN";default->"KHAC";
            };
            if(!TaiChinhUi.xacNhan(this,"Xác nhận thu "+TaiChinhService.hienTien(money)
                +" cho khoản #"+selected.id()+" tại "+coSo.ten()+"?"))return;
            TaiChinhUi.ghi(this,()->{
                ctrl.thu(coSo,selected.id(),money,method,actor.id(),note.getText().trim());
                return null;
            },this::load);
        }catch(Exception ex){TaiChinhUi.loi(this,ex);}
    }
}
