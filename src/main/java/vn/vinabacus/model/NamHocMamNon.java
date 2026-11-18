package vn.vinabacus.model;
import java.time.LocalDate;
/** Bảng dbo.NamHoc. */
public record NamHocMamNon(int maNamHoc, int maCoSo, String tenNamHoc,
                           LocalDate tuNgay, LocalDate denNgay) {
    @Override public String toString() { return tenNamHoc; }
}
