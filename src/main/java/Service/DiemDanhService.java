package Service;

import Model.DiemDanhModel;
import Repository.DiemDanhRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class DiemDanhService {
    private final DiemDanhRepository repo = new DiemDanhRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<DiemDanhModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<DiemDanhModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public DiemDanhModel getByMaDiemDanh(int maCoSo, int ma) { return repo.getByMaDiemDanh(maCoSo, ma); }
    public List<DiemDanhModel> getByMaHocVien(int maCoSo, int value) { return repo.getByMaHocVien(maCoSo, value); }
    public List<DiemDanhModel> getByMaBuoiHoc(int maCoSo, int value) { return repo.getByMaBuoiHoc(maCoSo, value); }
    public List<DiemDanhModel> getByMaDangKy(int maCoSo, int value) { return repo.getByMaDangKy(maCoSo, value); }
    public List<DiemDanhModel> getByMaDiemDanhNguon(int maCoSo, int value) { return repo.getByMaDiemDanhNguon(maCoSo, value); }
    public List<DiemDanhModel> getByMaNguoiCapNhat(int maCoSo, int value) { return repo.getByMaNguoiCapNhat(maCoSo, value); }

    public List<DiemDanhModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<DiemDanhModel> result = new ArrayList<>();
        for (DiemDanhModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getLoaiThamGia()).contains(tk) || normalize(x.getTrangThai()).contains(tk) || normalize(x.getGhiChu()).contains(tk) || String.valueOf(x.getMaDiemDanh()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, DiemDanhModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, DiemDanhModel x) {
        if (x == null || x.getMaDiemDanh() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public DiemDanhModel modify(int maCoSo, int ma, Predicate<DiemDanhModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(DiemDanhModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaHocVien() <= 0) throw new IllegalArgumentException("MaHocVien không hợp lệ");
        if (x.getMaBuoiHoc() <= 0) throw new IllegalArgumentException("MaBuoiHoc không hợp lệ");
        if (x.getMaDangKy() != null && x.getMaDangKy() <= 0) throw new IllegalArgumentException("MaDangKy không hợp lệ");
        if (x.getLoaiThamGia() == null || x.getLoaiThamGia().trim().isEmpty()) throw new IllegalArgumentException("LoaiThamGia không hợp lệ");
        if (x.getLoaiThamGia() != null && x.getLoaiThamGia().length() > 20) throw new IllegalArgumentException("LoaiThamGia không hợp lệ");
        if (x.getLoaiThamGia() != null && !Arrays.asList("CHINH_THUC","HOC_THU","HOC_BU","DOI_BUOI","BO_SUNG").contains(x.getLoaiThamGia())) throw new IllegalArgumentException("LoaiThamGia không hợp lệ");
        if (x.getTrangThai() == null || x.getTrangThai().trim().isEmpty()) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && x.getTrangThai().length() > 25) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && !Arrays.asList("CHUA_DIEM_DANH","CO_MAT","VANG_CO_PHEP","VANG_KHONG_PHEP","HUY").contains(x.getTrangThai())) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getMaDiemDanhNguon() != null && x.getMaDiemDanhNguon() <= 0) throw new IllegalArgumentException("MaDiemDanhNguon không hợp lệ");
        if (x.getMaNguoiCapNhat() <= 0) throw new IllegalArgumentException("MaNguoiCapNhat không hợp lệ");
        if (x.getNgayCapNhat() == null) throw new IllegalArgumentException("NgayCapNhat không hợp lệ");
        if (x.getGhiChu() != null && x.getGhiChu().length() > 500) throw new IllegalArgumentException("GhiChu không hợp lệ");
        if ("HOC_THU".equals(x.getLoaiThamGia()) != (x.getMaDangKy() == null)) throw new IllegalArgumentException("Dữ liệu DiemDanh không hợp lệ");
        if (x.getTinhBuoi() && ("HOC_THU".equals(x.getLoaiThamGia()) || Arrays.asList("CHUA_DIEM_DANH", "HUY").contains(x.getTrangThai()))) throw new IllegalArgumentException("Dữ liệu DiemDanh không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
