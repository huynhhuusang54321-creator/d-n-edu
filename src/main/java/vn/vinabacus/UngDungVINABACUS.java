package vn.vinabacus;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import vn.vinabacus.view.GiaoDienChinh;
import vn.vinabacus.model.CoSo;

/** Khởi động trực tiếp giao diện, KHÔNG có đăng nhập và KHÔNG kết nối SQL Server. */
public final class UngDungVINABACUS {
    private UngDungVINABACUS() {}
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        SwingUtilities.invokeLater(() -> {
            try {
                for(UIManager.LookAndFeelInfo x:UIManager.getInstalledLookAndFeels()) {
                    if(x.getName().equals("Nimbus")) {UIManager.setLookAndFeel(x.getClassName());break;}
                }
            } catch(Exception ignored) {}
            GiaoDienChinh frame=new GiaoDienChinh();
            if(args.length>0 && args[0].equalsIgnoreCase("--trungtam")) frame.chonCoSo(CoSo.TRUNG_TAM);
            frame.setVisible(true);
            // Công cụ chụp ảnh tự động phục vụ kiểm thử giao diện (không chạy mặc định).
            String duongDan=System.getProperty("vinabacus.anhChup");
            if(duongDan!=null && !duongDan.isBlank()) {
                new Timer(1500,e->{
                    try {
                        Rectangle box=frame.getBounds();
                        BufferedImage anh=new Robot().createScreenCapture(box);
                        ImageIO.write(anh,"png",new File(duongDan));
                    } catch (Exception ex) {ex.printStackTrace();}
                    ((Timer)e.getSource()).stop();
                }).start();
            }
        });
    }
}
