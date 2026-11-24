package Tv2.View;

import MainPage.View.Workspace;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** View thụ động: hồ sơ, thẻ khóa học, và các sự kiện hiển thị. */
public class HoSoHocVienPage extends Workspace {
    public record FieldItem(String label, String value, boolean marked, Runnable dismiss) {}
    public record CourseItem(String title, String subtitle, String state, String details,
            int percent, String progress, List<FieldItem> fields, Runnable diagram, Runnable assign, Runnable edit) {}
    private final JPanel document = new PortraitDocument();
    private final JPanel identity = new JPanel(new BorderLayout(16,10));
    private final JPanel cards = new JPanel();
    private final JLabel avatar = new JLabel("HV",SwingConstants.CENTER);
    private final JLabel name = new JLabel(), caption = new JLabel();
    private final JPanel fields = new JPanel(new GridLayout(0,2,12,10));
    private final JPanel courseHeader = new JPanel(new BorderLayout());
    private final JLabel courseCount = new JLabel();
    public HoSoHocVienPage(){
        super(); setBorder(new EmptyBorder(6,8,8,8));
        document.setBackground(HocVienPage.BG);document.setBorder(new EmptyBorder(4,4,12,4));
        identity.setBackground(Color.WHITE); identity.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HocVienPage.LINE),new EmptyBorder(16,18,16,18)));
        JPanel personal = new JPanel(new BorderLayout(0,8));personal.setOpaque(false);
        JPanel heading=new JPanel(new BorderLayout(0,4));heading.setOpaque(false);
        name.setFont(new Font("Segoe UI",Font.BOLD,25));name.setHorizontalAlignment(SwingConstants.CENTER);caption.setHorizontalAlignment(SwingConstants.CENTER);name.setForeground(Color.decode("#235D37"));caption.setForeground(Color.decode("#607568"));heading.add(name,BorderLayout.NORTH);heading.add(caption,BorderLayout.SOUTH);
        avatar.setFont(new Font("Segoe UI",Font.BOLD,36));avatar.setOpaque(true);avatar.setBackground(HocVienPage.GREEN);avatar.setForeground(Color.decode("#235D37"));avatar.setPreferredSize(new Dimension(110,125));
        JPanel photo=new JPanel(new BorderLayout());photo.setOpaque(false);JPanel portrait=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));portrait.setOpaque(false);portrait.add(avatar);photo.add(portrait,BorderLayout.NORTH);personal.add(heading,BorderLayout.NORTH);fields.setOpaque(false);personal.add(fields,BorderLayout.CENTER);identity.add(photo,BorderLayout.NORTH);identity.add(personal,BorderLayout.CENTER);
        cards.setLayout(new BoxLayout(cards,BoxLayout.Y_AXIS));cards.setOpaque(false);
        courseHeader.setOpaque(false);JLabel courses=new JLabel("KHÓA HỌC ĐÃ ĐĂNG KÝ");courses.setFont(new Font("Segoe UI",Font.BOLD,17));courseHeader.add(courses,BorderLayout.WEST);courseHeader.add(courseCount,BorderLayout.EAST);
        JPanel lower=new JPanel(new BorderLayout(0,8));lower.setOpaque(false);lower.add(courseHeader,BorderLayout.NORTH);lower.add(cards,BorderLayout.CENTER);
        JPanel stack=new JPanel();stack.setLayout(new BoxLayout(stack,BoxLayout.Y_AXIS));stack.setOpaque(false);identity.setAlignmentX(Component.LEFT_ALIGNMENT);lower.setAlignmentX(Component.LEFT_ALIGNMENT);stack.add(identity);stack.add(Box.createVerticalStrut(18));stack.add(lower);document.add(stack,BorderLayout.NORTH);
        JScrollPane sc=new JScrollPane(document);sc.setBorder(null);sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);sc.getVerticalScrollBar().setUnitIncrement(18);content(sc);
    }
    public void identity(String initials,String fullName,String subtitle,List<FieldItem> data){
        avatar.setText(initials);name.setText(wrap(fullName,480));caption.setText(wrap(subtitle,480));fields.removeAll();
        for(FieldItem f:data) fields.add(fieldPanel(f));
        fields.revalidate();fields.repaint();
    }
    private JPanel fieldPanel(FieldItem f){
        JPanel item=new JPanel(new BorderLayout(0,4));item.setOpaque(false);JPanel labelRow=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));labelRow.setOpaque(false);JLabel label=new JLabel(f.label());label.setForeground(Color.decode("#66776B"));label.setFont(new Font("Segoe UI",Font.PLAIN,12));labelRow.add(label);
        if(f.marked()){JButton dot=new JButton(new DotIcon());dot.setContentAreaFilled(false);dot.setBorder(new EmptyBorder(3,3,3,3));dot.setFocusPainted(false);dot.setToolTipText("Thông tin thiếu hoặc không hợp lệ — nhấn để bỏ đánh dấu");dot.getAccessibleContext().setAccessibleName("Bỏ đánh dấu "+f.label());dot.addActionListener(e->f.dismiss().run());labelRow.add(dot);}
        JTextArea value=new JTextArea(f.value(),Math.max(1,Math.min(12,(f.value().length()+23)/24)),12);value.setEditable(false);value.setOpaque(false);value.setLineWrap(true);value.setWrapStyleWord(true);value.setFont(new Font("Segoe UI",Font.BOLD,14));value.setBorder(new EmptyBorder(0,4,4,0));value.setToolTipText(f.value());item.add(labelRow,BorderLayout.NORTH);item.add(value,BorderLayout.CENTER);return item;
    }
    public void courses(List<CourseItem> items,String emptyText,Runnable assignNew){
        cards.removeAll();courseCount.setText(items.size()+" khóa học");
        if(items.isEmpty()||assignNew!=null){JPanel empty=new JPanel(new FlowLayout(FlowLayout.LEFT,10,8));empty.setBackground(Color.WHITE);empty.setBorder(BorderFactory.createLineBorder(HocVienPage.LINE));empty.add(new JLabel(items.isEmpty()?emptyText:"Học viên chưa có lớp đang học"));if(assignNew!=null)empty.add(actionButton("Xếp lớp",assignNew));empty.setAlignmentX(Component.LEFT_ALIGNMENT);cards.add(empty);}
        for(CourseItem item:items){JPanel card=new JPanel(new BorderLayout(0,12));card.setBackground(Color.WHITE);card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HocVienPage.LINE),new EmptyBorder(12,16,12,16)));card.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel top=new JPanel(new BorderLayout(8,0));top.setOpaque(false);JPanel text=new JPanel(new GridLayout(0,1,0,4));text.setOpaque(false);JLabel course=new JLabel(wrap(item.title(),460));course.setFont(new Font("Segoe UI",Font.BOLD,17));course.setForeground(Color.decode("#235D37"));text.add(course);text.add(new JLabel(wrap(item.subtitle(),460)));top.add(text,BorderLayout.CENTER);
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));actions.setOpaque(false);if(item.diagram()!=null)actions.add(actionButton("Xem sơ đồ",item.diagram()));if(item.assign()!=null)actions.add(actionButton("Xếp lớp",item.assign()));if(item.edit()!=null){JButton more=new JButton("⋯");more.setFont(new Font("Segoe UI",Font.BOLD,22));more.setToolTipText("Chức năng khóa học");more.getAccessibleContext().setAccessibleName("Chức năng khóa học");more.setFocusPainted(false);JPopupMenu pop=new JPopupMenu();JMenuItem edit=new JMenuItem("Sửa thông tin khóa học đã đăng ký");edit.addActionListener(e->item.edit().run());pop.add(edit);more.addActionListener(e->pop.show(more,0,more.getHeight()));actions.add(more);}top.add(actions,BorderLayout.SOUTH);card.add(top,BorderLayout.NORTH);
            JPanel middle=new JPanel(new GridLayout(0,1,0,6));middle.setOpaque(false);middle.add(new JLabel(item.state()));middle.add(new JLabel(wrap(item.details(),460)));JPanel detailGrid=new JPanel(new GridLayout(0,2,8,6));detailGrid.setOpaque(false);for(FieldItem field:item.fields())detailGrid.add(fieldPanel(field));middle.add(detailGrid);JProgressBar progress=new JProgressBar(0,100);progress.setValue(item.percent());progress.setStringPainted(true);progress.setString(item.progress());progress.setForeground(Color.decode("#75B65C"));progress.setBackground(Color.decode("#E7EDE3"));progress.setPreferredSize(new Dimension(250,22));middle.add(progress);card.add(middle,BorderLayout.CENTER);cards.add(card);cards.add(Box.createVerticalStrut(10));
        }cards.revalidate();cards.repaint();
    }
    /** Cửa sổ riêng; View chỉ quản lý kích thước và vòng đời hiển thị. */
    public JDialog createWindow(Component owner,String title){
        Window parent=owner==null?null:SwingUtilities.getWindowAncestor(owner);
        JDialog dialog=new JDialog(parent,title,Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(this);
        Rectangle screen=parent!=null&&parent.getGraphicsConfiguration()!=null
                ?parent.getGraphicsConfiguration().getBounds():GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if(parent!=null&&parent.getGraphicsConfiguration()!=null){Insets in=Toolkit.getDefaultToolkit().getScreenInsets(parent.getGraphicsConfiguration());screen=new Rectangle(screen.x+in.left,screen.y+in.top,screen.width-in.left-in.right,screen.height-in.top-in.bottom);}
        int w=Math.min(680,Math.max(1,screen.width-24)),h=Math.min(920,Math.max(1,screen.height-36));
        dialog.setMinimumSize(new Dimension(Math.min(570,w),Math.min(460,h)));dialog.setSize(w,h);
        dialog.setLocation(screen.x+(screen.width-w)/2,screen.y+(screen.height-h)/2);
        dialog.getRootPane().registerKeyboardAction(e->dialog.dispose(),KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        return dialog;
    }
    private static String wrap(String value,int width){return "<html><div style='width:"+width+"px'>"+value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")+"</div></html>";}
    private static final class PortraitDocument extends JPanel implements Scrollable {
        PortraitDocument(){super(new BorderLayout(12,12));}
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(620,720);}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 18;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(18,r.height-18);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    private static JButton actionButton(String text,Runnable action){JButton b=HocVienPage.button(text,HocVienPage.BUTTON);b.addActionListener(e->action.run());return b;}
    private static class DotIcon implements javax.swing.Icon {public int getIconWidth(){return 9;}public int getIconHeight(){return 9;}public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D a=(Graphics2D)g.create();a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);a.setColor(Color.decode("#D23B3B"));a.fillOval(x,y,9,9);a.dispose();}}
    public record CourseEdit(String note,String end){}
    public CourseEdit editCourse(String label,String note,String end,boolean editableEnd){
        JTextField notes=new JTextField(note,28),date=new JTextField(end,16);date.setEditable(editableEnd);
        int choice=Workspace.form(this,"Sửa đăng ký — "+label,new String[]{"Ghi chú","Ngày kết thúc dự kiến (yyyy-MM-dd)"},new JComponent[]{notes,date},"Lưu","Hủy");return choice==0?new CourseEdit(notes.getText(),date.getText()):null;
    }
}
