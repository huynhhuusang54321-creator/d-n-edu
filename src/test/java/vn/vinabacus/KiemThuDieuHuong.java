package vn.vinabacus;

import javax.swing.SwingUtilities;
import vn.vinabacus.giaodien.GiaoDienChinh;
import vn.vinabacus.model.CoSo;

/** Chạy trong môi trường desktop (hoặc Xvfb). */
public final class KiemThuDieuHuong {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GiaoDienChinh f=new GiaoDienChinh();
            try {
                assert f.layCoSo()==CoSo.MAM_NON;
                f.chonCoSo(CoSo.TRUNG_TAM);
                assert f.layCoSo()==CoSo.TRUNG_TAM;
                f.moMuc("Quản lý khóa học");
                f.moMuc("Kho – Dụng cụ");
                f.chonCoSo(CoSo.MAM_NON);
                assert f.layCoSo()==CoSo.MAM_NON;
                f.moMuc("Điểm danh");
                f.moMuc("Trang chủ");
                System.out.println("PASS: cửa sổ, điều hướng menu, chuyển cơ sở và đổi trang.");
            } finally {f.dispose();}
        });
    }
}
