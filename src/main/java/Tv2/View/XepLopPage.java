package Tv2.View;
import MainPage.View.Workspace;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
/** Chọn lớp; dữ liệu và xác nhận do Controller cung cấp. */
public class XepLopPage extends Workspace {
    private final JLabel selection=new JLabel("Chưa chọn lớp");
    public XepLopPage(){super();JPanel banner=new JPanel(new BorderLayout());banner.setBackground(HocVienPage.GREEN);banner.setBorder(new EmptyBorder(14,12,14,12));JLabel hint=new JLabel("Chọn một lớp trong danh sách để xác nhận");hint.setFont(new Font("Segoe UI",Font.BOLD,15));banner.add(hint,BorderLayout.WEST);banner.add(selection,BorderLayout.EAST);body.add(banner,BorderLayout.NORTH);table.setRowHeight(40);}
    public void setSelectionText(String text){selection.setText(text);}
    public void onSelection(Runnable r){table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())r.run();});}
}
