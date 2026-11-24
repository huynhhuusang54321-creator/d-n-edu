package vn.vinabacus.tv5;

import java.math.BigDecimal;
import java.time.LocalDate;
import vn.vinabacus.service.TaiChinhService;

/** Kiểm thử số học thu học phí độc lập SQL Server; dùng java -ea ...TaiChinhSelfTest */
public final class TaiChinhSelfTest {
    private TaiChinhSelfTest() {}
    private static void eq(String expected, BigDecimal actual) {
        if(new BigDecimal(expected).compareTo(actual)!=0)
            throw new AssertionError("Mong đợi "+expected+" nhưng nhận "+actual);
    }
    private static void error(Runnable run) {
        try {run.run();throw new AssertionError("Không báo lỗi như mong đợi");}
        catch(IllegalArgumentException expected) { /* pass */ }
    }
    public static void main(String[] args){
        eq("500000",TaiChinhService.conNo(new BigDecimal("700000"),new BigDecimal("200000")));
        eq("0",TaiChinhService.conNo(new BigDecimal("100000"),new BigDecimal("100000")));
        eq("-10000",TaiChinhService.conNo(new BigDecimal("100000"),new BigDecimal("110000")));
        eq("800000",TaiChinhService.sauGiam(new BigDecimal("1000000"),new BigDecimal("200000")));
        eq("125000.50",TaiChinhService.tien("125000.50"));
        if(!LocalDate.of(2026,11,1).equals(TaiChinhService.thangBatDau("2026-11")))throw new AssertionError("Sai tháng áp dụng");
        error(()->TaiChinhService.tien("1,000"));
        error(()->TaiChinhService.tien("-100"));
        error(()->TaiChinhService.tien("12.345"));
        error(()->TaiChinhService.sauGiam(new BigDecimal("100"),new BigDecimal("101")));
        error(()->TaiChinhService.thangBatDau("11/2026"));
        error(()->TaiChinhService.conNo(new BigDecimal("-1"),new BigDecimal("0")));
        error(()->TaiChinhService.tien("999999999999999999"));
        eq("-50000",TaiChinhService.tienDieuChinh("-50000"));
        eq("12500.25",TaiChinhService.tienDieuChinh("+12500.25"));
        error(()->TaiChinhService.tienDieuChinh("0"));
        error(()->TaiChinhService.tienDieuChinh("-0.00"));
        error(()->TaiChinhService.tienDieuChinh("1,000"));
        error(()->TaiChinhService.tienDieuChinh("-1.234"));
        vn.vinabacus.model.TaiChinhDTO.KhoanThu k=new vn.vinabacus.model.TaiChinhDTO.KhoanThu(
            1,2,"Học viên", "HOC_PHI_KHOA",new BigDecimal("2000000"),
            new BigDecimal("-50000"),new BigDecimal("600000"),null,"HIEU_LUC");
        eq("1950000",k.phaiThu());
        eq("1350000",k.conNo());
        vn.vinabacus.model.TaiChinhDTO.KhoanThu thuVuot=new vn.vinabacus.model.TaiChinhDTO.KhoanThu(
            2,2,"Học viên", "HOC_PHI_KHOA",new BigDecimal("100000"),
            BigDecimal.ZERO,new BigDecimal("110000"),null,"HIEU_LUC");
        eq("-10000",thuVuot.conNo());
        // Kiểm tra màu nền nút chính không bị Windows Look&Feel phủ màu.
        javax.swing.JButton button=vn.vinabacus.view.TaiChinhUi.nut("+ Lập phiếu thu",true);
        button.setSize(button.getPreferredSize());
        java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(
            button.getWidth(),button.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics=image.createGraphics();
        button.paint(graphics);graphics.dispose();
        int rgb=image.getRGB(5,5)&0xFFFFFF;
        if(rgb!=(vn.vinabacus.view.TaiChinhUi.XANH.getRGB()&0xFFFFFF))
            throw new AssertionError("Nút chính không giữ nền xanh lá.");
        // Kiem tra trung ma menu voi TV4 tren bo nguon da duoc cung cap.
        vn.vinabacus.tv4.TV4Module.dangKy();
        TV5Module.dangKy();
        System.out.println("PASS: tinh tien, dieu chinh, mau nut va dang ky TV4/TV5 (khong can SQL Server)");
    }
}
