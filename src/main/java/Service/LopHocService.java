package Service;

import Model.LopHocModel;
import Repository.LopHocRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class LopHocService {
    private final LopHocRepository repo = new LopHocRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<LopHocModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<LopHocModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public LopHocModel getByMaLop(int maCoSo, int ma) { return repo.getByMaLop(maCoSo, ma); }
    public List<LopHocModel> getByMaKhoaHoc(int maCoSo, int value) { return repo.getByMaKhoaHoc(maCoSo, value); }
    public List<LopHocModel> getByMaNamHoc(int maCoSo, int value) { return repo.getByMaNamHoc(maCoSo, value); }
    public List<LopHocModel> getByMaGiaoVien(int maCoSo, int value) { return repo.getByMaGiaoVien(maCoSo, value); }

    public List<LopHocModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<LopHocModel> result = new ArrayList<>();
        for (LopHocModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getMaLopHienThi()).contains(tk) || normalize(x.getTenLop()).contains(tk) || normalize(x.getTrangThai()).contains(tk) || normalize(x.getGhiChu()).contains(tk) || String.valueOf(x.getMaLop()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, LopHocModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, LopHocModel x) {
        if (x == null || x.getMaLop() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public LopHocModel modify(int maCoSo, int ma, Predicate<LopHocModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(LopHocModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaLopHienThi() == null || x.getMaLopHienThi().trim().isEmpty()) throw new IllegalArgumentException("MaLopHienThi không hợp lệ");
        if (x.getMaLopHienThi() != null && x.getMaLopHienThi().length() > 30) throw new IllegalArgumentException("MaLopHienThi không hợp lệ");
        if (x.getTenLop() == null || x.getTenLop().trim().isEmpty()) throw new IllegalArgumentException("TenLop không hợp lệ");
        if (x.getTenLop() != null && x.getTenLop().length() > 150) throw new IllegalArgumentException("TenLop không hợp lệ");
        if (x.getMaKhoaHoc() != null && x.getMaKhoaHoc() <= 0) throw new IllegalArgumentException("MaKhoaHoc không hợp lệ");
        if (x.getMaNamHoc() != null && x.getMaNamHoc() <= 0) throw new IllegalArgumentException("MaNamHoc không hợp lệ");
        if (x.getMaGiaoVien() != null && x.getMaGiaoVien() <= 0) throw new IllegalArgumentException("MaGiaoVien không hợp lệ");
        if (x.getNgayBatDau() == null) throw new IllegalArgumentException("NgayBatDau không hợp lệ");
        if (x.getHocPhiMacDinh() == null) throw new IllegalArgumentException("HocPhiMacDinh không hợp lệ");
        if (x.getHocPhiMacDinh() != null && x.getHocPhiMacDinh().signum() < 0) throw new IllegalArgumentException("HocPhiMacDinh không hợp lệ");
        if (x.getTienAnMacDinh() == null) throw new IllegalArgumentException("TienAnMacDinh không hợp lệ");
        if (x.getTienAnMacDinh() != null && x.getTienAnMacDinh().signum() < 0) throw new IllegalArgumentException("TienAnMacDinh không hợp lệ");
        if (x.getTrangThai() == null || x.getTrangThai().trim().isEmpty()) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && x.getTrangThai().length() > 15) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && !Arrays.asList("DANG_MO","TAM_DUNG","DA_DONG").contains(x.getTrangThai())) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getGhiChu() != null && x.getGhiChu().length() > 500) throw new IllegalArgumentException("GhiChu không hợp lệ");
        if (x.getNgayKetThuc() != null && x.getNgayKetThuc().isBefore(x.getNgayBatDau())) throw new IllegalArgumentException("Dữ liệu LopHoc không hợp lệ");
        if (x.getSiSoToiDa() != null && (x.getSiSoToiDa() <= 0 || x.getSiSoToiDa() > 32767)) throw new IllegalArgumentException("Dữ liệu LopHoc không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
