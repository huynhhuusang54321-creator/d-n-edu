package vn.vinabacus.model;

/** Phòng học thuộc một cơ sở. */
public record PhongHoc(int maPhong, int maCoSo, String tenPhong, boolean trangThai) {
    @Override public String toString() { return tenPhong; }
}
