package Service;

import Model.GiaoDichTaiChinhModel;
import Repository.GiaoDichTaiChinhRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class GiaoDichTaiChinhService {
    private final GiaoDichTaiChinhRepository repo = new GiaoDichTaiChinhRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<GiaoDichTaiChinhModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<GiaoDichTaiChinhModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public GiaoDichTaiChinhModel getByMaGiaoDich(int maCoSo, int ma) { return repo.getByMaGiaoDich(maCoSo, ma); }
    public List<GiaoDichTaiChinhModel> getByMaHocVien(int maCoSo, int value) { return repo.getByMaHocVien(maCoSo, value); }
    public List<GiaoDichTaiChinhModel> getByMaDangKy(int maCoSo, int value) { return repo.getByMaDangKy(maCoSo, value); }
    public List<GiaoDichTaiChinhModel> getByMaGiaoDichNguon(int maCoSo, int value) { return repo.getByMaGiaoDichNguon(maCoSo, value); }
    public List<GiaoDichTaiChinhModel> getByMaLoaiPhi(int maCoSo, int value) { return repo.getByMaLoaiPhi(maCoSo, value); }
    public List<GiaoDichTaiChinhModel> getByMaHinhThuc(int maCoSo, int value) { return repo.getByMaHinhThuc(maCoSo, value); }
    public List<GiaoDichTaiChinhModel> getByMaNhanSu(int maCoSo, int value) { return repo.getByMaNhanSu(maCoSo, value); }

    public List<GiaoDichTaiChinhModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<GiaoDichTaiChinhModel> result = new ArrayList<>();
        for (GiaoDichTaiChinhModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getMaNhomChungTu()).contains(tk) || normalize(x.getLoaiGiaoDich()).contains(tk) || normalize(x.getTrangThai()).contains(tk) || normalize(x.getDienGiai()).contains(tk) || String.valueOf(x.getMaGiaoDich()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, GiaoDichTaiChinhModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, GiaoDichTaiChinhModel x) {
        if (x == null || x.getMaGiaoDich() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public GiaoDichTaiChinhModel modify(int maCoSo, int ma, Predicate<GiaoDichTaiChinhModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(GiaoDichTaiChinhModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaHocVien() != null && x.getMaHocVien() <= 0) throw new IllegalArgumentException("MaHocVien không hợp lệ");
        if (x.getMaDangKy() != null && x.getMaDangKy() <= 0) throw new IllegalArgumentException("MaDangKy không hợp lệ");
        if (x.getMaGiaoDichNguon() != null && x.getMaGiaoDichNguon() <= 0) throw new IllegalArgumentException("MaGiaoDichNguon không hợp lệ");
        if (x.getMaNhomChungTu() == null || x.getMaNhomChungTu().trim().isEmpty()) throw new IllegalArgumentException("MaNhomChungTu không hợp lệ");
        if (x.getMaNhomChungTu() != null && x.getMaNhomChungTu().length() > 50) throw new IllegalArgumentException("MaNhomChungTu không hợp lệ");
        if (x.getLoaiGiaoDich() == null || x.getLoaiGiaoDich().trim().isEmpty()) throw new IllegalArgumentException("LoaiGiaoDich không hợp lệ");
        if (x.getLoaiGiaoDich() != null && x.getLoaiGiaoDich().length() > 15) throw new IllegalArgumentException("LoaiGiaoDich không hợp lệ");
        if (x.getLoaiGiaoDich() != null && !Arrays.asList("PHAI_THU","THU","CHI","HOAN","DIEU_CHINH").contains(x.getLoaiGiaoDich())) throw new IllegalArgumentException("LoaiGiaoDich không hợp lệ");
        if (x.getMaLoaiPhi() <= 0) throw new IllegalArgumentException("MaLoaiPhi không hợp lệ");
        if (x.getMaHinhThuc() != null && x.getMaHinhThuc() <= 0) throw new IllegalArgumentException("MaHinhThuc không hợp lệ");
        if (x.getSoLuong() != null && x.getSoLuong().signum() < 0) throw new IllegalArgumentException("SoLuong không hợp lệ");
        if (x.getDonGia() != null && x.getDonGia().signum() < 0) throw new IllegalArgumentException("DonGia không hợp lệ");
        if (x.getSoTien() == null) throw new IllegalArgumentException("SoTien không hợp lệ");
        if (x.getNgayGiaoDich() == null) throw new IllegalArgumentException("NgayGiaoDich không hợp lệ");
        if (x.getMaNhanSu() <= 0) throw new IllegalArgumentException("MaNhanSu không hợp lệ");
        if (x.getTrangThai() == null || x.getTrangThai().trim().isEmpty()) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && x.getTrangThai().length() > 15) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && !Arrays.asList("NHAP","XAC_NHAN","HUY").contains(x.getTrangThai())) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getDienGiai() == null || x.getDienGiai().trim().isEmpty()) throw new IllegalArgumentException("DienGiai không hợp lệ");
        if (x.getDienGiai() != null && x.getDienGiai().length() > 500) throw new IllegalArgumentException("DienGiai không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
