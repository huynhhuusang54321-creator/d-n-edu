package vn.vinabacus.model;

/** Danh muc phong hoc Trung tam (doc tu migration PhongHoc da duoc ap dung). */
public record PhongHoc(int maPhong, int maCoSo, String tenPhong,
                      int sucChua, String trangThai, String ghiChu) {
    /** Tuong thich cac ham tao cu chi co nam truong. */
    public PhongHoc(int maPhong, int maCoSo, String tenPhong, int sucChua, String trangThai) {
        this(maPhong, maCoSo, tenPhong, sucChua, trangThai, null);
    }
    @Override public String toString() {
        if (maPhong <= 0) return "-- Chọn phòng học --";
        String phu = "HOAT_DONG".equals(trangThai) ? "" : " [" + trangThai + "]";
        return tenPhong + " (" + sucChua + " chỗ)" + phu;
    }
}
