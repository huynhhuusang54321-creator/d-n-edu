package vn.vinabacus.model;

/** Phan loai giao dien, KHONG phai ban ghi lay tu SQL Server. */
public enum CoSo {
    TRUNG_TAM("Trung tâm"),
    MAM_NON("Mầm non");

    private final String ten;
    CoSo(String ten) { this.ten = ten; }
    public String ten() { return ten; }
    @Override public String toString() { return ten; }
}
