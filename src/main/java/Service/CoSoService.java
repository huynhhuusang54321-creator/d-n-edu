package Service;

import Model.CoSoModel;
import Repository.CoSoRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class CoSoService {
    private final CoSoRepository repo = new CoSoRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<CoSoModel> reload() { return repo.reload(); }
    public Repository.TableCache.Snapshot selectRaw() { return repo.selectRaw(); }
    public Repository.TableCache.Snapshot reloadRaw() { return repo.reloadRaw(); }
    public boolean isCacheLoaded() { return repo.isCacheLoaded(); }
    public void invalidateBranch() { repo.invalidateBranch(); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<CoSoModel> selectAll() { return repo.getAll(); }
    public CoSoModel getByMaCoSo(int ma) { return repo.getByMaCoSo(ma); }

    public List<CoSoModel> timKiem(String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<CoSoModel> result = new ArrayList<>();
        for (CoSoModel x : selectAll()) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getMaCoSoHienThi()).contains(tk) || normalize(x.getTenCoSo()).contains(tk) || normalize(x.getLoaiCoSo()).contains(tk) || normalize(x.getDiaChi()).contains(tk) || normalize(x.getSoDienThoai()).contains(tk) || String.valueOf(x.getMaCoSo()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(CoSoModel x) {
        if (x == null) return false;
        
        validate(x);
        return repo.insert(x);
    }
    public boolean update(CoSoModel x) {
        if (x == null || x.getMaCoSo() <= 0) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int ma) { return repo.delete(ma); }
    public CoSoModel modify(int ma, Predicate<CoSoModel> change) {
        if (change == null) return null;
        return repo.modify(ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int ma) { repo.evict(ma); }
    public void validate(CoSoModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSoHienThi() == null || x.getMaCoSoHienThi().trim().isEmpty()) throw new IllegalArgumentException("MaCoSoHienThi không hợp lệ");
        if (x.getMaCoSoHienThi() != null && x.getMaCoSoHienThi().length() > 20) throw new IllegalArgumentException("MaCoSoHienThi không hợp lệ");
        if (x.getTenCoSo() == null || x.getTenCoSo().trim().isEmpty()) throw new IllegalArgumentException("TenCoSo không hợp lệ");
        if (x.getTenCoSo() != null && x.getTenCoSo().length() > 200) throw new IllegalArgumentException("TenCoSo không hợp lệ");
        if (x.getLoaiCoSo() == null || x.getLoaiCoSo().trim().isEmpty()) throw new IllegalArgumentException("LoaiCoSo không hợp lệ");
        if (x.getLoaiCoSo() != null && x.getLoaiCoSo().length() > 15) throw new IllegalArgumentException("LoaiCoSo không hợp lệ");
        if (x.getLoaiCoSo() != null && !Arrays.asList("TRUNG_TAM","MAM_NON").contains(x.getLoaiCoSo())) throw new IllegalArgumentException("LoaiCoSo không hợp lệ");
        if (x.getDiaChi() != null && x.getDiaChi().length() > 300) throw new IllegalArgumentException("DiaChi không hợp lệ");
        if (x.getSoDienThoai() != null && x.getSoDienThoai().length() > 20) throw new IllegalArgumentException("SoDienThoai không hợp lệ");
        if (x.getLogo() != null && x.getLogo().length() > 500) throw new IllegalArgumentException("Logo không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
