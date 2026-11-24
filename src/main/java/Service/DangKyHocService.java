package Service;

import Model.DangKyHocModel;
import Repository.DangKyHocRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class DangKyHocService {
    private final DangKyHocRepository repo = new DangKyHocRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<DangKyHocModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<DangKyHocModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public DangKyHocModel getByMaDangKy(int maCoSo, int ma) { return repo.getByMaDangKy(maCoSo, ma); }
    public List<DangKyHocModel> getByMaHocVien(int maCoSo, int value) { return repo.getByMaHocVien(maCoSo, value); }
    public List<DangKyHocModel> getByMaKhoaHoc(int maCoSo, int value) { return repo.getByMaKhoaHoc(maCoSo, value); }
    public List<DangKyHocModel> getByMaLop(int maCoSo, int value) { return repo.getByMaLop(maCoSo, value); }
    public List<DangKyHocModel> getByMaDangKyGoc(int maCoSo, int value) { return repo.getByMaDangKyGoc(maCoSo, value); }
    public List<DangKyHocModel> getByMaDangKyTruoc(int maCoSo, int value) { return repo.getByMaDangKyTruoc(maCoSo, value); }
    public List<DangKyHocModel> getByMaNguoiDangKy(int maCoSo, int value) { return repo.getByMaNguoiDangKy(maCoSo, value); }

    public List<DangKyHocModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<DangKyHocModel> result = new ArrayList<>();
        for (DangKyHocModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getTrangThai()).contains(tk) || normalize(x.getGhiChu()).contains(tk) || String.valueOf(x.getMaDangKy()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, DangKyHocModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, DangKyHocModel x) {
        if (x == null || x.getMaDangKy() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public DangKyHocModel modify(int maCoSo, int ma, Predicate<DangKyHocModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(DangKyHocModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaHocVien() <= 0) throw new IllegalArgumentException("MaHocVien không hợp lệ");
        if (x.getMaKhoaHoc() != null && x.getMaKhoaHoc() <= 0) throw new IllegalArgumentException("MaKhoaHoc không hợp lệ");
        if (x.getMaLop() != null && x.getMaLop() <= 0) throw new IllegalArgumentException("MaLop không hợp lệ");
        if (x.getMaDangKyGoc() != null && x.getMaDangKyGoc() <= 0) throw new IllegalArgumentException("MaDangKyGoc không hợp lệ");
        if (x.getMaDangKyTruoc() != null && x.getMaDangKyTruoc() <= 0) throw new IllegalArgumentException("MaDangKyTruoc không hợp lệ");
        if (x.getNgayDangKy() == null) throw new IllegalArgumentException("NgayDangKy không hợp lệ");
        if (x.getHocPhiThoaThuan() != null && x.getHocPhiThoaThuan().signum() < 0) throw new IllegalArgumentException("HocPhiThoaThuan không hợp lệ");
        if (x.getSoTienGiam() == null) throw new IllegalArgumentException("SoTienGiam không hợp lệ");
        if (x.getSoTienGiam() != null && x.getSoTienGiam().signum() < 0) throw new IllegalArgumentException("SoTienGiam không hợp lệ");
        if (x.getMaNguoiDangKy() <= 0) throw new IllegalArgumentException("MaNguoiDangKy không hợp lệ");
        if (x.getTrangThai() == null || x.getTrangThai().trim().isEmpty()) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && x.getTrangThai().length() > 25) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && !Arrays.asList("CHO_XEP_LOP","DANG_HOC","HOAN_THANH","DA_CHUYEN","TAM_NGHI","NGHI_HOC","HUY").contains(x.getTrangThai())) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getGhiChu() != null && x.getGhiChu().length() > 500) throw new IllegalArgumentException("GhiChu không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
