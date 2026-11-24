package Service;

import Model.DanhMucModel;
import Repository.DanhMucRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class DanhMucService {
    private final DanhMucRepository repo = new DanhMucRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<DanhMucModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<DanhMucModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public DanhMucModel getByMaDanhMuc(int maCoSo, int ma) { return repo.getByMaDanhMuc(maCoSo, ma); }
    public List<DanhMucModel> getByMaCha(int maCoSo, int value) { return repo.getByMaCha(maCoSo, value); }
    public List<DanhMucModel> getByNhomDanhMuc(int maCoSo, String value) { return repo.getByNhomDanhMuc(maCoSo, value); }

    public List<DanhMucModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<DanhMucModel> result = new ArrayList<>();
        for (DanhMucModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getNhomDanhMuc()).contains(tk) || normalize(x.getMaKyHieu()).contains(tk) || normalize(x.getTenDanhMuc()).contains(tk) || String.valueOf(x.getMaDanhMuc()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, DanhMucModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, DanhMucModel x) {
        if (x == null || x.getMaDanhMuc() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public DanhMucModel modify(int maCoSo, int ma, Predicate<DanhMucModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(DanhMucModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getNhomDanhMuc() == null || x.getNhomDanhMuc().trim().isEmpty()) throw new IllegalArgumentException("NhomDanhMuc không hợp lệ");
        if (x.getNhomDanhMuc() != null && x.getNhomDanhMuc().length() > 30) throw new IllegalArgumentException("NhomDanhMuc không hợp lệ");
        if (x.getMaKyHieu() == null || x.getMaKyHieu().trim().isEmpty()) throw new IllegalArgumentException("MaKyHieu không hợp lệ");
        if (x.getMaKyHieu() != null && x.getMaKyHieu().length() > 40) throw new IllegalArgumentException("MaKyHieu không hợp lệ");
        if (x.getTenDanhMuc() == null || x.getTenDanhMuc().trim().isEmpty()) throw new IllegalArgumentException("TenDanhMuc không hợp lệ");
        if (x.getTenDanhMuc() != null && x.getTenDanhMuc().length() > 200) throw new IllegalArgumentException("TenDanhMuc không hợp lệ");
        if (x.getMaCha() != null && x.getMaCha() <= 0) throw new IllegalArgumentException("MaCha không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
