package vn.vinabacus.model;

/** Mon hoc thuoc mot co so. TrangThai trong SQL la BIT. */
public record MonHoc(int maMonHoc, int maCoSo, String tenMonHoc,
                     String ghiChu, boolean trangThai) {
}
