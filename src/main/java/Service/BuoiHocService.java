package Service;

import Model.BuoiHocModel;
import Repository.BuoiHocRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class BuoiHocService {
    private final BuoiHocRepository repo = new BuoiHocRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<BuoiHocModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<BuoiHocModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public BuoiHocModel getByMaBuoiHoc(int maCoSo, int ma) { return repo.getByMaBuoiHoc(maCoSo, ma); }
    public List<BuoiHocModel> getByMaLop(int maCoSo, int value) { return repo.getByMaLop(maCoSo, value); }
    public List<BuoiHocModel> getByMaThoiKhoaBieu(int maCoSo, int value) { return repo.getByMaThoiKhoaBieu(maCoSo, value); }
    public List<BuoiHocModel> getByMaCaHoc(int maCoSo, int value) { return repo.getByMaCaHoc(maCoSo, value); }
    public List<BuoiHocModel> getByMaGiaoVien(int maCoSo, int value) { return repo.getByMaGiaoVien(maCoSo, value); }
    public List<BuoiHocModel> getByMaNguoiKichHoat(int maCoSo, int value) { return repo.getByMaNguoiKichHoat(maCoSo, value); }

    public List<BuoiHocModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<BuoiHocModel> result = new ArrayList<>();
        for (BuoiHocModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getTrangThai()).contains(tk) || normalize(x.getGhiChu()).contains(tk) || String.valueOf(x.getMaBuoiHoc()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, BuoiHocModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, BuoiHocModel x) {
        if (x == null || x.getMaBuoiHoc() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public BuoiHocModel modify(int maCoSo, int ma, Predicate<BuoiHocModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(BuoiHocModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaLop() <= 0) throw new IllegalArgumentException("MaLop không hợp lệ");
        if (x.getMaThoiKhoaBieu() != null && x.getMaThoiKhoaBieu() <= 0) throw new IllegalArgumentException("MaThoiKhoaBieu không hợp lệ");
        if (x.getNgayHoc() == null) throw new IllegalArgumentException("NgayHoc không hợp lệ");
        if (x.getMaCaHoc() <= 0) throw new IllegalArgumentException("MaCaHoc không hợp lệ");
        if (x.getGioBatDau() == null) throw new IllegalArgumentException("GioBatDau không hợp lệ");
        if (x.getGioKetThuc() == null) throw new IllegalArgumentException("GioKetThuc không hợp lệ");
        if (x.getMaGiaoVien() != null && x.getMaGiaoVien() <= 0) throw new IllegalArgumentException("MaGiaoVien không hợp lệ");
        if (x.getMaNguoiKichHoat() != null && x.getMaNguoiKichHoat() <= 0) throw new IllegalArgumentException("MaNguoiKichHoat không hợp lệ");
        if (x.getTrangThai() == null || x.getTrangThai().trim().isEmpty()) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && x.getTrangThai().length() > 15) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && !Arrays.asList("DU_KIEN","DA_KICH_HOAT","HOAN_THANH","HUY").contains(x.getTrangThai())) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getGhiChu() != null && x.getGhiChu().length() > 500) throw new IllegalArgumentException("GhiChu không hợp lệ");
        if (!x.getGioKetThuc().isAfter(x.getGioBatDau())) throw new IllegalArgumentException("Dữ liệu BuoiHoc không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
