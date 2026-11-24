package Tv2.View;
import MainPage.View.Workspace;import javax.swing.*;import java.awt.*;import java.awt.datatransfer.*;import java.util.List;
/** Kéo hồ sơ vào buổi học thử, hoặc chọn dòng để xác nhận. */
public class HocThuPage extends Workspace {
 private final JComboBox<Workspace.Choice> course=new JComboBox<>();
 private final JLabel target=new JLabel("Chưa chọn buổi");
 public HocThuPage(){super();tools.add(new JLabel("Khóa học:"));tools.add(course);tools.add(target);JLabel drag=new JLabel("  HỒ SƠ HỌC THỬ   •   Kéo thả vào buổi học hoặc chọn một dòng  ");drag.setOpaque(true);drag.setBackground(HocVienPage.GREEN);drag.setForeground(Color.decode("#235D37"));drag.setFont(new Font("Segoe UI",Font.BOLD,15));drag.setBorder(BorderFactory.createEmptyBorder(14,12,14,12));drag.setTransferHandler(new TransferHandler("text"));drag.addMouseListener(new java.awt.event.MouseAdapter(){public void mousePressed(java.awt.event.MouseEvent e){drag.getTransferHandler().exportAsDrag(drag,e,TransferHandler.COPY);}});body.add(drag,BorderLayout.NORTH);table.setDropMode(DropMode.ON);table.setTransferHandler(new TransferHandler(){public boolean canImport(TransferSupport s){return s.isDrop()&&s.isDataFlavorSupported(DataFlavor.stringFlavor)&&((JTable.DropLocation)s.getDropLocation()).getRow()>=0;}public boolean importData(TransferSupport s){if(!canImport(s))return false;int r=((JTable.DropLocation)s.getDropLocation()).getRow();table.setRowSelectionInterval(r,r);return true;}});table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting()){int chosen=selected();target.setText(chosen<0?"Chưa chọn buổi":"Đã chọn dòng "+(chosen+1));}});}
 public void setCourseOptions(Workspace.Choice[] items,int selected){course.setModel(new DefaultComboBoxModel<>(items));for(int i=0;i<items.length;i++)if(items[i].id()==selected){course.setSelectedIndex(i);break;}}
 public int courseId(){Workspace.Choice x=(Workspace.Choice)course.getSelectedItem();return x==null?0:x.id();}
 public void onCourseChanged(Runnable r){course.addActionListener(e->r.run());}
}
