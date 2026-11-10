package vn.vinabacus.model;

import java.util.List;

/** Chỉ chứa dữ liệu giả lập cho bước thiết kế giao diện. */
public final class DuLieuMau {
    private DuLieuMau() {}
    public record SoLieu(int hocVien, int lop, int coMat, int vangCoPhep, int vangKhongPhep) {
        public int daDiemDanh() { return coMat + vangCoPhep + vangKhongPhep; }
        public double tiLe(int n) { return daDiemDanh() == 0 ? 0 : (n * 100.0 / daDiemDanh()); }
    }
    public record Lich(String gio, List<String> noiDung) {}
    public record Bang(String[] cot, Object[][] dong, String moTa) {}
}
