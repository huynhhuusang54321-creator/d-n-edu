package vn.vinabacus.view;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import vn.vinabacus.controller.TaiChinhController;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.TaiChinhDTO;
import vn.vinabacus.service.TaiChinhService;
import vn.vinabacus.repository.TaiChinhDangKyRepository;

/** Phiếu thu TV5: giao diện thống nhất với Quản lý thu phí, có kiểm tra khi bấm Lưu. */
public final class PhieuThuTaiChinhPanel extends JPanel {
    private final CoSo coSo;
    private final TaiChinhController ctrl = new TaiChinhController();
    private final JTextField thang = new JTextField(8), search = new JTextField(20);
    private final JTextField tuNgay=new JTextField(9),denNgay=new JTextField(9);
    private final JComboBox<String> filterMethod=new JComboBox<>(new String[]{
        "Tất cả hình thức","Tiền mặt","Chuyển khoản","Khác"});
    private final JComboBox<String> filterStatus=new JComboBox<>(new String[]{
        "Tất cả trạng thái","Đã xác nhận","Đã hủy"});
    private final JLabel status = new JLabel("Đang chờ dữ liệu...");
    private final JLabel count = new JLabel("0 phiếu"), total = new JLabel("0đ");
    private JButton add,addMulti;
    private boolean dangLuuPhieu;
    private final DefaultTableModel model = TaiChinhUi.model("Mã phiếu", "Số phiếu", "Học viên",
        "Số tiền", "Ngày thu", "Hình thức", "Trạng thái", "Nội dung", "Tài khoản thu", "Ghi chú");
    private final JTable table = TaiChinhUi.bang(model);
    private List<TaiChinhDTO.KhoanThu> khoan = List.of();
    private List<TaiChinhDTO.HocPhiTrungTam> hocPhi=List.of();
    private List<TaiChinhDTO.LuaChon> taiKhoan = List.of();
    private List<TaiChinhDTO.PhieuThu> all = List.of(), shown = List.of();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PhieuThuTaiChinhPanel(CoSo coSo) {
        super(new BorderLayout(0,10));this.coSo=coSo;
        setBackground(TaiChinhUi.NEN);
        setBorder(BorderFactory.createEmptyBorder(12,8,8,8));
        thang.setText(YearMonth.now().toString());
        TaiChinhUi.field(search);
        add=TaiChinhUi.nut("+ Lập phiếu thu",true);
        addMulti=TaiChinhUi.nut("Thu nhiều khóa",false);
        addMulti.setVisible(coSo==CoSo.TRUNG_TAM);
        JButton refresh=TaiChinhUi.nut("Làm mới",false);
        JPanel top=new JPanel(new BorderLayout(0,10));top.setOpaque(false);
        JPanel titleRow=new JPanel(new BorderLayout());titleRow.setOpaque(false);
        JLabel title=TaiChinhUi.title("PHIẾU THU – "+coSo.ten().toUpperCase());
        title.setFont(TaiChinhUi.font(Font.BOLD,17));titleRow.add(title,BorderLayout.WEST);
        JPanel actions=TaiChinhUi.hang(addMulti,add);
        titleRow.add(actions,BorderLayout.EAST);
        top.add(titleRow,BorderLayout.NORTH);
        JPanel filter=new JPanel(new BorderLayout(10,0));filter.setOpaque(false);
        JPanel left=new JPanel(new BorderLayout(6,0));left.setOpaque(false);
        left.add(new JLabel("Tìm phiếu thu:"),BorderLayout.WEST);
        left.add(search,BorderLayout.CENTER);
        filter.add(left,BorderLayout.CENTER);
        filter.add(TaiChinhUi.hang(new JLabel("Tháng:"),TaiChinhUi.oThang(thang,this),refresh),BorderLayout.EAST);
        tuNgay.setToolTipText("yyyy-MM-dd; để trống cả hai ô để dùng tháng");
        denNgay.setToolTipText("yyyy-MM-dd; ngày kết thúc được tính trong báo cáo");
        filter.add(TaiChinhUi.hang(new JLabel("Từ ngày:"),tuNgay,
            new JLabel("Đến ngày:"),denNgay,new JLabel("Hình thức:"),filterMethod,
            new JLabel("Trạng thái:"),filterStatus),BorderLayout.SOUTH);
        top.add(filter,BorderLayout.CENTER);
        JPanel overview=TaiChinhUi.card(new Color(235,249,239),TaiChinhUi.VIEN,14);
        overview.setLayout(new BorderLayout());overview.setBorder(BorderFactory.createEmptyBorder(9,16,9,16));
        count.setFont(TaiChinhUi.font(Font.BOLD,13));count.setForeground(TaiChinhUi.XANH_DAM);
        total.setFont(TaiChinhUi.font(Font.BOLD,16));total.setForeground(TaiChinhUi.XANH);
        overview.add(count,BorderLayout.WEST);overview.add(total,BorderLayout.EAST);
        top.add(overview,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths={85,220,180,120,150,115,100,300,140,300};
        for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        DefaultTableCellRenderer chuThich=new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,
                    boolean focus,int row,int col){
                super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                setToolTipText(value==null?null:value.toString());
                return this;
            }
        };
        table.getColumnModel().getColumn(7).setCellRenderer(chuThich);
        table.getColumnModel().getColumn(9).setCellRenderer(chuThich);
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
        JButton print=TaiChinhUi.nut("Xem trước / In",false);
        print.addActionListener(e->inPhieu());
        JButton cancel=TaiChinhUi.nut("Hủy phiếu đã chọn",false);
        cancel.addActionListener(e->huyPhieu());
        footer.add(detail);footer.add(print);footer.add(cancel);add(footer,BorderLayout.SOUTH);
        refresh.addActionListener(e->load());
        thang.addActionListener(e->load());
        tuNgay.addActionListener(e->load());denNgay.addActionListener(e->load());
        add.addActionListener(e->taoPhieu());
        addMulti.addActionListener(e->thuNhieuKhoa());
        filterMethod.addActionListener(e->filter());
        filterStatus.addActionListener(e->filter());
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
        List<TaiChinhDTO.KhoanThu> khoan,List<TaiChinhDTO.LuaChon> accounts,
        List<TaiChinhDTO.HocPhiTrungTam> hocPhi){}
    private void loadInternal(Integer maKhoanCanThu){
        final YearMonth month;
        final LocalDate from,to;
        try{
            boolean range=!tuNgay.getText().isBlank()||!denNgay.getText().isBlank();
            if(range){
                if(tuNgay.getText().isBlank()||denNgay.getText().isBlank())
                    throw new IllegalArgumentException("Nhập đủ từ ngày và đến ngày.");
                from=LocalDate.parse(tuNgay.getText().trim());
                to=LocalDate.parse(denNgay.getText().trim()).plusDays(1);
                if(!from.isBefore(to))throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
                month=null;
            }else{from=null;to=null;month=ky();}
        }catch(Exception ex){TaiChinhUi.loi(this,ex);return;}
        status.setText("Đang tải phiếu thu – "+coSo.ten()+"...");
        TaiChinhUi.tai(this,()->new Data(month==null?ctrl.phieu(coSo,from,to):ctrl.phieu(coSo,month),
                ctrl.khoan(coSo),ctrl.taiKhoan(coSo),coSo==CoSo.TRUNG_TAM
                    ?ctrl.hocPhiTrungTam():List.of()),result->{
            khoan=result.khoan();taiKhoan=result.accounts();hocPhi=result.hocPhi();
            all=result.receipts();filter();
            status.setText("Đã tải "+all.size()+" phiếu theo kỳ lọc – "+coSo.ten());
            if(maKhoanCanThu!=null)taoPhieu(maKhoanCanThu);
        });
    }
    private void filter(){
        String q=search.getText().toLowerCase(Locale.ROOT).trim();
        List<TaiChinhDTO.PhieuThu> filtered=new ArrayList<>();
        BigDecimal amount=BigDecimal.ZERO;
        model.setRowCount(0);
        for(TaiChinhDTO.PhieuThu p:all){
            String haystack=(p.soPhieu()+" "+p.hocVien()+" "+p.maPhieu()+" "
                +p.noiDung()+" "+p.taiKhoanThu()+" "+p.ghiChu()).toLowerCase(Locale.ROOT);
            if(!haystack.contains(q))continue;
            if(filterMethod.getSelectedIndex()>0
                    &&!tenHinhThuc(p.hinhThuc()).equals(filterMethod.getSelectedItem()))continue;
            if(filterStatus.getSelectedIndex()==1&& !"XAC_NHAN".equals(p.trangThai()))continue;
            if(filterStatus.getSelectedIndex()==2&& !"HUY".equals(p.trangThai()))continue;
            filtered.add(p);
            model.addRow(new Object[]{p.maPhieu(),p.soPhieu(),p.hocVien(),
                TaiChinhService.hienTien(p.soTien()),p.ngayThu()==null?"—":p.ngayThu().format(DATE),
                tenHinhThuc(p.hinhThuc()),"XAC_NHAN".equals(p.trangThai())?"Đã xác nhận":p.trangThai(),
                hienThi(p.noiDung()),hienThi(p.taiKhoanThu()),hienThi(p.ghiChu())});
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
    private static String hienThi(String value){return value==null||value.isBlank()?"—":value;}
    private String tenKhoan(TaiChinhDTO.KhoanThu k){
        if(k.maDangKy()!=null)for(TaiChinhDTO.HocPhiTrungTam h:hocPhi)
            if(h.maDangKy()==k.maDangKy())return h.khoaHoc()+" / "+h.monHoc();
        return k.tenLoai();
    }
    private void xemChiTiet(){
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Hãy chọn phiếu thu cần xem.");return;}
        int modelRow=table.convertRowIndexToModel(row);
        if(modelRow<0||modelRow>=shown.size())return;
        TaiChinhDTO.PhieuThu p=shown.get(modelRow);
        TaiChinhUi.tai(this,()->ctrl.phanBoPhieu(coSo,p.maPhieu()),phanBo->{
        DefaultTableModel allocations=TaiChinhUi.model("Mã khoản","Nội dung","Số tiền phân bổ","Loại");
        for(TaiChinhDTO.PhanBoPhieu item:phanBo)allocations.addRow(new Object[]{
            item.maKhoan(),item.noiDung(),TaiChinhService.hienTien(item.soTien())+"đ",
            item.loaiBienDong()});
        JTable bang=TaiChinhUi.bang(allocations);
        JScrollPane cuon=TaiChinhUi.scroll(bang);
        cuon.setPreferredSize(new Dimension(620,Math.min(210,80+phanBo.size()*27)));
        JPanel details=TaiChinhUi.doc(
            TaiChinhUi.theTien(new String[]{"Số tiền thực thu"},
                new String[]{TaiChinhService.hienTien(p.soTien())+"đ"},new Color[]{TaiChinhUi.XANH}),
            TaiChinhUi.nhom("Thông tin giao dịch",TaiChinhUi.thongTinHaiCot(
                "Số phiếu",p.soPhieu(),"Học viên",p.hocVien(),
                "Ngày thu",p.ngayThu()==null?"—":p.ngayThu().format(DATE),
                "Hình thức",tenHinhThuc(p.hinhThuc()),
                "Nội dung",hienThi(p.noiDung()),
                "Tài khoản thu",hienThi(p.taiKhoanThu()),
                "Ghi chú",hienThi(p.ghiChu()),
                "Trạng thái","XAC_NHAN".equals(p.trangThai())?"Đã xác nhận":p.trangThai(),
                "Ngày hủy",p.ngayHuy()==null?"—":p.ngayHuy().format(DATE),
                "Lý do hủy",hienThi(p.lyDoHuy()),
                "Mã phiếu",String.valueOf(p.maPhieu()))),
            TaiChinhUi.nhom("Phân bổ đã lưu",cuon));
        TaiChinhUi.xemThongTin(this,"Chi tiết phiếu thu #"+p.maPhieu(),
            "Thông tin giao dịch đã ghi nhận",details);
        });
    }

    private void inPhieu(){
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Hãy chọn phiếu cần xem trước.");return;}
        TaiChinhDTO.PhieuThu p=shown.get(table.convertRowIndexToModel(row));
        TaiChinhUi.tai(this,()->ctrl.phanBoPhieu(coSo,p.maPhieu()),phanBo->{
            StringBuilder text=new StringBuilder("VINABACUS – PHIẾU THU HỌC PHÍ\n\n")
                .append("Số phiếu: ").append(p.soPhieu()).append('\n')
                .append("Học viên: ").append(p.hocVien()).append('\n')
                .append("Ngày thu: ").append(p.ngayThu().format(DATE)).append('\n')
                .append("Hình thức: ").append(tenHinhThuc(p.hinhThuc())).append('\n')
                .append("Người ghi nhận: ").append(hienThi(p.taiKhoanThu())).append('\n')
                .append("Trạng thái: ").append(p.trangThai()).append("\n\n")
                .append("CÁC KHOẢN ĐÃ PHÂN BỔ\n");
            for(TaiChinhDTO.PhanBoPhieu item:phanBo)
                text.append("#").append(item.maKhoan()).append("  ")
                    .append(item.noiDung()).append("  ")
                    .append(TaiChinhService.hienTien(item.soTien())).append("đ\n");
            text.append("\nTỔNG THU: ").append(TaiChinhService.hienTien(p.soTien()))
                .append("đ\nGhi chú: ").append(hienThi(p.ghiChu())).append('\n');
            if("HUY".equals(p.trangThai()))text.append("ĐÃ HỦY: ")
                .append(hienThi(p.lyDoHuy())).append('\n');
            JTextArea preview=new JTextArea(text.toString(),22,68);
            preview.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            preview.setEditable(false);preview.setLineWrap(true);preview.setWrapStyleWord(true);
            int choice=JOptionPane.showOptionDialog(this,new JScrollPane(preview),
                "Xem trước phiếu "+p.soPhieu(),JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,null,new Object[]{"In phiếu","Đóng"},"Đóng");
            if(choice==0)try{preview.print();}
                catch(java.awt.print.PrinterException ex){TaiChinhUi.loi(this,ex);}
        });
    }

    private void huyPhieu(){
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Hãy chọn phiếu cần hủy.");return;}
        TaiChinhDTO.PhieuThu p=shown.get(table.convertRowIndexToModel(row));
        if(!"XAC_NHAN".equals(p.trangThai())){
            JOptionPane.showMessageDialog(this,"Chỉ hủy phiếu đã xác nhận và chưa hủy.");return;
        }
        if(taiKhoan.isEmpty()){
            JOptionPane.showMessageDialog(this,"Chưa có tài khoản đang hoạt động để xác nhận hủy.");return;
        }
        JComboBox<TaiChinhDTO.LuaChon> actor=new JComboBox<>(taiKhoan.toArray(new TaiChinhDTO.LuaChon[0]));
        JTextField reason=new JTextField(32);
        JPanel form=TaiChinhUi.form("Tài khoản hủy",actor,"Lý do hủy (*)",reason);
        if(!TaiChinhUi.hopThoaiForm(this,"Hủy phiếu #"+p.maPhieu(),
                "Phiếu sẽ giữ nguyên lịch sử; công nợ tăng lại theo phân bổ đã hủy.",
                form,"Tiếp tục",()->{
                    if(reason.getText().isBlank()||reason.getText().trim().length()>500)
                        throw new IllegalArgumentException("Cần lý do hủy từ 1 đến 500 ký tự.");
                }))return;
        if(!TaiChinhUi.xacNhan(this,"Xác nhận hủy phiếu "+p.soPhieu()+"?"))return;
        int maTaiKhoan=((TaiChinhDTO.LuaChon)actor.getSelectedItem()).id();
        TaiChinhUi.ghi(this,()->{ctrl.huyPhieu(coSo,p.maPhieu(),maTaiKhoan,reason.getText());return null;},this::load);
    }

    private record DongNhap(TaiChinhDTO.KhoanThu khoan,JTextField tien) { }
    private static List<TaiChinhDangKyRepository.PhanBo> docPhanBo(List<DongNhap> inputs){
        List<TaiChinhDangKyRepository.PhanBo> result=new ArrayList<>();
        for(DongNhap input:inputs){
            String raw=input.tien().getText().trim();
            if(raw.isEmpty())continue;
            BigDecimal amount=TaiChinhService.tien(raw);
            if(amount.signum()<=0||amount.compareTo(input.khoan().conNo())>0)
                throw new IllegalArgumentException("Số tiền của khoản #"+input.khoan().maKhoan()
                    +" phải > 0 và không vượt số nợ.");
            result.add(new TaiChinhDangKyRepository.PhanBo(input.khoan().maKhoan(),amount));
        }
        if(result.isEmpty())throw new IllegalArgumentException("Hãy nhập tiền cho ít nhất một khóa.");
        return result;
    }
    private void thuNhieuKhoa(){
        if(coSo!=CoSo.TRUNG_TAM||dangLuuPhieu)return;
        Map<Integer,String> students=new TreeMap<>();
        for(TaiChinhDTO.KhoanThu k:khoan)if("HOC_PHI_KHOA".equals(k.loai())
                &&k.conNo().signum()>0)students.put(k.maHocVien(),k.hocVien());
        if(students.isEmpty()||taiKhoan.isEmpty()){
            JOptionPane.showMessageDialog(this,"Cần khoản học phí còn nợ và tài khoản thu hợp lệ.");return;
        }
        JComboBox<TaiChinhDTO.LuaChon> student=new JComboBox<>();
        students.forEach((id,name)->student.addItem(new TaiChinhDTO.LuaChon(id,"#"+id+" | "+name)));
        JComboBox<TaiChinhDTO.LuaChon> actor=new JComboBox<>(taiKhoan.toArray(new TaiChinhDTO.LuaChon[0]));
        JComboBox<String> method=new JComboBox<>(new String[]{"Tiền mặt","Chuyển khoản","Khác"});
        JTextField note=new JTextField(25);
        JPanel rows=new JPanel();rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));
        List<DongNhap> inputs=new ArrayList<>();
        JLabel total=new JLabel("Tổng thu lần này: 0đ");
        Runnable calculate=()->{
            try{
                BigDecimal sum=BigDecimal.ZERO;
                for(DongNhap entry:inputs)if(!entry.tien().getText().isBlank())
                    sum=sum.add(TaiChinhService.tien(entry.tien().getText()));
                total.setText("Tổng thu lần này: "+TaiChinhService.hienTien(sum)+"đ");
            }catch(Exception ex){total.setText("Kiểm tra lại số tiền nhập.");}
        };
        Runnable rebuild=()->{
            rows.removeAll();inputs.clear();
            TaiChinhDTO.LuaChon selected=(TaiChinhDTO.LuaChon)student.getSelectedItem();
            if(selected!=null)for(TaiChinhDTO.KhoanThu k:khoan)if(k.maHocVien()==selected.id()
                    &&"HOC_PHI_KHOA".equals(k.loai())&&k.conNo().signum()>0){
                JTextField field=new JTextField(12);
                field.getDocument().addDocumentListener(new DocumentListener(){
                    public void insertUpdate(DocumentEvent e){calculate.run();}
                    public void removeUpdate(DocumentEvent e){calculate.run();}
                    public void changedUpdate(DocumentEvent e){calculate.run();}
                });
                inputs.add(new DongNhap(k,field));
                JPanel line=TaiChinhUi.hang(new JLabel("Khoản #"+k.maKhoan()+" – "
                    +tenKhoan(k)+" – còn "+TaiChinhService.hienTien(k.conNo())+"đ"),field);
                rows.add(line);
            }
            calculate.run();rows.revalidate();rows.repaint();
        };
        student.addActionListener(e->rebuild.run());rebuild.run();
        JScrollPane scroll=new JScrollPane(rows);
        scroll.setPreferredSize(new Dimension(600,Math.min(220,70+inputs.size()*40)));
        JPanel form=TaiChinhUi.doc(
            TaiChinhUi.chuThich("Nhập riêng số tiền cho từng khoản. Để trống khóa chưa thu; mỗi lần lưu tạo một phiếu.",
                new Color(240,249,243)),
            TaiChinhUi.nhom("Học viên và khóa",TaiChinhUi.doc(
                TaiChinhUi.form("Học viên",student),scroll)),
            TaiChinhUi.nhom("Thanh toán",TaiChinhUi.formHaiCot(
                "Hình thức",method,"Tài khoản thu",actor,"Ghi chú",note,
                "Ngày thu",new JLabel("Thời điểm lưu"))),total);
        if(!TaiChinhUi.hopThoaiForm(this,"Thu học phí nhiều khóa",
                "Phân bổ theo số tiền bạn nhập, không tự chia giữa các khóa.",form,"Tiếp tục",()->{
                    List<TaiChinhDangKyRepository.PhanBo> ds=docPhanBo(inputs);
                    BigDecimal sum=ds.stream().map(TaiChinhDangKyRepository.PhanBo::soTien)
                        .reduce(BigDecimal.ZERO,BigDecimal::add);
                    TaiChinhDangKyRepository.kiemTraPhanBo(sum,ds);
                }))return;
        List<TaiChinhDangKyRepository.PhanBo> ds=docPhanBo(inputs);
        BigDecimal sum=ds.stream().map(TaiChinhDangKyRepository.PhanBo::soTien)
            .reduce(BigDecimal.ZERO,BigDecimal::add);
        int maHocVien=((TaiChinhDTO.LuaChon)student.getSelectedItem()).id();
        int maTaiKhoan=((TaiChinhDTO.LuaChon)actor.getSelectedItem()).id();
        String hinhThuc=switch(method.getSelectedIndex()){
            case 0->"TIEN_MAT";case 1->"CHUYEN_KHOAN";default->"KHAC";
        };
        if(!TaiChinhUi.xacNhan(this,"Xác nhận thu "+TaiChinhService.hienTien(sum)
                +"đ cho học viên #"+maHocVien+" trên "+ds.size()+" khoản?"))return;
        UUID token=UUID.randomUUID();
        dangLuuPhieu=true;add.setEnabled(false);addMulti.setEnabled(false);
        TaiChinhUi.ghi(this,()->ctrl.thuNhieuKhoa(coSo,maHocVien,sum,ds,hinhThuc,
                maTaiKhoan,note.getText().trim(),token),this::load,()->{
                    dangLuuPhieu=false;add.setEnabled(true);addMulti.setEnabled(true);
                });
    }
    private void taoPhieu(){taoPhieu(null);}
    private void taoPhieu(Integer maKhoanUuTien){
        if(dangLuuPhieu)return;
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
                "#"+k.maKhoan()+" | "+k.hocVien()+" | "+tenKhoan(k)+" | Nợ "+TaiChinhService.hienTien(k.conNo())));
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
            String ghiChu=note.getText().trim();
            if(!TaiChinhUi.xacNhan(this,"Xác nhận thu "+TaiChinhService.hienTien(money)
                +" cho khoản #"+selected.id()+" tại "+coSo.ten()+"?"))return;
            dangLuuPhieu=true;
            add.setEnabled(false);
            TaiChinhUi.ghi(this,()->{
                ctrl.thu(coSo,selected.id(),money,method,actor.id(),ghiChu);
                return null;
            },this::load,()->{
                dangLuuPhieu=false;
                add.setEnabled(true);
            });
        }catch(Exception ex){
            dangLuuPhieu=false;
            add.setEnabled(true);
            TaiChinhUi.loi(this,ex);
        }
    }
}
