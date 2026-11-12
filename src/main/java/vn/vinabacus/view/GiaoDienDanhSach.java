package vn.vinabacus.view;

import vn.vinabacus.view.*;
import vn.vinabacus.model.*;
import vn.vinabacus.controller.DieuKhienTrangChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.regex.Pattern;

/** Trang giao diện xem trước cho các menu chưa phát triển nghiệp vụ. Tìm kiếm trên dữ liệu mẫu hoạt động thật. */
public final class GiaoDienDanhSach extends JPanel {
    public GiaoDienDanhSach(CoSo coSo, String ten, GiaoDienChinh cha) {
        setLayout(new BorderLayout());setBackground(MauSacUngDung.NEN);
        JPanel container=new JPanel();container.setOpaque(false);
        container.setLayout(new BoxLayout(container,BoxLayout.Y_AXIS));
        container.setBorder(new EmptyBorder(25,27,30,27));
        add(container,BorderLayout.NORTH);

        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        JLabel name=ThanhPhanGiaoDien.nhan(ten,27,true,MauSacUngDung.CHU);
        header.add(name,BorderLayout.WEST);
        JLabel badge=ThanhPhanGiaoDien.nhan("BẢN XEM TRƯỚC – CHƯA KẾT NỐI CSDL",11,true,MauSacUngDung.XANH_DAM);
        header.add(badge,BorderLayout.EAST);
        container.add(header);container.add(Box.createVerticalStrut(9));
        DuLieuMau.Bang data=new DieuKhienTrangChu(coSo).layBang(ten);
        JLabel sub=ThanhPhanGiaoDien.nhan(data.moTa(),14,false,MauSacUngDung.PHU);
        container.add(sub);container.add(Box.createVerticalStrut(20));

        ThanhPhanGiaoDien.TheTron card=new ThanhPhanGiaoDien.TheTron(Color.WHITE,MauSacUngDung.VIEN,20);
        card.setLayout(new BorderLayout(0,16));card.setBorder(new EmptyBorder(24,24,24,24));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,700));
        JPanel top=new JPanel(new BorderLayout(15,0));top.setOpaque(false);
        JPanel search=new JPanel(new FlowLayout(FlowLayout.LEFT,9,0));search.setOpaque(false);
        JLabel searchIcon=new JLabel(new BieuTuong("hocvien",MauSacUngDung.XANH,22));search.add(searchIcon);
        JTextField searchField=new JTextField(27);searchField.setFont(MauSacUngDung.font(14,false));
        searchField.setToolTipText("Nhập để tìm trong bảng dữ liệu mẫu");
        searchField.setPreferredSize(new Dimension(250,39));
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MauSacUngDung.VIEN,1,true),
                new EmptyBorder(8,10,8,10)));
        search.add(searchField);top.add(search,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);
        ThanhPhanGiaoDien.NutTron xem=new ThanhPhanGiaoDien.NutTron("Xem chi tiết",new Color(0xDFF8E2),MauSacUngDung.XANH_DAM,14);
        ThanhPhanGiaoDien.NutTron them=new ThanhPhanGiaoDien.NutTron("+  Thêm mới",new Color(0x008D2C),Color.WHITE,14);
        actions.add(xem);actions.add(them);top.add(actions,BorderLayout.EAST);card.add(top,BorderLayout.NORTH);

        DefaultTableModel model=new DefaultTableModel(data.dong(),data.cot()) {
            @Override public boolean isCellEditable(int row,int col){return false;}
        };
        JTable table=new JTable(model);table.setFont(MauSacUngDung.font(13,false));
        table.setRowHeight(44);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEDF0EC));
        table.setSelectionBackground(new Color(0xE1F9DF));table.setSelectionForeground(MauSacUngDung.CHU);
        table.setIntercellSpacing(new Dimension(0,0));
        JTableHeader th=table.getTableHeader();th.setBackground(new Color(0xEDF6ED));
        th.setForeground(MauSacUngDung.XANH_DAM);th.setPreferredSize(new Dimension(0,46));
        th.setFont(MauSacUngDung.font(13,true));
        DefaultTableCellRenderer cells=new DefaultTableCellRenderer();cells.setBorder(new EmptyBorder(0,13,0,13));
        for(int i=0;i<table.getColumnCount();i++){table.getColumnModel().getColumn(i).setCellRenderer(cells);}
        TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);table.setRowSorter(sorter);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            private void tim(){String q=searchField.getText().trim();sorter.setRowFilter(q.isEmpty()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(q)));}
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e){tim();}
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e){tim();}
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e){tim();}
        });
        xem.addActionListener(e->{
            int row=table.getSelectedRow();
            if(row<0){JOptionPane.showMessageDialog(cha,"Vui lòng chọn một dòng để xem chi tiết.","Thông báo",JOptionPane.INFORMATION_MESSAGE);return;}
            StringBuilder sb=new StringBuilder("THÔNG TIN DỮ LIỆU MẪU\n\n");
            for(int i=0;i<table.getColumnCount();i++)sb.append(table.getColumnName(i)).append(":  ").append(table.getValueAt(row,i)).append("\n");
            sb.append("\nNội dung thực tế sẽ lấy từ SQL Server ở giai đoạn sau.");
            JOptionPane.showMessageDialog(cha,sb.toString(),"Xem chi tiết – "+ten,JOptionPane.INFORMATION_MESSAGE);
        });
        them.addActionListener(e->cha.hienChuaPhatTrien("Thêm mới – "+ten));
        JPanel tableBox=new JPanel(new BorderLayout());tableBox.setOpaque(false);
        JScrollPane scroll=new JScrollPane(table);scroll.setPreferredSize(new Dimension(800,330));
        scroll.setBorder(BorderFactory.createLineBorder(MauSacUngDung.VIEN));
        tableBox.add(scroll,BorderLayout.CENTER);card.add(tableBox,BorderLayout.CENTER);
        container.add(card);container.add(Box.createVerticalStrut(15));
        JLabel note=ThanhPhanGiaoDien.nhan("Tìm kiếm, chọn dòng và nút Xem chi tiết đã hoạt động trên dữ liệu mẫu. Thêm/Sửa/Xóa/Thu tiền/Điểm danh thật sẽ làm sau.",12,false,MauSacUngDung.PHU);
        container.add(note);
    }
}
