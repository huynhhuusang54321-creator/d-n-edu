package vn.vinabacus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import vn.vinabacus.config.PhanCongModule;
import vn.vinabacus.model.VaiTroNhanVien;

/** Kiểm tra 12 module, hợp đồng repository và đường gọi controller -> service. */
public final class KiemThuKhungSauNguoi {
    public static void main(String[] args) throws Exception {
        Map<String, Integer> p = PhanCongModule.layNguoiPhuTrach();
        assert p.size() == 12 : p;
        assert VaiTroNhanVien.values().length == 3;
        String[] modules = {"hocvien", "tuyensinh", "khoahoc", "lophoc", "lichhoc",
            "diemdanh", "hocvu", "hocphi", "tienan", "kho", "baocao", "hethong"};
        String[] prefixes = {"HocVien", "TuyenSinh", "KhoaHoc", "LopHoc", "LichHoc",
            "DiemDanh", "HocVu", "HocPhi", "TienAn", "Kho", "BaoCao", "HeThong"};
        String[] packages = {"view", "controller", "service", "data", "model"};
        String[] cls = {"GiaoDien", "DieuKhien", "XuLy", "DuLieu", "ThongTin"};
        for (int i=0; i<modules.length; i++) {
            assert p.containsKey(modules[i]);
            for (int j=0; j<packages.length; j++) {
                Class.forName("vn.vinabacus." + packages[j] + "." + cls[j] + prefixes[i]);
            }
            String module = modules[i], prefix = prefixes[i];
            Class<?> repository = Class.forName("vn.vinabacus.repository." + prefix + "Repository");
            Class<?> data = Class.forName("vn.vinabacus.data.DuLieu" + prefix);
            Class<?> service = Class.forName("vn.vinabacus.service.XuLy" + prefix);
            Class<?> controller = Class.forName("vn.vinabacus.controller.DieuKhien" + prefix);
            Class<?> model = Class.forName("vn.vinabacus.model.ThongTin" + prefix);
            assert repository.isInterface() : repository;
            assert repository.isAssignableFrom(data) : data;
            assert repository.getMethod("layDanhSach", int.class).getReturnType() == List.class;
            Object record = model.getConstructor(String.class, String.class, String.class)
                    .newInstance("TEST01", "Kiểm thử", "Hoạt động");
            List<?> expected = List.of(record);
            AtomicInteger calls = new AtomicInteger();
            Object fakeRepository = Proxy.newProxyInstance(repository.getClassLoader(), new Class<?>[]{repository},
                    (proxy, method, values) -> {
                        assert method.getName().equals("layDanhSach") : method;
                        assert values.length == 1 && Integer.valueOf(2).equals(values[0]);
                        calls.incrementAndGet();
                        return expected;
                    });
            Object useCase = service.getConstructor(repository).newInstance(fakeRepository);
            Object handler = controller.getConstructor(service).newInstance(useCase);
            assert controller.getMethod("layDanhSach", int.class).invoke(handler, 2) == expected : module;
            assert calls.get() == 1 : module;
            try {
                service.getMethod("layDanhSach", int.class).invoke(useCase, 0);
                throw new AssertionError("Service phải từ chối mã cơ sở không hợp lệ: " + module);
            } catch (InvocationTargetException ex) {
                assert ex.getCause() instanceof IllegalArgumentException : ex.getCause();
            }
            assert calls.get() == 1 : "Không được gọi repository khi mã cơ sở sai: " + module;
            try {
                service.getConstructor(repository).newInstance(new Object[]{null});
                throw new AssertionError("Service phải từ chối repository null: " + module);
            } catch (InvocationTargetException ex) {
                assert ex.getCause() instanceof NullPointerException : ex.getCause();
            }
            try {
                data.getMethod("layDanhSach", int.class).invoke(data.getConstructor().newInstance(), 2);
                throw new AssertionError("DAO chưa có SQL phải báo chưa hỗ trợ: " + module);
            } catch (InvocationTargetException ex) {
                assert ex.getCause() instanceof UnsupportedOperationException : ex.getCause();
            }
        }
        System.out.println("PASS: 12 modules x 6 layers, repository contracts, service validation and delegation, 3 roles.");
    }
}
