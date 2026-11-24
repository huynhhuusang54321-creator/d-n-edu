package Tv4.View;
import MainPage.View.Workspace;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
/** Hiển thị ba ô chọn loại điểm danh và trường ghi chú. */
public class DiemDanhPage extends Workspace {
 public record Option(int id,String label){public String toString(){return label;}}
 public record Input(int hv,Integer reg,String kind,String state,boolean count,String note){}
 private final JComboBox<Option> classes=new JComboBox<>(),lessons=new JComboBox<>();
 private final DefaultTableModel edit=new DefaultTableModel(new String[]{"Mã HV","Họ tên","Mã đăng ký","Loại","Có mặt","Vắng có phép","Vắng không phép","Tính buổi","Ghi chú"},0){
  public boolean isCellEditable(int r,int c){return c>=4;}
  public Class<?> getColumnClass(int c){return c>=4&&c<=7?Boolean.class:Object.class;}
  public void setValueAt(Object value,int row,int col){super.setValueAt(value,row,col);if(col>=4&&col<=6&&Boolean.TRUE.equals(value)){for(int c=4;c<=6;c++)if(c!=col)super.setValueAt(false,row,c);}}
 };
 public DiemDanhPage(){super();tools.add(new JLabel("Lớp:"));tools.add(classes);tools.add(new JLabel("Buổi:"));tools.add(lessons);table.setModel(edit);int[] widths={75,200,95,110,85,120,135,85,250};for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);}
 public void classes(Option[] a){classes.setModel(new DefaultComboBoxModel<>(a));}public void lessons(Option[] a){lessons.setModel(new DefaultComboBoxModel<>(a));}public int classId(){Option x=(Option)classes.getSelectedItem();return x==null?0:x.id();}public int lessonId(){Option x=(Option)lessons.getSelectedItem();return x==null?0:x.id();}public void onClass(Runnable r){classes.addActionListener(e->r.run());}public void onLesson(Runnable r){lessons.addActionListener(e->r.run());}
 public void attendanceRows(Object[][] a){edit.setRowCount(0);for(Object[] r:a){String state=String.valueOf(r[4]);edit.addRow(new Object[]{r[0],r[1],r[2],r[3],"CO_MAT".equals(state),"VANG_CO_PHEP".equals(state),"VANG_KHONG_PHEP".equals(state),r[5],r[6]});}}
 public Input[] inputs(){if(table.isEditing()&&!table.getCellEditor().stopCellEditing())return null;Input[] a=new Input[edit.getRowCount()];for(int i=0;i<a.length;i++){String state=Boolean.TRUE.equals(edit.getValueAt(i,4))?"CO_MAT":Boolean.TRUE.equals(edit.getValueAt(i,5))?"VANG_CO_PHEP":Boolean.TRUE.equals(edit.getValueAt(i,6))?"VANG_KHONG_PHEP":"CHUA_DIEM_DANH";a[i]=new Input((int)edit.getValueAt(i,0),(Integer)edit.getValueAt(i,2),String.valueOf(edit.getValueAt(i,3)),state,Boolean.TRUE.equals(edit.getValueAt(i,7)),java.util.Objects.toString(edit.getValueAt(i,8),""));}return a;}
}
