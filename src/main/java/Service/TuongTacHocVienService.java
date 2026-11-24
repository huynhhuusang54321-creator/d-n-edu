package Service;

import Model.TuongTacHocVienModel;
import Repository.TuongTacHocVienRepository;
import java.util.*;
import java.text.Normalizer;
import java.util.function.Predicate;

/** Service kiểm tra đầu vào và gọi Repository. */
public class TuongTacHocVienService {
    private final TuongTacHocVienRepository repo = new TuongTacHocVienRepository();

    // Cache được quản lý duy nhất tại Repository, không tạo cache riêng ở Service.
    public List<TuongTacHocVienModel> reload(int maCoSo) { return repo.reload(maCoSo); }
    public Repository.TableCache.Snapshot selectRaw(int maCoSo) { return repo.selectRaw(maCoSo); }
    public Repository.TableCache.Snapshot reloadRaw(int maCoSo) { return repo.reloadRaw(maCoSo); }
    public boolean isCacheLoaded(int maCoSo) { return repo.isCacheLoaded(maCoSo); }
    public void invalidateBranch(int maCoSo) { repo.invalidateBranch(maCoSo); }
    public void mapColumn(String oldName, String actualName) { repo.mapColumn(oldName, actualName); }

    // ==================== ĐỌC ====================
    public List<TuongTacHocVienModel> selectAll(int maCoSo) { return repo.getAll(maCoSo); }
    public TuongTacHocVienModel getByMaTuongTac(int maCoSo, int ma) { return repo.getByMaTuongTac(maCoSo, ma); }
    public List<TuongTacHocVienModel> getByMaHocVien(int maCoSo, int value) { return repo.getByMaHocVien(maCoSo, value); }
    public List<TuongTacHocVienModel> getByMaNhanSu(int maCoSo, int value) { return repo.getByMaNhanSu(maCoSo, value); }
    public List<TuongTacHocVienModel> getByMaKhoaHocMongMuon(int maCoSo, int value) { return repo.getByMaKhoaHocMongMuon(maCoSo, value); }

    public List<TuongTacHocVienModel> timKiem(int maCoSo, String tuKhoa, String trangThai) {
        String tk = normalize(tuKhoa);
        List<TuongTacHocVienModel> result = new ArrayList<>();
        for (TuongTacHocVienModel x : selectAll(maCoSo)) {
            
            if (tk.isEmpty() || normalize(x.getLoaiTuongTac()).contains(tk) || normalize(x.getThoiGianMongMuon()).contains(tk) || normalize(x.getNoiDung()).contains(tk) || normalize(x.getKetQua()).contains(tk) || normalize(x.getTrangThaiTruoc()).contains(tk) || normalize(x.getTrangThaiSau()).contains(tk) || String.valueOf(x.getMaTuongTac()).contains(tk)) result.add(x);
        }
        return result;
    }

    // ==================== GHI ====================
    public boolean insert(int maCoSo, TuongTacHocVienModel x) {
        if (x == null || maCoSo <= 0) return false;
        x.setMaCoSo(maCoSo);
        validate(x);
        return repo.insert(x);
    }

    public void Reset() { repo.resetCache(); }
    public void evict(int maCoSo, int ma) { repo.evict(maCoSo, ma); }
    public void validate(TuongTacHocVienModel x) {
        if (x == null) throw new IllegalArgumentException("Dữ liệu không được null");
        if (x.getMaCoSo() <= 0) throw new IllegalArgumentException("MaCoSo không hợp lệ");
        if (x.getMaHocVien() <= 0) throw new IllegalArgumentException("MaHocVien không hợp lệ");
        if (x.getMaNhanSu() <= 0) throw new IllegalArgumentException("MaNhanSu không hợp lệ");
        if (x.getLoaiTuongTac() == null || x.getLoaiTuongTac().trim().isEmpty()) throw new IllegalArgumentException("LoaiTuongTac không hợp lệ");
        if (x.getLoaiTuongTac() != null && x.getLoaiTuongTac().length() > 25) throw new IllegalArgumentException("LoaiTuongTac không hợp lệ");
        if (x.getLoaiTuongTac() != null && !Arrays.asList("TIEP_NHAN","TU_VAN","NGUYEN_VONG","DOI_TRANG_THAI","GHI_CHU").contains(x.getLoaiTuongTac())) throw new IllegalArgumentException("LoaiTuongTac không hợp lệ");
        if (x.getMaKhoaHocMongMuon() != null && x.getMaKhoaHocMongMuon() <= 0) throw new IllegalArgumentException("MaKhoaHocMongMuon không hợp lệ");
        if (x.getThoiGianMongMuon() != null && x.getThoiGianMongMuon().length() > 300) throw new IllegalArgumentException("ThoiGianMongMuon không hợp lệ");
        if (x.getNoiDung() == null || x.getNoiDung().trim().isEmpty()) throw new IllegalArgumentException("NoiDung không hợp lệ");
        if (x.getNoiDung() != null && x.getNoiDung().length() > 1000) throw new IllegalArgumentException("NoiDung không hợp lệ");
        if (x.getKetQua() != null && x.getKetQua().length() > 500) throw new IllegalArgumentException("KetQua không hợp lệ");
        if (x.getTrangThaiTruoc() != null && x.getTrangThaiTruoc().length() > 25) throw new IllegalArgumentException("TrangThaiTruoc không hợp lệ");
        if (x.getTrangThaiSau() != null && x.getTrangThaiSau().length() > 25) throw new IllegalArgumentException("TrangThaiSau không hợp lệ");
        if (x.getNgayTao() == null) throw new IllegalArgumentException("NgayTao không hợp lệ");
    }
    private static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
