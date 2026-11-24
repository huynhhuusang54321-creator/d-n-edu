package Service;

import Model.HocVienModel;
import Repository.HocVienRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class HocVienService {
    private final HocVienRepository repo = new HocVienRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<HocVienModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<HocVienModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public HocVienModel getByMaHocVien(int maCoSo, int ma) { return repo.getByMaHocVien(maCoSo, ma); }
    public List<HocVienModel> getByMaNguonTiepNhan(int maCoSo, int value) { return repo.getByMaNguonTiepNhan(maCoSo, value); }
    public List<HocVienModel> getByMaNguoiTuVan(int maCoSo, int value) { return repo.getByMaNguoiTuVan(maCoSo, value); }
    public List<HocVienModel> getByMaNguoiTao(int maCoSo, int value) { return repo.getByMaNguoiTao(maCoSo, value); }

    public List<HocVienModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<HocVienModel> result = new ArrayList<>();
        for (HocVienModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getMaHoSo()).contains(tk) || normalize(x.getHoTen()).contains(tk) || normalize(x.getGioiTinh()).contains(tk) || normalize(x.getTenPhuHuynh()).contains(tk) || normalize(x.getSoDienThoai()).contains(tk) || normalize(x.getEmail()).contains(tk) || normalize(x.getDiaChi()).contains(tk) || normalize(x.getTrangThai()).contains(tk) || normalize(x.getGhiChu()).contains(tk) || String.valueOf(x.getMaHocVien()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, HocVienModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, HocVienModel x) {
        if (x == null || x.getMaHocVien() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public HocVienModel modify(int maCoSo, int ma, Predicate<HocVienModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(HocVienModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaHoSo() == null || x.getMaHoSo().trim().isEmpty()) throw new IllegalArgumentException("MaHoSo không hợp lệ");
        if (x.getMaHoSo() != null && x.getMaHoSo().length() > 30) throw new IllegalArgumentException("MaHoSo không hợp lệ");
        if (x.getHoTen() == null || x.getHoTen().trim().isEmpty()) throw new IllegalArgumentException("HoTen không hợp lệ");
        if (x.getHoTen() != null && x.getHoTen().length() > 150) throw new IllegalArgumentException("HoTen không hợp lệ");
        if (x.getGioiTinh() != null && x.getGioiTinh().length() > 10) throw new IllegalArgumentException("GioiTinh không hợp lệ");
        if (x.getGioiTinh() != null && !Arrays.asList("NAM","NU","KHAC").contains(x.getGioiTinh())) throw new IllegalArgumentException("GioiTinh không hợp lệ");
        if (x.getAnhDaiDien() != null && x.getAnhDaiDien().length() > 500) throw new IllegalArgumentException("AnhDaiDien không hợp lệ");
        if (x.getTenPhuHuynh() != null && x.getTenPhuHuynh().length() > 150) throw new IllegalArgumentException("TenPhuHuynh không hợp lệ");
        if (x.getSoDienThoai() != null && x.getSoDienThoai().length() > 20) throw new IllegalArgumentException("SoDienThoai không hợp lệ");
        if (x.getEmail() != null && x.getEmail().length() > 254) throw new IllegalArgumentException("Email không hợp lệ");
        if (x.getDiaChi() != null && x.getDiaChi().length() > 300) throw new IllegalArgumentException("DiaChi không hợp lệ");
        if (x.getMaNguonTiepNhan() != null && x.getMaNguonTiepNhan() <= 0) throw new IllegalArgumentException("MaNguonTiepNhan không hợp lệ");
        if (x.getMaNguoiTuVan() != null && x.getMaNguoiTuVan() <= 0) throw new IllegalArgumentException("MaNguoiTuVan không hợp lệ");
        if (x.getMaNguoiTao() <= 0) throw new IllegalArgumentException("MaNguoiTao không hợp lệ");
        if (x.getNgayTao() == null) throw new IllegalArgumentException("NgayTao không hợp lệ");
        if (x.getTrangThai() == null || x.getTrangThai().trim().isEmpty()) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && x.getTrangThai().length() > 25) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && !Arrays.asList("TIEP_NHAN","HOC_THU","CHO_XEP_LOP","CHINH_THUC","TAM_NGHI","NGHI_HOC","KHONG_DANG_KY").contains(x.getTrangThai())) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getGhiChu() != null && x.getGhiChu().length() > 1000) throw new IllegalArgumentException("GhiChu không hợp lệ");
        if ("CHINH_THUC".equals(x.getTrangThai()) && x.getNgayChinhThuc() == null) throw new IllegalArgumentException("Dữ liệu HocVien không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
