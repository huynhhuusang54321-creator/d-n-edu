package vn.vinabacus.model;
import java.time.LocalDate;
/** Bảng dbo.XepLopMamNon. */
public record XepLopMamNon(int maXepLop, int maHocVien, int maCoSo, int maLop,
                            String tenHocVien, String tenLop, LocalDate tuNgay,
                            LocalDate denNgay, String trangThai, String lyDo) { }
