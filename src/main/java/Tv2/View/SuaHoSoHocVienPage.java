package Tv2.View;
import MainPage.View.Workspace;
import javax.swing.*;
import java.awt.Component;

/** View chỉnh sửa hồ sơ, độc lập với chỉnh sửa đăng ký khóa học. */
public class SuaHoSoHocVienPage {
    public record Input(String name,String birth,String gender,String parent,String phone,String email,String address,String photo,String note){}
    public Input show(Component owner,Input initial){
        JTextField name=new JTextField(initial.name(),26), birth=new JTextField(initial.birth()), parent=new JTextField(initial.parent()),phone=new JTextField(initial.phone()),email=new JTextField(initial.email()),address=new JTextField(initial.address()),photo=new JTextField(initial.photo()),note=new JTextField(initial.note());
        JComboBox<String> gender=new JComboBox<>(new String[]{"","NAM","NU","KHAC"});gender.setSelectedItem(initial.gender());
        int i=Workspace.form(owner,"Sửa hồ sơ học viên",new String[]{"Họ tên","Ngày sinh (yyyy-MM-dd)","Giới tính","Phụ huynh","Điện thoại","Email","Địa chỉ","Đường dẫn ảnh đại diện","Ghi chú"},new JComponent[]{name,birth,gender,parent,phone,email,address,photo,note},"Lưu hồ sơ","Hủy");
        return i==0?new Input(name.getText(),birth.getText(),(String)gender.getSelectedItem(),parent.getText(),phone.getText(),email.getText(),address.getText(),photo.getText(),note.getText()):null;
    }
}
