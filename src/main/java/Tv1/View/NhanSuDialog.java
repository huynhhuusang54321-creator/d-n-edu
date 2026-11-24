package Tv1.View;

import MainPage.View.Workspace;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.io.File;

/** Form dùng chung cho thêm/sửa; không gọi Service. */
public class NhanSuDialog extends JDialog {
    public final JTextField name=new JTextField(22), phone=new JTextField(22), email=new JTextField(22), username=new JTextField(22), image=new JTextField(22);
    public final JTextArea address=new JTextArea(3,22);
    public final JComboBox<String> gender=new JComboBox<>(new String[]{"Chưa chọn","Nam","Nữ","Khác"}), role=new JComboBox<>(new String[]{"Quản trị viên","Tổ trưởng","Giáo viên","Nhân viên"});
    public final JCheckBox active=new JCheckBox("Đang hoạt động",true);
    private final JTextField birth=new JTextField(12);
    private final JButton calendar=Workspace.button("▦ Chọn ngày");
    private static final DateTimeFormatter DATE_FORMAT=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public final JButton save=Workspace.button("Lưu nhân sự"), cancel=Workspace.button("Hủy bỏ");
    private final JLabel preview=new JLabel("Chưa có ảnh",SwingConstants.CENTER);
    public NhanSuDialog(Component owner,boolean editing,String branch){
        super(SwingUtilities.getWindowAncestor(owner),editing?"Cập nhật nhân sự":"Thêm nhân sự",ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); JPanel root=new JPanel(new BorderLayout(0,8));root.setBackground(Workspace.BG);root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));setContentPane(root);
        JLabel title=new JLabel(editing?"CẬP NHẬT NHÂN SỰ":"THÊM NHÂN SỰ");title.setOpaque(true);title.setBackground(Workspace.GREEN);title.setFont(new Font("Segoe UI",Font.BOLD,22));title.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));root.add(title,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridBagLayout());fields.setBackground(Workspace.BG);int row=0;
        preview.setPreferredSize(new Dimension(90,90)); JPanel img=new JPanel(new BorderLayout(6,4));img.setOpaque(false);img.add(preview,BorderLayout.WEST); JPanel pick=new JPanel(new FlowLayout(FlowLayout.LEFT));pick.setOpaque(false);JButton choose=Workspace.button("Chọn ảnh"), clear=Workspace.button("Bỏ ảnh");pick.add(choose);pick.add(clear);img.add(pick,BorderLayout.CENTER);image.setEditable(false);img.add(image,BorderLayout.SOUTH);
        choose.addActionListener(e->{JFileChooser fc=new JFileChooser();fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ảnh JPG, PNG, GIF","jpg","jpeg","png","gif"));if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){image.setText(fc.getSelectedFile().getAbsolutePath());updatePreview();}});clear.addActionListener(e->{image.setText("");updatePreview();});
        field(fields,row++,"Ảnh đại diện",img);field(fields,row++,"Họ và tên *",name);
        birth.setToolTipText("Nhập dd/MM/yyyy hoặc chọn lịch; để trống nếu chưa có ngày sinh");
        calendar.addActionListener(e->showCalendar());
        JPanel dates=new JPanel(new BorderLayout(6,0));dates.setOpaque(false);dates.add(birth,BorderLayout.CENTER);dates.add(calendar,BorderLayout.EAST);field(fields,row++,"Ngày sinh",dates);
        field(fields,row++,"Giới tính",gender);field(fields,row++,"Số điện thoại",phone);field(fields,row++,"Email",email);address.setLineWrap(true);address.setWrapStyleWord(true);field(fields,row++,"Địa chỉ",new JScrollPane(address));field(fields,row++,"Vai trò *",role);field(fields,row++,"Tên đăng nhập *",username);field(fields,row++,"Cơ sở",new JLabel(branch));field(fields,row++,"Trạng thái",active);field(fields,row++,"Quyền đăng nhập",new JLabel("Quản lý tài khoản: chờ API Admin"));
        role.setSelectedIndex(3); // Mặc định nhân viên, không mặc định quản trị viên.
        JScrollPane scroll=new JScrollPane(fields);scroll.setBorder(null);root.add(scroll,BorderLayout.CENTER);JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT));bottom.setOpaque(false);bottom.add(cancel);bottom.add(save);root.add(bottom,BorderLayout.SOUTH);cancel.addActionListener(e->dispose());getRootPane().setDefaultButton(save);setSize(680,650);setMinimumSize(new Dimension(440,360));setLocationRelativeTo(owner);
    }
    private static void field(JPanel p,int row,String label,Component c){GridBagConstraints g=new GridBagConstraints();g.gridy=row;g.gridx=0;g.anchor=GridBagConstraints.NORTHWEST;g.insets=new Insets(5,8,5,8);p.add(new JLabel(label),g);g.gridx=1;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;p.add(c,g);}
    public void setBirth(LocalDate date){birth.setText(date==null?"":DATE_FORMAT.format(date));}
    public LocalDate birthValue() throws java.text.ParseException {return parseBirth(birth.getText());}
    static LocalDate parseBirth(String text) throws java.text.ParseException {
        String value=text.trim();if(value.isEmpty())return null;
        try {LocalDate date=LocalDate.parse(value,DATE_FORMAT);if(date.getYear()<1||date.isAfter(LocalDate.now()))throw new DateTimeParseException("Ngày sinh ngoài phạm vi",value,0);return date;}
        catch(DateTimeParseException ex){throw new java.text.ParseException("Ngày sinh không hợp lệ (dd/MM/yyyy)",0);}
    }
    private void showCalendar(){
        LocalDate today=LocalDate.now(), initial=today;
        try{LocalDate current=birthValue();if(current!=null)initial=current;}catch(java.text.ParseException ignored){}
        JPopupMenu popup=new JPopupMenu();JPanel panel=new JPanel(new BorderLayout(4,6));panel.setBackground(Workspace.BG);panel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JComboBox<Integer> month=new JComboBox<>(), year=new JComboBox<>();for(int m=1;m<=12;m++)month.addItem(m);for(int y=today.getYear();y>=Math.min(1900,initial.getYear());y--)year.addItem(y);month.setSelectedItem(initial.getMonthValue());year.setSelectedItem(initial.getYear());
        JPanel header=new JPanel(new FlowLayout(FlowLayout.CENTER,4,0));header.setOpaque(false);JButton prev=Workspace.button("‹"),next=Workspace.button("›");header.add(prev);header.add(new JLabel("Tháng"));header.add(month);header.add(new JLabel("Năm"));header.add(year);header.add(next);panel.add(header,BorderLayout.NORTH);
        JPanel days=new JPanel(new GridLayout(7,7,2,2));days.setOpaque(false);panel.add(days,BorderLayout.CENTER);
        final LocalDate selected=initial;
        Runnable render=()->{days.removeAll();YearMonth ym=YearMonth.of((Integer)year.getSelectedItem(),(Integer)month.getSelectedItem());for(String label:new String[]{"T2","T3","T4","T5","T6","T7","CN"})days.add(new JLabel(label,SwingConstants.CENTER));int offset=ym.atDay(1).getDayOfWeek().getValue()-1;for(int i=0;i<42;i++){int d=i-offset+1;if(d<1||d>ym.lengthOfMonth()){days.add(new JLabel());continue;}LocalDate date=ym.atDay(d);JButton button=new JButton(String.valueOf(d));button.setMargin(new Insets(2,3,2,3));button.setEnabled(!date.isAfter(LocalDate.now()));if(date.equals(selected))button.setBackground(Workspace.GREEN);button.addActionListener(e->{setBirth(date);popup.setVisible(false);});days.add(button);}prev.setEnabled(ym.isAfter(YearMonth.of(year.getItemAt(year.getItemCount()-1),1)));next.setEnabled(ym.isBefore(YearMonth.from(LocalDate.now())));days.revalidate();days.repaint();};
        month.addActionListener(e->render.run());year.addActionListener(e->render.run());
        java.util.function.IntConsumer move=delta->{YearMonth target=YearMonth.of((Integer)year.getSelectedItem(),(Integer)month.getSelectedItem()).plusMonths(delta);if(target.getYear()>=year.getItemAt(year.getItemCount()-1)&&!target.isAfter(YearMonth.from(LocalDate.now()))){year.setSelectedItem(target.getYear());month.setSelectedItem(target.getMonthValue());}};
        prev.addActionListener(e->move.accept(-1));next.addActionListener(e->move.accept(1));JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT));footer.setOpaque(false);JButton clear=Workspace.button("Bỏ trống"),now=Workspace.button("Hôm nay");clear.addActionListener(e->{setBirth(null);popup.setVisible(false);});now.addActionListener(e->{setBirth(LocalDate.now());popup.setVisible(false);});footer.add(clear);footer.add(now);panel.add(footer,BorderLayout.SOUTH);popup.add(panel);render.run();popup.show(calendar,0,calendar.getHeight());
    }
    public void updatePreview(){preview.setIcon(null);preview.setText("Chưa có ảnh");String path=image.getText();if(!path.isBlank()&&new File(path).isFile()){ImageIcon icon=new ImageIcon(path);if(icon.getIconWidth()>0){preview.setIcon(new ImageIcon(icon.getImage().getScaledInstance(90,90,Image.SCALE_SMOOTH)));preview.setText("");}}}
    public void busy(boolean b){save.setEnabled(!b);cancel.setEnabled(!b);birth.setEnabled(!b);calendar.setEnabled(!b);setDefaultCloseOperation(b?DO_NOTHING_ON_CLOSE:DISPOSE_ON_CLOSE);}
    public static void profile(Component owner,String title,String[][] data,String imagePath,Runnable edit){JPanel p=new JPanel(new GridLayout(0,2,12,8));p.setBackground(Workspace.BG);for(String[] pair:data){p.add(new JLabel(pair[0]));JTextArea a=new JTextArea(pair[1]);a.setEditable(false);a.setOpaque(false);a.setLineWrap(true);a.setWrapStyleWord(true);p.add(a);}JPanel content=new JPanel(new BorderLayout(8,8));content.setBackground(Workspace.BG);JLabel photo=new JLabel("Chưa có ảnh",SwingConstants.CENTER);if(imagePath!=null&&!imagePath.isBlank()&&new File(imagePath).isFile()){ImageIcon icon=new ImageIcon(imagePath);if(icon.getIconWidth()>0){photo.setIcon(new ImageIcon(icon.getImage().getScaledInstance(90,90,Image.SCALE_SMOOTH)));photo.setText("");}}content.add(photo,BorderLayout.NORTH);content.add(p,BorderLayout.CENTER);JScrollPane sc=new JScrollPane(content);sc.setPreferredSize(new Dimension(560,420));int result=JOptionPane.showOptionDialog(owner,sc,title,JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[]{"Sửa thông tin","Đóng"},"Đóng");if(result==0)edit.run();}
}
