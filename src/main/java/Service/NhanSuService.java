package Service;

import Model.NhanSuModel;
import Repository.NhanSuRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class NhanSuService {
    private final NhanSuRepository repo = new NhanSuRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<NhanSuModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<NhanSuModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public NhanSuModel getByMaNhanSu(int maCoSo, int ma) { return repo.getByMaNhanSu(maCoSo, ma); }
    public List<NhanSuModel> getByTenDangNhap(int maCoSo, String value) { return repo.getByTenDangNhap(maCoSo, value); }

    public List<NhanSuModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<NhanSuModel> result = new ArrayList<>();
        for (NhanSuModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getHoTen()).contains(tk) || normalize(x.getGioiTinh()).contains(tk) || normalize(x.getSoDienThoai()).contains(tk) || normalize(x.getEmail()).contains(tk) || normalize(x.getDiaChi()).contains(tk) || normalize(x.getTenDangNhap()).contains(tk) || normalize(x.getVaiTro()).contains(tk) || String.valueOf(x.getMaNhanSu()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, NhanSuModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, NhanSuModel x) {
        if (x == null || x.getMaNhanSu() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public NhanSuModel modify(int maCoSo, int ma, Predicate<NhanSuModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(NhanSuModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getHoTen() == null || x.getHoTen().trim().isEmpty()) throw new IllegalArgumentException("HoTen không hợp lệ");
        if (x.getHoTen() != null && x.getHoTen().length() > 150) throw new IllegalArgumentException("HoTen không hợp lệ");
        if (x.getGioiTinh() != null && x.getGioiTinh().length() > 10) throw new IllegalArgumentException("GioiTinh không hợp lệ");
        if (x.getGioiTinh() != null && !Arrays.asList("NAM","NU","KHAC").contains(x.getGioiTinh())) throw new IllegalArgumentException("GioiTinh không hợp lệ");
        if (x.getSoDienThoai() != null && x.getSoDienThoai().length() > 20) throw new IllegalArgumentException("SoDienThoai không hợp lệ");
        if (x.getEmail() != null && x.getEmail().length() > 254) throw new IllegalArgumentException("Email không hợp lệ");
        if (x.getDiaChi() != null && x.getDiaChi().length() > 300) throw new IllegalArgumentException("DiaChi không hợp lệ");
        if (x.getAnhDaiDien() != null && x.getAnhDaiDien().length() > 500) throw new IllegalArgumentException("AnhDaiDien không hợp lệ");
        if (x.getTenDangNhap() == null || x.getTenDangNhap().trim().isEmpty()) throw new IllegalArgumentException("TenDangNhap không hợp lệ");
        if (x.getTenDangNhap() != null && x.getTenDangNhap().length() > 100) throw new IllegalArgumentException("TenDangNhap không hợp lệ");
        if (x.getMatKhauBam() != null && x.getMatKhauBam().length() > 500) throw new IllegalArgumentException("MatKhauBam không hợp lệ");
        if (x.getVaiTro() == null || x.getVaiTro().trim().isEmpty()) throw new IllegalArgumentException("VaiTro không hợp lệ");
        if (x.getVaiTro() != null && x.getVaiTro().length() > 20) throw new IllegalArgumentException("VaiTro không hợp lệ");
        if (x.getVaiTro() != null && !Arrays.asList("ADMIN","TO_TRUONG","GIAO_VIEN","NHAN_VIEN").contains(x.getVaiTro())) throw new IllegalArgumentException("VaiTro không hợp lệ");
        if (x.getNgayTao() == null) throw new IllegalArgumentException("NgayTao không hợp lệ");
        if (x.getChoPhepDangNhap() && (x.getMatKhauBam() == null || x.getMatKhauBam().trim().isEmpty())) throw new IllegalArgumentException("Dữ liệu NhanSu không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
