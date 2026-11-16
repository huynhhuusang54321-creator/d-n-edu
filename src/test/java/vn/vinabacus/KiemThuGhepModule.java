package vn.vinabacus;

import javax.swing.JLabel;
import javax.swing.JPanel;
import vn.vinabacus.config.MenuConfig;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.view.DangKyManHinh;
import vn.vinabacus.view.KhungNoiDungPanel;

/** Bài kiểm tra đơn giản, không cần JUnit và không cần SQL Server. */
public final class KiemThuGhepModule {
    private KiemThuGhepModule() {}

    public static void main(String[] args) {
        String ma = "tt-mon-hoc";
        DangKyManHinh.dangKy(ma, () -> {
            JPanel panel = new JPanel();
            panel.add(new JLabel("Module kiểm thử"));
            return panel;
        });
        KhungNoiDungPanel khung = new KhungNoiDungPanel(MenuConfig.choCoSo(CoSo.TRUNG_TAM)
                .stream().filter(m -> m.ma().equals(ma)).findFirst().orElseThrow());
        if (!(khung.getComponent(0) instanceof JPanel)) throw new AssertionError("Chưa lấy được module");
        JPanel panel = (JPanel) khung.getComponent(0);
        if (panel.getComponentCount() != 1) throw new AssertionError("Nội dung module không đúng");
        boolean loiTrung = false;
        try { DangKyManHinh.dangKy(ma, JPanel::new); }
        catch (IllegalArgumentException ex) { loiTrung = true; }
        if (!loiTrung) throw new AssertionError("Không chặn đăng ký mã menu trùng");
        System.out.println("KIEM THU GHEP MODULE: DAT");
    }
}
