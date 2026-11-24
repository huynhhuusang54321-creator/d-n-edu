package MainPage.View;

import java.awt.*;
import java.awt.event.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Thành phần hiển thị dùng chung; không đọc dữ liệu hay thực hiện nghiệp vụ. */
public class Workspace extends JPanel {
    public static final Color BG=Color.decode("#FCFAED"), GREEN=Color.decode("#C1DFA0"), FILTER=Color.decode("#FFF8C4"), BUTTON=Color.decode("#A4D36D"), LINE=Color.decode("#A4CD69");
    public final JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6)), body=new JPanel(new BorderLayout(8,8));
    private final JLabel title=new JLabel(), context=new JLabel(), status=new JLabel();
    private final DefaultTableModel model=new DefaultTableModel(){public boolean isCellEditable(int r,int c){return false;}};
    public final JTable table=new JTable(model);
    public Workspace(){
        super(new BorderLayout(0,10));setBackground(BG);setBorder(new EmptyBorder(12,12,12,12));
        JPanel top=new JPanel(new BorderLayout(0,8)), header=new JPanel(new BorderLayout(0,4));header.setBackground(GREEN);header.setBorder(new EmptyBorder(12,16,12,16));title.setFont(new Font("Segoe UI",Font.BOLD,23));header.add(title,BorderLayout.NORTH);header.add(context,BorderLayout.SOUTH);top.setOpaque(false);top.add(header,BorderLayout.NORTH);tools.setBackground(FILTER);tools.setBorder(BorderFactory.createLineBorder(LINE));top.add(tools,BorderLayout.CENTER);add(top,BorderLayout.NORTH);
        table.setRowHeight(31);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setGridColor(LINE);table.setFillsViewportHeight(true);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.getTableHeader().setBackground(GREEN);table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,14));table.setFont(new Font("Segoe UI",Font.PLAIN,14));table.setDefaultRenderer(Object.class,new javax.swing.table.DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){Component x=super.getTableCellRendererComponent(t,v,s,f,r,c);if(!s)x.setBackground(r%2==0?Color.WHITE:Color.decode("#F2F8E7"));setToolTipText(v==null?null:v.toString());return x;}});
        body.setOpaque(false);body.add(new JScrollPane(table),BorderLayout.CENTER);add(body,BorderLayout.CENTER);status.setOpaque(true);status.setBackground(GREEN);status.setBorder(new EmptyBorder(8,12,8,12));add(status,BorderLayout.SOUTH);
    }
    public void title(String a,String b){title.setText(a);context.setText(b);}
    public void status(String s){status.setText(s);}
    public void columns(String... c){model.setColumnIdentifiers(c);for(int i=0;i<c.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(i==0?85:160);}
    public void rows(Object[][] rows){model.setRowCount(0);for(Object[] r:rows)model.addRow(r);}
    public int selected(){int r=table.getSelectedRow();return r<0?-1:table.convertRowIndexToModel(r);}
    public Object value(int r,int c){return model.getValueAt(r,c);}
    public JButton action(String text,Runnable event){JButton b=button(text);b.addActionListener(e->event.run());tools.add(b);return b;}
    public record Choice(int id,String label){public String toString(){return label;}}
    public record Field(String label,String value,Choice[] options,Integer selected,boolean checkbox){
        public static Field text(String label,String value){return new Field(label,value,null,null,false);}
        public static Field choice(String label,java.util.List<Choice> options,Integer selected){return new Field(label,"",options.toArray(Choice[]::new),selected,false);}
        public static Field check(String label,boolean value){return new Field(label,Boolean.toString(value),null,null,true);}
    }
    public static class Form {
        private final String[] labels; private final JComponent[] fields;
        public Form(Field... spec){labels=new String[spec.length];fields=new JComponent[spec.length];for(int i=0;i<spec.length;i++){Field f=spec[i];labels[i]=f.label();if(f.options()!=null){JComboBox<Choice> c=new JComboBox<>(f.options());for(int j=0;j<c.getItemCount();j++)if(java.util.Objects.equals(c.getItemAt(j).id(),f.selected()))c.setSelectedIndex(j);fields[i]=c;}else if(f.checkbox())fields[i]=new JCheckBox("Có",Boolean.parseBoolean(f.value()));else fields[i]=new JTextField(java.util.Objects.toString(f.value(),""),22);}}
        public String text(int i){return ((JTextField)fields[i]).getText();}public void text(int i,String value){((JTextField)fields[i]).setText(value);}
        public boolean checked(int i){return ((JCheckBox)fields[i]).isSelected();}public int choice(int i){Choice c=(Choice)((JComboBox<?>)fields[i]).getSelectedItem();return c==null?0:c.id();}
        public void onChoice(int i,Runnable action){((JComboBox<?>)fields[i]).addActionListener(e->action.run());}
        public int show(Component owner,String title,String... actions){return form(owner,title,labels,fields,actions);}
    }
    public JCheckBox checkFilter(String label,Runnable action){JCheckBox c=new JCheckBox(label);c.addActionListener(e->action.run());tools.add(c);return c;}
    public JComboBox<String> comboFilter(String... items){JComboBox<String> c=new JComboBox<>(items);tools.add(c);return c;}
    public JTextField textFilter(String label,String value,int cols){tools.add(new JLabel(label));JTextField c=new JTextField(value,cols);tools.add(c);return c;}
    public static JButton button(String text){JButton b=new JButton(text);b.setBackground(BUTTON);b.setFocusPainted(false);b.setBorder(new EmptyBorder(8,12,8,12));return b;}
    public void content(JComponent c){body.removeAll();body.add(c,BorderLayout.CENTER);body.revalidate();body.repaint();}
    public static void message(Component owner,String text){JOptionPane.showMessageDialog(owner,text);}
    public static boolean confirm(Component owner,String text){return JOptionPane.showConfirmDialog(owner,text,"Xác nhận",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    public static int form(Component owner,String title,String[] labels,JComponent[] fields,String... choices){JPanel panel=new JPanel(new GridLayout(0,2,8,8));for(int i=0;i<labels.length;i++){panel.add(new JLabel(labels[i]));panel.add(fields[i]);}return JOptionPane.showOptionDialog(owner,panel,title,JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,choices,choices[0]);}
    public static void window(Component owner,String title,JComponent content){JDialog d=new JDialog(SwingUtilities.getWindowAncestor(owner),title,Dialog.ModalityType.MODELESS);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);d.setContentPane(content);d.setSize(1000,650);d.setLocationRelativeTo(owner);d.setVisible(true);}
    public static void popup(JTable table,String[] labels,IntConsumer event){JPopupMenu p=new JPopupMenu();for(int i=0;i<labels.length;i++){int n=i;JMenuItem item=new JMenuItem(labels[i]);item.addActionListener(e->event.accept(n));p.add(item);}table.addMouseListener(new MouseAdapter(){void open(MouseEvent e){if(!e.isPopupTrigger())return;int r=table.rowAtPoint(e.getPoint());if(r<0)return;table.setRowSelectionInterval(r,r);p.show(table,e.getX(),e.getY());}public void mousePressed(MouseEvent e){open(e);}public void mouseReleased(MouseEvent e){open(e);}});}
}
