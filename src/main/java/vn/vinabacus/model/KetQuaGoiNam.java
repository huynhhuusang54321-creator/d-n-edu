package vn.vinabacus.model;

/** Kết quả tính học phí gói năm; chỉ mang dữ liệu giữa các tầng. */
public record KetQuaGoiNam(int soKhoa, long tong, long giam) {
    public long phaiThu() { return tong - giam; }
}
