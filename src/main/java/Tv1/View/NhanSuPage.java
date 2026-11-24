package Tv1.View;

import MainPage.View.Workspace;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** View thụ động: chỉ hiển thị và phát sự kiện. */
public class NhanSuPage extends Workspace {
    public final JTextField search = new JTextField(18);
    public final JComboBox<String> role = new JComboBox<>(new String[]{"Tất cả vai trò","Quản trị viên","Tổ trưởng","Giáo viên","Nhân viên"});
    public final JComboBox<String> state = new JComboBox<>(new String[]{"Tất cả trạng thái","Đang hoạt động","Ngừng hoạt động"});
    public final JButton find = button("Tìm kiếm"), refresh = button("Làm mới"), add = button("+ Thêm nhân sự"), edit = button("Sửa thông tin"), detail = button("Xem hồ sơ"), stop = button("Ngừng hoạt động"), restore = button("Khôi phục"), remove = button("Xóa nhân sự");
    private final JLabel[] totals = new JLabel[4];
    public NhanSuPage() {
        super();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT,8,5)); filters.setOpaque(false);
        filters.add(new JLabel("Tìm kiếm:")); filters.add(search); filters.add(role); filters.add(state); filters.add(find); filters.add(refresh);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,5)); actions.setOpaque(false);
        for (JButton b : new JButton[]{add,edit,detail,stop,restore,remove}) actions.add(b);
        remove.setBackground(Color.decode("#F4DEDA"));
        JPanel stats = new JPanel(new GridLayout(1,4,8,0)); stats.setOpaque(false);
        String[] labels={"Tổng nhân sự","Giáo viên","Nhân viên","Đang hoạt động"};
        for(int i=0;i<4;i++) { JPanel p=new JPanel(new BorderLayout(0,4)); p.setBackground(Color.WHITE); p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),BorderFactory.createEmptyBorder(8,10,8,10))); p.add(new JLabel(labels[i]),BorderLayout.NORTH); totals[i]=new JLabel("—"); totals[i].setFont(new Font("Segoe UI",Font.BOLD,23)); p.add(totals[i],BorderLayout.CENTER); stats.add(p); }
        tools.setLayout(new BorderLayout(0,4)); tools.add(stats,BorderLayout.NORTH); tools.add(filters,BorderLayout.CENTER); tools.add(actions,BorderLayout.SOUTH);
        // Thanh công cụ cuộn ngang khi cửa sổ hẹp.
        JPanel top=(JPanel)tools.getParent(); top.remove(tools); JScrollPane sc=new JScrollPane(tools,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); sc.setBorder(null); top.add(sc,BorderLayout.CENTER);
        columns("Mã NS","Họ và tên","Vai trò","Điện thoại","Trạng thái","Đăng nhập");
        int[] widths={75,225,145,125,155,145}; for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        // Co giãn các cột để lấp đầy chiều rộng vùng hiển thị.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoCreateRowSorter(true);
        ((javax.swing.table.TableRowSorter<?>)table.getRowSorter()).setComparator(0, java.util.Comparator.comparingInt(x -> ((Number)x).intValue()));
    }
    public void stats(int total,int teachers,int staff,int active){int[] values={total,teachers,staff,active};for(int i=0;i<4;i++)totals[i].setText(Integer.toString(values[i]));}
    public void clearStats(){for(JLabel l:totals)l.setText("—");}
    public void controls(boolean busy, boolean loaded, Boolean active){
        search.setEnabled(!busy); role.setEnabled(!busy); state.setEnabled(!busy); find.setEnabled(!busy&&loaded); refresh.setEnabled(!busy); add.setEnabled(!busy&&loaded); table.setEnabled(!busy);
        boolean selected=!busy&&loaded&&active!=null; edit.setEnabled(selected); detail.setEnabled(selected); remove.setEnabled(selected); stop.setEnabled(selected&&Boolean.TRUE.equals(active)); restore.setEnabled(selected&&Boolean.FALSE.equals(active));
    }
    public void onSelection(Runnable r){table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())r.run();});}
    public void onOpen(Runnable r){table.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&SwingUtilities.isLeftMouseButton(e)&&table.rowAtPoint(e.getPoint())>=0)r.run();}});}
}
