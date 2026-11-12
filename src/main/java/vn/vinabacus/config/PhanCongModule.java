package vn.vinabacus.config;

import java.util.Map;
import java.util.LinkedHashMap;

/** So do phan cong module cho nhom, KHONG thay the kiem tra phan quyen tai SQL. */
public final class PhanCongModule {
    private PhanCongModule() {}

    public static Map<String, Integer> layNguoiPhuTrach() {
        Map<String, Integer> kq = new LinkedHashMap<>();
        kq.put("hocvien", 2);
        kq.put("tuyensinh", 2);
        kq.put("khoahoc", 3);
        kq.put("lophoc", 3);
        kq.put("lichhoc", 3);
        kq.put("diemdanh", 4);
        kq.put("hocvu", 4);
        kq.put("hocphi", 5);
        kq.put("tienan", 5);
        kq.put("kho", 6);
        kq.put("baocao", 6);
        kq.put("hethong", 6);
        return Map.copyOf(kq);
    }
}
