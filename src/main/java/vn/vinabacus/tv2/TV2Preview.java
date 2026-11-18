package vn.vinabacus.tv2;
import vn.vinabacus.model.CoSo;
import vn.vinabacus.view.HocVienPanel;
import vn.vinabacus.view.TuyenSinhPanel;
import javax.swing.*;
/** Chay rieng TV2 de test, KHONG can thay Main.java cua TV1. */
public final class TV2Preview {
    private TV2Preview(){}
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            JFrame frame=new JFrame("VINABACUS - Kiểm thử module TV2");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1360,750);frame.setLocationRelativeTo(null);
            JTabbedPane tab=new JTabbedPane();
            tab.addTab("Học viên TT",new HocVienPanel(CoSo.TRUNG_TAM));
            tab.addTab("Học sinh MN",new HocVienPanel(CoSo.MAM_NON));
            tab.addTab("Tuyển sinh TT",new TuyenSinhPanel(CoSo.TRUNG_TAM));
            tab.addTab("Tuyển sinh MN",new TuyenSinhPanel(CoSo.MAM_NON));
            frame.setContentPane(tab);frame.setVisible(true);
        });
    }
}
