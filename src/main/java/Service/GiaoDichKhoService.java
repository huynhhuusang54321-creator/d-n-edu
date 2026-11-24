package Service;

import Model.GiaoDichKhoModel;
import Repository.GiaoDichKhoRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class GiaoDichKhoService {
    private final GiaoDichKhoRepository repo = new GiaoDichKhoRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<GiaoDichKhoModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<GiaoDichKhoModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public GiaoDichKhoModel getByMaGiaoDichKho(int maCoSo, int ma) { return repo.getByMaGiaoDichKho(maCoSo, ma); }
    public List<GiaoDichKhoModel> getByMaHangHoa(int maCoSo, int value) { return repo.getByMaHangHoa(maCoSo, value); }
    public List<GiaoDichKhoModel> getByMaHocVien(int maCoSo, int value) { return repo.getByMaHocVien(maCoSo, value); }
    public List<GiaoDichKhoModel> getByMaGiaoDichTaiChinh(int maCoSo, int value) { return repo.getByMaGiaoDichTaiChinh(maCoSo, value); }
    public List<GiaoDichKhoModel> getByMaNhanSu(int maCoSo, int value) { return repo.getByMaNhanSu(maCoSo, value); }

    public List<GiaoDichKhoModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<GiaoDichKhoModel> result = new ArrayList<>();
        for (GiaoDichKhoModel x : selectAll(maCoSo)) {
            if (trangThai != null && !trangThai.trim().isEmpty() && !"TAT_CA".equals(trangThai) && !trangThai.equalsIgnoreCase(String.valueOf(x.getTrangThai()))) continue;
            if (tk.isEmpty() || normalize(x.getMaNhomChungTu()).contains(tk) || normalize(x.getLoaiGiaoDich()).contains(tk) || normalize(x.getTrangThai()).contains(tk) || normalize(x.getGhiChu()).contains(tk) || String.valueOf(x.getMaGiaoDichKho()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, GiaoDichKhoModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }
    public boolean update(int maCoSo, GiaoDichKhoModel x) {
        if (x == null || x.getMaGiaoDichKho() <= 0 || maCoSo <= 0 || x.getMaCoSo() != maCoSo) return false;
        validate(x);
        return repo.update(x);
    }
    public boolean delete(int maCoSo, int ma) { return repo.delete(maCoSo, ma); }
    public GiaoDichKhoModel modify(int maCoSo, int ma, Predicate<GiaoDichKhoModel> change) {
        if (change == null) return null;
        return repo.modify(maCoSo, ma, x -> { boolean changed = change.test(x); if (changed) validate(x); return changed; });
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(GiaoDichKhoModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaNhomChungTu() == null || x.getMaNhomChungTu().trim().isEmpty()) throw new IllegalArgumentException("MaNhomChungTu không hợp lệ");
        if (x.getMaNhomChungTu() != null && x.getMaNhomChungTu().length() > 50) throw new IllegalArgumentException("MaNhomChungTu không hợp lệ");
        if (x.getMaHangHoa() <= 0) throw new IllegalArgumentException("MaHangHoa không hợp lệ");
        if (x.getMaHocVien() != null && x.getMaHocVien() <= 0) throw new IllegalArgumentException("MaHocVien không hợp lệ");
        if (x.getMaGiaoDichTaiChinh() != null && x.getMaGiaoDichTaiChinh() <= 0) throw new IllegalArgumentException("MaGiaoDichTaiChinh không hợp lệ");
        if (x.getLoaiGiaoDich() == null || x.getLoaiGiaoDich().trim().isEmpty()) throw new IllegalArgumentException("LoaiGiaoDich không hợp lệ");
        if (x.getLoaiGiaoDich() != null && x.getLoaiGiaoDich().length() > 15) throw new IllegalArgumentException("LoaiGiaoDich không hợp lệ");
        if (x.getLoaiGiaoDich() != null && !Arrays.asList("NHAP","XUAT","TON_DAU","KIEM_KE","DIEU_CHINH").contains(x.getLoaiGiaoDich())) throw new IllegalArgumentException("LoaiGiaoDich không hợp lệ");
        if (x.getSoLuong() == null) throw new IllegalArgumentException("SoLuong không hợp lệ");
        if (x.getTonThucTe() != null && x.getTonThucTe().signum() < 0) throw new IllegalArgumentException("TonThucTe không hợp lệ");
        if (x.getDonGia() == null) throw new IllegalArgumentException("DonGia không hợp lệ");
        if (x.getDonGia() != null && x.getDonGia().signum() < 0) throw new IllegalArgumentException("DonGia không hợp lệ");
        if (x.getNgayGiaoDich() == null) throw new IllegalArgumentException("NgayGiaoDich không hợp lệ");
        if (x.getMaNhanSu() <= 0) throw new IllegalArgumentException("MaNhanSu không hợp lệ");
        if (x.getTrangThai() == null || x.getTrangThai().trim().isEmpty()) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && x.getTrangThai().length() > 15) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getTrangThai() != null && !Arrays.asList("NHAP","XAC_NHAN","HUY").contains(x.getTrangThai())) throw new IllegalArgumentException("TrangThai không hợp lệ");
        if (x.getGhiChu() != null && x.getGhiChu().length() > 500) throw new IllegalArgumentException("GhiChu không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
