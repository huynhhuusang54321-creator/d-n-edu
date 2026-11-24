package vn.vinabacus.model;

/** Muc menu co ma duy nhat, de nhom ket noi module sau nay. */
public record MucMenu(String ma, String ten, String nhom, String bieuTuong) {
    public MucMenu {
        if (ma == null || ma.isBlank() || ten == null || ten.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã hoặc tên menu");
        }
    }
}
