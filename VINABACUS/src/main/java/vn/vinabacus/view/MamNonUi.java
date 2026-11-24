package vn.vinabacus.view;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Các thành phần giao diện đồng bộ VINABACUS và tác vụ JDBC ngoài EDT. */
public final class MamNonUi {
    public static final Color XANH=new Color(0,106,54), VANG=new Color(255,216,0);
    private MamNonUi(){}
    @FunctionalInterface public interface Doc<T>{T read()throws Exception;}
    @FunctionalInterface public interface Ghi{void run()throws Exception;}
    public static <T> void tai(JComponent parent,Doc<T> work,Consumer<T> done){
        new SwingWorker<T,Void>(){
            @Override protected T doInBackground()throws Exception{return work.read();}
            @Override protected void done(){try{done.accept(get());}
            catch(InterruptedException e){Thread.currentThread().interrupt();loi(parent,e);}
            catch(ExecutionException e){loi(parent,e.getCause()==null?e:e.getCause());}}
        }.execute();
    }
    public static void ghi(JComponent parent,Ghi work,Runnable done){
        new SwingWorker<Void,Void>(){
            @Override protected Void doInBackground()throws Exception{work.run();return null;}
            @Override protected void done(){try{get();done.run();}catch(InterruptedException e){Thread.currentThread().interrupt();loi(parent,e);}catch(ExecutionException e){loi(parent,e.getCause()==null?e:e.getCause());}}
        }.execute();
    }
    public static void loi(Component parent,Throwable e){JOptionPane.showMessageDialog(parent,e.getMessage(),"VINABACUS – Thông báo",JOptionPane.ERROR_MESSAGE);}
    public static JButton nut(String text, boolean primary){
        JButton b=new JButton(text);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFocusPainted(false);b.setOpaque(true);b.setContentAreaFilled(true);
        b.setBackground(primary?XANH:Color.WHITE);
        b.setForeground(primary?Color.WHITE:XANH);
        b.setFont(b.getFont().deriveFont(Font.BOLD,13f));
        b.setRolloverEnabled(true);
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(XANH,1),BorderFactory.createEmptyBorder(9,13,9,13)));
        b.getModel().addChangeListener(e->{
            if(!b.isEnabled()){
                b.setBackground(new Color(225,232,227));b.setForeground(new Color(65,85,75));
            }else if(b.getModel().isPressed()){
                b.setBackground(primary?new Color(0,75,38):new Color(210,236,218));
                b.setForeground(primary?Color.WHITE:XANH);
            }else if(b.getModel().isRollover()){
                b.setBackground(primary?new Color(0,88,46):new Color(235,247,238));
                b.setForeground(primary?Color.WHITE:XANH);
            }else{
                b.setBackground(primary?XANH:Color.WHITE);b.setForeground(primary?Color.WHITE:XANH);
            }
        });
        return b;
    }
    /** Đồng bộ chiều cao, giữ chiều rộng đủ đọc toàn bộ nội dung. Chỉ dùng cho thanh công cụ. */
    public static <T extends JComponent> T coCongCu(T c,int width){
        Dimension original=c.getPreferredSize();
        int w=width>0?width:Math.max(original.width,72);
        c.setPreferredSize(new Dimension(w,40));
        c.setMinimumSize(new Dimension(w,40));
        if(c instanceof JButton || c instanceof JTextField || c instanceof JComboBox<?>)
            c.setFont(c.getFont().deriveFont(c instanceof JButton?Font.BOLD:Font.PLAIN,13f));
        return c;
    }
    /** Thanh công cụ tự ngắt dòng khi cửa sổ hẹp, không cắt chữ trên các nút. */
    public static JPanel thanhCongCu(Component... contents){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,5)){
            private int lastWidth=-1;
            @Override public Dimension getPreferredSize(){
                Dimension d=super.getPreferredSize();
                int width=getWidth();
                if(width<=0){Container par=getParent();if(par!=null)width=par.getWidth();}
                if(width<=0)return d;
                Insets in=getInsets();int limit=Math.max(1,width-in.left-in.right-20);
                int row=0,rowH=0,total=10;
                for(Component c:getComponents())if(c.isVisible()){
                    Dimension size=c.getPreferredSize();int extra=row==0?0:10;
                    if(row>0&&row+extra+size.width>limit){total+=rowH+10;row=0;rowH=0;extra=0;}
                    row+=extra+size.width;rowH=Math.max(rowH,size.height);
                }
                return new Dimension(d.width,total+rowH+in.top+in.bottom);
            }
            @Override public void doLayout(){
                super.doLayout();
                if(getWidth()>0&&getWidth()!=lastWidth){
                    lastWidth=getWidth();
                    SwingUtilities.invokeLater(()->{
                        if(!isDisplayable())return;
                        revalidate();Container parent=getParent();if(parent!=null)parent.revalidate();
                    });
                }
            }
        };
        for(Component c:contents)p.add(c);
        return p;
    }
    /** Hộp thoại xác nhận riêng: hai nút dùng cùng bộ màu rõ chữ như thanh công cụ TV4. */
    public static boolean xacNhan(Component parent, Object content,String title,boolean warning){
        Window owner=SwingUtilities.getWindowAncestor(parent);
        JDialog dialog=new JDialog(owner,title,Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(12,12));
        root.setBorder(BorderFactory.createEmptyBorder(18,20,16,20));
        JComponent body;
        if(content instanceof JComponent jc)body=jc;
        else {
            JTextArea words=new JTextArea(String.valueOf(content));words.setEditable(false);words.setOpaque(false);
            words.setLineWrap(true);words.setWrapStyleWord(true);
            words.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));words.setColumns(42);
            body=words;
        }
        root.add(body,BorderLayout.CENTER);
        JButton cancel=nut("Đóng",false),ok=nut(warning?"Xác nhận thao tác":"Xác nhận",true);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.add(cancel);actions.add(ok);
        root.add(actions,BorderLayout.SOUTH);
        final boolean[] accepted={false};
        cancel.addActionListener(e->dialog.dispose());
        ok.addActionListener(e->{accepted[0]=true;dialog.dispose();});
        dialog.setContentPane(root);dialog.pack();
        dialog.setMinimumSize(new Dimension(Math.max(450,dialog.getWidth()),dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);dialog.getRootPane().setDefaultButton(ok);
        dialog.getRootPane().registerKeyboardAction(e->dialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setVisible(true);
        return accepted[0];
    }
    public static JLabel tieuDe(String text){JLabel t=new JLabel(text);t.setForeground(XANH);t.setFont(t.getFont().deriveFont(Font.BOLD,21f));return t;}
    public static JPanel dong(Component... contents){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,9,7));for(Component c:contents)p.add(c);return p;}
    public static JPanel vung(String title){JPanel p=new JPanel(new BorderLayout(8,8));p.setBorder(BorderFactory.createTitledBorder(title));return p;}
    public static JPanel form(Object... pairs){
        JPanel p=new JPanel(new GridLayout(pairs.length/2,2,10,9));p.setBorder(BorderFactory.createEmptyBorder(12,15,12,15));
        for(int i=0;i<pairs.length;i+=2){p.add(new JLabel(String.valueOf(pairs[i])));p.add((Component)pairs[i+1]);}return p;
    }
    public static LocalDate ngay(String raw)throws DateTimeParseException{return LocalDate.parse(raw.trim());}
    public static LocalDate ngayNull(String raw){return raw==null||raw.trim().isEmpty()?null:ngay(raw);}
    public static DefaultTableModel model(String... columns){return new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int r,int c){return false;}};}
    public static JTable bang(DefaultTableModel model){JTable t=new JTable(model);t.setRowHeight(29);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.getTableHeader().setBackground(new Color(219,241,224));return t;}
    public static String ngay(LocalDate d){return d==null?"—":d.toString();}
    public static <T> void chon(JComboBox<T> cb,List<T> list,T first){
        cb.removeAllItems();if(first!=null)cb.addItem(first);for(T e:list)cb.addItem(e);
    }
}
