package vn.vinabacus;

import java.util.Map;
import vn.vinabacus.cautruc.PhanCongModule;
import vn.vinabacus.doituong.VaiTroNhanVien;

/** Kiem tra cau truc package da duoc tao, khong ket noi SQL hay GUI. */
public final class KiemThuKhungSauNguoi {
    public static void main(String[] args) throws Exception {
        Map<String, Integer> p = PhanCongModule.layNguoiPhuTrach();
        assert p.size() == 12 : p;
        assert VaiTroNhanVien.values().length == 3;
        String[] modules = {"hocvien", "tuyensinh", "khoahoc", "lophoc", "lichhoc",
            "diemdanh", "hocvu", "hocphi", "tienan", "kho", "baocao", "hethong"};
        String[] prefixes = {"HocVien", "TuyenSinh", "KhoaHoc", "LopHoc", "LichHoc",
            "DiemDanh", "HocVu", "HocPhi", "TienAn", "Kho", "BaoCao", "HeThong"};
        String[] packages = {"giaodien", "dieukhien", "xuly", "dulieu", "doituong"};
        String[] cls = {"GiaoDien", "DieuKhien", "XuLy", "DuLieu", "ThongTin"};
        for (int i=0; i<modules.length; i++) {
            assert p.containsKey(modules[i]);
            for (int j=0; j<packages.length; j++) {
                Class.forName("vn.vinabacus." + packages[j] + "." + modules[i] + "." + cls[j] + prefixes[i]);
            }
        }
        System.out.println("PASS: 12 modules x 5 layers + 3 roles, total 60 skeleton classes.");
    }
}
