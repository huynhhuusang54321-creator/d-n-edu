package vn.vinabacus.model;
/** Bảng dbo.LopMamNon với tên năm học để hiển thị. */
public record LopMamNon(int maLop, int maNamHoc, int maCoSo,
                        String tenLop, boolean trangThai, String tenNamHoc,
                        String khoi, Integer siSoToiDa, int siSoHienTai) {
    @Override public String toString() { return tenLop + " (" + tenNamHoc + ")"; }
}
