package Service;

import Model.ThoiKhoaBieuModel;
import Repository.ThoiKhoaBieuRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class ThoiKhoaBieuService {
    private final ThoiKhoaBieuRepository repo = new ThoiKhoaBieuRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<ThoiKhoaBieuModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<ThoiKhoaBieuModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public ThoiKhoaBieuModel getByMaThoiKhoaBieu(int maCoSo, int ma) { return repo.getByMaThoiKhoaBieu(maCoSo, ma); }
    public List<ThoiKhoaBieuModel> getByMaLop(int maCoSo, int value) { return repo.getByMaLop(maCoSo, value); }
    public List<ThoiKhoaBieuModel> getByMaCaHoc(int maCoSo, int value) { return repo.getByMaCaHoc(maCoSo, value); }

    public List<ThoiKhoaBieuModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<ThoiKhoaBieuModel> result = new ArrayList<>();
        for (ThoiKhoaBieuModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || String.valueOf(x.getMaThoiKhoaBieu()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, ThoiKhoaBieuModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, ThoiKhoaBieuModel x) {
        if (x == null || x.getMaThoiKhoaBieu() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public ThoiKhoaBieuModel modify(int maCoSo, int ma, Predicate<ThoiKhoaBieuModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(ThoiKhoaBieuModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaLop() <= 0) throw new IllegalArgumentException("MaLop không hợp lệ");
        if (x.getMaCaHoc() <= 0) throw new IllegalArgumentException("MaCaHoc không hợp lệ");
        if (x.getTuNgay() == null) throw new IllegalArgumentException("TuNgay không hợp lệ");
        if (x.getThuTrongTuan() < 1 || x.getThuTrongTuan() > 7) throw new IllegalArgumentException("Dữ liệu ThoiKhoaBieu không hợp lệ");
        if (x.getDenNgay() != null && x.getDenNgay().isBefore(x.getTuNgay())) throw new IllegalArgumentException("Dữ liệu ThoiKhoaBieu không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
