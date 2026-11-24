package vn.vinabacus.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import vn.vinabacus.model.PhongHoc;
import vn.vinabacus.view.NutMau;
import vn.vinabacus.view.PhongHocFormPanel;
import vn.vinabacus.view.PhongHocPanel;

/** Tao anh kiem tra giao dien thu tu ma KHONG can SQL Server. */
public final class XemTruocPhongHocTV1 {
    public static void main(String[] args) throws Exception {
        String dich = args.length > 0 ? args[0] : "TV1_FORM_SUA_PHONG_HOC_XEM_TRUOC.png";
        boolean them = args.length > 1 && "them".equalsIgnoreCase(args[1]);
        final JFrame[] ref = {null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                JFrame frame = new JFrame((them ? "Thêm" : "Sửa") + " phòng học – VINABACUS (bản xem trước)");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setLayout(new BorderLayout());
                Method tao = PhongHocPanel.class.getDeclaredMethod("taoTieuDeForm", String.class);
                tao.setAccessible(true);
                frame.add((JPanel) tao.invoke(null, them ? "THÊM PHÒNG HỌC" : "SỬA PHÒNG HỌC"), BorderLayout.NORTH);
                frame.add(new PhongHocFormPanel(them ? null : new PhongHoc(1, 1,
                    "P101 - Phòng 101", 20, "HOAT_DONG", "Phòng học Toán tư duy; dữ liệu mẫu")), BorderLayout.CENTER);
                JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 15));
                footer.setBackground(Color.WHITE);
                footer.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1,0,0,0,new Color(0xBFDDCA)),
                    BorderFactory.createEmptyBorder(4,22,4,20)));
                NutMau huy = new NutMau("Hủy", NutMau.Kieu.PHU);
                NutMau luu = new NutMau(them ? "Lưu phòng" : "Lưu thay đổi", NutMau.Kieu.CHINH);
                huy.setPreferredSize(new Dimension(138,43));
                luu.setPreferredSize(new Dimension(170,43));
                footer.add(huy); footer.add(luu);
                frame.add(footer, BorderLayout.SOUTH);
                frame.setSize(930,607);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                ref[0] = frame;
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        Thread.sleep(600);
        JFrame frame = ref[0];
        java.awt.Point p = frame.getLocationOnScreen();
        BufferedImage img = new Robot().createScreenCapture(new Rectangle(p.x,p.y,frame.getWidth(),frame.getHeight()));
        ImageIO.write(img,"png",new File(dich));
        SwingUtilities.invokeAndWait(frame::dispose);
        System.out.println("Đã tạo ảnh xem trước: " + dich);
    }
}
